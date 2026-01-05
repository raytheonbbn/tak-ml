package com.bbn.takml_server.docs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "com.bbn.takml_server")
public class ExportOpenApi {

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(ExportOpenApi.class);
        app.setWebApplicationType(WebApplicationType.SERVLET);
        app.setAdditionalProfiles("docs");
        app.setDefaultProperties(Map.of(
                "spring.config.name", "application-docs",             // filename (no extension)
                "spring.config.location", "classpath:/application-docs.properties"
        ));

        try (ConfigurableApplicationContext ctx = app.run()) {

            Thread.sleep(200); // give springdoc time to start
            RestTemplate rest = new RestTemplate();

            String yaml = rest.getForObject("http://localhost:8234/v3/api-docs.yaml", String.class);

            File out = new File("../schema.yaml");

            try (FileWriter fw = new FileWriter(out)) {
                fw.write(yaml);
            }

            System.out.println("OpenAPI written to: " + out.getAbsolutePath());
        }

        System.exit(0);
    }
}
