# UI stage: build the dashboard. Only the image needs Node; the Maven build and the jar stay free of it.
FROM node:22-bookworm-slim AS ui
ENV NG_CLI_ANALYTICS=false
WORKDIR /ui
COPY ui/package.json ui/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY ui ./
RUN npx ng build

# Build stage: compile and package the jar. Tests run in CI and locally, not in the image build.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Runtime stage: the jar, the template, git and a non-root user. No secret enters a layer.
FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends git ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
RUN useradd --uid 10001 --create-home --shell /usr/sbin/nologin mb2e \
    && mkdir -p /var/lib/mb2e /opt/mb2e \
    && chown mb2e /var/lib/mb2e
COPY --from=build /build/target/mb2e.jar /opt/mb2e/mb2e.jar
COPY src/main/resources/template/mbs-blog /opt/mb2e/template/mbs-blog
COPY --from=ui /ui/dist/mb2e-dashboard/browser /opt/mb2e/dashboard
RUN chmod -R a+rX /opt/mb2e/dashboard
USER mb2e
VOLUME /var/lib/mb2e
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fsS http://127.0.0.1:${MB2E_HTTP_PORT:-8080}/healthz || exit 1
# The exec form sends SIGTERM to the JVM, which then finishes the run in progress.
# The memory flags are arguments, not JAVA_TOOL_OPTIONS: that variable makes every java run print a notice on stderr.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/opt/mb2e/mb2e.jar"]
CMD ["serve"]
