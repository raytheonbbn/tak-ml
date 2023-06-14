package com.atakmap.android.takml_android;

import static com.atakmap.android.takml_android.ExampleMXPlugin.FAKE_EXAMPLE_MX_PLUGIN;
import static com.atakmap.android.takml_android.ExampleMXPlugin2.FAKE_EXAMPLE_MX_PLUGIN_2;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.util.Log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.atakmap.android.takml_android.lib.TakmlInitializationException;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UnitTests {
    private static final String TAG = UnitTests.class.getName();
    private static final byte[] testModel = new byte[10];
    private static final byte[] testModel2 = new byte[10];

    private static final byte[] testInput = new byte[10];
    private static final byte[] testInput2 = new byte[10];

    static {
        new Random().nextBytes(testModel);
        new Random().nextBytes(testModel2);
        new Random().nextBytes(testInput);
        new Random().nextBytes(testInput2);
    }

    private TakmlModel model1, model2;
    private TestTakml takml;
    private TakmlExecutor takmlExecutor;
    private TakmlExecutor takmlExecutor2;
    private static final List<String> labels = Arrays.asList("label1", "label2");

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() throws TakmlInitializationException {
        FAKE_EXAMPLE_MX_PLUGIN = mock(ExampleMXPlugin.class);
        FAKE_EXAMPLE_MX_PLUGIN_2 = mock(ExampleMXPlugin2.class);

        model1 = new TakmlModel.TakmlModelBuilder("model1", testModel, ".test",
                ModelTypeConstants.IMAGE_CLASSIFICATION).setLabels(labels).build();
        model2 = new TakmlModel.TakmlModelBuilder("model2", testModel2, ".test2",
                ModelTypeConstants.IMAGE_CLASSIFICATION).setLabels(labels).build();

        takml = new TestTakml();
        takml.addTakmlModel(model1);
        takml.addTakmlModel(model2);
        List<TakmlModel> models = takml.getModels();
        Assert.assertNotNull(models);
        Assert.assertEquals(models.size(), 2);
        Assert.assertEquals(models.get(0), model1);
        Assert.assertEquals(models.get(1), model2);

        Assert.assertNotNull(model1);
        Assert.assertNotNull(model2);
        takmlExecutor = takml.createExecutor(model1);
        Assert.assertNotNull(takmlExecutor);
        if(testName.getMethodName().equals("testTwoTakmlExecutors")){
            takmlExecutor2 = takml.createExecutor(model2);
            Assert.assertNotNull(takmlExecutor2);
            Assert.assertNotNull(takmlExecutor2.getSelectedModel());
        }
    }

    /**
     * Tests TAKML Executor
     *
     * <ol>
     * <li>Selects a TAK ML Model with an TAK ML Executor and validates</li>
     * <li>Checks that there are two MX Plugins, and selects the first one</li>
     * <li>Executes a prediction and validates callback is executed</li>
     * </ol>
     */
    @Test
    public void testTakmlExecutor() throws TakmlInitializationException {
        Assert.assertNotNull(takmlExecutor.getSelectedModel());
        Assert.assertEquals(takmlExecutor.getSelectedModel(), model1);
        verify(FAKE_EXAMPLE_MX_PLUGIN, timeout(5000)).instantiate(model1);

        List<String> mxPlugins = takml.getModelExecutionPlugins();
        Assert.assertNotNull(mxPlugins);
        int index = 0;
        for(String mxPlugin : mxPlugins) {
            if(mxPlugin.equals(ExampleMXPlugin.class.getName())){
                break;
            }
            index++;
        }
        Assert.assertEquals(mxPlugins.size(), 2);
        takmlExecutor.selectModelExecutionPlugin(mxPlugins.get(index)); // ExampleMXPlugin
        MXExecuteModelCallback mxExecuteModelCallback = (bytes, success, msg) -> {
        };
        takmlExecutor.executePrediction(testInput, mxExecuteModelCallback);
        verify(FAKE_EXAMPLE_MX_PLUGIN, timeout(5000)).execute(testInput, mxExecuteModelCallback);
    }

    private void validateTwoModelsInputted() throws TakmlInitializationException {
        /// check the correct inputs were passed along
        ArgumentCaptor<TakmlModel> argument = ArgumentCaptor.forClass(TakmlModel.class);
        verify(FAKE_EXAMPLE_MX_PLUGIN, Mockito.times(2)).instantiate(
                argument.capture());
        Assert.assertEquals(argument.getAllValues().size(), 2);
        Assert.assertArrayEquals(argument.getAllValues().get(0).getModelBytes(), testModel);
        Assert.assertEquals(argument.getAllValues().get(0).getLabels(), labels);
        Assert.assertNull(argument.getAllValues().get(0).getProcessingParams());
        Assert.assertArrayEquals(argument.getAllValues().get(1).getModelBytes(), testModel2);
        Assert.assertNull(argument.getAllValues().get(1).getProcessingParams());
        Assert.assertEquals(argument.getAllValues().get(1).getLabels(), labels);
    }

    public void testTakmlExecutorChangeModel(boolean changeModelPriorToMXPlugin,
                                             boolean changeMXPlugin) throws TakmlInitializationException {
        takmlExecutor.selectModel(model2);
        Assert.assertNotNull(takmlExecutor.getSelectedModel());
        Assert.assertEquals(takmlExecutor.getSelectedModel(), model2);

        if(changeModelPriorToMXPlugin) {
            // TODO
           // validateTwoModelsInputted();
        }

        List<String> mxPlugins = takml.getModelExecutionPlugins();
        Assert.assertNotNull(mxPlugins);
        int index = 0;
        for(String mxPlugin : mxPlugins) {
            if(mxPlugin.equals(ExampleMXPlugin.class.getName())){
                break;
            }
            index++;
        }
        Assert.assertEquals(mxPlugins.size(), 2);
        takmlExecutor.selectModelExecutionPlugin(mxPlugins.get(index)); // ExampleMXPlugin
        MXExecuteModelCallback mxExecuteModelCallback = (bytes, success, msg) -> {
        };

        if(!changeModelPriorToMXPlugin) {
            //validateTwoModelsInputted();
        }

        takmlExecutor.executePrediction(testInput, mxExecuteModelCallback);
        verify(FAKE_EXAMPLE_MX_PLUGIN, timeout(5000)).execute(testInput, mxExecuteModelCallback);

        if(changeMXPlugin){
            MXExecuteModelCallback mxExecuteModelCallback2 = (bytes, success, msg) -> {
            };
            takmlExecutor.selectModelExecutionPlugin(mxPlugins.get(index == 0 ? 1 : 0)); // ExampleMXPlugin2
            takmlExecutor.executePrediction(testInput, mxExecuteModelCallback2);
            verify(FAKE_EXAMPLE_MX_PLUGIN_2, timeout(5000)).execute(testInput, mxExecuteModelCallback2);
        }
    }

    /**
     * Tests TAK ML Executor with TAK ML Model Selection Prior to selecting MX Plugin
     *
     * <ol>
     * <li>Selects a TAK ML Model with an TAK ML Executor and validates</li>
     * <li>Change TAK ML Model selection and validate</li>
     * <li>Checks that there are two MX Plugins, and selects the first one and validates</li>
     * <li>Executes a prediction and validates callback is executed</li>
     * </ol>
     */
    @Test
    public void testTakmlExecutorChangeModelPriorSettingMXPlugin() throws TakmlInitializationException {
        testTakmlExecutorChangeModel(true, false);
    }

    /**
     * Tests TAK ML Executor with TAK ML Model Selection after selecting MX Plugin
     *
     * <ol>
     * <li>Selects a TAK ML Model with an TAK ML Executor and validates</li>
     * <li>Checks that there are two MX Plugins, and selects the first one and validates</li>
     * <li>Change TAK ML Model selection and validate</li>
     * <li>Executes a prediction and validates callback is executed</li>
     * </ol>
     */
    @Test
    public void testTakmlExecutorChangeModelPostSettingMXPlugin() throws TakmlInitializationException {
        testTakmlExecutorChangeModel(false, false);
    }

    /**
     * Tests TAK ML Executor with modified MX Plugin and TAK ML Model selections
     *
     * <ol>
     * <li>Selects a TAK ML Model with an TAK ML Executor and validates</li>
     * <li>Checks that there are two MX Plugins, and selects the first one and validates</li>
     * <li>Changes MX Plugin and validates</li>
     * <li>Executes a prediction and validates callback is executed</li>
     * </ol>
     */
    @Test
    public void testTakmlExecutorChangeMXPlugin() throws TakmlInitializationException {
        testTakmlExecutorChangeModel(false, true);
    }

    /**
     * Tests TAK ML Executor with modified MX Plugin and TAK ML Model selections #2
     *
     * <ol>
     * <li>Selects a TAK ML Model with an TAK ML Executor and validates</li>
     * <li>Checks that there are two MX Plugins, and selects the first one and validates</li>
     * <li>Changes MX Plugin and validates</li>
     * <li>Change TAK ML Model selection and validate</li>
     * <li>Executes a prediction and validates callback is executed</li>
     * </ol>
     */
    @Test
    public void testTakmlExecutorChangeMXPlugin2() throws TakmlInitializationException {
        testTakmlExecutorChangeModel(true, true);
    }

    /**
     * Tests Two TAK ML Executors with TAK ML Model Selection after selecting MX Plugin
     *
     * <ol>
     * <li>Selects same TAK ML Model with both TAK ML Executors and validates</li>
     * <li>Change TAK ML Model for TAK ML Executor 2 and validate</li>
     * <li>Checks that there are two MX Plugins per TAK ML Executor</li>
     * <li>Changes MX Plugins per TAK ML Executor and validate</li>
     * <li>Executes a prediction for both TAK ML Executors and validates callbacks are executed</li>
     * </ol>
     */
    @Test
    public void testTwoTakmlExecutors() throws TakmlInitializationException {
        takmlExecutor.selectModel(model2);
        takmlExecutor2.selectModel(model2);
        Assert.assertNotNull(takmlExecutor.getSelectedModel());
        Assert.assertEquals(takmlExecutor.getSelectedModel(), model2);
        Assert.assertNotNull(takmlExecutor2.getSelectedModel());
        Assert.assertEquals(takmlExecutor2.getSelectedModel(), model2);

        takmlExecutor.selectModel(model1);
        Assert.assertNotNull(takmlExecutor.getSelectedModel());
        Assert.assertEquals(takmlExecutor.getSelectedModel(), model1);
        Assert.assertNotNull(takmlExecutor2.getSelectedModel());
        Assert.assertEquals(takmlExecutor2.getSelectedModel(), model2);

        List<String> mxPlugins = takml.getModelExecutionPlugins();
        Assert.assertNotNull(mxPlugins);
        Assert.assertEquals(mxPlugins.size(), 2);

        List<String> mxPlugins2 = takml.getModelExecutionPlugins();
        Assert.assertNotNull(mxPlugins2);
        Assert.assertEquals(mxPlugins2.size(), 2);

        Assert.assertNotNull(mxPlugins);
        int index = 0;
        for(String mxPlugin : mxPlugins) {
            if(mxPlugin.equals(ExampleMXPlugin.class.getName())){
                break;
            }
            index++;
        }
        takmlExecutor.selectModelExecutionPlugin(mxPlugins.get(index)); // ExampleMXPlugin

        Assert.assertNotNull(mxPlugins);
        int index2 = 0;
        for(String mxPlugin : mxPlugins) {
            if(mxPlugin.equals(ExampleMXPlugin2.class.getName())){
                break;
            }
            index2++;
        }
        takmlExecutor2.selectModelExecutionPlugin(mxPlugins.get(index2)); // ExampleMXPlugin2
        MXExecuteModelCallback mxExecuteModelCallback = (bytes, success, msg) -> {
        };
        MXExecuteModelCallback mxExecuteModelCallback2 = (bytes, success, msg) -> {
        };
        takmlExecutor.executePrediction(testInput, mxExecuteModelCallback);
        verify(FAKE_EXAMPLE_MX_PLUGIN, timeout(5000)).execute(testInput, mxExecuteModelCallback);
        takmlExecutor2.executePrediction(testInput, mxExecuteModelCallback2);
        verify(FAKE_EXAMPLE_MX_PLUGIN_2, timeout(5000)).execute(testInput, mxExecuteModelCallback2);
    }

   /* @Test
    public void testBuiltInModel() throws TakmlInitializationException {
        Assert.assertNotNull(TestTakml.getInstance().getModels());
        Assert.assertEquals(TestTakml.getInstance().getModels().size(), 2);
        TakmlExecutor builtInExecutor
                = TestTakml.getInstance().createBuiltInModelExecutor(ModelTypeConstants.LINEAR_REGRESSION);
        Assert.assertNotNull(builtInExecutor);
    }

    @After
    public void after(){
        takmlExecutor.shutdown();
        if(takmlExecutor2 != null){
            takmlExecutor2.shutdown();
        }
        TestTakml.clearInstance();
    }*/
}