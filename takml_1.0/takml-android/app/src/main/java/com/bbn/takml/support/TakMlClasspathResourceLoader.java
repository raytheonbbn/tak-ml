package com.bbn.takml.support;

import static com.bbn.tak.ml.TakMlConstants.*;
import static com.bbn.tak.ml.TakMlConstants.TAK_ML_MAX_MESSAGE_SIZE;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import io.moquette.broker.config.IResourceLoader;

public class TakMlClasspathResourceLoader implements IResourceLoader {

    private final String port;
    private final String host;
    private final String allow_anonymous;

    public TakMlClasspathResourceLoader(int in_port, String in_host, boolean in_allow_anonymous) {
        this.port = Integer.toString(in_port);
        this.host = in_host;
        this.allow_anonymous = Boolean.toString(in_allow_anonymous);
    }

    @Override
    public Reader loadDefaultResource() {
        return loadResource("");
    }

    @Override
    public Reader loadResource(String relativePath) {

        String config = "port " + this.port + "\n" +
                "host " + this.host + "\n" +
                "allow_anonymous " + this.allow_anonymous + "\n" +
                "netty.mqtt.message_size " + TAK_ML_MAX_MESSAGE_SIZE + "\n";

        return new InputStreamReader(new ByteArrayInputStream(config.getBytes()), UTF_8);
    }

    @Override
    public String getName() {
        return "classpath resource";
    }
}
