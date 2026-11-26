package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The dashboard login against a fake GitHub, through the real server and its full filter chain: the
 * OAuth redirect, the code exchange, the owner check, the session, CSRF on logout and the open endpoints.
 */
class DashboardLoginEndToEndTest {

    private static final String CLIENT_ID = "client-id-1";
    private static final String CLIENT_SECRET = "client-secret-1";
    private static final String ACCESS_TOKEN = "gho_fake_access_token_1";

    @TempDir
    static Path temp;

    private static HttpServer github;
    private static volatile String githubLogin = "me";
    private static final List<String> tokenRequests = new ArrayList<>();
    private static ConfigurableApplicationContext context;
    private static int port;
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    /** A browser in a few lines: it keeps cookies by name and sends them back, whatever their flags. */
    private static final class Browser {
        private final Map<String, String> cookies = new LinkedHashMap<>();
        private final List<String> setCookies = new ArrayList<>();

        HttpResponse<String> get(String pathOrUrl) throws Exception {
            return send(HttpRequest.newBuilder(uri(pathOrUrl)).GET());
        }

        HttpResponse<String> post(String path, Map<String, String> headers) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.noBody());
            headers.forEach(builder::header);
            return send(builder);
        }

        private static URI uri(String pathOrUrl) {
            return URI.create(pathOrUrl.startsWith("http") ? pathOrUrl : "http://127.0.0.1:" + port + pathOrUrl);
        }

        private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
            if (!cookies.isEmpty()) {
                builder.header("Cookie", String.join("; ", cookies.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue()).toList()));
            }
            HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            for (String header : response.headers().allValues("Set-Cookie")) {
                setCookies.add(header);
                String pair = header.split(";", 2)[0];
                int equals = pair.indexOf('=');
                String value = pair.substring(equals + 1);
                if (value.isEmpty() || header.contains("Max-Age=0")) {
                    cookies.remove(pair.substring(0, equals));
                } else {
                    cookies.put(pair.substring(0, equals), value);
                }
            }
            return response;
        }

        /** Runs the whole login: mb2e sends the browser to GitHub, which sends it back with a code. */
        HttpResponse<String> signIn() throws Exception {
            HttpResponse<String> toGithub = get("/oauth2/authorization/github");
            assertThat(toGithub.statusCode()).isEqualTo(302);
            URI authorize = URI.create(toGithub.headers().firstValue("Location").orElseThrow());
            Map<String, String> query = query(authorize.getRawQuery());
            assertThat(query).containsEntry("client_id", CLIENT_ID).doesNotContainKey("scope");
            return get(query.get("redirect_uri") + "?code=fake-code&state="
                    + java.net.URLEncoder.encode(query.get("state"), StandardCharsets.UTF_8));
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            out.put(pair[0], pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        }
        return out;
    }

    @BeforeAll
    static void start() throws Exception {
        github = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        github.createContext("/login/oauth/access_token", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized (tokenRequests) {
                tokenRequests.add(exchange.getRequestHeaders().getFirst("Authorization") + " " + body);
            }
            answer(exchange, "{\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"bearer\",\"scope\":\"\"}");
        });
        github.createContext("/user", exchange -> {
            boolean bearer = ("Bearer " + ACCESS_TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization"));
            answer(exchange, bearer ? "{\"login\":\"" + githubLogin + "\",\"id\":7,\"email\":\"me@example.com\"}" : "{}");
        });
        github.start();
        String githubUrl = "http://127.0.0.1:" + github.getAddress().getPort();

        Map<String, String> env = new HashMap<>();
        env.put("MB2E_SOURCE_REPO_URL", "https://github.com/me/notes");
        env.put("MB2E_TARGET_REPO_URL", "https://github.com/me/me.github.io");
        env.put("MB2E_WEBHOOK_SECRET", "0123456789abcdef0123456789abcdef");
        env.put("MB2E_GITHUB_TOKEN", "unused");
        env.put("MB2E_GIT_USER_NAME", "Mb2e Bot");
        env.put("MB2E_GIT_USER_EMAIL", "bot@example.com");
        env.put("MB2E_WORKSPACE", temp.resolve("workspace").toString());
        env.put("MB2E_SYNC_ON_START", "false");
        env.put("MB2E_LOG_FILE", temp.resolve("mb2e.log").toString());
        env.put("MB2E_DASHBOARD_ALLOWED_LOGIN", "Me");
        env.put("MB2E_DASHBOARD_OAUTH_CLIENT_ID", CLIENT_ID);
        env.put("MB2E_DASHBOARD_OAUTH_CLIENT_SECRET", CLIENT_SECRET);
        Map<String, Object> properties = new HashMap<>(ServerEnvironment.properties(env, path -> null));
        properties.put("mb2e.server.dashboard.github-url", githubUrl);
        properties.put("mb2e.server.dashboard.github-api-url", githubUrl);
        try (ServerSocket free = new ServerSocket(0)) {
            port = free.getLocalPort();
        }
        properties.put("server.port", port);
        context = new SpringApplicationBuilder(Mb2eApplication.class)
                .environment(Mb2eEnvironment.forServer(null, properties)).bannerMode(Banner.Mode.OFF).run();
    }

    private static void answer(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @AfterAll
    static void stop() {
        if (context != null) {
            context.close();
        }
        if (github != null) {
            github.stop(0);
        }
    }

    @Test
    void withoutASessionEveryApiPathIsAShortUnauthorizedJsonAnswer() throws Exception {
        Browser browser = new Browser();
        for (String path : new String[] {"/api/me", "/api/status", "/api/deployments"}) {
            HttpResponse<String> response = browser.get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(401);
            assertThat(response.body()).as(path).isEqualTo("{\"error\":\"Sign in first.\"}");
        }
    }

    @Test
    void theAllowedLoginGetsASessionAndTheApiAnswersWithItsLogin() throws Exception {
        githubLogin = "me";
        Browser browser = new Browser();
        HttpResponse<String> back = browser.signIn();
        assertThat(back.statusCode()).isEqualTo(302);
        assertThat(back.headers().firstValue("Location").orElseThrow()).endsWith("/dashboard/");
        assertThat(browser.get("/api/me").body()).isEqualTo("{\"login\":\"me\"}");
        assertThat(browser.get("/api/status").statusCode()).isEqualTo(200);
        assertThat(browser.get("/api/deployments").body()).isEqualTo("[]");
        String session = browser.setCookies.stream().filter(c -> c.startsWith("mb2e_session=")).reduce((a, b) -> b)
                .orElseThrow();
        assertThat(session).contains("HttpOnly").contains("Secure").containsIgnoringCase("SameSite=Lax");
        synchronized (tokenRequests) {
            assertThat(tokenRequests).anySatisfy(request -> assertThat(request).startsWith("Basic ")
                    .contains("code=fake-code"));
        }
    }

    @Test
    void anotherGithubAccountIsSentToTheDeniedPageAndGetsNoSession() throws Exception {
        githubLogin = "intruder";
        Browser browser = new Browser();
        HttpResponse<String> back = browser.signIn();
        assertThat(back.headers().firstValue("Location").orElseThrow()).endsWith("/dashboard/denied?login=intruder");
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(401);
        githubLogin = "me";
    }

    @Test
    void logoutNeedsTheCsrfTokenAndEndsTheSession() throws Exception {
        githubLogin = "me";
        Browser browser = new Browser();
        browser.signIn();
        browser.get("/api/me");
        HttpResponse<String> noToken = browser.post("/api/logout", Map.of());
        assertThat(noToken.statusCode()).isEqualTo(403);
        assertThat(noToken.body()).isEqualTo("{\"error\":\"The request has no valid CSRF token.\"}");
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(200);
        String token = browser.cookies.get("XSRF-TOKEN");
        assertThat(token).isNotBlank();
        assertThat(browser.post("/api/logout", Map.of("X-XSRF-TOKEN", token)).statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    void theWebhookAndTheHealthProbeStayOutsideTheLogin() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.get("/healthz").statusCode()).isEqualTo(200);
        HttpResponse<String> unsigned = browser.post("/webhook/github", Map.of("X-GitHub-Event", "push"));
        assertThat(unsigned.statusCode()).isEqualTo(401);
        assertThat(unsigned.body()).doesNotContain("CSRF");
    }

    @Test
    void theLogNamesLoginEventsAndNeverATokenOrACode() throws Exception {
        githubLogin = "me";
        new Browser().signIn();
        githubLogin = "intruder";
        new Browser().signIn();
        githubLogin = "me";
        String log = Files.readString(temp.resolve("mb2e.log"), StandardCharsets.UTF_8);
        assertThat(log).contains("step=login outcome=success login=me")
                .contains("step=login outcome=denied login=intruder")
                .doesNotContain(ACCESS_TOKEN).doesNotContain("fake-code").doesNotContain(CLIENT_SECRET);
    }
}
