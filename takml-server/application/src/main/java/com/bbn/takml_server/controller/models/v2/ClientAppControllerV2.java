package com.bbn.takml_server.controller.models.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ClientAppControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(ClientAppControllerV2.class);

    @RequestMapping(value = "/details/**")
    public String forwardUnmatchedPaths() {
        logger.info("Forwarding to index.html...");
        return "forward:/index.html";
    }
}
