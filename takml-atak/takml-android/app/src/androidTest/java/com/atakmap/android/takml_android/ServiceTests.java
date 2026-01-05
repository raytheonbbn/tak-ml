package com.atakmap.android.takml_android;


import static com.atakmap.android.takml_android.ExampleMXPlugin.FAKE_EXAMPLE_MX_PLUGIN;
import static com.atakmap.android.takml_android.ExampleMXPlugin2.FAKE_EXAMPLE_MX_PLUGIN_2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.atakmap.android.takml_android.emm.TestTakml;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ServiceTests {
    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    private Context context;

    @Before
    public void before(){
        // Get the context of the application under test
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TestTakml.enableEmm = false;
    }

    /**
     * Tests Mx Plugin Service with a single model execution
     *
     * <ol>
     * <li>Create and bind to Example Mx Service</li>
     * <li>Create a fake TAK ML model</li>
     * <li>Execute model with extension</li>
     * <li>Verify model execution was invoked for Mx Plugin</li>
     * </ol>
     */
    @Test
    public void testMxPluginService() throws TimeoutException, RemoteException, IOException, InterruptedException {
        // Create the service Intent.
        Intent serviceIntent =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ExampleMxPluginService.class);

        // Bind the service and grab a reference to the binder.
        IBinder binder = mServiceRule.bindService(serviceIntent);

        IMxPluginService iMxPluginService = IMxPluginService.Stub.asInterface(binder);
        assertNotNull(iMxPluginService);

        byte[] testInput = "hi".getBytes(StandardCharsets.UTF_8);
        createDummyModelAndExecute(testInput, iMxPluginService, ".test");

        verify(FAKE_EXAMPLE_MX_PLUGIN, timeout(5000)).execute(testInput, null);
    }

    /**
     * Tests Mx Plugin Service with a single model execution
     *
     * <ol>
     * <li>Create and bind to Example Mx Service</li>
     * <li>Create a fake TAK ML model</li>
     * <li>Execute model with extension</li>
     * <li>Verify model execution was invoked for Mx Plugin</li>
     * </ol>
     */
    @Test
    public void testMxPluginService2() throws TimeoutException, RemoteException, IOException, InterruptedException {
        // Create the service Intent.
        Intent serviceIntent =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ExampleMxPluginService2.class);

        // Bind the service and grab a reference to the binder.
        IBinder binder = mServiceRule.bindService(serviceIntent);

        IMxPluginService iMxPluginService = IMxPluginService.Stub.asInterface(binder);
        assertNotNull(iMxPluginService);

        byte[] testInput = "hi".getBytes(StandardCharsets.UTF_8);
        createDummyModelAndExecute(testInput, iMxPluginService, ".test2");

        verify(FAKE_EXAMPLE_MX_PLUGIN_2, timeout(5000)).execute(testInput, null);
    }

    /**
     * Tests Mx Plugin Services with multiple models and executions
     *
     * <ol>
     * <li>Create and bind to Example Mx Service</li>
     * <li>Create 2 fake TAK ML models</li>
     * <li>Execute models with extension</li>
     * <li>Verify model executions were invoked for Mx Plugins</li>
     * </ol>
     */
    @Test
    public void testMxPluginServiceMultipleModels() throws TimeoutException, RemoteException, IOException, InterruptedException {
        // Create the service Intent.
        Intent serviceIntent =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ExampleMxPluginService.class);

        // Bind the service and grab a reference to the binder.
        IBinder binder = mServiceRule.bindService(serviceIntent);

        IMxPluginService iMxPluginService = IMxPluginService.Stub.asInterface(binder);
        assertNotNull(iMxPluginService);

        byte[] testInput = "hi".getBytes(StandardCharsets.UTF_8);
        createDummyModelAndExecute(testInput, iMxPluginService, ".test");

        byte[] testInput2 = "hi2".getBytes(StandardCharsets.UTF_8);
        createDummyModelAndExecute(testInput2, iMxPluginService, ".test");

        verify(FAKE_EXAMPLE_MX_PLUGIN, timeout(5000)).execute(testInput, null);
        verify(FAKE_EXAMPLE_MX_PLUGIN, timeout(5000)).execute(testInput2, null);
    }

    /**
     * Tests multiple Mx Plugin Services, each with own model execution
     *
     * <ol>
     * <li>Create and bind to Example Mx Service</li>
     * <li>Create and bind to Example Mx Service 2</li>
     * <li>Create fake TAK ML model and execute with first service</li>
     * <li>Create 2nd fake TAK ML model and execute with 2nd service</li>
     * <li>Verify model executions were invoked for Mx Plugins</li>
     * </ol>
     */
    @Test
    public void testMultipleMxPluginServices() throws IOException, RemoteException, TimeoutException {
        // Create the service Intent.
        Intent serviceIntent =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ExampleMxPluginService.class);

        // Bind the service and grab a reference to the binder.
        IBinder binder = mServiceRule.bindService(serviceIntent);

        IMxPluginService iMxPluginService = IMxPluginService.Stub.asInterface(binder);
        assertNotNull(iMxPluginService);

        // Create the service Intent.
        Intent serviceIntent2 =
                new Intent(ApplicationProvider.getApplicationContext(),
                        ExampleMxPluginService2.class);

        // Bind the service and grab a reference to the binder.
        IBinder binder2 = mServiceRule.bindService(serviceIntent2);

        IMxPluginService iMxPluginService2 = IMxPluginService.Stub.asInterface(binder2);
        assertNotNull(iMxPluginService2);

        byte[] testInput = "hi".getBytes(StandardCharsets.UTF_8);
        createDummyModelAndExecute(testInput, iMxPluginService, ".test");

        byte[] testInput2 = "hi2".getBytes(StandardCharsets.UTF_8);
        createDummyModelAndExecute(testInput2, iMxPluginService2, ".test2");

        verify(FAKE_EXAMPLE_MX_PLUGIN, timeout(5000)).execute(testInput, null);
        verify(FAKE_EXAMPLE_MX_PLUGIN_2, timeout(5000)).execute(testInput2, null);
    }

    private void createDummyModelAndExecute(byte[] testInput, IMxPluginService mxPluginService,
                                            String modelExtension)
            throws IOException, RemoteException {
        // Create a temporary file in the cache directory
        File tempFile = File.createTempFile("tempFile", ".txt", context.getCacheDir());

        String modelUUID = UUID.randomUUID().toString();
        mxPluginService.registerModel(modelUUID, "test", modelExtension,
                ModelTypeConstants.IMAGE_CLASSIFICATION, tempFile.getPath(),
                null, Collections.emptyList());
        mxPluginService.execute(UUID.randomUUID().toString() + "_" + "0", modelUUID, testInput);
    }


    /**
     * Tests running Takml Executor predictions with run as Android Service flag set to true. Each
     * execution should result in the applicable Mx Plugin started as an Android Service (if not already started),
     * and the model is executed through the Service (via an Mx Plugin instance).
     * <br>
     * There should be an Android Service started at most once per type of Mx Plugin. Each Mx Plugin Service
     * is associated with at least one Mx Plugin instance. Each instance of an Mx Plugin behaves just like it
     * does outside the Android Service -- one per Mx Plugin and TAK ML model pairing.
     * <br>
     * This tests creating 2 different Takml Executors that are applicable to two different Mx Plugins.
     * The executors are then ran against 6 different Takml Model inputs each, concurrently.
     * <br>
     * <ol>
     * <li>Create two TAK ML Models</li>
     * <li>Create two TAK ML Executors with runAsService flag set to true</li>
     * <li>Run 6 predictions per Takml Executor</li>
     * <li>Verify there were 6, and only 6, calls to each Mx Plugin</li>
     * </ol>
     */
    @Test
    public void takmlServiceTest() throws IOException, TakmlInitializationException, InterruptedException {
        // Create a temporary file in the cache directory
        File tempFile = File.createTempFile("tempFile", ".txt", context.getCacheDir());
        TestTakml takml = new TestTakml(context);

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder("test", Uri.parse(tempFile.toURI().toString()), ".test",
                ModelTypeConstants.IMAGE_CLASSIFICATION).build();
        takml.addTakmlModel(takmlModel);

        // Create another test TAK ML Model, applicable to ExampleMxPlugin2
        TakmlModel takmlModel2 = new TakmlModel.TakmlModelBuilder("test2", Uri.parse(tempFile.toURI().toString()), ".test2",
                ModelTypeConstants.GENERIC_RECOGNITION).build();
        takml.addTakmlModel(takmlModel2);

        // Create two Takml Executors
        TakmlExecutor takmlExecutor = takml.createExecutor(takmlModel, true);
        TakmlExecutor takmlExecutor2 = takml.createExecutor(takmlModel2, true);

        int numberOfExpectedMxPluginExecutions = 6;
        CountDownLatch countDownLatchExampleMXPlugin = new CountDownLatch(numberOfExpectedMxPluginExecutions);
        CountDownLatch countDownLatchExampleMXPlugin2 = new CountDownLatch(numberOfExpectedMxPluginExecutions);
        AtomicInteger counter = new AtomicInteger(0);
        for(int i=0; i<numberOfExpectedMxPluginExecutions; i++){
            int finalI = i;
            AsyncTask.execute(() -> executePrediction(countDownLatchExampleMXPlugin, "test" + finalI, takmlExecutor, counter));
            AsyncTask.execute(() -> executePrediction(countDownLatchExampleMXPlugin2, "test" + finalI, takmlExecutor2, counter));
        }

        assertTrue(countDownLatchExampleMXPlugin.await(10, TimeUnit.SECONDS));
        assertTrue(countDownLatchExampleMXPlugin2.await(10, TimeUnit.SECONDS));
        assertEquals(numberOfExpectedMxPluginExecutions * 2, counter.get());
    }

    /**
     * Same test as {@link #takmlServiceTest()}, except also test direct function calls Takml executors too.
     * This is the default mechanism of execution predictions in Takml (runAsService == false), where
     * Mx Plugins are invoked to run predictions with direct function calls.
     *
     * <li>Run same setup as other test</li>
     * <li>Create 2 additional Takml Executors without the runAsService flag (so set to false)</li>
     * <li>Check that there were 6 calls per Takml Executor</li>
     */
    @Test
    public void takmlServiceAndFunctionCallTest() throws IOException, TakmlInitializationException, InterruptedException {
        // Create a temporary file in the cache directory
        File tempFile = File.createTempFile("tempFile", ".txt", context.getCacheDir());
        TestTakml takml = new TestTakml(context);

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder("test", Uri.parse(tempFile.toURI().toString()), ".test",
                ModelTypeConstants.IMAGE_CLASSIFICATION).build();
        takml.addTakmlModel(takmlModel);

        // Create another test TAK ML Model, applicable to ExampleMxPlugin2
        TakmlModel takmlModel2 = new TakmlModel.TakmlModelBuilder("test2", Uri.parse(tempFile.toURI().toString()), ".test2",
                ModelTypeConstants.GENERIC_RECOGNITION).build();
        takml.addTakmlModel(takmlModel2);

        // Create two Takml Executors with runAsService flag set to true
        TakmlExecutor takmlExecutor = takml.createExecutor(takmlModel, true);
        TakmlExecutor takmlExecutor2 = takml.createExecutor(takmlModel2, true);

        // Create two Takml Executors with runAsService flag set to false (default)
        TakmlExecutor takmlExecutor3 = takml.createExecutor(takmlModel);
        TakmlExecutor takmlExecutor4 = takml.createExecutor(takmlModel2);

        int numberOfExpectedMxPluginExecutions = 6;
        CountDownLatch countDownLatchExampleMXPlugin = new CountDownLatch(numberOfExpectedMxPluginExecutions);
        CountDownLatch countDownLatchExampleMXPlugin2 = new CountDownLatch(numberOfExpectedMxPluginExecutions);
        CountDownLatch countDownLatchExampleMXPluginService = new CountDownLatch(numberOfExpectedMxPluginExecutions);
        CountDownLatch countDownLatchExampleMXPlugin2Service = new CountDownLatch(numberOfExpectedMxPluginExecutions);
        AtomicInteger counter = new AtomicInteger(0);
        for(int i=0; i<numberOfExpectedMxPluginExecutions; i++){
            int finalI = i;
            AsyncTask.execute(() -> executePrediction(countDownLatchExampleMXPlugin, "test" + finalI, takmlExecutor, counter));
            AsyncTask.execute(() -> executePrediction(countDownLatchExampleMXPlugin2, "test2" + finalI, takmlExecutor2, counter));
            AsyncTask.execute(() -> executePrediction(countDownLatchExampleMXPluginService, "test3" + finalI, takmlExecutor3, counter));
            AsyncTask.execute(() -> executePrediction(countDownLatchExampleMXPlugin2Service, "test4" + finalI, takmlExecutor4, counter));
        }

        // Check that the mx plugins were called exactly correct
        assertTrue(countDownLatchExampleMXPlugin.await(10, TimeUnit.SECONDS));
        assertTrue(countDownLatchExampleMXPlugin2.await(10, TimeUnit.SECONDS));
        assertTrue(countDownLatchExampleMXPluginService.await(10, TimeUnit.SECONDS));
        assertTrue(countDownLatchExampleMXPlugin2Service.await(10, TimeUnit.SECONDS));
        assertEquals(numberOfExpectedMxPluginExecutions * 4, counter.get());
    }


    /**
     * Tests running Takml Executor predictions with run as Android Service flag set to true. Each
     * execution should result in the applicable Mx Plugin started as an Android Service (if not already started),
     * and the model is executed through the Service (via an Mx Plugin instance).
     * <br>
     * There should be an Android Service started at most once per type of Mx Plugin. Each Mx Plugin Service
     * is associated with at least one Mx Plugin instance. Each instance of an Mx Plugin behaves just like it
     * does outside the Android Service -- one per Mx Plugin and TAK ML model pairing.
     * <br>
     * This tests creating 2 different Takml Executors that are applicable to two different Mx Plugins.
     * The executors are then ran against 6 different Takml Model inputs each, concurrently.
     * <br>
     * <ol>
     * <li>Create two TAK ML Models</li>
     * <li>Create two TAK ML Executors with runAsService flag set to true</li>
     * <li>Run 6 predictions per Takml Executor</li>
     * <li>Verify there were 6, and only 6, calls to each Mx Plugin</li>
     * </ol>
     */
    @Test
    public void takmlServiceTestSwitchModel() throws IOException, TakmlInitializationException, InterruptedException {
        // Create a temporary file in the cache directory
        File tempFile = File.createTempFile("tempFile", ".txt", context.getCacheDir());
        TestTakml takml = new TestTakml(context);

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder("test", Uri.parse(tempFile.toURI().toString()), ".test",
                ModelTypeConstants.IMAGE_CLASSIFICATION).build();
        takml.addTakmlModel(takmlModel);

        // Create another test TAK ML Model, applicable to ExampleMxPlugin2
        TakmlModel takmlModel2 = new TakmlModel.TakmlModelBuilder("test2", Uri.parse(tempFile.toURI().toString()), ".test2",
                ModelTypeConstants.GENERIC_RECOGNITION).build();
        takml.addTakmlModel(takmlModel2);
        TakmlExecutor takmlExecutor = takml.createExecutor(takmlModel, true);

        CountDownLatch countDownLatchExampleMXPlugin = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);
        executePrediction(countDownLatchExampleMXPlugin, "test1", takmlExecutor, counter);
        assertTrue(countDownLatchExampleMXPlugin.await(30, TimeUnit.SECONDS));

        takmlExecutor.selectModel(takmlModel2);
        CountDownLatch countDownLatchExampleMXPlugin2 = new CountDownLatch(1);
        AtomicInteger counter2 = new AtomicInteger(0);
        executePrediction(countDownLatchExampleMXPlugin2, "test2", takmlExecutor, counter2);
        assertTrue(countDownLatchExampleMXPlugin2.await(30, TimeUnit.SECONDS));
    }


    private void executePrediction(CountDownLatch countDownLatch, String input, TakmlExecutor takmlExecutor, AtomicInteger counter){
        byte[] testInput = input.getBytes(StandardCharsets.UTF_8);
        takmlExecutor.executePrediction(testInput, (takmlResults, success, modelName, modelType) -> {
            counter.incrementAndGet();
            countDownLatch.countDown();
        });
    }

    @After
    public void after(){
        FAKE_EXAMPLE_MX_PLUGIN = mock(ExampleMXPlugin.class);
        FAKE_EXAMPLE_MX_PLUGIN_2 = mock(ExampleMXPlugin2.class);
    }
}
