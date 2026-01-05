package com.atakmap.android.takml_android.emm;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlExecutor;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class EmmTests {
    private static final String TAG = EmmTests.class.getName();

    @Rule
    public TestName testName = new TestName();

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    private Context context;


    private TestTakml takml;
    private final Set<String> modelsToDeny = new HashSet<>();
    private static final String TEST_REAL_MODEL = "test";
    private static final String TEST_REAL_MODEL_2 = "test2";
    private static final String TEST_PSEUDO_MODEL = "test5";

    @Before
    public void before() throws IOException, TimeoutException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TestTakml.enableEmm = testName.getMethodName().contains("testInferenceHookEmmEnabled");
        FakeEmmApiService.isPseudoModel = testName.getMethodName().contains("PseudoModel");
        takml = new TestTakml(context);

        // Add small sleep period to allow for EMM Android Service to load
        if(TestTakml.enableEmm) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private TakmlExecutor createTestTakmlModelAndExecutor(Takml takml, boolean runAsService) throws IOException, TakmlInitializationException {
        // Create a temporary file in the cache directory
        File tempFile = File.createTempFile("tempFile2" + UUID.randomUUID(), ".test", context.getCacheDir());

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder("test", Uri.fromFile(tempFile), ".test",
                ModelTypeConstants.IMAGE_CLASSIFICATION).build();
        takml.addTakmlModel(takmlModel);
        return takml.createExecutor(takmlModel, runAsService);
    }

    private boolean tryInference(TakmlExecutor takmlExecutor,
                                 String expectedModelExecution, boolean denyRegister, Set<String> modelsToDeny)
            throws InterruptedException {
        FakeEmmApiService.MODELS_TO_DENY = modelsToDeny;

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicBoolean val = new AtomicBoolean(false);
        AtomicReference<String> outputModelExecution = new AtomicReference<>();
        takmlExecutor.executePrediction("Test".getBytes(StandardCharsets.UTF_8),
                (takmlResults, success, modelName, modelType) -> {
            val.set(success);
            outputModelExecution.set(modelName);
            countDownLatch.countDown();
        });
        if(!countDownLatch.await(10, TimeUnit.SECONDS)){
            Log.w(TAG, "Timed out waiting for prediction request");
        }

        if(!modelsToDeny.contains(expectedModelExecution)) {
            Assert.assertEquals(outputModelExecution.get(), expectedModelExecution);
        }

        // the inference should not be allowed to run if all atak plugins have not been initalized
        return val.get();
    }

    private boolean testInferenceHook(boolean emmEnabled, boolean emmDenyRegister, String modelName, String expectedModelExecution) throws IOException,
            TakmlInitializationException, InterruptedException {


        TakmlModel takmlModel = takml.getModel(modelName);
        Assert.assertNotNull(takmlModel);
        TakmlExecutor takmlExecutor = takml.createExecutor(takmlModel);
        Assert.assertNotNull(takmlExecutor);

        // (Try to) run the inference
        return tryInference(takmlExecutor, expectedModelExecution, emmDenyRegister, modelsToDeny);
    }

    private boolean testInferenceHook(boolean emmEnabled, boolean emmDenyRegister) throws IOException, TakmlInitializationException, InterruptedException {
        createTestTakmlModelAndExecutor(takml, false);
        return testInferenceHook(emmEnabled, emmDenyRegister, TEST_REAL_MODEL, TEST_REAL_MODEL);
    }

    /**
     * Tests inference hook behavior with the following conditions:
     * <ol>
     *     <li>EMM is disabled</li>
     * </ol>
     *
     * Steps:
     * <ul>
     *     <li>Do not create an EMM server or associate any EMM with TAK ML</li>
     *     <li>Attempt to run an inference</li>
     *     <li>TAK ML should function as default, allowing inference since no EMM is enabled</li>
     * </ul>
     */
    @Test
    public void testInferenceNotHookEmmEnabledSupportsRegisterSupportsOperation() throws
            IOException, TakmlInitializationException, InterruptedException {
        Assert.assertTrue(testInferenceHook(false, false));
    }

    /**
     * Tests inference hook with EMM enabled, where all operations are allowed.
     * <ol>
     *     <li>EMM is enabled</li>
     *     <li>Registration and model operations are supported</li>
     * </ol>
     *
     * Steps:
     * <ul>
     *     <li>Create an EMM server and associate it with TAK ML</li>
     *     <li>Allow registration and all model operations</li>
     *     <li>TAK ML should allow inference execution</li>
     * </ul>
     */
    @Test
    public void testInferenceHookEmmEnabledSupportsRegisterSupportsOperation() throws
            IOException, TakmlInitializationException, InterruptedException {
        Assert.assertTrue(testInferenceHook(true, false));
    }

    /**
     * Tests inference hook with EMM enabled, where a specific model operation is denied.
     * <ol>
     *     <li>EMM is enabled</li>
     *     <li>Registration is supported</li>
     *     <li>Specific model operation (e.g., "test" model) is denied</li>
     * </ol>
     *
     * Steps:
     * <ul>
     *     <li>Create an EMM server and associate it with TAK ML</li>
     *     <li>Deny operations on the "test" model</li>
     *     <li>TAK ML should deny inference execution for the denied model</li>
     *     <li>On Inference Start call should be invoked</li>
     * </ul>
     */
    @Test
    public void testInferenceHookEmmEnabledSupportsRegisterNotSupportsModelOperation() throws
            IOException, TakmlInitializationException, InterruptedException {
        modelsToDeny.add("test");
        Assert.assertFalse(testInferenceHook(true, false));
    }

    /**
     * Tests inference hook with EMM enabled, where all operations including specific models are allowed.
     * <ol>
     *     <li>EMM is enabled</li>
     *     <li>Registration and all model operations are supported</li>
     * </ol>
     *
     * Steps:
     * <ul>
     *     <li>Create an EMM server and associate it with TAK ML</li>
     *     <li>Allow registration and all model operations</li>
     *     <li>TAK ML should allow inference execution for all models</li>
     *     <li>On Inference Start call should be invoked</li>
     *     <li>On Inference End call should be invoked</li>
     * </ul>
     */
    @Test
    public void testInferenceHookEmmEnabledSupportsRegisterSupportsModelOperation() throws
            IOException, TakmlInitializationException, InterruptedException {
        Assert.assertTrue(testInferenceHook(true, false));
    }

    /**
     * Test Pseudo Model -- a model category that a hook endpoint (e.g. an Enterprise Mobile Manager)
     * maps to a real model to run on the end user device
     * <ul>
     *     <li>Create a pseudo model</li>
     *     <li>Run a test inference</li>
     *     <li>TAK ML should allow inference execution for all models</li>
     *     <li>On Inference Start call should be invoked</li>
     *     <li>On Inference End call should be invoked</li>
     * </ul>
     *
     * @throws IOException
     * @throws TakmlInitializationException
     * @throws InterruptedException
     */
    @Test
    public void testInferenceHookEmmEnabledPseudoModel() throws IOException, TakmlInitializationException, InterruptedException {
        createTestTakmlModelAndExecutor(takml, false);

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel = new TakmlModel.TakmlPsuedoModelBuilder(TEST_PSEUDO_MODEL).build();
        takml.addTakmlModel(takmlModel);

        Assert.assertNotNull(takml.getModel(TEST_REAL_MODEL));
        Assert.assertNotNull(takml.getModel(TEST_PSEUDO_MODEL));

        testInferenceHook(true, false, TEST_PSEUDO_MODEL, TEST_REAL_MODEL);
    }

    /**
     * Test Pseudo Model with model execution on Android Service -- a model category that a hook
     * endpoint (e.g. an Enterprise Mobile Manager) maps to a real model to run on the end user device.
     * <ul>
     *     <li>Create a pseudo model</li>
     *     <li>Run a test inference</li>
     *     <li>TAK ML should allow inference execution for all models</li>
     *     <li>On Inference Start call should be invoked</li>
     *     <li>On Inference End call should be invoked</li>
     * </ul>
     *
     * @throws IOException
     * @throws TakmlInitializationException
     * @throws InterruptedException
     */
    @Test
    public void testInferenceHookEmmEnabledPseudoModelWithRealModelRunningAsService() throws IOException, TakmlInitializationException, InterruptedException {
        createTestTakmlModelAndExecutor(takml, true);

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel = new TakmlModel.TakmlPsuedoModelBuilder(TEST_PSEUDO_MODEL).build();
        takml.addTakmlModel(takmlModel);

        Assert.assertNotNull(takml.getModel(TEST_REAL_MODEL));
        Assert.assertNotNull(takml.getModel(TEST_PSEUDO_MODEL));

        testInferenceHook(true, false, TEST_PSEUDO_MODEL, TEST_REAL_MODEL);
    }

    /**
     * Same as test above but changing model and mx plugin, then running inference again. The result should be the Android Service for
     * the old Mx Plugin is stopped, and a new Mx Plugin Android Service is started
     * <ul>
     *     <li>Create a pseudo model</li>
     *     <li>Run a test inference</li>
     *     <li>TAK ML should allow inference execution for all models</li>
     *     <li>On Inference Start call should be invoked</li>
     *     <li>On Inference End call should be invoked</li>
     *     <li>Switch TAK ML Model (and Mx Plugin)</li>
     *     <li>Run same verifications</li>
     * </ul>
     *
     * @throws IOException
     * @throws TakmlInitializationException
     * @throws InterruptedException
     */
    @Test
    public void testInferenceHookEmmEnabledPseudoModelWithRealModelRunningAsServiceThenSwitchMxPluginAndModel() throws IOException, TakmlInitializationException, InterruptedException {
        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel = new TakmlModel.TakmlPsuedoModelBuilder(TEST_PSEUDO_MODEL).build();
        takml.addTakmlModel(takmlModel);

        TakmlExecutor takmlExecutor = takml.createExecutor(takmlModel, true);

        // Create a temporary file in the cache directory
        File tempFile = File.createTempFile("tempFile2" + UUID.randomUUID(), ".test", context.getCacheDir());

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel2 = new TakmlModel.TakmlModelBuilder(TEST_REAL_MODEL, Uri.fromFile(tempFile), ".test",
                ModelTypeConstants.IMAGE_CLASSIFICATION).build();
        takml.addTakmlModel(takmlModel2);

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel3 = new TakmlModel.TakmlModelBuilder(TEST_REAL_MODEL_2, Uri.fromFile(tempFile), ".test2",
                ModelTypeConstants.IMAGE_CLASSIFICATION).build();
        takml.addTakmlModel(takmlModel3);

        Assert.assertNotNull(takml.getModel(TEST_PSEUDO_MODEL));
        Assert.assertNotNull(takml.getModel(TEST_REAL_MODEL));
        Assert.assertNotNull(takml.getModel(TEST_REAL_MODEL_2));

        Assert.assertTrue(tryInference(takmlExecutor, TEST_REAL_MODEL, false, modelsToDeny));

        FakeEmmApiService.modelToReturn = TEST_REAL_MODEL_2;

        Assert.assertTrue(tryInference(takmlExecutor, TEST_REAL_MODEL_2, false, modelsToDeny));
        FakeEmmApiService.modelToReturn = TEST_REAL_MODEL;
    }

    /**
     * Same as test above but not running as a Service
     * <ul>
     *     <li>Create a pseudo model</li>
     *     <li>Run a test inference</li>
     *     <li>TAK ML should allow inference execution for all models</li>
     *     <li>On Inference Start call should be invoked</li>
     *     <li>On Inference End call should be invoked</li>
     *     <li>Switch TAK ML Model (and Mx Plugin)</li>
     *     <li>Run same verifications</li>
     * </ul>
     *
     * @throws IOException
     * @throws TakmlInitializationException
     * @throws InterruptedException
     */
    @Test
    public void testInferenceHookEmmEnabledPseudoModelWithRealModelThenSwitchMxPluginAndModel() throws IOException, TakmlInitializationException, InterruptedException {
        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel = new TakmlModel.TakmlPsuedoModelBuilder(TEST_PSEUDO_MODEL).build();
        takml.addTakmlModel(takmlModel);

        TakmlExecutor takmlExecutor = takml.createExecutor(takmlModel, true);

        // Create a temporary file in the cache directory
        File tempFile = File.createTempFile("tempFile2" + UUID.randomUUID(), ".test", context.getCacheDir());

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel2 = new TakmlModel.TakmlModelBuilder(TEST_REAL_MODEL, Uri.fromFile(tempFile), ".test",
                ModelTypeConstants.IMAGE_CLASSIFICATION).build();
        takml.addTakmlModel(takmlModel2);

        // Create a test TAK ML Model, applicable to ExampleMxPlugin
        TakmlModel takmlModel3 = new TakmlModel.TakmlModelBuilder(TEST_REAL_MODEL_2, Uri.fromFile(tempFile), ".test2",
                ModelTypeConstants.IMAGE_CLASSIFICATION).build();
        takml.addTakmlModel(takmlModel3);

        Assert.assertNotNull(takml.getModel(TEST_PSEUDO_MODEL));
        Assert.assertNotNull(takml.getModel(TEST_REAL_MODEL));
        Assert.assertNotNull(takml.getModel(TEST_REAL_MODEL_2));

        Assert.assertTrue(tryInference(takmlExecutor, TEST_REAL_MODEL, false, modelsToDeny));

        FakeEmmApiService.modelToReturn = TEST_REAL_MODEL_2;

        Assert.assertTrue(tryInference(takmlExecutor, TEST_REAL_MODEL_2, false, modelsToDeny));
        FakeEmmApiService.modelToReturn = TEST_REAL_MODEL;
    }

    @After
    public void after(){
        modelsToDeny.clear();
    }
}
