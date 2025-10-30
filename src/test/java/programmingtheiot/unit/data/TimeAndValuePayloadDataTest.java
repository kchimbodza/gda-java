/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */
package programmingtheiot.unit.data;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.TimeAndValuePayloadData;

/**
 * Unit tests for TimeAndValuePayloadData class.
 * 
 */
public class TimeAndValuePayloadDataTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(TimeAndValuePayloadDataTest.class.getName());
	
	public static final float DEFAULT_TEST_VALUE = 42.5f;
	public static final float DEFAULT_TEST_CPU = 55.0f;
	
	// member var's
	
	
	// test setup methods
	
	@Before
	public void setUp() throws Exception
	{
	}
	
	@After
	public void tearDown() throws Exception
	{
	}
	
	
	// test methods
	
	@Test
	public void testDefaultConstructor()
	{
		TimeAndValuePayloadData data = new TimeAndValuePayloadData();
		
		assertNotNull(data);
		assertTrue(data.getValue() == ConfigConst.DEFAULT_VAL);
		assertTrue(data.getTimeStamp() > 0);
	}
	
	@Test
	public void testConstructorWithSensorData()
	{
		SensorData sensorData = new SensorData();
		sensorData.setValue(DEFAULT_TEST_VALUE);
		
		TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(sensorData);
		
		assertNotNull(tvData);
		assertTrue(tvData.getValue() == DEFAULT_TEST_VALUE);
		assertTrue(tvData.getTimeStamp() > 0);
	}
	
	@Test
	public void testConstructorWithActuatorData()
	{
		ActuatorData actuatorData = new ActuatorData();
		actuatorData.setValue(DEFAULT_TEST_VALUE);
		
		TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(actuatorData);
		
		assertNotNull(tvData);
		assertTrue(tvData.getValue() == DEFAULT_TEST_VALUE);
		assertTrue(tvData.getTimeStamp() > 0);
	}
	
	@Test
	public void testConstructorWithSystemPerformanceData()
	{
		SystemPerformanceData perfData = new SystemPerformanceData();
		perfData.setCpuUtilization(DEFAULT_TEST_CPU);
		
		TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(perfData);
		
		assertNotNull(tvData);
		assertTrue(tvData.getValue() == DEFAULT_TEST_CPU);
		assertTrue(tvData.getTimeStamp() > 0);
	}
	
	@Test
	public void testGetSetValue()
	{
		TimeAndValuePayloadData data = new TimeAndValuePayloadData();
		
		data.setValue(12.34f);
		assertTrue(data.getValue() == 12.34f);
		
		data.setValue(56.78f);
		assertTrue(data.getValue() == 56.78f);
	}
	
	@Test
	public void testGetSetTimeStamp()
	{
		TimeAndValuePayloadData data = new TimeAndValuePayloadData();
		
		long timestamp1 = System.currentTimeMillis();
		data.setTimeStamp(timestamp1);
		assertTrue(data.getTimeStamp() == timestamp1);
		
		long timestamp2 = System.currentTimeMillis() + 1000;
		data.setTimeStamp(timestamp2);
		assertTrue(data.getTimeStamp() == timestamp2);
	}
	
	@Test
	public void testToString()
	{
		TimeAndValuePayloadData data = new TimeAndValuePayloadData();
		data.setValue(99.9f);
		
		String str = data.toString();
		
		assertNotNull(str);
		assertTrue(str.contains("TimeAndValuePayloadData"));
		assertTrue(str.contains("99.9"));
	}
	
	@Test
	public void testConstructorWithNullSensorData()
	{
		TimeAndValuePayloadData tvData = new TimeAndValuePayloadData((SensorData) null);
		
		assertNotNull(tvData);
		assertTrue(tvData.getValue() == ConfigConst.DEFAULT_VAL);
		assertTrue(tvData.getTimeStamp() > 0);
	}
	
	@Test
	public void testConstructorWithNullActuatorData()
	{
		TimeAndValuePayloadData tvData = new TimeAndValuePayloadData((ActuatorData) null);
		
		assertNotNull(tvData);
		assertTrue(tvData.getValue() == ConfigConst.DEFAULT_VAL);
		assertTrue(tvData.getTimeStamp() > 0);
	}
	
	@Test
	public void testConstructorWithNullSystemPerformanceData()
	{
		TimeAndValuePayloadData tvData = new TimeAndValuePayloadData((SystemPerformanceData) null);
		
		assertNotNull(tvData);
		assertTrue(tvData.getValue() == ConfigConst.DEFAULT_VAL);
		assertTrue(tvData.getTimeStamp() > 0);
	}
}