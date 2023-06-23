package com.bbn.tak.ml.sensor;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.builder.ThingBuilder;
import de.fraunhofer.iosb.ilt.sta.model.ext.EntityList;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;

public class SensorThingsServerTest {
	@Test
	public void testServerCRUD() throws Exception {
		URL serviceEndpoint = new URL("http://127.0.0.1:8080/v1.0/");
		SensorThingsService service = new SensorThingsService(serviceEndpoint);
		
		Thing thing1 = ThingBuilder.builder()
			    .name("Thingything")
			    .description("I'm a thing!")
			    .build();
		service.create(thing1);
		
		System.out.println(thing1.getId());

		Thing thing2 =  service.things().find(thing1.getId());
		
		Assert.assertEquals("Thing2 not the same as thing1", thing1, thing2);
		System.out.println(thing2.getId());
		
		thing2.setDescription("Very much a thing");
		service.update(thing2);
		
		Thing thing3 = service.things().find(thing1.getId());
		
		Assert.assertNotEquals("Thing3 the same as thing1, but shouldn't be", thing1, thing3);
		Assert.assertEquals("Thing3 not the same as thing2", thing2, thing3);
		
		service.delete(thing3);
		
		Thing thing4 = null;
		try {
			thing4 =  service.things().find(thing1.getId());
		} catch (Exception e) {
			// ** should fall in here, and leave thing4 as null
		} finally {
			Assert.assertNull("Thing4 should be null", thing4);
		}
		
		EntityList<Thing> things = service.things()
                .query()
                .count()
                .orderBy("description")
                .select("name","id","description")
                .filter("")
                .list();

		// ** clear out any other things in the DB
		for (Thing thing : things) {
			System.out.println(thing.getId());
			service.delete(thing);
		}
		
		
		for (Sensor sensor: service.sensors().query().list()) {
			System.out.println(sensor.getId());
			service.delete(sensor);
		}
	}
}
