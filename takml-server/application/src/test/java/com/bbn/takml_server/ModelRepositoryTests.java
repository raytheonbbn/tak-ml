package com.bbn.takml_server;

import com.bbn.tak_sync_file_manager.TakFileManagerServer;
import com.bbn.tak_sync_file_manager.model.FileInfo;
import com.bbn.tak_sync_file_manager.request.FileCallback;
import com.bbn.takml_server.db.access_token.AccessTokenRepository;
import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.model_management.takfs.DefaultModelRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
@AutoConfigureMockMvc
public class ModelRepositoryTests extends BaseTest {
    private static final Logger logger = LogManager.getLogger(ModelRepositoryTests.class);

    @Autowired
    private DefaultModelRepository defaultModelRepository;

    @BeforeEach
    public void before() {

    }

    private void testInitialization(String propertyToSet, String value){
        Object previous = ReflectionTestUtils.getField(defaultModelRepository, propertyToSet);
        ReflectionTestUtils.setField(defaultModelRepository, propertyToSet, value);
        Assertions.assertThrows(TakmlInitializationException.class,
                () -> defaultModelRepository.initialize());
        ReflectionTestUtils.setField(defaultModelRepository, propertyToSet, previous);
    }

    @Test
    public void testInitialization() {
        testInitialization("takIpAddr", null);
        testInitialization("takPort", null);
        testInitialization("takClientStorePath", null);
        testInitialization("takClientStorePath", "src/test/not_exists");
        testInitialization("takClientStorePassword", null);
        testInitialization("takTrustStorePath", null);
        testInitialization("takTrustStorePath", "src/test/not_exists");
        testInitialization("takTrustStorePassword", null);
        testInitialization("takfsMissionName", null);
        testInitialization("takserverEnterpriseSyncLimitSize", null);
    }
}
