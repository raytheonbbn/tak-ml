package com.bbn.tak.ml;

/**
 * Constants used in TAK-ML.
 */
public class TakMlConstants {

    //========================================
    //  Network Communication config items
    //========================================
    /**
     * The default port that the TAK-ML MQTT listener uses.
     */
	public static final Integer TAK_ML_LISTENER_DEFAULT_PORT = 9095;
    /**
     * The default host that the TAK-ML MQTT listener uses.
     */
	public static final String TAK_ML_LISTENER_DEFAULT_HOST = "localhost";

    /**
     * Maximum message size for the MQTT messaging system.
     */
	public static final Integer TAK_ML_MAX_MESSAGE_SIZE = 32 * (int)Math.pow(2, 20);
    /**
     * Maximum payload size for the MQTT messaging system.
     * <p>
     * Maximum payload size is max message size - max size of header estimate.
     */
	public static final Integer TAK_ML_MAX_PAYLOAD_SIZE = TAK_ML_MAX_MESSAGE_SIZE - 1024;

    //=========================================
    //  MQTT message topics
    //=========================================
	/**
	 * MQTT topic for sensor registration.
	 */
	public static final String SENSOR_REGISTER = "/sensor_register";
    /**
     * MQTT topic for sensor data reporting.
     */
    public static final String SENSOR_DATA_REPORT_PREFIX = "/sensor_data_report_";
    /**
     * MQTT topic for sensor deregistration.
     */
    public static final String SENSOR_DEREGISTER = "/sensor_deregister";
    /**
     * MQTT topic for sensor database querying.
     */
    public static final String SENSOR_DB_QUERY = "/sensor_db_query";
    /**
     * MQTT topic for sensor database query responses.
     */
	public static final String SENSOR_DB_QUERY_RESPONSE = "/sensor_db__query_response";
    /**
     * MQTT topic for MX plugin instantiation.
     */
    public static final String MX_INSTANTIATE = "/mx_instantiate";
    /**
     * MQTT topic for MX plugin execution.
     */
    public static final String MX_EXECUTE = "/mx_execute";
    /**
     * MQTT topic for MX plugin destruction.
     */
    public static final String MX_DESTROY = "/mx_destroy";
    /**
     * MQTT topic for MX plugin registration.
     */
    public static final String MX_REGISTER = "/mx_register";
    /**
     * MQTT topic for MX plugin deregistration.
     */
    public static final String MX_DEREGISTER = "/mx_deregister";
    /**
     * MQTT topic for refreshing the registration of any running MX plugins.
     */
    public static final String MX_REFRESH = "/mx_refresh";
    /**
     * MQTT topic for a request to list MX framework resources.
     */
    public static final String MX_LIST_RESOURCES_REQ = "/mx_list_resources_req";
    /**
     * MQTT topic for responses to list MX framework resources.
     */
    public static final String MX_LIST_RESOURCES_REPLY = "/mx_list_resources_reply";
    /**
     * MQTT topic for responses to TAKML App data directory queries.
     */
    public static final String TAKML_APP_DATA_DIRECTORY_RESPONSE = "/takml_app_data_directory_response";
    /**
     * MQTT topic for responses to TAKML App data resources list queries.
     */
    public static final String TAKML_APP_DATA_RESOURCES_LIST_RESPONSE = "/takml_app_data_resources_list_response";
    /**
     * MQTT topic for the sensor framework to request to start reading.
     */
    public static final String SENSOR_READ_START_REQUEST = "/sensor_read_start_request";
    /**
     * MQTT topic for the sensor framework to request to stop reading.
     */
    public static final String SENSOR_READ_STOP_REQUEST = "/sensor_read_stop_request";
    /**
     * MQTT topic for the sensor framework query list.
     */
    public static final String SENSOR_QUERY_LIST = "/sensor_query_list";
    /**
     * MQTT topic for the TAK-ML Framework app data directory query.
     */
    public static final String TAKML_APP_DATA_DIRECTORY_QUERY = "/takml_app_data_directory_query";
    /**
     * MQTT topic for the TAK-ML Framework app data resources list query.
     */
    public static final String TAKML_APP_DATA_RESOURCES_LIST_QUERY = "/takml_app_data_resources_list_query";

    /**
     * MQTT topic prefix for MX plugin commands.
     */
    public static final String MX_PLUGIN_COMMAND_PREFIX = "/takml_mxf_plugin_";
    /**
     * MQTT topic prefix for sensor commands.
     */
    public static final String SENSOR_COMMAND_PREFIX = "/takml_sf_plugin_";
    /**
     * MQTT topic prefix for sensor list query replies.
     */
    public static final String SENSOR_LIST_QUERY_REPLY_PREFIX = "/takml_sf_sensors_query_";

    // directory of model files can be found in client-side/TAKML_Framework_app/assets/config.properties
}
