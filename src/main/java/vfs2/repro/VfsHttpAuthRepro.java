package vfs2.repro;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.http4.Http4FileProvider;
import org.apache.commons.vfs2.provider.http5.Http5FileProvider;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reproduces VFS2 Http5 Basic auth failure: credentials use the same char[] as authData,
 * then UserAuthenticatorUtils.cleanup(authData) zeros it before the request.
 * Http4 works because it uses toString() (copy); Http5 does not.
 */
public final class VfsHttpAuthRepro {

    public static final String USER = "testuser";
    public static final String PASS = "testpass";
    public static final String EXPECTED_BODY = "hello-auth";

    public static String readViaVfs(FileSystemManager manager, FileSystemOptions opts, String url) throws Exception {
        FileObject file = manager.resolveFile(url, opts);
        try (InputStream in = file.getContent().getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static FileSystemOptions optionsWithAuth(String username, String password) {
        FileSystemOptions opts = new FileSystemOptions();
        StaticUserAuthenticator auth = new StaticUserAuthenticator(null, username, password);
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
        return opts;
    }

    /** FileSystemManager with Http4 only */
    public static FileSystemManager createHttp4Manager() throws Exception {
        DefaultFileSystemManager m = new DefaultFileSystemManager();
        m.addProvider("http", new Http4FileProvider());
        m.init();
        return m;
    }

    /** FileSystemManager with Http5 only */
    public static FileSystemManager createHttp5Manager() throws Exception {
        DefaultFileSystemManager m = new DefaultFileSystemManager();
        m.addProvider("http", new Http5FileProvider());
        m.init();
        return m;
    }

    public static void main(String[] args) throws Exception {
        BasicAuthHttpServer server = new BasicAuthHttpServer()
                .withBasicAuth(USER, PASS)
                .addPath("/data.txt", EXPECTED_BODY);
        server.start();
        try {
            String url = server.getBaseUrl() + "/data.txt";
            FileSystemOptions opts = optionsWithAuth(USER, PASS);

            String withHttp4 = readViaVfs(createHttp4Manager(), opts, url);
            String withHttp5 = readViaVfs(createHttp5Manager(), opts, url);

            System.out.println("Http4 result: " + (EXPECTED_BODY.equals(withHttp4) ? "OK" : "FAIL"));
            System.out.println("Http5 result: " + (EXPECTED_BODY.equals(withHttp5) ? "OK" : "FAIL"));
        } finally {
            server.stop();
        }
    }
}
