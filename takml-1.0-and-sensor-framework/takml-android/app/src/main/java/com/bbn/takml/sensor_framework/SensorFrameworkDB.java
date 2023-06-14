package com.bbn.takml.sensor_framework;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.database.StatementIface;
import com.bbn.tak.ml.sensor.SensorDataStream;
import com.bbn.tak.ml.sensor_framework.SensorDBQuery_Observation;
import com.bbn.takml.sensor_framework.postgresql_dump_parsing.PostgreslDumpParsingUtils;

//=====================================================
//  Github for reference:
//  https://github.com/FraunhoferIOSB/FROST-Server
//=====================================================
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Id;
import de.fraunhofer.iosb.ilt.sta.model.IdLong;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import io.perfmark.Link;

import static com.bbn.takml.sensor_framework.SensorThingsTableConstants.*;

public class SensorFrameworkDB {

    private static final String TAG = SensorFrameworkDB.class.getSimpleName();
    protected final static int DATABASE_VERSION = 12;
    private static final String TABLE_DATATX = "DATATX";
    private static final String CREATE_DATA_TX_TABLE = "CREATE TABLE " + TABLE_DATATX + " (\"LAST_SENT_ID\" bigint not null, \"TOTAL_SENT_COUNT\" bigint not null);";
    private static final ObservedProperty DEFAULT_OBSERVED_PROPERTY = new ObservedProperty();
    public static IdLong DEFAULT_OBS_PROP_ID = null;
    private static Map<String, Long> dataStreamNameIDMap = new ConcurrentHashMap<>();
    private static final LinkedBlockingQueue OBSERVATION_QUEUE = new LinkedBlockingQueue();
    protected DatabaseIface database;

    protected Context ctx_;
    private Thread observationWritingThread;
    protected List<PostgreslDumpParsingUtils.SQLiteCreateTableCmdStruct> createTableCmdStructs;

    //**********************
    //  Constructor
    //**********************
    public SensorFrameworkDB(File db, Context ctx) {
        DEFAULT_OBSERVED_PROPERTY.setName("DEFAULT_OBS_PROP_NAME");
        DEFAULT_OBSERVED_PROPERTY.setDefinition("DEFAULT_OBS_PROP_DEF");
        DEFAULT_OBSERVED_PROPERTY.setDescription("DEFAULT_OBS_PROP_DESC");
        DEFAULT_OBSERVED_PROPERTY.setProperties(new HashMap<String, Object>());
        DEFAULT_OBSERVED_PROPERTY.setId(DEFAULT_OBS_PROP_ID);
        ctx_ = ctx;

        // load SQL create table commands from frostdb pg dump file and execute them
        createTableCmdStructs =
                PostgreslDumpParsingUtils.loadTables(ctx_, "frostdb_pg_dump.txt");

        // create a handle to the database
        this.database = Databases.openOrCreateDatabase(db.getAbsolutePath());

        // if database does not have certain tables, create a fresh copy of the database
        boolean create = false;
        if( !checkThatDatabaseTablesMatchesPgDumpTables()) {
            Log.d(TAG, "Found that existing database did not pg dump tables.");
            create = true;
        }

        if (!this.checkDatabaseVersion()) {
            Log.d(TAG, "Found that existing database did not match pg dump tables.");
            this.dropTables();
            create = true;
        }

        if (create) {
            if (this.database.isReadOnly())
                throw new IllegalArgumentException("Database is read-only.");

            this.buildTables();
            this.setDatabaseVersion();
        }

        try {
            CursorIface result = this.database.compileQuery("SELECT * FROM sqlite_master WHERE type='table'");
            while (result.moveToNext()) {
                Log.d(TAG, "schema table name: " + result.getString(result.getColumnIndex("name")));
            }
            Log.d(TAG, "Finished sql_master query.");
        } catch (Exception e) {
            Log.e(TAG, "Exception while trying to get schema table: " + e.getMessage());
        }
    }

    public void close() {
        this.database.close();
    }

    /**
     * Returns <code>true</code> if the database version is current, <code>false</code> otherwise.
     * If this method returns <code>false</code>, the database will be rebuilt with a call to
     * {@link #dropTables()} followed by {@link #buildTables()}. Subclasses may override this method
     * to support their own versioning mechanism.
     * <P>
     * The default implementation compares the version of the database via
     * <code>this.database</code>.{@link DatabaseIface#getVersion()} against
     * {@link #DATABASE_VERSION}.
     *
     * @return <code>true</code> if the database version is current, <code>false</code> otherwise.
     */
    protected boolean checkDatabaseVersion() {
        return (this.database.getVersion() == DATABASE_VERSION);
    }

    /**
     * Drops the tables present in the database. This method is invoked in the constructor when the
     * database version is not current.
     */
    protected void dropTables() {
        for (PostgreslDumpParsingUtils.SQLiteCreateTableCmdStruct cmdStruct : createTableCmdStructs) {
            String dropTableCmd = "DROP TABLE IF EXISTS " + cmdStruct.tableName;
            Log.d(TAG, "Executing drop table command: " + dropTableCmd);
            try {
                this.database.execute(dropTableCmd, null);
            } catch (SQLiteException e) {
                Log.e(TAG, "Failed to drop table: " + e.getMessage());
            }
        }

        try {
            database.execute("DROP TABLE " + TABLE_DATATX + ";", null);
        } catch (SQLiteException e) {
            Log.w(TAG, "Unable to drop table " + TABLE_DATATX + ". This may be because it didn't previously exist", e);
        }
    }

    protected boolean checkThatDatabaseTablesMatchesPgDumpTables() {
        Set<String> tableNames = Databases.getTableNames(this.database);

        Log.d(TAG, "Table names of current db: " + tableNames);

        for (PostgreslDumpParsingUtils.SQLiteCreateTableCmdStruct cmdStruct : createTableCmdStructs) {
            if (!tableNames.contains(cmdStruct.tableName)) {
                Log.d(TAG, "Found that existing db did not contain table " + cmdStruct.tableName);
                return false;
            }
        }

        return true;
    }

    /**
     * Builds the tables for the database. This method is invoked in the constructor when the
     * database lacks the catalog table or when if the database version is not current.
     * <P>
     * The default implementation invokes {@link #createSensorThingsTables()} and returns.
     */
    protected void buildTables() {
        createSensorThingsTables();
        createTAKMLTables();
    }

    /**
     * Creates the tables in the SensorThings DB.
     */
    protected final void createSensorThingsTables() {

        for (PostgreslDumpParsingUtils.SQLiteCreateTableCmdStruct s : createTableCmdStructs) {
            String createTableCmd = PostgreslDumpParsingUtils.getSQLiteCreateTableCommand(s);

            try {
                Log.d(TAG, "Executing create table command: " + createTableCmd);
                this.database.execute(createTableCmd, null);
            } catch (SQLiteException e) {
                Log.e(TAG, "Failed to create/alter table: " + e.getMessage());
            }
        }

    }

    private void createTAKMLTables() {
        database.execute(CREATE_DATA_TX_TABLE, null);
        database.execute("INSERT INTO " + TABLE_DATATX + " VALUES (-1,0);", null);
    }

    /**
     * Sets the database versioning to the current version. Subclasses may override this method to
     * support their own versioning mechanism.
     * <P>
     * The default implementation sets the version of the database via <code>this.database</code>.
     * {@link DatabaseIface#setVersion(int)} with {@link #DATABASE_VERSION}.
     */
    protected void setDatabaseVersion() {
        this.database.setVersion(DATABASE_VERSION);
    }

    public Sensor getSensorByName(String name) {
        Log.d(TAG, "Looking up sensor from DB. Sensor name = " + name);
        CursorIface result = null;

        try {
            result = this.database.query("select * from " + TABLE_SENSORS + " where name = ?", new String[]{name});
            if (result.moveToNext()) {
                Sensor sensor = new Sensor();
                sensor.setId(Id.tryToParse(result.getString(result.getColumnIndex(COLUMN_SENSORS_ID))));
                sensor.setName(name);
                sensor.setDescription(result.getString(result.getColumnIndex(COLUMN_SENSORS_DESCRIPTION)));
                sensor.setEncodingType(result.getString(result.getColumnIndex(COLUMN_SENSORS_ENCODING_TYPE)));
                sensor.setMetadata(result.getString(result.getColumnIndex(COLUMN_SENSORS_METADATA)));
                Log.d(TAG, "Found sensor " + name + " with ID " + sensor.getId());
                result.close();
                return sensor;
            } else {
               return null;
            }
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    public Datastream getDataStreamByName(String name) {
        //Log.d(TAG, "Finding data stream with name: " + name);
        CursorIface result = null;
        try {
            result = this.database.query("select * from datastreams where name = ?", new String[]{name});
            if (result.moveToNext()) {
                Datastream datastream = new Datastream();
                datastream.setId(Id.tryToParse(result.getString(result.getColumnIndex(COLUMN_DATASTREAMS_ID))));
                datastream.setName(name);
                datastream.setDescription(result.getString(result.getColumnIndex(COLUMN_DATASTREAMS_DESCRIPTION)));
                String uomName = result.getString(result.getColumnIndex(COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_NAME));
                String uomSymbol = result.getString(result.getColumnIndex(COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_SYMBOL));
                String uomDefinition = result.getString(result.getColumnIndex(COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_DEFINITION));
                datastream.setUnitOfMeasurement(new UnitOfMeasurement(uomName, uomSymbol, uomDefinition));
                result.close();
                return datastream;
            } else {
                return null;
            }
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    public void insertSensor(Sensor sensor) {
        StatementIface stmt = null;
        try {
            // ** insert into sensors
            stmt = this.database.compileStatement("INSERT INTO " + TABLE_SENSORS +
                    " (" +
                    COLUMN_SENSORS_NAME + ", " +
                    COLUMN_SENSORS_DESCRIPTION + ", " +
                    COLUMN_SENSORS_ENCODING_TYPE + ", " +
                    COLUMN_SENSORS_METADATA +
                    ") " +
                    "VALUES (?, ?, ?, ?)");
            stmt.bind(1, sensor.getName());
            stmt.bind(2, sensor.getDescription());
            stmt.bind(3, sensor.getEncodingType());
            stmt.bind(4, sensor.getMetadata().toString());

            stmt.execute();

        } finally {
            if (stmt != null)
                stmt.close();
        }
    }

    public void updateLastSentID(Long lastSent, Integer additionalSent) {
        StatementIface stmt = null;
        Log.d(TAG, "Updating last sent info [last sent ID: " + lastSent + ", new items sent: " + additionalSent + "]");
        try {
            // ** insert into sensors
            stmt = this.database.compileStatement("UPDATE " + TABLE_DATATX + " SET LAST_SENT_ID = ?, TOTAL_SENT_COUNT = TOTAL_SENT_COUNT + ?;");
            stmt.bind(1, lastSent);
            stmt.bind(2, additionalSent);
            stmt.execute();
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }

    public Long getLastSentID() {
        CursorIface result = null;

        try {
            result = this.database.query("select * from " + TABLE_DATATX + ";", null);
            if (result.moveToNext()) {
                return result.getLong(0);
            } else {
                database.execute("INSERT INTO " + TABLE_DATATX + " VALUES (-1,0);", null);
               return -1L;
            }
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    private void insertDataStream(SensorDataStream sensorDataStream) {
        StatementIface stmt = null;
        try {
            // ** insert into sensors
            stmt = this.database.compileStatement("INSERT INTO " + TABLE_DATASTREAMS +
                    " (" +
                    COLUMN_DATASTREAMS_NAME + ", " +
                    COLUMN_DATASTREAMS_DESCRIPTION + ", " +
                    COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_NAME + ", " +
                    COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_SYMBOL + ", " +
                    COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_DEFINITION + ", " +
                    COLUMN_DATASTREAMS_SENSOR_ID + ", " +
                    COLUMN_DATASTREAMS_THING_ID + ", " +
                    COLUMN_DATASTREAMS_OBS_PROPERTY_ID +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.bind(1, sensorDataStream.getStreamName());
            stmt.bind(2, sensorDataStream.getStreamDescription());
            stmt.bind(3, sensorDataStream.getUnitOfMeasurement().getName());
            stmt.bind(4, sensorDataStream.getUnitOfMeasurement().getSymbol());
            stmt.bind(5, sensorDataStream.getUnitOfMeasurement().getDefinition());
            stmt.bind(6, sensorDataStream.getSensor().getId().toString());
            stmt.bind(7, TAK_ML_THING_ID);
            stmt.bind(8, TAK_ML_OBS_PROPERTY_ID);
            stmt.execute();
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }

    public void registerSensor(SensorDataStream sensorDataStream) {

        Sensor sensor = sensorDataStream.getSensor();

        // ** if the sensor isn't already in the DB, insert it
        if(getSensorByName(sensor.getName()) == null) {
            Log.d(TAG, "Inserting sensor record for " + sensor.getName());
            insertSensor(sensor);
            // ** ensure the ID is on the sensor object
            sensor.setId(getSensorByName(sensor.getName()).getId());
        }

        // ** TODO: save UOM to DB
        if(getDataStreamByName(sensorDataStream.getStreamName()) == null) {
            Log.d(TAG, "Inserting datastream record for " + sensorDataStream.getStreamName());
            insertDataStream(sensorDataStream);
        }

        checkRegisteredSensors();

    }



    public void deleteSensorByName(String SensorName) {

        StatementIface stmt = null;
        try {
            stmt = this.database.compileStatement("DELETE FROM " + TABLE_SENSORS +
                    " WHERE " + COLUMN_SENSORS_NAME + " = ? ");
            stmt.bind(1, SensorName);

            stmt.execute();
        } finally {
            if (stmt != null)
                stmt.close();
        }

        checkRegisteredSensors();

        return;

    }

    private void checkRegisteredSensors() {
        try {
            Log.d(TAG, "Querying for currently registered sensors...");
            CursorIface result = this.database.compileQuery("SELECT * FROM " + TABLE_SENSORS);
            while (result.moveToNext()) {
                Log.d(TAG, "sensor name: " + result.getString(result.getColumnIndex(COLUMN_SENSORS_NAME)));
            }
            Log.d(TAG, "Finished registered sensors query.");
        } catch (Exception e) {
            Log.e(TAG, "Exception while trying to get schema table: " + e.getMessage());
        }
    }

    private String safeToString(Object obj) {
        if(obj == null) {
            return "";
        } else {
            return obj.toString();
        }
    }

    public Long getLastAutoGeneratedID(String tableName) {
        CursorIface result = null;

        try {
            result = this.database.query("select seq from sqlite_sequence where name=?", new String[] {tableName});
            if (result.moveToNext()) {
                return result.getLong(0);
            }
        } finally {
            if (result != null) {
                result.close();
            }
        }


        // ** probably should throw exception isntead
        return -1L;
    }

    public void recordLocation(Location location) {
        Log.d(TAG, "Recording location:\n" + location.getLocation());

        StatementIface locStmt = null;
        try {
            locStmt = this.database.compileStatement("INSERT INTO " + TABLE_LOCATIONS +
                    " (" + COLUMN_LOCATIONS_NAME + ", " +
                    COLUMN_LOCATIONS_DESCRIPTION + ", " +
                    COLUMN_LOCATIONS_ENCODING_TYPE + ", " +
                    COLUMN_LOCATIONS_GEOM + ") " +
                    "VALUES (?, ?, ?)");
            locStmt.bind(1, safeToString(location.getName()));
            locStmt.bind(2, safeToString(location.getDescription()));
            locStmt.bind(4, safeToString(location.getEncodingType()));
            locStmt.bind(3, safeToString(location.getLocation().toString()));

            locStmt.execute();
        } finally {
            if (locStmt != null)
                locStmt.close();
        }

        Long locID = getLastAutoGeneratedID(TABLE_LOCATIONS);

        StatementIface histLocStmt = null;
        try {
            histLocStmt = this.database.compileStatement("INSERT INTO " + TABLE_HIST_LOCATIONS +
                    " (" + COLUMN_HIST_LOCATIONS_THING_ID + ", " +
                    COLUMN_HIST_LOCATIONS_TIME + ") " +
                    "VALUES (?, ?)");
            histLocStmt.bind(1, TAK_ML_THING_ID);
            histLocStmt.bind(2, safeToString(ZonedDateTime.now()));
            histLocStmt.execute();
        } finally {
            if (histLocStmt != null)
                histLocStmt.close();
        }

        Long histLocID = getLastAutoGeneratedID(TABLE_HIST_LOCATIONS);

        StatementIface locHistLocStmt = null;
        try {
            locHistLocStmt = this.database.compileStatement("INSERT INTO " + TABLE_LOCATIONS_HIST_LOCATIONS +
                    " (" + COLUMN_LOCATIONS_HIST_LOCATIONS_LOCATION_ID + ", " +
                    COLUMN_LOCATIONS_HIST_LOCATIONS_HIST_LOCATION_ID + ") " +
                    "VALUES (?, ?)");
            locHistLocStmt.bind(1, locID);
            locHistLocStmt.bind(2, histLocID);
            locHistLocStmt.execute();
        } finally {
            if (locHistLocStmt != null)
                locHistLocStmt.close();
        }
    }

    public void recordObservation(Observation observationToRecord) {
        OBSERVATION_QUEUE.add(observationToRecord);

        if(observationWritingThread == null || !observationWritingThread.isAlive()) {
            observationWritingThread = new Thread() {
                @Override
                public void run() {
                    writeRecordsToDB();
                }
            };
            observationWritingThread.start();
        }
    }
    private void writeRecordsToDB() {
        while(true) {
            try {
                List<Observation> observations = new ArrayList<>();
                OBSERVATION_QUEUE.drainTo(observations, 1000);
                if(observations.isEmpty()) {
                    Thread.sleep(500);
                    continue;
                }
                //Log.d(TAG, "Writing " + observations.size() + " observations in batch");

                database.beginTransaction();
                for (Observation obs : observations) {
                    writeRecordToDB(obs);
                }
                database.setTransactionSuccessful();
            } catch (Exception e) {
                Log.d(TAG, "Exception occurred: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if(database.inTransaction()) {
                    database.endTransaction();
                }
            }
        }
    }

    private void writeRecordToDB(Observation observationToRecord) {

        //Log.d(TAG, "Recording observation:\n" + observationToString(observationToRecord));

        // ** the MLA code doesn't get the actual IDs back to them when they register a sensor,
        // ** so they can't pass it in again here with the observation. Instead we just look for
        // ** the name and lookup the ID based on that
        //long stime = System.currentTimeMillis();
        Long datastreamID = -1L;
        try {
            Datastream inDS = observationToRecord.getDatastream();
            if (observationToRecord.getDatastream() != null) {
                String dsName = observationToRecord.getDatastream().getName();
                datastreamID = dataStreamNameIDMap.get(dsName);
                if(datastreamID == null) {
                    //Log.d(TAG, "Getting DS ID from DB");
                    Datastream ds = getDataStreamByName(dsName);
                    if(ds != null) {
                        datastreamID = ((IdLong) ds.getId()).value;
                        dataStreamNameIDMap.put(dsName, datastreamID);
                    } else {
                        datastreamID = -1L;
                        Log.w(TAG, "No data stream found for " + observationToRecord.getDatastream().getName());
                    }
                }
            }
        } catch (ServiceFailureException sfe) {
            Log.e(TAG, "Unable to get datastream from local observation object", sfe);
        }
        //long mtime = System.currentTimeMillis();

        StatementIface stmt = null;
        try {
            stmt = this.database.compileStatement("INSERT INTO " + TABLE_OBSERVATIONS +
                    " (" + COLUMN_OBSERVATIONS_PHENOMENON_TIME_START + ", " +
                    COLUMN_OBSERVATIONS_RESULT_STRING + ", " +
                    COLUMN_OBSERVATIONS_RESULT_TIME + ", " +
                    COLUMN_OBSERVATIONS_RESULT_QUALITY + ", " +
                    COLUMN_OBSERVATIONS_VALID_TIME_START + ", " +
                    COLUMN_OBSERVATIONS_PARAMETERS + ", " +
                    COLUMN_OBSERVATIONS_DATASTREAM_ID + ", " +
                    COLUMN_OBSERVATIONS_FEATURE_ID + ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.bind(1, safeToString(observationToRecord.getPhenomenonTime()));
            stmt.bind(2, safeToString(observationToRecord.getResult()));
            stmt.bind(3, safeToString(observationToRecord.getResultTime()));
            stmt.bind(4, safeToString(observationToRecord.getResultQuality()));
            stmt.bind(5, safeToString(observationToRecord.getValidTime()));
            stmt.bind(6, safeToString(observationToRecord.getParameters()));
            try {
                stmt.bind(7, datastreamID);
                if(observationToRecord.getFeatureOfInterest() != null && observationToRecord.getFeatureOfInterest().getId() != null) {
                    stmt.bind(8, (long)(observationToRecord.getFeatureOfInterest().getId().getValue()));
                } else {
                    stmt.bind(8, 0);
                }
            } catch (ServiceFailureException sfe) {
                Log.d(TAG, "Service failure talking to frost server. Shouldn't happen as this data is local only");
            }

            stmt.execute();
        } finally {
            if (stmt != null)
                stmt.close();
        }
        //long etime = System.currentTimeMillis();

        //Log.d(TAG, "DB insert times: " + (mtime - stime) + " : " + (etime - mtime));
//        SensorDBQuery_Observation observationsQuery = new SensorDBQuery_Observation();
//        observationsQuery.setResultTimePrevXhrs(1);
//        List<Observation> observations = processDBQuery_Observation(observationsQuery);
//
//        if (observations.isEmpty()) {
//            Log.d(TAG, "Found no observations in db.");
//        }
//
//        Log.d(TAG, "Got observations in db:\n");
//        for (Observation o : observations) {
//            Log.d(TAG, observationToString(o) + "\n" + "---" + "\n");
//        }

    }

    private String observationToString(Observation observationToRecord) {
        String resString = safeToString(observationToRecord.getResult());
        String response = null;

        try {
            response = "Observation start time: " + safeToString(observationToRecord.getPhenomenonTime()) + "\n" +
                    "Result string: " + resString.substring(0, Math.min(30, resString.length())) + "\n" +
                    "Result time: " + safeToString(observationToRecord.getResultTime()) + "\n" +
                    "Result quality: " + safeToString(observationToRecord.getResultQuality()) + "\n" +
                    "Valid time: " + safeToString(observationToRecord.getValidTime()) + "\n" +
                    "Parameters: " + safeToString(observationToRecord.getParameters()) + "\n" +
                    "Data stream: " + safeToString(observationToRecord.getDatastream()) + "\n" +
                    "Feature of interest: " + safeToString(observationToRecord.getFeatureOfInterest()) + "\n" +
                    "Id: " + safeToString(observationToRecord.getId());
        } catch (ServiceFailureException sfe) {
            Log.d(TAG, "Service failure talking to frost server. Shouldn't happen as this data is local only");
        }

        return response;
    }

    public ArrayList<Observation> processDBQuery_Observation(String queryString) throws IOException {
        //-----------------------------
        //  deserialize the query
        //-----------------------------
        SensorDBQuery_Observation queryObject = SensorDBQuery_Observation.deserializeQuery(queryString);

        return processDBQuery_Observation(queryObject);
    }

    public ArrayList<Observation> processDBQuery_Observation(SensorDBQuery_Observation queryObject) {

        ArrayList queryResultObservations = new ArrayList<Observation>();

        //-----------------------------------------------------------
        //  construct a database query based on what is in "queryObject"
        //-----------------------------------------------------------
        CursorIface result = null;
        try {
            String dbQueryString = "SELECT * FROM " + TABLE_OBSERVATIONS + " O INNER JOIN " + TABLE_DATASTREAMS + " D ON O.DATASTREAM_ID = D.ID INNER JOIN " + TABLE_SENSORS + " S ON D.SENSOR_ID = S.ID";


            if(queryObject.getPhenomenonTime1() != null || queryObject.getResultTime1() != null || queryObject.getSensorName() != null) {
                dbQueryString += " WHERE ";
            }
            //-----------------------------------------------
            // build the "phenomenonTime" into the query
            //-----------------------------------------------
            if(queryObject.getPhenomenonTime1() != null) {
                if (queryObject.getPhenomenonTimeOperator().compareTo(SensorDBQuery_Observation.BETWEEN) != 0) {
                    dbQueryString = dbQueryString + COLUMN_OBSERVATIONS_PHENOMENON_TIME_START + " " + queryObject.getPhenomenonTimeOperator() + " '" + queryObject.getPhenomenonTime1() + "'";
                } else {
                    dbQueryString = dbQueryString + COLUMN_OBSERVATIONS_PHENOMENON_TIME_START + " BETWEEN '" + queryObject.getPhenomenonTime2() + "' AND '" + queryObject.getPhenomenonTime1() + "'";
                }
            }

            if(queryObject.getPhenomenonTime1() != null && (queryObject.getResultTime1() != null || queryObject.getSensorName() != null)) {
                dbQueryString += " AND ";
            }

            //----------------------------------------------
            //  build the "resultTime" into the query
            //----------------------------------------------
            if(queryObject.getResultTime1() != null) {
                if (queryObject.getResultTimeOperator().compareTo(SensorDBQuery_Observation.BETWEEN) != 0) {
                    dbQueryString = dbQueryString + COLUMN_OBSERVATIONS_RESULT_TIME + " " + queryObject.getResultTimeOperator() + " '" + queryObject.getResultTime1() + "'";
                } else {
                    dbQueryString = dbQueryString + COLUMN_OBSERVATIONS_RESULT_TIME + " BETWEEN '" + queryObject.getResultTime2() + "' AND '" + queryObject.getResultTime1() + "'";
                }
            }

            if(queryObject.getSensorName() != null) {
                if (queryObject.getPhenomenonTime1() != null || queryObject.getResultTime1() != null) {
                    dbQueryString += " AND ";
                }
                dbQueryString += "S." + COLUMN_SENSORS_NAME + " like '" + queryObject.getSensorName() + "%'";
            }

            Log.d(TAG, "Executing query: " + dbQueryString);

            //----------------------------------------------
            //  Execute the query
            //----------------------------------------------
            result = this.database.compileQuery(dbQueryString);

            //---------------------------------------------------------
            //  transform query result to ArrayList of Observations
            // --------------------------------------------------------
            while(result.moveToNext()) {
                Observation newObservation = new Observation();

                newObservation.setPhenomenonTime(TimeObject.parse(result.getString(result.getColumnIndex(COLUMN_OBSERVATIONS_PHENOMENON_TIME_START))));
                newObservation.setResult(result.getString(result.getColumnIndex(COLUMN_OBSERVATIONS_RESULT_STRING)));
                newObservation.setResultTime(TimeObject.parse(result.getString(result.getColumnIndex(COLUMN_OBSERVATIONS_RESULT_TIME))).getAsDateTime());
                newObservation.setResultQuality(result.getString(result.getColumnIndex(COLUMN_OBSERVATIONS_RESULT_QUALITY)));
                //newObservation.setValidTime(TimeInterval.parse(result.getString(result.getColumnIndex(COLUMN_OBSERVATIONS_RESULT_QUALITY))));
                newObservation.setParameters(null);
                newObservation.setDatastream(null);
                newObservation.setFeatureOfInterest(null);

                queryResultObservations.add(newObservation);
            }

        } catch(SQLiteException sle) {
            System.out.println("Failed to execute database query: " + sle);
        }finally
         {
            if (result != null)
                result.close();
        }

        Log.d(TAG, "Results of query: " + queryResultObservations);

        return queryResultObservations;
    }

    public long getObservationCount() {
        long totalCount = 0;
        CursorIface result = this.database.compileQuery("SELECT COUNT(*) FROM " + TABLE_OBSERVATIONS + ";");
        if(result.moveToNext()) {
            totalCount = result.getLong(0);
        }
        result.close();

        return totalCount;
    }

    public List<Observation> getObservationBatch(long startID, long endID) {
        Log.d(TAG, "Requesting observations from " + startID + " to " + endID);
        List<Observation> queryResultObservations = new ArrayList<>();
        CursorIface result = this.database.compileQuery("SELECT " +
                "O." + COLUMN_OBSERVATIONS_PHENOMENON_TIME_START + ", " + // 0
                "O." + COLUMN_OBSERVATIONS_RESULT_STRING + ", " +
                "O." + COLUMN_OBSERVATIONS_RESULT_TIME + ", " +
                "O." + COLUMN_OBSERVATIONS_RESULT_QUALITY + ", " +
                "S." + COLUMN_SENSORS_NAME + ", " +
                "S." + COLUMN_SENSORS_DESCRIPTION + ", " +
                "S." + COLUMN_SENSORS_ENCODING_TYPE + ", " +
                "S." + COLUMN_SENSORS_ID + ", " +
                "DS." + COLUMN_DATASTREAMS_NAME + ", " +
                "DS." + COLUMN_DATASTREAMS_DESCRIPTION + ", " +
                "DS." + COLUMN_DATASTREAMS_ID + ", " +
                "DS." + COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_SYMBOL + ", " +
                "DS." + COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_DEFINITION + ", " +
                "DS." + COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_NAME + " " +
                "FROM " + TABLE_OBSERVATIONS + " O LEFT OUTER JOIN " + TABLE_DATASTREAMS + " DS on O." + COLUMN_OBSERVATIONS_DATASTREAM_ID + " = DS." + COLUMN_DATASTREAMS_ID + " LEFT OUTER JOIN " + TABLE_SENSORS + " S ON DS." + COLUMN_DATASTREAMS_SENSOR_ID + " = S." + COLUMN_SENSORS_ID + " WHERE O.ID >= " + startID + " AND O.ID <= " + endID + ";");
        while(result.moveToNext()) {
            Observation newObservation = new Observation();

            newObservation.setPhenomenonTime(TimeObject.parse(result.getString(0)));
            newObservation.setResult(result.getString(1));
            newObservation.setResultTime(TimeObject.parse(result.getString(2)).getAsDateTime());
            newObservation.setResultQuality(result.getString(3));
            //newObservation.setValidTime(TimeInterval.parse(result.getString(result.getColumnIndex(COLUMN_OBSERVATIONS_RESULT_QUALITY))));
            newObservation.setParameters(null);

            Sensor sensor = new Sensor();
            sensor.setName(result.getString(4));
            sensor.setDescription(result.getString(5));
            sensor.setEncodingType(result.getString(6));
            sensor.setMetadata("");
            long sensorID = result.getLong(7);
            if(sensorID >= 0) {
                sensor.setId(new IdLong(sensorID));
            }

            Datastream datastream = new Datastream();
            datastream.setName(result.getString(8));
            datastream.setSensor(sensor);
            datastream.setObservationType("DEFAULT_OBS_TYPE");
            datastream.setObservedProperty(DEFAULT_OBSERVED_PROPERTY);
            datastream.setDescription(result.getString(9));
            datastream.setThing(SensorFramework.TAK_ML_THING);

            long datastreamID = result.getLong(10);
            if(datastreamID >= 0) {
                datastream.setId(new IdLong(datastreamID));
            }

            /*
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
             */

            UnitOfMeasurement unitOfMeasurement = new UnitOfMeasurement();
            unitOfMeasurement.setSymbol(result.getString(11));
            unitOfMeasurement.setDefinition(result.getString(12));
            unitOfMeasurement.setName(result.getString(13));
            datastream.setUnitOfMeasurement(unitOfMeasurement);

            newObservation.setDatastream(datastream);
            newObservation.setFeatureOfInterest(null);

            queryResultObservations.add(newObservation);
            Log.d(TAG, "About to send observation for sensor/datastream: " + sensor.getName() + " : "+ datastream.getName());
        }

        return queryResultObservations;
    }

    public long findMinObservationID() {
        CursorIface result = this.database.compileQuery("SELECT MIN(ID) FROM " + TABLE_OBSERVATIONS + ";");
        if(result.moveToNext()) {
            long minID = result.getLong(0);
            Log.d(TAG, "Found minimum ID of " + minID + " in observations table");
            return minID;
        } else {
            Log.d(TAG, "Did not find any observations when looking for minimum ID");
            return 0;
        }
    }

    public void clearDB() {
        this.database.compileStatement("DELETE FROM " + TABLE_OBSERVATIONS + ";").execute();
        this.database.compileStatement("DELETE FROM " + TABLE_SENSORS + ";").execute();
        this.database.compileStatement("DELETE FROM " + TABLE_DATASTREAMS + ";").execute();
        this.database.compileStatement("UPDATE " + TABLE_DATATX + " SET LAST_SENT_ID = -1, TOTAL_SENT_COUNT = 0;").execute();
    }

    public long getTotalSentCount() {
        CursorIface result = null;

        try {
            result = this.database.query("select * from " + TABLE_DATATX + ";", null);
            if (result.moveToNext()) {
                return result.getLong(1);
            } else {
                return 0L;
            }
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }

    public void addIdToDS(Datastream ds) {
        this.database.compileStatement("UPDATE " + TABLE_DATASTREAMS + " SET ID = " + ds.getId() + " WHERE " + COLUMN_DATASTREAMS_NAME + " = '" + ds.getName() + "'").execute();
    }

    public void addIdToSensor(Sensor sensor) {
        this.database.compileStatement("UPDATE " + TABLE_SENSORS + " SET ID = " + sensor.getId() + " WHERE " + COLUMN_SENSORS_NAME + " = '" + sensor.getName() + "'").execute();
    }
}
