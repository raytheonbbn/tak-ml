package com.bbn.takml_server.auth;

import com.bbn.takml_server.model_execution.ModelExecutionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

public class AuthenticationFilter extends GenericFilterBean {
    public static final String JAVAX_SERVLET_X509_CERTIFICATE_ATTR = "jakarta.servlet.request.X509Certificate";

    @Value("${server.api.key.param_name}")
    private String apiKeyHeader;

    @Value("${server.api.key}")
    private String validApiKey;

    @Value("${server.api.key.enabled}")
    private boolean apiKeyEnabled;

    @Value("${server.ssl.client-auth:none}")
    private String clientAuth;

    private static final Logger logger = LogManager.getLogger(AuthenticationFilter.class);

    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {
        // Debug all relevant properties
        logger.error("PROPERTY DEBUG ===================================");
        logger.error("server.api.key.enabled: {}", env.getProperty("server.api.key.enabled"));
        logger.error("server.api.key.param_name: {}", env.getProperty("server.api.key.param_name"));
        logger.error("server.api.key: {}", env.getProperty("server.api.key"));
        logger.error("server.ssl.client-auth: {}", env.getProperty("server.ssl.client-auth"));
        logger.error("=================================================");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 1. Check for client certificate first
            X509Certificate[] certificates = (X509Certificate[]) httpRequest.getAttribute(JAVAX_SERVLET_X509_CERTIFICATE_ATTR);
            boolean certValid = false;

            if (certificates != null && certificates.length > 0) {
                try {
                    // Validate certificate
                    String subjectDN = certificates[0].getSubjectX500Principal().getName();
                    logger.debug("Authenticated via client cert: {}", subjectDN);
                    SecurityContextHolder.getContext().setAuthentication(
                            new X509AuthenticationToken(certificates[0], subjectDN, Collections.emptyList())
                    );
                    certValid = true;
                    chain.doFilter(request, response);
                    return;
                } catch (Exception e) {
                    logger.warn("Invalid client certificate", e);
                }
            }

            // 2. If no valid cert, check API key if enabled
            if (apiKeyEnabled) {
                String apiKey = httpRequest.getHeader(apiKeyHeader);
                logger.info("api key: " + apiKey);
                if (validApiKey.equals(apiKey)) {
                    logger.debug("Authenticated via API key");
                    SecurityContextHolder.getContext().setAuthentication(
                            new ApiKeyAuthenticationToken(apiKey, Collections.emptyList())
                    );
                    chain.doFilter(request, response);
                    return;
                }
            }else{
                logger.info("api key enabled: " + apiKeyEnabled);
            }

            // 3. Reject if neither auth method succeeded
            logger.info("clientAuth: " + clientAuth);

            String errorMsg;
            if (apiKeyEnabled) {
                errorMsg = "Valid client certificate or API key required";
            } else if(clientAuth.contains("need")){
                errorMsg = "Client certificate authentication required";
            }else{
                chain.doFilter(request, response);
                return;
            }
            throw new AuthenticationCredentialsNotFoundException(errorMsg);

        } catch (AuthenticationException ex) {
            handleAuthenticationError(httpResponse, ex);
        } catch (Exception ex) {
            handleGenericError(httpResponse, ex);
        }
    }

    private void handleAuthenticationError(HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        logger.warn("Authentication failed: {}", ex.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(String.format(
                    "{\"error\":\"%s\",\"code\":\"UNAUTHORIZED\"}",
                    ex.getMessage()
            ));
        }
    }

    private void handleGenericError(HttpServletResponse response, Exception ex)
            throws IOException {
        logger.error("Authentication processing error", ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(
                    "{\"error\":\"Internal server error\",\"code\":\"INTERNAL_ERROR\"}"
            );
        }
    }

    // Custom Authentication Tokens (remain the same)
    private static class X509AuthenticationToken extends AbstractAuthenticationToken {
        private final X509Certificate certificate;
        private final String principal;

        public X509AuthenticationToken(X509Certificate certificate, String principal,
                                       Collection<? extends GrantedAuthority> authorities) {
            super(authorities);
            this.certificate = certificate;
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return certificate;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }

    private static class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
        private final String apiKey;

        public ApiKeyAuthenticationToken(String apiKey,
                                         Collection<? extends GrantedAuthority> authorities) {
            super(authorities);
            this.apiKey = apiKey;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return apiKey;
        }
    }
}