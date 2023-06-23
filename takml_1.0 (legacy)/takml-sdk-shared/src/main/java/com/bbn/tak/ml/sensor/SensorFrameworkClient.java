package com.bbn.tak.ml.sensor;

import static com.bbn.tak.ml.TakMlConstants.SENSOR_DATA_REPORT_PREFIX;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_READ_START_REQUEST;
import static com.bbn.tak.ml.TakMlConstants.SENSOR_READ_STOP_REQUEST;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.tak.ml.FrameworkCommsErrorHandler;
import com.bbn.tak.ml.MQTTHelper;
import com.bbn.tak.ml.TakMlConstants;
import com.bbn.tak.ml.sensor.request.SFReadStartRequest;
import com.bbn.tak.ml.sensor.request.SFReadStopRequest;
import com.bbn.tak.ml.sensor.request.SFRequest;
import com.bbn.tak.ml.sensor_framework.SensorDBQuery_Observation;
import com.bbn.tak.ml.sensor_framework.SensorDataUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.jackson.ObjectMapperFactory;
import de.fraunhofer.iosb.ilt.sta.model.Observation;

public class SensorFrameworkClient implements MqttCallback {
	private static final Logger LOGGER = LoggerFactory.getLogger(SensorFrameworkClient.class);
	
	private String host;
	private Integer port;
    private MqttClient client;
    private FrameworkCommsErrorHandler errorHandler;
	private Map<String, MqttMessage> receivedMqttMessageQueue = new HashMap<String, MqttMessage>();
	private Map<String, SensorDataCallback> queryCallbackMap = new HashMap<>();
	private Map<String, List<SensorDataCallback>> subscriptionCallbackMap = new HashMap<>();
	private SensorCommandCallbacks cb;
	private String clientID;

	// triggered by the TAKML Framework sending out sensor commands
	public interface SensorCommandCallbacks {
		public void sensorReadStartRequest();
		public void sensorReadStopRequest();
	}
    
    public SensorFrameworkClient(FrameworkCommsErrorHandler errorHandler, SensorCommandCallbacks cb,
								 String clientID) {
		this(TakMlConstants.TAK_ML_LISTENER_DEFAULT_HOST, TakMlConstants.TAK_ML_LISTENER_DEFAULT_PORT,
				errorHandler, cb, clientID);
	}
	
	public SensorFrameworkClient(String host, Integer port, FrameworkCommsErrorHandler errorHandler,
								 SensorCommandCallbacks cb, String clientID) {
		this.host = host;
		this.port = port;
		this.errorHandler = errorHandler;
		this.cb = cb;
		this.clientID = clientID;
	}

    public boolean registerSensor(SensorPlugin plugin) throws MqttException{

		if(client == null) {
			initMQTTClient(plugin.getID());
		}

        //==========================================================
        //  Send the SENSOR_REGISTER message to the TAK-ML API
        //==========================================================
        try {
        	// ** to start the stream name and description just match the sensor name and description
        	SensorDataStream sensorDataStream = new SensorDataStream();
        	sensorDataStream.setStreamName(plugin.getSensorDescription().getName());
        	sensorDataStream.setStreamDescription(plugin.getSensorDescription().getDescription());
        	sensorDataStream.setSensor(plugin.getSensorDescription());
        	sensorDataStream.setUnitOfMeasurement(plugin.getUnitOfMeasurement());
        	
            String serialisedMessage = SensorDataUtils.objToJSON(sensorDataStream);
            LOGGER.error("Serialized registration message: " + serialisedMessage);

            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialisedMessage.getBytes());

            new Thread(() -> {
            	try {
					client.publish(TakMlConstants.SENSOR_REGISTER, msg);
				} catch (MqttException e) {
					errorHandler.errorOccurred("Unable to register sensor", e);
				}
            }).start();

        } catch(IOException ioe) {
            LOGGER.error("Sensor Plugin was unable to send the SENSOR_REGISTER message.", ioe);
        }

        try {
        	String topic = TakMlConstants.SENSOR_COMMAND_PREFIX + plugin.getID();

        	LOGGER.debug("Sensor listening to command topic " + topic);

        	client.subscribe(topic, new IMqttMessageListener() {
				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					LOGGER.debug("Message arrived");

					SFRequest sfRequest = null;
					try {
						sfRequest = (SFRequest) deserialize(message.getPayload());
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}

					LOGGER.debug("Message arrived (type " + sfRequest.getType().toString());

					if (sfRequest.getType().equals(SFRequest.Type.READ_START_CMD)) {
						cb.sensorReadStartRequest();
					}
					else if (sfRequest.getType().equals(SFRequest.Type.READ_STOP_CMD)) {
						cb.sensorReadStopRequest();
					} else {
						LOGGER.error("Unrecognized message type: " + sfRequest.getType().toString());
					}
				}
			});
		} catch (MqttException e) {
        	LOGGER.error("Exception when subscribing to command topic: " + e.getMessage());
		}

        return true;

    }

    public boolean deRegisterSensor(SensorPlugin sensorPlugin) throws MqttException, IOException{
        //==========================================================
        //  Send the SENSOR_DEREGISTER message to the TAKML Listener
        //==========================================================
      
        //EntityChangedMessage message = new EntityChangedMessage().setEntity(this.sensorObject);

        //ObjectMapper mapper = EntityFormatter.getObjectMapper();
        //String serialisedMessage = mapper.writeValueAsString(message);
        String serialisedMessage = SensorDataUtils.objToJSON(sensorPlugin.getSensorDescription());

        MqttMessage msg = new MqttMessage();
        msg.setPayload(serialisedMessage.getBytes());
        msg.setQos(0);

		try {
			String topic = TakMlConstants.SENSOR_COMMAND_PREFIX + sensorPlugin.getID();

			LOGGER.debug("Sensor unsubscribing from command topic " + topic);

			client.unsubscribe(topic);
		} catch (MqttException e) {
			LOGGER.error("Exception when unsubscribing from command topic: " + e.getMessage());
		}
        
        new Thread(() -> {
        	try {
				client.publish(TakMlConstants.SENSOR_DEREGISTER, msg);

				//==================================================
				//  disconnect the connection to the TAKML Listener
				//==================================================
				client.disconnect();
				client.close();
			} catch (MqttException e) {
				errorHandler.errorOccurred("Unable to unregister sensor", e);
			}
        }).start();

        return true;
    }

    public void unsubscribe(String sensorName, SensorDataCallback callback) {
		List<SensorDataCallback> callbacks = subscriptionCallbackMap.get(sensorName);
		if(callbacks != null) {
			callbacks.remove(callback);
		}
	}

	public void subscribe(String sensorName, SensorDataCallback callback) {
		if(client == null) {
			try {
				initMQTTClient();
			} catch (MqttException e) {
				LOGGER.error("Unable to init MQTT client", e);
			}
		}

		List<SensorDataCallback> callbacks = subscriptionCallbackMap.get(sensorName);
		if(callbacks == null) {
			callbacks = new ArrayList<>();
			subscriptionCallbackMap.put(sensorName, callbacks);
		}
		callbacks.add(callback);
		client.setCallback(this);
		try {
			client.subscribe(SENSOR_DATA_REPORT_PREFIX + sensorName);
		} catch (MqttException e) {
			LOGGER.error("Unable to subscribe for sensor data", e);
		}
	}

    public boolean publishSensorData(Observation inputObservation) {
    	LOGGER.info("Received sensor data to publish");

    	//-----------------------------------------------------------
        // put serialized Observation object into an Mqtt message
        //-----------------------------------------------------------
        try {
            String serialisedMessage = SensorDataUtils.objToJSON(inputObservation);

            MqttMessage msg = new MqttMessage();
            msg.setPayload(serialisedMessage.getBytes());
            
            new Thread(() -> {
            	try {
            		if(client == null) {
            			initMQTTClient();
            		}
    				client.publish(TakMlConstants.SENSOR_DATA_REPORT_PREFIX + inputObservation.getDatastream().getSensor().getName(), msg);
    			} catch (MqttException | ServiceFailureException e) {
    				errorHandler.errorOccurred("Unable to publish observation", e);
    			}
            }).start();

        } catch(IOException ioe) {
        	LOGGER.error("Unable to publish sensor data", ioe);
            return false;
        }

        return true;
    }

	public boolean requestSensorReadStart(String sensorPluginID) {

		SFReadStartRequest req = new SFReadStartRequest(sensorPluginID, sensorPluginID);

		try {
			// Serialize creation request.
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(req);
			byte[] serialized = bos.toByteArray();
			out.close();
			bos.close();

			// Send MQTT message to TAKML Framework App.
			MqttMessage msg = new MqttMessage();
			msg.setPayload(serialized);
			client.publish(SENSOR_READ_START_REQUEST, msg);
		} catch (MqttException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean requestSensorReadStop(String sensorPluginID) {

		SFReadStopRequest req = new SFReadStopRequest(sensorPluginID, sensorPluginID);

		try {
			// Serialize creation request.
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(req);
			byte[] serialized = bos.toByteArray();
			out.close();
			bos.close();

			// Send MQTT message to TAKML Framework App.
			MqttMessage msg = new MqttMessage();
			msg.setPayload(serialized);
			client.publish(SENSOR_READ_STOP_REQUEST, msg);
		} catch (MqttException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void querySensorDB(SensorDBQuery_Observation query, SensorDataCallback dataCallback) {
		try {
			if(client == null) { initMQTTClient(); }
			client.setCallback(this);
			String thisQueryUUID = UUID.randomUUID().toString();
			String responseTopic = TakMlConstants.SENSOR_DB_QUERY_RESPONSE + "_" + thisQueryUUID;
			queryCallbackMap.put(responseTopic, dataCallback);
			//-----------------------------------------------------------
			//	send the serialized SensorDBQuery_Observation object in
			//	an Mqtt message and listen for the response
			//-----------------------------------------------------------
			String serialisedMessage = SensorDBQuery_Observation.serializeQuery(query);

			MqttMessage msg = new MqttMessage();
			msg.setPayload(serialisedMessage.getBytes());

			String publish_topic = TakMlConstants.SENSOR_DB_QUERY + "_" + thisQueryUUID;
			client.subscribe(responseTopic);
			client.publish(publish_topic, msg);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, MqttMessage> getReceivedMqttMessageQueue() {return receivedMqttMessageQueue;}
	
	//=====================================
	//	MQTT Callback functions
	//=====================================
	
	// this function called when "client" subscribes to a topic and a message is received on that topic
	@Override
	public void messageArrived(String topic, MqttMessage message)throws Exception {
		LOGGER.debug("Message received for topic: " + topic);

		if(queryCallbackMap.containsKey(topic)) {
			SensorDataCallback callback = queryCallbackMap.get(topic);
			Observation observation = getObservationFromMQTTMessage(message);
			callback.dataAvailable(observation);

			if(client == null) {
				initMQTTClient();
			}

			// ** TODO: when query results are completely back, should remove callback from map, and unsubscribe
			// ** may need some special marker message to know when that occurs?
			//client.unsubscribe(topic);
			//sensorDataCallbackMap.remove(topic);
		} else if(topic.startsWith(SENSOR_DATA_REPORT_PREFIX)) {
			Observation obs = getObservationFromMQTTMessage(message);
			//System.out.println("Observation received with delay: " + ChronoUnit.MILLIS.between(obs.getResultTime(), ZonedDateTime.now()));
			String sensorName = obs.getDatastream().getSensor().getName();

			for(String subscriptionSensor : subscriptionCallbackMap.keySet()) {
				if(sensorName.startsWith(subscriptionSensor)) {
					List<SensorDataCallback> callbacks = subscriptionCallbackMap.get(subscriptionSensor);
					for(SensorDataCallback callback : callbacks) {
						callback.dataAvailable(obs);
					}
				}
			}
		} else {
			receivedMqttMessageQueue.put(topic, message);
		}
	}

	private Observation getObservationFromMQTTMessage(MqttMessage message) throws IOException {
		String resultStr = new String(message.getPayload());

		ObjectMapper mapper = ObjectMapperFactory.get();
		return mapper.readerFor(Observation.class).readValue(resultStr);
	}

	private void initMQTTClient(String clientID) throws MqttException {
		client = MQTTHelper.getMQTTClient(host, port, clientID);
	}

	private void initMQTTClient() throws MqttException {
		initMQTTClient("sensorDataClient_" + UUID.randomUUID().toString());
	}

	@Override
    public void connectionLost(Throwable cause) {
        LOGGER.warn("MQTT connection lost", cause);
    }
	
	@Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        LOGGER.debug("MQTT message delivery complete for message " + token.getMessageId());
    }

	private Object deserialize(byte[] bytes) {
		Object o;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInput oi = new ObjectInputStream(bis);
			o = (Object)oi.readObject();
			oi.close();
			bis.close();
		} catch (IOException e) {
			LOGGER.error("Exception when constructing input data: " + e);
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			LOGGER.error("Exception when constructing input data: " + e);
			e.printStackTrace();
			return null;
		}
		return o;
	}

	public static void main(String[] args) throws IOException {
		try {
			String msg = "{\"sensor\":{\"description\":\"MagnetometerSensorPlugin_ANDROID-353626073056058_pressure\",\"encodingType\":\"http://schema.org/description\",\"metadata\":\"Standard Android pressure\",\"name\":\"MagnetometerSensorPlugin_ANDROID-353626073056058_pressure\"},\"streamDescription\":\"MagnetometerSensorPlugin_ANDROID-353626073056058_pressure\",\"streamName\":\"MagnetometerSensorPlugin_ANDROID-353626073056058_pressure\",\"unitOfMeasurement\":{\"name\":\"muT\",\"symbol\":\"muT\",\"definition\":\"Raw field strength in mu T for each coordinate axis\"},\"id\":\"MagnetometerSensorPlugin_ANDROID-353626073056058_pressure\"}";
			SensorDataStream sensorDataStream = (SensorDataStream) SensorDataUtils.jsonToObj(msg, SensorDataStream.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
/*
		Observation obs = new Observation();
		obs.setResult("abc");
		obs.setParameters(new HashMap<>());

		String serialisedMessage = SensorDataUtils.objToJSON(obs);

		ObjectMapper mapper = ObjectMapperFactory.get();
		Observation obs2 = mapper.readerFor(Observation.class).readValue(serialisedMessage);


		System.out.println(serialisedMessage);
		System.out.println(obs2.getParameters());*/
	}
}
