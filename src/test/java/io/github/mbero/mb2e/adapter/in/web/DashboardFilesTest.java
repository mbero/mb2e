package io.github.mbero.mb2e.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardFilesTest {

    @TempDir
    Path temp;
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        Path ui = Files.createDirectories(temp.resolve("ui"));
        Files.writeString(ui.resolve("index.html"), "<mb-root></mb-root>");
        Files.writeString(ui.resolve("main-YN5QCACI.js"), "boot()");
        Files.writeString(ui.resolve("mb2e-logo.jpg"), "jpeg");
        Files.writeString(temp.resolve("secret.txt"), "outside");
        mvc = MockMvcBuilders.routerFunctions(new DashboardFiles(ui).routes()).build();
    }

    @Test
    void theRootServesIndexWithoutCaching() throws Exception {
        mvc.perform(get("/dashboard/")).andExpect(status().isOk())
                .andExpect(content().string("<mb-root></mb-root>"))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    void thePathWithoutTheSlashRedirectsToTheRoot() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isPermanentRedirect())
                .andExpect(header().string("Location", "/dashboard/"));
    }

    @Test
    void aPageOfTheAppGetsIndexSoADirectLinkWorks() throws Exception {
        mvc.perform(get("/dashboard/denied").param("login", "x")).andExpect(status().isOk())
                .andExpect(content().string("<mb-root></mb-root>"));
        mvc.perform(get("/dashboard/sign-in")).andExpect(status().isOk());
    }

    @Test
    void aHashedBundleIsCachedForAYearAndOtherFilesForAnHour() throws Exception {
        mvc.perform(get("/dashboard/main-YN5QCACI.js")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/javascript"))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
        mvc.perform(get("/dashboard/mb2e-logo.jpg")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/jpeg"))
                .andExpect(header().string("Cache-Control", "max-age=3600"));
    }

    @Test
    void aMissingAssetIsNotFoundAndNeverTheIndex() throws Exception {
        mvc.perform(get("/dashboard/main-AAAAAAAA.js")).andExpect(status().isNotFound());
    }

    @Test
    void aPathOutsideTheFolderIsNotFound() throws Exception {
        mvc.perform(get("/dashboard/%2e%2e/secret.txt")).andExpect(status().isNotFound());
        mvc.perform(get("/dashboard/..%2fsecret.txt")).andExpect(status().isNotFound());
        mvc.perform(get("/dashboard/a%5c..%5c..%5csecret.txt")).andExpect(status().isNotFound());
    }

    @Test
    void aFileTheServiceCannotReadIsNotFoundAndNoServerError() throws Exception {
        Path locked = temp.resolve("ui/locked.jpg");
        Files.writeString(locked, "jpeg");
        Files.setPosixFilePermissions(locked, java.util.Set.of());
        mvc.perform(get("/dashboard/locked.jpg")).andExpect(status().isNotFound());
    }

    @Test
    void withoutABuiltUiEveryPathIsNotFound() throws Exception {
        MockMvc empty = MockMvcBuilders.routerFunctions(new DashboardFiles(temp.resolve("none")).routes()).build();
        empty.perform(get("/dashboard/")).andExpect(status().isNotFound());
        empty.perform(get("/dashboard/sign-in")).andExpect(status().isNotFound());
    }
}
