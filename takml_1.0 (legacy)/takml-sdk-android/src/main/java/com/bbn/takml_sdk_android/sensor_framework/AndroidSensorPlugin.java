package com.bbn.takml_sdk_android.sensor_framework;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.tak.ml.TakMlConstants;
import com.bbn.tak.ml.sensor.SensorPlugin;

import java.io.ObjectInput;
import java.io.ObjectInputStream;

//=====================================================
//  Github for reference:
//  https://github.com/FraunhoferIOSB/FROST-Server
//=====================================================
import de.fraunhofer.iosb.ilt.frostserver.json.serialize.EntityFormatter;
import de.fraunhofer.iosb.ilt.frostserver.model.Datastream;
import de.fraunhofer.iosb.ilt.frostserver.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.frostserver.model.Observation;
//import de.fraunhofer.iosb.ilt.frostserver.model.EntityChangedMessage;
import de.fraunhofer.iosb.ilt.frostserver.model.Sensor;

//import com.atakmap.coremap.log.Log;

public abstract class AndroidSensorPlugin implements SensorPlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger(SensorPlugin.class);
	
    private String clientID;
    private String tak_ml_api_listening_address;
    public MqttClient client;
    private Sensor sensorObject;
    private Datastream datastreamObject;
    private FeatureOfInterest featureOfInterestObject;

    public static final String TAG = "SensorPlugin";

    //================================
    //      CONSTRUCTORS
    //================================
    public AndroidSensorPlugin(String _clientID, String _tak_ml_api_listening_address, Sensor _sensorObject, Datastream _datastreamObject, FeatureOfInterest _featureOfInterestObject){
        this.clientID = _clientID;
        this.tak_ml_api_listening_address = _tak_ml_api_listening_address;
        this.sensorObject = _sensorObject;
        this.datastreamObject = _datastreamObject;
        this.featureOfInterestObject = _featureOfInterestObject;
    }

    protected AndroidSensorPlugin(String _clientID, Sensor _sensorObject, Datastream _datastreamObject, FeatureOfInterest _featureOfInterestObject){
        this(_clientID, "tcp://localhost:" + Integer.toString(TakMlConstants.TAK_ML_LISTENER_DEFAULT_PORT), _sensorObject, _datastreamObject, _featureOfInterestObject);
    }


    //========================================
    //  implemented methods - DO NOT OVERRIDE!!
    //========================================
    public boolean registerSensor(){

        //=============================================
        //  Create the connection to the TAK-ML API
        //=============================================
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setMaxInflight(55000);

        try {
            client = new MqttClient(this.tak_ml_api_listening_address, this.clientID, null);

            client.connect(connectOptions);
        }
        catch (MqttException e) {
            LOGGER.error("Caught: " + e);
            e.printStackTrace();
        }

        //=============================================
        //  Have the sensor subscribe to topic to receive SFReadStartRequest and
        //  SFReadStopRequest publishes from the TAKML Framework App
        //=============================================

        try {
            String topic = TakMlConstants.SENSOR_COMMAND_PREFIX + getID();
            LOGGER.debug("Sensor is listening for topic " + topic);
            client.subscribe(topic, null);
        }
        catch (MqttException e) {
            LOGGER.error("Caught: " + e);
            e.printStackTrace();
        }

        //==========================================================
        //  Send the SENSOR_REGISTER message to the TAK-ML API
        //==========================================================
        try {
            String serialisedMessage = EntityFormatter.writeEntity(this.sensorObject);

            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialisedMessage.getBytes());

            new Thread(() -> {
            	try {
					client.publish(TakMlConstants.SENSOR_REGISTER, msg);
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }).start();

        } catch(IOException ioe) {
            LOGGER.error("Sensor Plugin was unable to send the SENSOR_REGISTER message.", ioe);
        }

        return true;

    }

    public boolean deRegisterSensor(){

        //==========================================================
        //  Send the SENSOR_DEREGISTER message to the TAKML Listener
        //==========================================================
        try {
            //EntityChangedMessage message = new EntityChangedMessage().setEntity(this.sensorObject);

            //ObjectMapper mapper = EntityFormatter.getObjectMapper();
            //String serialisedMessage = mapper.writeValueAsString(message);
            String serialisedMessage = EntityFormatter.writeEntity(this.sensorObject);

            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialisedMessage.getBytes());

            client.publish(TakMlConstants.SENSOR_DEREGISTER, msg);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch(MqttException e) {
            e.printStackTrace();
        }

        //==================================================
        //  disconnect the connection to the TAKML Listener
        //==================================================
        try {
            client.disconnect();
	    client.close();
        }
        catch (MqttException e) {
            LOGGER.error("Caught: " + e);
            e.printStackTrace();
        }

        return true;
    }

    public Sensor getSensor() {
        return this.sensorObject;
    }


    public boolean publishSensorData(Observation inputObservation, String publishTopic) {
    	LOGGER.info("Received sensor data to publish");
        //------------------------------------------------
        //  add properties that are not yet included
        //------------------------------------------------
        if(!inputObservation.isSetDatastream())
            inputObservation.setDatastream(this.datastreamObject);
        if(!inputObservation.isSetFeatureOfInterest())
            inputObservation.setFeatureOfInterest(this.featureOfInterestObject);
/*
        try {
            //----------------------------------
            //  serialise the Observation
            //----------------------------------
            String serialisedMessage = EntityFormatter.writeEntity(inputObservation);

            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialisedMessage.getBytes());
            // TODO - add sensor name to topic so it's easy to differentiate messages according to which sensor they came from
            String msgTopic = TakMlConstants.SENSOR_DATA_REPORT_PREFIX + "sensor_specific_name";

            //----------------------------------
            //  publish the Observation to the TAKML Listener
            //----------------------------------
            client.publish(msgTopic, msg);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }  catch(MqttException e) {
            e.printStackTrace();
        }

 */
        

        //-----------------------------------------------------------
        // put serialized Observation object into an Mqtt message
        //-----------------------------------------------------------
        try {
            String serialisedMessage = EntityFormatter.writeEntity(inputObservation);

            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialisedMessage.getBytes());
            client.publish(publishTopic, msg);

        } catch(IOException ioe) {
            //Log.e(TAG,"Sensor Plugin was unable to report data.", ioe);
        	LOGGER.error("Unable to publish sensor data", ioe);
            return false;
        } catch(MqttException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //========================================
    //  methods that should be overridden by the SensorPlugin implementation
    //========================================
    public abstract void startSensorDataReporting();

    public abstract void stopSensorDataReporting();

    public abstract void startSensorReadingRequest();

    public abstract void stopSensorReadingRequest();

    private Object deserialize(byte[] bytes) {
        Object o;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInput oi = new ObjectInputStream(bis);
            o = (Object)oi.readObject();
            oi.close();
            bis.close();
        } catch (IOException e) {
            LOGGER.error(TAG, "Exception when constructing input data: " + e);
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            LOGGER.error(TAG, "Exception when constructing input data: " + e);
            e.printStackTrace();
            return null;
        }
        return o;
    }
}
