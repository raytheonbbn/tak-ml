package com.bbn.takml.sensor_framework;

import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.DEFAULT_SENSOR_PORT_VALUE;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.HOST_ADDR;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.SENSOR_PORT;
import static com.atakmap.android.takml_framework.receivers.SettingsDropDownReceiver.USE_TLS;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_framework.receivers.DataDropDownReceiver;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.bbn.tak.ml.TakMlConstants;
import com.bbn.tak.ml.sensor.SensorDataStream;
import com.bbn.tak.ml.sensor.info.SensorList;
import com.bbn.tak.ml.sensor.request.SFQuerySensorList;
import com.bbn.tak.ml.sensor.request.SFReadStartCommand;
import com.bbn.tak.ml.sensor.request.SFReadStartRequest;
import com.bbn.tak.ml.sensor.request.SFReadStopCommand;
import com.bbn.tak.ml.sensor.request.SFReadStopRequest;
import com.bbn.tak.ml.sensor_framework.SensorDBQuery_Observation;
import com.bbn.tak.ml.sensor_framework.SensorDataUtils;
import com.bbn.takml.framework.ChangeType;
import com.bbn.takml.framework.SensorListChangedCallback;
import com.bbn.takml.listener.ServerPublisher;
import com.bbn.takml.listener.TakMlListener;
import com.bbn.takml.support.TakMlConfigPropertyLoader;
import com.google.gson.Gson;

//=====================================================
//  Github for reference:
//  https://github.com/FraunhoferIOSB/FROST-Server
//=====================================================

import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Point;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.IdLong;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.ext.DataArrayDocument;
import de.fraunhofer.iosb.ilt.sta.model.ext.DataArrayValue;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;

public class SensorFramework implements SensorTagUpdateListener{

    public static final String TAG = "TAK_ML_SensorFramework";
    private final Context context;
    public static final Thing TAK_ML_THING = new Thing("tak-ml-sensor-framework", "TAK-ML Sensor Framework");

    private String sensorFrameworkID;
    public TakMlListener takMlListener;

    private ServerPublisher publisher;

    private static SensorFrameworkDB db;
    private SensorListChangedCallback changeCallback;

    private Map<String, SensorDataStream> registeredSensors;
    private List<String> dataStreamsSentToServer = new ArrayList<>();

    private static long lastStreamIDSent = -1;

    // Sensor Plugin ID -> List of MLA Plugin ID's for who is subscribed to sensor output
    private HashMap<String, List<String>> subscriptions;
    private SensorThingsService serverService;
    private static boolean shouldReloadConfig = false;

    /***************CONSTRUCTOR**********/
    public SensorFramework(Context context, SensorListChangedCallback changeCallback) {

        //this._prefs = new UnitPreferences(MapView.getMapView());
        //this.sensorFrameworkID = "ATAK_Sensor_Framework_" + _prefs.get("bestDeviceUID", "");//UUID.randomUUID();
        this.sensorFrameworkID = "ATAK_Sensor_Framework_" + UUID.randomUUID();
        this.context = context;
        this.changeCallback = changeCallback;
        shouldReloadConfig = true;
        //parser = new EntityParser(IdString.class);
        // stand up the Sensor Framework database
        this.initializeDB(context);
        subscriptions = new HashMap<>();
        registeredSensors = new HashMap<>();

        try {
            connectToServer();
        } catch (MalformedURLException e) {
            Log.w(TAG, "Failed attempt to configure communications with server. Properties may not have been set yet.");
        }
    }

    public void deleteAllData() {
        db.clearDB();
        DataDropDownReceiver.clearDataToServerCount();
        lastStreamIDSent = -1;
    }

    public static void invalidateSettings() {
        shouldReloadConfig = true;
    }

    private void sendDataStreamInfoToServer(Datastream ds) throws ServiceFailureException{
        if(ds == null) {
            // ** here for backward compatibility? - at some point this should be removed
            ds = new Datastream();
            UnitOfMeasurement uom = new UnitOfMeasurement("unknown", "unknown", "unknown");
            ObservedProperty obsProp = new ObservedProperty("unknown", "unknown", "unknown");
            Sensor sensor = new Sensor("sensor1", "sensor1", "UTF-8", "");

            ds.setName("ds1");
            ds.setDescription("ds1 desc");
            ds.setObservationType("obs type");
            ds.setUnitOfMeasurement(uom);
            ds.setObservedProperty(obsProp);
            ds.setSensor(sensor);
            ds.setThing(TAK_ML_THING);
        }

        serverService.create(ds);

        Log.d(TAG, "Sent data stream. New ID: " + ds.getId());

        db.addIdToDS(ds);
        db.addIdToSensor(ds.getSensor());
        db.DEFAULT_OBS_PROP_ID = (IdLong)ds.getObservedProperty().getId();

        Log.d(TAG, "Got resposne with IDS: " + ds.getId() + " : " + ds.getSensor().getId() + " : " + ds.getObservedProperty().getId());

        // ** TODO: get ID out of ds, put in a map somewhere? Or do we need to query the server first? Ugh

        dataStreamsSentToServer.add(ds.getName());
    }

    public boolean streamData(long batchSize) {
        if(lastStreamIDSent == -1) {
            // ** load last ID we sent from the DB, if we don't have that info in memory yet
            lastStreamIDSent = db.getLastSentID();
            if(lastStreamIDSent == -1) {
                // ** if we've never sent any data, the last ID sent becomes one less than the lowest ID we have recorded
                lastStreamIDSent = db.findMinObservationID() - 1; // ** we increment in the next step, so must decrement on the first pass
                db.updateLastSentID(lastStreamIDSent, 0);
            }

        }

        long startTime = System.currentTimeMillis();

        long startID = lastStreamIDSent + 1;
        long endID = startID + batchSize;
        List<Observation> observationBatch = db.getObservationBatch(startID, endID);

        long retrievedObsTime = System.currentTimeMillis();

        if(observationBatch.isEmpty()) {
            Log.d(TAG, "No observations to send");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) { }
            return false;
        }

        if(serverService == null || shouldReloadConfig) {
            try {
                connectToServer();
            } catch (MalformedURLException e) {
                return false;
            }
        }

        long connectionSetupTime = System.currentTimeMillis();



        // ** build a per-datastream observation map of data stream names to pairs of <data stream, observation list>
        Map<String, Pair<Datastream, List<Observation>>> dataStreamObsMap = new HashMap<>();
        for(Observation obs : observationBatch) {
            try {
                Pair<Datastream, List<Observation>> dsObsPair = dataStreamObsMap.get(obs.getDatastream().getName());
                if(dsObsPair == null) {
                    List<Observation> obsList = new ArrayList<>();
                    dsObsPair = Pair.create(obs.getDatastream(), obsList);
                    dataStreamObsMap.put(obs.getDatastream().getName(), dsObsPair);
                }
                dsObsPair.second.add(obs);
            } catch (ServiceFailureException e) {
                Log.e(TAG, "Error getting data stream from object. Shouldn't occur since object is just local.");
            }
        }

        final Set<DataArrayValue.Property> properties = new HashSet<>();
        properties.add(DataArrayValue.Property.Result);
        properties.add(DataArrayValue.Property.PhenomenonTime);

        long startSendTimeTime = System.currentTimeMillis();
        Log.d(TAG, "Sending data streams");
        for(String dataStreamName : dataStreamObsMap.keySet()) {
            Log.d(TAG, "Checking if need to send data stream " + dataStreamName + " to server: " + !dataStreamsSentToServer.contains(dataStreamName));
            if(!dataStreamsSentToServer.contains(dataStreamName)) {
                Log.d(TAG, "Sending data stream: " + dataStreamObsMap.get(dataStreamName).first.getName());
                try {
                    sendDataStreamInfoToServer(dataStreamObsMap.get(dataStreamName).first);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending data stream to server: " + e.getMessage());
                    e.printStackTrace();

                    return false;
                }
            }

            // Create the dataArray associated to the datastream
            final DataArrayValue observations = new DataArrayValue(dataStreamObsMap.get(dataStreamName).first, properties);
            for(Observation obs : dataStreamObsMap.get(dataStreamName).second) {
                observations.addObservation(obs);
            }

            final DataArrayDocument dataArrayDocument = new DataArrayDocument();
            dataArrayDocument.addDataArrayValue(observations);

            try {
                serverService.create(dataArrayDocument);

                int obsSetSize = dataStreamObsMap.get(dataStreamName).second.size();

                DataDropDownReceiver.updateDataToServerCount(obsSetSize);

                lastStreamIDSent += obsSetSize;

                db.updateLastSentID(lastStreamIDSent, obsSetSize);

                Log.d(TAG, "Observation batch of size " + obsSetSize + " sent to server for data stream: " + dataStreamName);
            } catch (Exception e) {
                Log.e(TAG, "Unable to send observation batch to server", e);
                return false;
            }
        }
        long endSendTimeTime = System.currentTimeMillis();


        //Log.d(TAG, "Instrumentation [DB_ACCESS]: " + (retrievedObsTime - startTime));
        //Log.d(TAG, "Instrumentation [CONN_SETUP]: " + (connectionSetupTime - retrievedObsTime));
        //Log.d(TAG, "Instrumentation [DATA_SEND]: " + (endSendTimeTime - startSendTimeTime));
        Log.d(TAG, "Instrumentation," + (retrievedObsTime - startTime) + "," + (connectionSetupTime - retrievedObsTime) + "," + (endSendTimeTime - startSendTimeTime));
        return true;
    }

    public boolean connectToServer(boolean isHTTPS, String host, Integer port) throws MalformedURLException {
        URL serviceEndpoint = new URL(isHTTPS ? "https" : "http", host, port, "v1.0/");
        //URL serviceEndpoint = new URL(isHTTPS ? "https" : "http", hostAddress, sensorPort, "v1.0/");

        try {
            Log.d(TAG, "Setting up SensorThingsService. Using HTTPS: " + isHTTPS);
            serverService = new SensorThingsService(serviceEndpoint);
            if(isHTTPS) {
                SecureSensorThingsUtils.setupForMutualAuth(serverService, context);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to connect to TAk-ML Sensor Server endpoint: " + serviceEndpoint);
            e.printStackTrace();
            return false;
        }

        Log.d(TAG, "Prepared for connection to server-side sensor framework at " + serviceEndpoint.toExternalForm());
        return true;
    }

    public boolean connectToServer() throws MalformedURLException {
        boolean tls = TakMlConfigPropertyLoader.getBoolean(MapView.getMapView().getContext(), USE_TLS, Boolean.FALSE);
        String ip = TakMlConfigPropertyLoader.getString(MapView.getMapView().getContext(), HOST_ADDR, null);
        int remoteServPort = TakMlConfigPropertyLoader.getInt(MapView.getMapView().getContext(), SENSOR_PORT, DEFAULT_SENSOR_PORT_VALUE);

        return connectToServer(tls, ip, remoteServPort);
    }

    public void setPublisher(ServerPublisher publisher) {
        this.publisher = publisher;
    }

    public static void initializeDB(Context context) {
        db = new SensorFrameworkDB(
                FileSystemUtils.getItem("Databases/takml_sensor_framework.db"),
                context);
    }

    public static void teardownDB() {
        if(db != null) {
            db.close();
            db = null;
        }
    }


    /***************************************************************
     *  Sensor Framework functions
     ***************************************************************/

    public void registerSensor(String msg) throws IOException {
        Log.d(TAG, "Registration message received: " + msg);
        SensorDataStream sensorDataStream = (SensorDataStream) SensorDataUtils.jsonToObj(msg, SensorDataStream.class);

        Sensor registeredSensor = sensorDataStream.getSensor();
        Log.d(TAG, "Received registration for sensor data stream with sensor name " + registeredSensor.getName() + " and stream name " + sensorDataStream.getStreamName());

        registeredSensors.put(sensorDataStream.getID(), sensorDataStream);
        subscriptions.put(registeredSensor.getName(), new ArrayList<String>());

        db.registerSensor(sensorDataStream);
        changeCallback.sensorChangeOccurred(sensorDataStream, ChangeType.ADDED);

        return;
    }

    public void deRegisterSensor(String msg, boolean isSensorObject) throws IOException {


        //---------------------------------------------------
        //  extract the name of the sensor to de-register
        //---------------------------------------------------
        if(isSensorObject) {
            Sensor sensorToDeRegister = (Sensor)SensorDataUtils.jsonToObj(msg, Sensor.class);
            String sensorName = sensorToDeRegister.getName();
            SensorDataStream sensorDataStream = registeredSensors.get(sensorName);
            if(sensorDataStream != null) {
                Log.d(TAG, "Received deregistration for sensor data stream with sensor name " + sensorName + " and stream name " + sensorDataStream.getStreamName());
            } else {
                Log.d(TAG, "Received deregistration for sensor data stream with sensor name " + sensorName + ", but unable to locate data stream for that sensor.");
                return;
            }
            registeredSensors.remove(sensorName);
            subscriptions.remove(sensorName);

            changeCallback.sensorChangeOccurred(sensorDataStream, ChangeType.REMOVED);
        }
        // ** Don't remove the sensor from the DB - as we need that data to offload, which might happen after the sensor is turned off

        return;
    }

    public void processSensorReadStartRequest(byte[] msg) {
        SFReadStartRequest subReq = (SFReadStartRequest) deserialize(msg);
        Log.d(TAG, "Got a read start request " +
                "(read requester id " + subReq.getReadRequesterID() + ", " +
                "sensor plugin id " + subReq.getSensorPluginID() + ")");

        // if get a sensor subscribe request for a sensor that has not registered yet,
        // ignore it
        if (!subscriptions.containsKey(subReq.getSensorPluginID())) {
            Log.w(TAG, "Got read start request for unregistered sensor: " + subReq.getSensorPluginID());
            return;
        }

        List<String> readRequestersList = subscriptions.get(subReq.getSensorPluginID());
        readRequestersList.add(subReq.getReadRequesterID());

        // if this is the first subscription to the sensor, send a request to the sensor to start
        // reading
        if (readRequestersList.size() == 1) {
            Log.d(TAG, "Found first read start request to sensor " + subReq.getSensorPluginID());

            SFReadStartCommand startReadCommand = new SFReadStartCommand();

            String publishTopic = TakMlConstants.SENSOR_COMMAND_PREFIX + subReq.getSensorPluginID();

            byte[] serialized = null;
            try {
                serialized = serialize(startReadCommand);
            } catch (IOException e) {
                Log.e(TAG, "Failed to serialize startReadCommand: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            publisher.publish(publishTopic, serialized, "");

            Log.d(TAG, "Published startReadCommand to topic " + publishTopic);
        }

    }

    public void processSensorReadStopRequest(byte[] msg) {
        SFReadStopRequest unsubReq = (SFReadStopRequest) deserialize(msg);
        Log.d(TAG, "Got a read stop request.");

        // if get a sensor unsubscribe request for a sensor that has not registered yet,
        // ignore it
        if (!subscriptions.containsKey(unsubReq.getSensorPluginID())) {
            Log.w(TAG, "Got read stop request for unregistered sensor: " + unsubReq.getSensorPluginID());
            return;
        }

        List<String> readRequestersList = subscriptions.get(unsubReq.getSensorPluginID());
        readRequestersList.remove(unsubReq.getReadRequesterID());

        // if there are no subscriptions left to sensor, send a request to stop reading
        if (readRequestersList.size() == 0) {
            Log.d(TAG, "No remaining read requesters for " + unsubReq.getSensorPluginID());

            SFReadStopCommand stopReadCommand = new SFReadStopCommand();

            String publishTopic = TakMlConstants.SENSOR_COMMAND_PREFIX + unsubReq.getSensorPluginID();

            byte[] serialized = null;
            try {
                serialized = serialize(stopReadCommand);
            } catch (IOException e) {
                Log.e(TAG, "Failed to serialize stopReadCommand: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            publisher.publish(publishTopic, serialized, "");

            Log.d(TAG, "Published stopReadCommand to topic " + publishTopic);
        }
    }

    public void processSensorQueryList(byte[] msg) {
        SFQuerySensorList queryListReq = (SFQuerySensorList) deserialize(msg);
        Log.d(TAG, "Got a query list request.");

        List<String> serializedSensors = new ArrayList<>();
        for (SensorDataStream sensorDataStream : registeredSensors.values()) {
            try {
                serializedSensors.add(SensorDataUtils.objToJSON(sensorDataStream.getSensor()));
            } catch (IOException e) {
                Log.e(TAG, "Failed to serialized sensor: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }
        SensorList sensorList = new SensorList(serializedSensors);

        String publishTopic = TakMlConstants.SENSOR_LIST_QUERY_REPLY_PREFIX + queryListReq.getRequesterID();

        byte[] serialized = null;
        try {
            serialized = serialize(sensorList);
        } catch (IOException e) {
            Log.e(TAG, "Failed to serialize sensor list: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        publisher.publish(publishTopic, serialized, "");

        Log.d(TAG, "Published sensor list to topic " + publishTopic);
    }

    public void recordSensorData(String msg) throws IOException {
        updateLocations();

        Observation observationToRecord = (Observation) SensorDataUtils.jsonToObj(msg, Observation.class);

        db.recordObservation(observationToRecord);
        Log.d(TAG, "Sensor observation received: " + msg);

        return;
    }

    private void updateLocations() {
        if(TAK_ML_THING.getLocations() == null) {
            TAK_ML_THING.setLocations(new ArrayList<>());
        }

        if(TAK_ML_THING.getLocations().isEmpty()) {
            Log.d(TAG, "Adding first location");
            GeoJsonObject gjo = new Point(new LngLatAlt(MapView.getMapView().getSelfMarker().getPoint().getLongitude(), MapView.getMapView().getSelfMarker().getPoint().getLatitude()));
            Location loc = new Location("TAK-ML Android Sensor", "location at " + new Date().toString(), "application/geo+json", gjo);
            List<Location> locations = new ArrayList<>();
            locations.add(loc);
            TAK_ML_THING.setLocations(locations);
        } else {

            Location mostRecentLocation = TAK_ML_THING.getLocations().toList().get(TAK_ML_THING.getLocations().size() - 1);

            GeoJsonObject gjo = (GeoJsonObject)mostRecentLocation.getLocation();
            if(!isGeospatiallyClose((Point)gjo, MapView.getMapView().getSelfMarker().getPoint().getLongitude(), MapView.getMapView().getSelfMarker().getPoint().getLatitude())) {
                Location loc = new Location("TAK-ML Android Sensor", "location at " + new Date().toString(), "application/geo+json", gjo);
                TAK_ML_THING.getLocations().add(loc);
                Log.d(TAG, "Adding location");
            } else {
                Log.d(TAG, "Not adding location - too close");
            }
        }
    }

    private boolean isGeospatiallyClose(Point point, double longitude, double latitude) {
        double latDif = Math.abs(latitude - point.getCoordinates().getLatitude());
        double lonDif = Math.abs(longitude - point.getCoordinates().getLatitude());

        Log.d(TAG, "Lat diff: " + latDif + " Lon Diff: " + lonDif);

        return latDif < .0005 && lonDif < .0005;
    }

    public void executeDBquery(String msg, String topic, String clientID) {

        String returnChannelUUID = topic.substring(topic.lastIndexOf("_") + 1);
        String returnChannelTopic = TakMlConstants.SENSOR_DB_QUERY_RESPONSE + "_" + returnChannelUUID;

        try {
            ArrayList<Observation> result = null;

            //----------------------------------------------------------
            //  Determine which type of database query 'msg' contains
            //----------------------------------------------------------
            //  'msg' contains a query that has 'Observation' return type
            if(msg.contains("SensorDBQuery_Observation")) {
                result = db.processDBQuery_Observation(SensorDBQuery_Observation.deserializeQuery(msg));
            }

            if(result != null) {
                Log.d(TAG, "Responding to DB query on topic: " + returnChannelTopic + ". Starting new response thread.");
                final List<Observation> finalResults = result;
                new Thread() {
                    @Override
                    public void run() {
                        int errorCount = 0;
                        for(Observation local_obs: finalResults) {
                            try {
                                takMlListener.getPublisher().publish(returnChannelTopic, SensorDataUtils.objToJSON(local_obs).getBytes(), clientID);
                            } catch (IOException e) {
                                errorCount++;
                                if(errorCount < 3) {
                                    Log.e(TAG, "Error serializing response object.", e);
                                } else if (errorCount == 3){
                                    Log.e(TAG, "Multiple serialization errors occured when attempting to send response. No further error messages for this request will be displayed");
                                }
                            }
                        }
                    }
                }.start();
            }

        } catch(Exception e) {
            Log.e(TAG, "Unable to execute Sensor Database query: " + e);
        }

        return;
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
            com.atakmap.coremap.log.Log.e(TAG, "Exception when constructing input data: " + e);
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            com.atakmap.coremap.log.Log.e(TAG, "Exception when constructing input data: " + e);
            e.printStackTrace();
            return null;
        }
        return o;
    }

    private byte[] serialize(Object o) throws IOException {
        // Serialize execution request.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(o);
        byte[] serialized = bos.toByteArray();
        out.close();
        bos.close();
        return serialized;
    }

    public long getObservationCount() {
        return db.getObservationCount();
    }

    public long getDataTXFromDB() {
        return db.getTotalSentCount();
    }

    @Override
    public void sensorTagUpdated(String sensorID, String tag) {
        Log.d(TAG, "Sensor framework received tag update notice for sensor " + sensorID);
        SensorDataStream sensorDataStream = registeredSensors.get(sensorID);
        sensorDataStream.setStreamName(tag);
    }
}
