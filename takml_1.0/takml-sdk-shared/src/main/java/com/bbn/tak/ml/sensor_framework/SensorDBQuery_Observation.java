package com.bbn.tak.ml.sensor_framework;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import de.fraunhofer.iosb.ilt.sta.model.TimeObject;


//==============================================================================
//	class SensorDBQuery_Observation
//
//	sensor data sent to the Sensor Framework is stored in the Sensor
//	Framework Database.  Because it is sent in SensorThingsAPI format
//	(formatted as Observations, Datastreams, Sensors, etc.) but stored in
//	tables and columns in a SQLite database, the objects have to be
//	disassembled to be stored in the database.  Consequently, querying the
//	database is a bit tricky.  We query the database using this pre-built
//	SensorDBQuery_Observation.  It is very restrictive in terms of what types of queries
//	can be executed.  Initially, queries can only return Observation objects.
//
//	Queries return all Observation objects that meet the filters in this
//	SensorDBQuery_Observation.  For 'phenomenonTime' and 'resultTime', the filter depends on
//	'phenomenonTime1' and 'phenomenonTimeOperator', which can be '=', '<', '>=' etc.
//	'phenomenonTime2' can be used if the operator is 'BETWEEN'.
//	For 'sensorName' etc. we can specify a name as a filter with '%' as a 
//	wildcard character.
//
//	This SensorDBQuery_Observation is transformed into a SQLite query by the Sensor Framework.
//==============================================================================
public class SensorDBQuery_Observation implements Serializable{
	
	public static String EQUALS = "=";
	public static String NOT_EQUALS = "!=";
	public static String GREATER_THAN = ">";
	public static String LESS_THAN = "<";
	public static String GREATER_THAN_EQUAL_TO = ">=";
	public static String LESS_THAN_EQUAL_TO = "<=";
	public static String BETWEEN = "BETWEEN";
	
	private TimeObject phenomenonTime1;
	private TimeObject phenomenonTime2;
	private String phenomenonTimeOperator;
	
	private TimeObject resultTime1;
	private TimeObject resultTime2;
	private String resultTimeOperator;
	
	private String datastreamName;
	private String sensorName;
	private String featureOfInterestName;
	
	//=================================
	//	CONSTRUCTOR
	//=================================
	public SensorDBQuery_Observation() {
		phenomenonTime1 = null;
		phenomenonTime2 = null;
		phenomenonTimeOperator = null;
		
		resultTime1 = null;;
		resultTime2 = null;
		resultTimeOperator = null;
		
		datastreamName = null;
		sensorName = null;;
		featureOfInterestName = null;;
	}
	
	//public SensorDBQuery_Observation(String inputQuery) {
	//	this = deserializeQuery(inputQuery);
	//}
	
	//-----------------------
	//	Set methods
	//-----------------------
	public void setPhenomenenTimeParams(TimeObject time1, String operator) {
		setPhenomenenTimeParams(time1, null, operator);
	}
	
	public void setPhenomenenTimeParams(TimeObject time1, TimeObject time2, String operator) {
		this.phenomenonTime1 = time1;
		this.phenomenonTime2 = time2;
		this.phenomenonTimeOperator = operator;
	}
	
	public void setResultTimeParams(TimeObject time1, String operator) {
		setResultTimeParams(time1, null, operator);
	}
	
	public void setResultTimeParams(TimeObject time1, TimeObject time2, String operator) {
		this.resultTime1 = time1;
		this.resultTime2 = time2;
		this.resultTimeOperator = operator;
	}
	
	public void setPhenomenonTimePrevXhrs(int numHours) {
		this.phenomenonTime1 = new TimeObject(ZonedDateTime.now(ZoneId.of("UTC")));
		this.phenomenonTime2 = new TimeObject(ZonedDateTime.now(ZoneId.of("UTC")).minusHours(numHours));
		this.phenomenonTimeOperator = SensorDBQuery_Observation.BETWEEN;
	}
	
	public void setPhenomenonTimePrevXdays(int numDays) {
		
		this.phenomenonTime1 = new TimeObject(ZonedDateTime.now(ZoneId.of("UTC")));
		this.phenomenonTime2 = new TimeObject(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(numDays));
		this.phenomenonTimeOperator = SensorDBQuery_Observation.BETWEEN;
	}
	
	public void setResultTimePrevXhrs(int numHours) {
		this.resultTime1 = new TimeObject(ZonedDateTime.now(ZoneId.of("UTC")));
		this.resultTime2 = new TimeObject(ZonedDateTime.now(ZoneId.of("UTC")).minusHours(numHours));
		this.resultTimeOperator = SensorDBQuery_Observation.BETWEEN;
	}
	
	public void setResultTimePrevXdays(int numDays) {
		this.resultTime1 = new TimeObject(ZonedDateTime.now(ZoneId.of("UTC")));
		this.resultTime2 = new TimeObject(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(numDays));
		this.resultTimeOperator = SensorDBQuery_Observation.BETWEEN;
	}	
	
	public void setSensorName(String name) {
		this.sensorName = name;
	}
	
	public void setDatastreamName(String name) {
		this.datastreamName = name;
	}
	
	public void setFeatureOfInterestName(String name) {
		this.featureOfInterestName = name;
	}
	
	//----------------------------------
	//	GET methods
	//----------------------------------

	public String getSensorName() { return this.sensorName; }

	public TimeObject getPhenomenonTime1() {
		return this.phenomenonTime1;
	}
	
	public TimeObject getPhenomenonTime2() {
		return this.phenomenonTime2;
	}
	
	public String getPhenomenonTimeOperator() {
		return this.phenomenonTimeOperator;
	}
	
	public TimeObject getResultTime1() {
		return this.resultTime1;
	}
	
	public TimeObject getResultTime2() {
		return this.resultTime2;
	}
	
	public String getResultTimeOperator() {
		return this.resultTimeOperator;
	}	
	
	//-----------------------------------
	//	Helper methods
	//-----------------------------------
	static public SensorDBQuery_Observation deserializeQuery(String inputQuery) throws IOException {
		/*
		SensorDBQuery_Observation object1;
		
		
		try {
			// deserialize the query object             
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(inputQuery.getBytes())); 
              
            // Method for deserialization of object 
            object1 = (SensorDBQuery_Observation)in.readObject();        
		}
		catch(ClassNotFoundException cnfe) {
			System.out.println("Unable to parse Sensor DB query: " + cnfe);
            return null;
		}
		catch(IOException ex) { 
            System.out.println("Unable to parse Sensor DB query: " + ex);
            return null;
        } 
        */
		String trimmedInputQuery;
		
		//-------------------------------------------------------------
		//	trim the leading 
		//		"{"query_type": "SensorDBQuery_Observation", "query":"
		//	if it is part of "inputQuery"
		//-------------------------------------------------------------
		if(inputQuery.contains("SensorDBQuery")) {
			trimmedInputQuery = inputQuery.substring(inputQuery.indexOf("{", inputQuery.indexOf("\"query\"")), inputQuery.lastIndexOf("}")+1);
		}
		else {
			trimmedInputQuery = inputQuery;
		}

		//------------------------------------------------------------
		//	De-serialize the 
		//------------------------------------------------------------
		return SensorDataUtils.jsonToObj(trimmedInputQuery, SensorDBQuery_Observation.class);

	}

	/*
	static public String serializeQuery(SensorDBQuery_Observation inputQuery) {
		/*
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outputStream);
			out.writeObject(inputQuery);
			out.flush();
			return out.toString();
		} catch (Exception e) {
			System.out.println("Failed to serialize query: " + e);
			return null;
		}
		
		
		String serializedQuery = "{";
		
		serializedQuery += "\"phenomenonTime1\":";
		serializedQuery += "\"" + EntityFormatter.writeEntity(inputQuery.phenomenonTime1) + "\"";
		
		//return SensorDBQuery_Observation.createObjectMapper().writeValueAsString(inputQuery);

	}
*/
	
	static public String serializeQuery(SensorDBQuery_Observation inputQuery) {

		try {
			String serializedQuery = "{\"query_type\": \"SensorDBQuery_Observation\", \"query\": ";
			serializedQuery += SensorDataUtils.objToJSON(inputQuery);
			serializedQuery += "}";
			
			return serializedQuery;
		} catch(IOException ioe) {
			System.out.println("Failed to serialize query: " + ioe);
			return null;
		}
	}

/*	
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setPropertyNamingStrategy(new EntitySetCamelCaseNamingStrategy());

        MixinUtils.addMixins(mapper);

        SimpleModule module = new SimpleModule();
        GeoJsonSerializer geoJsonSerializer = new GeoJsonSerializer();
        for (String encodingType : GeoJsonDeserializier.ENCODINGS) {
            CustomSerializationManager.getInstance().registerSerializer(encodingType, geoJsonSerializer);
        }

        module.addSerializer(Entity.class, new EntitySerializer());
        module.addSerializer(EntitySetResult.class, new EntitySetResultSerializer());
        module.addSerializer(TimeValue.class, new TimeValueSerializer());
        mapper.registerModule(module);
        return mapper;
    }
    */
}