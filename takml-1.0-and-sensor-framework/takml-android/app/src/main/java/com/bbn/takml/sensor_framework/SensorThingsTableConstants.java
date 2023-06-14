package com.bbn.takml.sensor_framework;

public class SensorThingsTableConstants {

    //-----------------------------------------------------
    //  Database Table and Column names corresponding
    //  to SensorThings API format
    //-----------------------------------------------------
    public static final String TABLE_OBSERVATIONS = "OBSERVATIONS";
    public static final String TABLE_DATASTREAMS = "DATASTREAMS";
    public static final String TABLE_SENSORS = "SENSORS";
    public static final String TABLE_LOCATIONS = "LOCATIONS";
    public static final String TABLE_HIST_LOCATIONS = "HIST_LOCATIONS";
    public static final String TABLE_LOCATIONS_HIST_LOCATIONS = "LOCATIONS_HIST_LOCATIONS";
    public static final String TABLE_FEATURES_OF_INTEREST = "FEATURES_OF_INTEREST";

    public static final String COLUMN_OBSERVATIONS_ID = "ID";
    public static final String COLUMN_OBSERVATIONS_PHENOMENON_TIME_START = "PHENOMENON_TIME_START";
    public static final String COLUMN_OBSERVATIONS_RESULT_STRING = "RESULT_STRING";
    public static final String COLUMN_OBSERVATIONS_RESULT_TIME = "RESULT_TIME";
    public static final String COLUMN_OBSERVATIONS_RESULT_QUALITY = "RESULT_QUALITY";
    public static final String COLUMN_OBSERVATIONS_VALID_TIME_START = "VALID_TIME_START";
    public static final String COLUMN_OBSERVATIONS_PARAMETERS = "PARAMETERS";
    public static final String COLUMN_OBSERVATIONS_DATASTREAM_ID = "DATASTREAM_ID";
    public static final String COLUMN_OBSERVATIONS_FEATURE_ID = "FEATURE_ID";

    public static final String COLUMN_LOCATIONS_ID = "ID";
    public static final String COLUMN_LOCATIONS_DESCRIPTION = "DESCRIPTION";
    public static final String COLUMN_LOCATIONS_ENCODING_TYPE = "ENCODING_TYPE";
    public static final String COLUMN_LOCATIONS_LOCATION = "LOCATION";
    public static final String COLUMN_LOCATIONS_GEOM = "GEOM";
    public static final String COLUMN_LOCATIONS_NAME = "NAME";
    public static final String COLUMN_LOCATIONS_GEN_FOI_ID = "GEN_FOI_ID";
    public static final String COLUMN_LOCATIONS_PROPERTIES = "PROPERTIES";

    public static final String COLUMN_HIST_LOCATIONS_ID = "ID";
    public static final String COLUMN_HIST_LOCATIONS_TIME = "TIME";
    public static final String COLUMN_HIST_LOCATIONS_THING_ID = "THING_ID";

    public static final String COLUMN_LOCATIONS_HIST_LOCATIONS_LOCATION_ID = "LOCATION_ID";
    public static final String COLUMN_LOCATIONS_HIST_LOCATIONS_HIST_LOCATION_ID = "HIST_LOCATION_ID";

    public static final String COLUMN_DATASTREAMS_ID = "ID";
    public static final String COLUMN_DATASTREAMS_NAME = "NAME";
    public static final String COLUMN_DATASTREAMS_DESCRIPTION = "DESCRIPTION";
    public static final String COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_NAME = "UNIT_NAME";
    public static final String COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_SYMBOL = "UNIT_SYMBOL";
    public static final String COLUMN_DATASTREAMS_UNIT_OF_MEASUREMENT_DEFINITION = "UNIT_DEFINITION";
    public static final String COLUMN_DATASTREAMS_OBSERVATION_TYPE = "OBSERVATION_TYPE";
    public static final String COLUMN_DATASTREAMS_OBSERVED_AREA = "OBSERVED_AREA";
    public static final String COLUMN_DATASTREAMS_PHENOMENON_TIME = "PHENOMENON_TIME";
    public static final String COLUMN_DATASTREAMS_RESULT_TIME = "RESULT_TIME";
    public static final String COLUMN_DATASTREAMS_THING = "THING";
    public static final String COLUMN_DATASTREAMS_OBSERVED_PROPERTY = "OBSERVED_PROPERTY";
    public static final String COLUMN_DATASTREAMS_SENSOR_ID = "SENSOR_ID";
    public static final String COLUMN_DATASTREAMS_THING_ID = "THING_ID";
    public static final String COLUMN_DATASTREAMS_OBS_PROPERTY_ID = "OBS_PROPERTY_ID";

    public static final String COLUMN_SENSORS_ID = "ID";
    public static final String COLUMN_SENSORS_NAME = "NAME";
    public static final String COLUMN_SENSORS_DESCRIPTION = "DESCRIPTION";
    public static final String COLUMN_SENSORS_ENCODING_TYPE = "ENCODING_TYPE";
    public static final String COLUMN_SENSORS_METADATA = "METADATA";
    public static final String COLUMN_SENSORS_PROPERTIES = "PROPERTIES";

    public static final String COLUMN_FEATURES_OF_INTEREST_ID = "ID";
    public static final String COLUMN_FEATURES_OF_INTEREST_NAME = "NAME";
    public static final String COLUMN_FEATURES_OF_INTEREST_DESCRIPTION = "DESCRIPTION";
    public static final String COLUMN_FEATURES_OF_INTEREST_ENCODING_TYPE = "ENCODING_TYPE";
    public static final String COLUMN_FEATURES_OF_INTEREST_FEATURE = "FEATURE";

    public static final Integer TAK_ML_THING_ID = 1;
    public static final Integer TAK_ML_OBS_PROPERTY_ID = 1;
}
