Apache Commons VFS2 issue

## Title

Http5FileProvider Basic authentication fails: password in credentials is wiped by UserAuthenticatorUtils.cleanup(authData)

## Description

We discovered authentication problem when migrating our application from deprecated Http4FileProvider  to Http5FileProvider.

**Our analysis of the problem:** In `Http5FileProvider.createHttpClientContext()`, the password is passed to `UsernamePasswordCredentials` as the **same `char[]`** reference returned from `UserAuthenticatorUtils.getData(authData, PASSWORD, ...)`. In `doCreateFileSystem()`, `UserAuthenticatorUtils.cleanup(authData)` is called in a `finally` block to clear sensitive data. That method zeros the character arrays inside `authData`. Because the credentials object holds a **reference** to the same array, the password in the credentials is also zeroed **before** any HTTP request is made. Http4 does not have this bug because it uses `UserAuthenticatorUtils.toString(getData(...))`, which creates a **new String** (a copy), so the credentials keep their own data.

## Steps to reproduce

1. Clone the minimal reproduction project: (https://github.com/VaishKumbhar/vfs2-issue)
2. Run: `mvn test`
3. Observe: test `http4WithBasicAuthSucceeds` passes; test `http5WithBasicAuthFailsBecausePasswordWipedByCleanup` fails with Http5 does **not** return the protected content (auth failed).

The project contains:
- A minimal HTTP server with Basic auth (JDK `HttpServer`, no extra deps).
- VFS2 `FileSystemManager` with either Http4 or Http5 provider.
- `FileSystemOptions` with `StaticUserAuthenticator` (username/password).
- Resolve a `http://...` URL and read content; with Http5 the password is already wiped so the request is sent without Authorization.

## Expected behavior

Http5FileProvider should authenticate successfully, same as Http4FileProvider.

## Actual behavior

Server returns 401.

## References

- Http5FileProvider.createHttpClientContext: https://github.com/apache/commons-vfs/blob/master/commons-vfs2/src/main/java/org/apache/commons/vfs2/provider/http5/Http5FileProvider.java
- Http4FileProvider.createHttpClientContext (correct pattern): https://github.com/apache/commons-vfs/blob/master/commons-vfs2/src/main/java/org/apache/commons/vfs2/provider/http4/Http4FileProvider.java
- doCreateFileSystem finally block: https://github.com/apache/commons-vfs/blob/master/commons-vfs2/src/main/java/org/apache/commons/vfs2/provider/http5/Http5FileProvider.java (cleanup(authData) in finally)

## Reproduction project

https://github.com/VaishKumbhar/vfs2-issue

Clone and run `mvn test` to reproduce.
