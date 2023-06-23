package com.bbn.tak.ml.sensor;

import java.net.URL;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.junit.Test;

import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.ext.DataArrayDocument;
import de.fraunhofer.iosb.ilt.sta.model.ext.DataArrayValue;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;

public class CurrentDBStateTest {
	@Test
	public void testReportCurrentState() throws Exception {
		URL serviceEndpoint = new URL("http://127.0.0.1:8080/v1.0/");
		SensorThingsService service = new SensorThingsService(serviceEndpoint);
		
		/*Thing thing1 = ThingBuilder.builder()
			    .name("Thingything")
			    .description("I'm a thing!")
			    .build();
		service.create(thing1);*/
		
		/*System.out.println("Good so far");
		Observation obs = ObservationBuilder.builder().result("res1").phenomenonTime(new TimeObjectobsProperties(ZonedDateTime.now())).datastream(new Datastream()).featureOfInterest(new FeatureOfInterest()).build();
		service.create(obs);*/
		
		GeoJsonObject gjo = new Point(new LngLatAlt(43, 72));
		
		Thing thing = new Thing("tak-ml-sensor-framework", "TAK-ML Sensor Framework");
		Location loc = new Location("loc1", "loc1", "application/geo+json", gjo);
		List<Location> locations = new ArrayList<>();
		locations.add(loc);
		thing.setLocations(locations);
		
		UnitOfMeasurement uom = new UnitOfMeasurement("inces", "inches", "inches");
		ObservedProperty obsProp = new ObservedProperty("prop1", "prop1", "prop1");
		Sensor sensor = new Sensor("sensor1", "sensor1", "UTF-8", "");
		
		Datastream ds = new Datastream();
		ds.setName("ds1");
		ds.setDescription("ds1 desc");
		ds.setObservationType("obs type");
		ds.setUnitOfMeasurement(uom);
		ds.setObservedProperty(obsProp);
		ds.setSensor(sensor);
		ds.setThing(thing);
		service.create(ds);
		
		final Set<DataArrayValue.Property> properties = new HashSet<>();
        properties.add(DataArrayValue.Property.Result);
        properties.add(DataArrayValue.Property.PhenomenonTime);
        
        // Create the dataArray associated to the datastream
        final DataArrayValue observations = new DataArrayValue(ds, properties);
		
        int obsCount = 10;
		for(int i = 0; i < obsCount; i++) {
			Observation obs = new Observation();
			obs.setResult(obsCount);
			obs.setResultTime(ZonedDateTime.now());
			obs.setPhenomenonTimeFrom(ZonedDateTime.now());
			obs.setDatastream(ds);
			observations.addObservation(obs);
		}
        
        final DataArrayDocument dataArrayDocument = new DataArrayDocument();
        dataArrayDocument.addDataArrayValue(observations);
        
        long startTime = System.currentTimeMillis();
        service.create(dataArrayDocument);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Wrote " + obsCount + " observations in " + (endTime - startTime) + "ms");
        
        /*List<Observation> observationBatch = new ArrayList<>();
        observationBatch.add(obs);
        
        // Declare the set of Observation properties that have to be included in the dataArray
        final Set<DataArrayValue.Property> properties = new HashSet<>();
        properties.add(DataArrayValue.Property.Result);
        properties.add(DataArrayValue.Property.PhenomenonTime);
        //properties.add(DataArrayValue.Property.FeatureOfInterest);

        // ** TODO: update this to pull from the data (should do queries by sensor or data stream)
        Sensor sensor = new Sensor();
        sensor.setName("TAK-ML Sensor Framework");
        Datastream dataStream = new Datastream();
        dataStream.setSensor(sensor);
        dataStream.setName("ds1");
        dataStream.setObservations(observationBatch);
        ObservedProperty obsProperty = new ObservedProperty();
        obsProperty.setName("obsProp1");
        dataStream.setObservedProperty(obsProperty);
        obs.setDatastream(dataStream);
        
        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        featureOfInterest.setName("fos1");
        featureOfInterest.setObservations(observationBatch);
        featureOfInterest.setDescription("fos1desc");
        obs.setFeatureOfInterest(featureOfInterest);
        */
		
		/*Datastream dataStream1 = new Datastream("ds1", "ds1", "type", new UnitOfMeasurement("q", "q", "q"));
		Set<DataArrayValue.Property> properties = new HashSet<>();
		properties.add(DataArrayValue.Property.Result);
		properties.add(DataArrayValue.Property.PhenomenonTime);
		DataArrayValue dav1 = new DataArrayValue(dataStream1, properties);
		dav1.addObservation(obs);*/
        
        // Create the dataArray associated to the datastream
        /*final DataArrayValue observations = new DataArrayValue(dataStream, properties);
        observations.addObservation(obs);
        
        final DataArrayDocument dataArrayDocument = new DataArrayDocument();
        dataArrayDocument.addDataArrayValue(observations);*/
        /*
        DataArrayDocument dad = new DataArrayDocument();
        dad.addDataArrayValue(dav1);

        try {
        	List<String> ids = service.create(dad);
        	System.out.println(ids.size());
        	for(String id : ids) {
        		System.out.println(id);
        	}
        } catch (ServiceFailureException e) {
            e.printStackTrace();
        }*/
        
        
        // ** Delete all
        service.observations().query().delete();
        service.datastreams().query().delete();
        service.things().query().delete();
        service.sensors().query().delete();
        
        EntityList<Observation> receivedObservations = service.observations().query().list();
        System.out.println("Received " + receivedObservations.size() + " observations");
        /*for(Observation o : receivedObservations) {
        	System.out.println("Observation " + o.getResult() + " : " + o.getDatastream().getThing().getLocations());
        }*/
		
		
	}
}
