package io.github.mbero.mb2e.adapter.in.web;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/** The HTTP endpoints of the server mode; any other method or path gets a plain 404. */
public final class WebRoutes {

    private WebRoutes() {
    }

    public static RouterFunction<ServerResponse> of(GithubWebhookController webhook, HealthController health) {
        return RouterFunctions.route()
                .POST("/webhook/github", request -> webhook.receive(request.servletRequest()))
                .GET("/healthz", request -> health.health())
                .build();
    }

    /** The read-only JSON API of the dashboard. */
    public static RouterFunction<ServerResponse> dashboard(DashboardApiController api) {
        return RouterFunctions.route()
                .GET("/api/status", request -> api.status())
                .GET("/api/deployments", api::deployments)
                .build();
    }
}
