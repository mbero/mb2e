package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A signed webhook to a commit in a bare target repository: the real server context, real git, and two bare
 * repositories reached by {@code file://} URLs. The tests share one running server and run in order.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerModeEndToEndTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String SUBJECT = "chore(content): refresh Knowledge Hub";
    private static final Pattern FINISHED = Pattern.compile("\"finishedAt\":\"([^\"]+)\"");
    private static final Pattern OUTCOME = Pattern.compile("\"outcome\":\"([a-z_]+)\"");

    @TempDir
    static Path temp;

    private static Path sourceBare;
    private static Path targetBare;
    private static Path sourceWork;
    private static int port;
    private static ConfigurableApplicationContext context;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String lastFinished = "";

    private static String git(Path in, String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-c", "user.name=seed", "-c", "user.email=s@example.com",
                "-c", "commit.gpgsign=false", "-c", "core.quotepath=off"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(in.toFile()).redirectErrorStream(true).start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(String.join(" ", arguments) + ": " + out).isZero();
        return out.trim();
    }

    private static String sourceCommit(String message, String file, String content) throws Exception {
        Files.writeString(sourceWork.resolve(file), content, StandardCharsets.UTF_8);
        git(sourceWork, "add", "-A");
        git(sourceWork, "commit", "-q", "-m", message);
        git(sourceWork, "push", "-q", "origin", "HEAD:refs/heads/main");
        return git(sourceWork, "rev-parse", "HEAD");
    }

    private static String note(String title, boolean tagged) {
        return "---\ntags: [" + (tagged ? "blog-ready" : "private") + "]\n---\n# " + title + "\n\nBody of " + title + ".\n";
    }

    @BeforeAll
    static void startServer() throws Exception {
        sourceBare = temp.resolve("source.git");
        targetBare = temp.resolve("target.git");
        git(temp, "init", "-q", "--bare", "-b", "main", sourceBare.toString());
        git(temp, "init", "-q", "--bare", "-b", "main", targetBare.toString());
        sourceWork = temp.resolve("source-work");
        git(temp, "clone", "-q", sourceBare.toUri().toString(), sourceWork.toString());
        Files.writeString(sourceWork.resolve("about.md"), note("About", true), StandardCharsets.UTF_8);
        git(sourceWork, "add", "-A");
        git(sourceWork, "commit", "-q", "-m", "first note");
        git(sourceWork, "push", "-q", "origin", "HEAD:refs/heads/main");
        Path targetWork = temp.resolve("target-work");
        git(temp, "clone", "-q", targetBare.toUri().toString(), targetWork.toString());
        Files.writeString(targetWork.resolve("CNAME"), "blog.example.com\n");
        git(targetWork, "add", "-A");
        git(targetWork, "commit", "-q", "-m", "custom domain");
        git(targetWork, "push", "-q", "origin", "HEAD:refs/heads/main");

        Map<String, String> env = new HashMap<>();
        env.put("MB2E_SOURCE_REPO_URL", "https://github.com/me/notes");
        env.put("MB2E_TARGET_REPO_URL", "https://github.com/me/me.github.io");
        env.put("MB2E_WEBHOOK_SECRET", SECRET);
        env.put("MB2E_GITHUB_TOKEN", "unused-for-file-urls");
        env.put("MB2E_GIT_USER_NAME", "Mb2e Bot");
        env.put("MB2E_GIT_USER_EMAIL", "bot@example.com");
        env.put("MB2E_WORKSPACE", temp.resolve("workspace").toString());
        env.put("MB2E_TEMPLATE_PATH", Path.of("src/main/resources/template/mbs-blog").toAbsolutePath().toString());
        env.put("MB2E_SYNC_ON_START", "false");
        Map<String, Object> properties = new HashMap<>(ServerEnvironment.properties(env, path -> null));
        // ServerEnvironment refuses a file URL, so the test sets the two URLs after the validation; the
        // repository name that webhooks carry still comes from the https URL above.
        properties.put("mb2e.server.source.url", sourceBare.toUri().toString());
        properties.put("mb2e.server.target.url", targetBare.toUri().toString());
        // The target is not a Pages repository by name, so the remote check is relaxed to a warning.
        properties.put("mb2e.force", true);
        try (ServerSocket free = new ServerSocket(0)) {
            port = free.getLocalPort();
        }
        properties.put("server.port", port);
        context = new SpringApplicationBuilder(Mb2eApplication.class)
                .environment(Mb2eEnvironment.forServer(null, properties)).bannerMode(Banner.Mode.OFF).run();
    }

    @AfterAll
    static void stopServer() {
        if (context != null) {
            context.close();
        }
    }

    private static String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private static int push(String after) throws Exception {
        String body = "{\"ref\":\"refs/heads/main\",\"after\":\"" + after + "\",\"deleted\":false,"
                + "\"repository\":{\"full_name\":\"me/notes\"}}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/webhook/github"))
                .header("X-GitHub-Event", "push").header("X-GitHub-Delivery", "d-" + after)
                .header("X-Hub-Signature-256", sign(body)).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private static String health() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/healthz")).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    /** Waits until a deployment newer than the last seen one has finished, and returns its outcome. */
    private static String awaitNewDeployment() throws Exception {
        for (int i = 0; i < 300; i++) {
            String body = health();
            Matcher finished = FINISHED.matcher(body);
            Matcher outcome = OUTCOME.matcher(body);
            if (finished.find() && outcome.find() && !finished.group(1).equals(lastFinished)) {
                lastFinished = finished.group(1);
                return outcome.group(1);
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No deployment finished within 30 s. Health: " + health());
    }

    private static int targetCommits() throws Exception {
        return Integer.parseInt(git(targetBare, "rev-list", "--count", "main"));
    }

    private static String targetFile(String path) throws Exception {
        return git(targetBare, "show", "main:" + path);
    }

    @Test
    @Order(0)
    void theRealServerAnswersErrorsWithoutAStackTrace() throws Exception {
        URI webhook = URI.create("http://127.0.0.1:" + port + "/webhook/github");
        List<HttpResponse<String>> answers = List.of(
                HTTP.send(HttpRequest.newBuilder(webhook).build(), HttpResponse.BodyHandlers.ofString()),
                HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/nothing")).build(),
                        HttpResponse.BodyHandlers.ofString()),
                HTTP.send(HttpRequest.newBuilder(webhook).header("X-GitHub-Event", "push")
                        .POST(HttpRequest.BodyPublishers.ofString("{")).build(), HttpResponse.BodyHandlers.ofString()));
        assertThat(answers).extracting(HttpResponse::statusCode).containsExactly(404, 404, 401);
        assertThat(answers).allSatisfy(answer -> assertThat(answer.body()).doesNotContain("Exception", "at org.",
                "at io.", "trace"));
    }

    @Test
    @Order(1)
    void aSignedPushBuildsTheSiteAndCommitsItToTheTargetWithoutTouchingOtherFiles() throws Exception {
        String source = git(sourceWork, "rev-parse", "HEAD");
        assertThat(push(source)).isEqualTo(202);
        assertThat(awaitNewDeployment()).isEqualTo("deployed");
        assertThat(targetCommits()).isEqualTo(2);
        assertThat(git(targetBare, "ls-tree", "-r", "--name-only", "main")).contains("index.html", ".nojekyll",
                "assets/js/data.js", "CNAME");
        assertThat(targetFile("CNAME")).isEqualTo("blog.example.com");
        assertThat(git(targetBare, "log", "-1", "--format=%s", "main")).isEqualTo(SUBJECT);
        assertThat(git(targetBare, "log", "-1", "--format=%b", "main")).contains("Source commit: " + source);
        assertThat(git(targetBare, "log", "-1", "--format=%an", "main")).isEqualTo("Mb2e Bot");
    }

    @Test
    @Order(2)
    void aSecondPushWithNoSourceChangeIsUpToDateAndAddsNoCommit() throws Exception {
        int before = targetCommits();
        assertThat(push(git(sourceWork, "rev-parse", "HEAD"))).isEqualTo(202);
        assertThat(awaitNewDeployment()).isEqualTo("up_to_date");
        assertThat(targetCommits()).isEqualTo(before);
    }

    @Test
    @Order(3)
    void aNoteWithNonAsciiNameAppearsInTheSite() throws Exception {
        String after = sourceCommit("add a note with Polish letters", "zażółć.md",
                note("Zażółć gęślą jaźń", true));
        assertThat(push(after)).isEqualTo(202);
        assertThat(awaitNewDeployment()).isEqualTo("deployed");
        assertThat(targetFile("assets/js/data.js")).contains("zażółć");
    }

    @Test
    @Order(4)
    void tenParallelPushesGiveTenAcceptedAnswersAndAtMostTwoMoreCommits() throws Exception {
        String after = sourceCommit("change the about note", "about.md", note("About again", true));
        int before = targetCommits();
        List<CompletableFuture<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            calls.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return push(after);
                } catch (Exception failure) {
                    throw new IllegalStateException(failure);
                }
            }));
        }
        for (CompletableFuture<Integer> call : calls) {
            assertThat(call.get()).isEqualTo(202);
        }
        assertThat(awaitNewDeployment()).isIn("deployed", "up_to_date");
        Thread.sleep(3000);
        assertThat(targetCommits()).isBetween(before + 1, before + 2);
        assertThat(targetFile("assets/js/data.js")).contains("About again");
    }

    @Test
    @Order(5)
    void removingEveryTagIsRefusedAndTheTargetStaysAsItIs() throws Exception {
        sourceCommit("untag the about note", "about.md", note("About", false));
        String after = sourceCommit("untag the polish note", "zażółć.md", note("Zażółć", false));
        int before = targetCommits();
        String head = git(targetBare, "rev-parse", "main");
        assertThat(push(after)).isEqualTo(202);
        String outcome = awaitNewDeployment();
        // The burst of the previous test can leave one waiting run; the last outcome decides.
        if (!outcome.equals("refused")) {
            outcome = awaitNewDeployment();
        }
        assertThat(outcome).isEqualTo("refused");
        assertThat(targetCommits()).isEqualTo(before);
        assertThat(git(targetBare, "rev-parse", "main")).isEqualTo(head);
        assertThat(health()).contains("\"status\":\"up\"");
    }

    @Test
    @Order(6)
    void theDashboardApiListsEveryRunNewestFirstFromTheHistoryFileOnTheVolume() throws Exception {
        String deployments = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                + "/api/deployments?limit=500")).build(), HttpResponse.BodyHandlers.ofString()).body();
        Matcher outcomes = OUTCOME.matcher(deployments);
        java.util.List<String> seen = new java.util.ArrayList<>();
        while (outcomes.find()) {
            seen.add(outcomes.group(1));
        }
        assertThat(seen).first().isEqualTo("refused");
        assertThat(seen).contains("deployed", "up_to_date");
        assertThat(deployments).doesNotContain(temp.toString());
        String status = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/status"))
                .build(), HttpResponse.BodyHandlers.ofString()).body();
        assertThat(status).contains("\"maxAttempts\":3").contains("\"state\":\"");
        assertThat(temp.resolve("workspace").resolve("history.db")).isRegularFile();
    }
}
