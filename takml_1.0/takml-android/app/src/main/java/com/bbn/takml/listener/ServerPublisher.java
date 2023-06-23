package com.bbn.takml.listener;

import io.moquette.broker.Server;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * Publishes to MQTT
 */
public class ServerPublisher {

    private Server server;
    private int msgID;

    public ServerPublisher(Server server) {
        this.server = server;
        this.msgID = 0;
    }

    public void publish(String topic, byte[] output, String clientID) {
        ByteBuf buf = Unpooled.wrappedBuffer(output);
        MqttPublishMessage msg = MqttMessageBuilders.publish()
                .messageId(this.msgID++)
                .qos(MqttQoS.AT_LEAST_ONCE)
                .retained(false)
                .topicName(topic)
                .payload(buf)
                .build();
        this.server.internalPublish(msg, clientID);
    }
}
