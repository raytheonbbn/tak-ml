package com.bbn.tak.ml;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MQTTHelper {
	public static MqttClient getMQTTClient(String host, Integer port, String clientID) throws MqttException {
		MqttClient client;
		MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setMaxInflight(55000);

        client = new MqttClient("tcp://" + host + ":" + port, clientID, null);
        client.connect(connectOptions);
        
        return client;
	}
}
