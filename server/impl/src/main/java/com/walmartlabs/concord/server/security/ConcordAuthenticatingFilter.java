package com.walmartlabs.concord.server.security;

import com.google.common.base.Throwables;
import com.walmartlabs.concord.server.cfg.SecretStoreConfiguration;
import com.walmartlabs.concord.server.security.apikey.ApiKey;
import com.walmartlabs.concord.server.security.apikey.ApiKeyDao;
import com.walmartlabs.concord.server.security.secret.SecretUtils;
import com.walmartlabs.concord.server.security.sessionkey.SessionKey;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.UUID;

public class ConcordAuthenticatingFilter extends AuthenticatingFilter {

    private static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;
    private static final String SESSION_TOKEN_HEADER = "X-Concord-SessionToken";
    private static final String BASIC_AUTH_PREFIX = "Basic ";

    /**
     * List of URLs which do not require authentication or authorization.
     */
    private static final String[] ANON_URLS = {
            "/api/v1/server/ping",
            "/api/v1/server/version",
            "/api/service/console/logout"};

    /**
     * List of URLs which enforces use of basic authentication.
     */
    private static final String[] FORCE_BASIC_AUTH_URLS = {
            "/forms/.*",
            "/api/service/process_portal/.*",
            "/jolokia/.*"
    };

    private final ApiKeyDao apiKeyDao;

    private final SecretStoreConfiguration secretCfg;

    @Inject
    public ConcordAuthenticatingFilter(ApiKeyDao apiKeyDao, SecretStoreConfiguration secretCfg) {
        this.apiKeyDao = apiKeyDao;
        this.secretCfg = secretCfg;
    }

    @Override
    public boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        HttpServletRequest r = WebUtils.toHttp(request);
        String p = r.getRequestURI();
        for (String s : ANON_URLS) {
            if (p.matches(s)) {
                return true;
            }
        }

        return super.onPreHandle(request, response, mappedValue);
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = WebUtils.toHttp(request);
        String h = req.getHeader(AUTHORIZATION_HEADER);
        if (h != null) {
            return createFromAuthHeader(h, request);
        }

        h = req.getHeader(SESSION_TOKEN_HEADER);
        if (h != null) {
            return createFromSessionHeader(h, request);
        }

        return new UsernamePasswordToken();
    }

    private AuthenticationToken createFromAuthHeader(String h, ServletRequest request) {
        if (h.startsWith(BASIC_AUTH_PREFIX)) {
            // create sessions if users are using username/password auth
            request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.TRUE);
            return parseBasicAuth(h);
        } else {
            // disable session creation for api token users
            request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);

            UUID userId = apiKeyDao.findUserId(h);
            if (userId == null) {
                return new UsernamePasswordToken();
            }

            return new ApiKey(userId, h);
        }
    }

    private AuthenticationToken createFromSessionHeader(String h, ServletRequest request) {
        request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);
        return new SessionKey(decryptSessionKey(h));
    }

    private UUID decryptSessionKey(String h) {
        byte[] salt = secretCfg.getSecretStoreSalt();
        byte[] pwd = secretCfg.getServerPwd();

        try {
            byte[] ab = SecretUtils.decrypt(Base64.getDecoder().decode(h), pwd, salt);
            return UUID.fromString(new String(ab));
        } catch (GeneralSecurityException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        boolean loggedId = executeLogin(request, response);

        if (!loggedId) {
            HttpServletResponse resp = WebUtils.toHttp(response);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            forceBasicAuthIfNeeded(request, response);
        }

        return loggedId;
    }

    private static void forceBasicAuthIfNeeded(ServletRequest request, ServletResponse response) {
        // send "WWW-Authenticate: Basic", but only for specific requests w/o
        // authentication header or with basic authentication

        HttpServletRequest req = WebUtils.toHttp(request);
        HttpServletResponse resp = WebUtils.toHttp(response);

        String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.contains("Basic")) {
            String p = req.getRequestURI();
            for (String s : FORCE_BASIC_AUTH_URLS) {
                if (p.matches(s)) {
                    resp.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic");
                    break;
                }
            }
        }
    }

    private static UsernamePasswordToken parseBasicAuth(String s) {
        s = s.substring(BASIC_AUTH_PREFIX.length());
        s = new String(Base64.getDecoder().decode(s));

        int idx = s.indexOf(":");
        if (idx < 0 || idx + 1 >= s.length()) {
            throw new IllegalArgumentException("Invalid basic auth header");
        }

        String username = s.substring(0, idx);
        String password = s.substring(idx + 1, s.length());

        return new UsernamePasswordToken(username, password);
    }
}
