package com.bbn.tak.ml.sensor_framework;

import java.io.IOException;
import java.io.StringWriter;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.ObjectReader;

import de.fraunhofer.iosb.ilt.sta.jackson.ObjectMapperFactory;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.IdString;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.TimeObject;

public class SensorDataUtils {
	public static String objToJSON(Object obj) throws IOException {
        StringWriter writer = new StringWriter();
        ObjectMapperFactory.get().writer().writeValue(writer, obj);
        return writer.toString();
    }

	public static <T> T jsonToObj(String json, Class<T> clazz) throws IOException {
		ObjectReader reader = ObjectMapperFactory.get().readerFor(clazz);
		return reader.readValue(json);
    }
	
	public static void main(String[] args) throws IOException {
		Observation obs = new Observation();
		obs.setDatastream(new Datastream());
		obs.setFeatureOfInterest(new FeatureOfInterest());
		obs.setId(new IdString("id123"));
		obs.setPhenomenonTime(new TimeObject(ZonedDateTime.now()));
		obs.setResult("result");
		
		String obsJsonStr = objToJSON(obs);
		
		System.out.println(obsJsonStr);
		
		Observation obs2 = jsonToObj(obsJsonStr, Observation.class);
		System.out.println(obs2.getResult());
	}
}
