package vfs2.repro;

import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Reproduces Apache Commons VFS2 issue: Http5FileProvider Basic auth fails because
 * credentials hold the same char[] as authData; UserAuthenticatorUtils.cleanup(authData)
 * zeros that array before the HTTP request. Http4 works (uses toString = copy).
 */
class VfsHttp5AuthTest {

    private BasicAuthHttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new BasicAuthHttpServer()
                .withBasicAuth(VfsHttpAuthRepro.USER, VfsHttpAuthRepro.PASS)
                .addPath("/data.txt", VfsHttpAuthRepro.EXPECTED_BODY);
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop();
    }

    private String url() {
        return server.getBaseUrl() + "/data.txt";
    }

    @Test
    void http4WithBasicAuthSucceeds() throws Exception {
        FileSystemManager manager = VfsHttpAuthRepro.createHttp4Manager();
        FileSystemOptions opts = VfsHttpAuthRepro.optionsWithAuth(VfsHttpAuthRepro.USER, VfsHttpAuthRepro.PASS);
        String body = VfsHttpAuthRepro.readViaVfs(manager, opts, url());
        assertEquals(VfsHttpAuthRepro.EXPECTED_BODY, body);
        System.out.println("==============================================================================");
        System.out.println("Http4 result: " + (VfsHttpAuthRepro.EXPECTED_BODY.equals(body) ? "OK" : "FAIL") + " (body: " + body + ")");
        System.out.println("==============================================================================");
    }

    @Test
    void http5WithBasicAuthFailsBecausePasswordWipedByCleanup() throws Exception {
        FileSystemManager manager = VfsHttpAuthRepro.createHttp5Manager();
        FileSystemOptions opts = VfsHttpAuthRepro.optionsWithAuth(VfsHttpAuthRepro.USER, VfsHttpAuthRepro.PASS);
        String body = VfsHttpAuthRepro.readViaVfs(manager, opts, url());
        assertEquals(VfsHttpAuthRepro.EXPECTED_BODY, body);
        System.out.println("==============================================================================");
        System.out.println("Http5 result: " + (VfsHttpAuthRepro.EXPECTED_BODY.equals(body) ? "OK" : "FAIL") + " (body: " + body + ")");
        System.out.println("==============================================================================");  
    }
}
