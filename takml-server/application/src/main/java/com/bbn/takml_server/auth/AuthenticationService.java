package com.bbn.takml_server.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collections;
import java.util.List;

public class AuthenticationService {
    private static final Logger logger = LogManager.getLogger(AuthenticationService.class);

    public static Authentication getAuthentication(String apiKeyParamName, String validApiKey, boolean apiKeyEnabled,
                                                   HttpServletRequest request) throws AuthenticationException {
        // Log all headers for debugging
        if (request.getHeaderNames() != null) {
            List<String> list = Collections.list(request.getHeaderNames());
        }

        // Log the API key validation flag
        logger.info("API Key validation enabled: {}", apiKeyEnabled);

        // Skip API key validation if disabled
        if (!apiKeyEnabled) {
            logger.info("API Key validation is disabled.");
            return new ApiKeyAuthentication(null, AuthorityUtils.NO_AUTHORITIES);
        }

        // Extract and log the API key
        String apiKey = request.getHeader(apiKeyParamName);
        logger.info("API Key from request: {}", apiKey);

        // Validate the API key
        if (apiKey == null || !apiKey.equals(validApiKey)) {
            logger.error("Invalid API Key provided: {}", apiKey);
            throw new BadCredentialsException("Invalid API Key");
        }

        return new ApiKeyAuthentication(apiKey, AuthorityUtils.NO_AUTHORITIES);
    }
}