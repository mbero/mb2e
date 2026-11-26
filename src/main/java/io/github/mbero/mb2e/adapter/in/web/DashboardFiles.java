package io.github.mbero.mb2e.adapter.in.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Serves the built dashboard UI from one folder under {@code /dashboard/}. A path with no file extension
 * is a page of the single-page app and gets {@code index.html}; a missing asset or a path outside the
 * folder gets 404. Hashed bundles are cached for a year, {@code index.html} never.
 */
public final class DashboardFiles {

    static final String PREFIX = "/dashboard/";
    private static final Pattern HASHED = Pattern.compile(".*-[A-Z0-9]{8}\\.(js|css)$");
    private static final CacheControl NO_CACHE = CacheControl.noCache();

    private final Path root;

    public DashboardFiles(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /** Returns true when the folder holds a built UI. */
    public boolean present() {
        return Files.isRegularFile(root.resolve("index.html"));
    }

    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .GET("/dashboard", request -> ServerResponse.permanentRedirect(java.net.URI.create(PREFIX)).build())
                .GET(PREFIX + "**", this::serve)
                .build();
    }

    ServerResponse serve(ServerRequest request) {
        String relative = request.path().substring(PREFIX.length());
        if (relative.contains("\0") || relative.contains("\\")) {
            return ServerResponse.notFound().build();
        }
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) {
            return ServerResponse.notFound().build();
        }
        if (Files.isRegularFile(file)) {
            return Files.isReadable(file) ? send(file, cacheFor(file)) : ServerResponse.notFound().build();
        }
        String name = file.getFileName() == null ? "" : file.getFileName().toString();
        Path index = root.resolve("index.html");
        if (!name.contains(".") && Files.isRegularFile(index) && Files.isReadable(index)) {
            return send(index, NO_CACHE);
        }
        return ServerResponse.notFound().build();
    }

    private static CacheControl cacheFor(Path file) {
        String name = file.getFileName().toString();
        if (name.equals("index.html")) {
            return NO_CACHE;
        }
        return HASHED.matcher(name).matches()
                ? CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable()
                : CacheControl.maxAge(java.time.Duration.ofHours(1));
    }

    private static ServerResponse send(Path file, CacheControl cache) {
        FileSystemResource resource = new FileSystemResource(file);
        MediaType type = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ServerResponse.ok().contentType(type).cacheControl(cache).body(resource);
    }
}
