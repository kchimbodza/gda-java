/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */ 

package programmingtheiot.integration.connection;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.connection.MqttClientConnector;

/**
 * This test case class contains performance benchmarking tests for
 * MqttClientConnector using different QoS levels.
 * Tests measure the time to publish 10,000 messages at each QoS level.
 * 
 * IMPORTANT NOTE: This test expects MqttClientConnector to be
 * configured using the synchronous MqttClient (NOT MqttAsyncClient).
 *
 */
public class MqttClientPerformanceTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientPerformanceTest.class.getName());
	
	public static final int MAX_TEST_RUNS = 10000;
	
	// member var's
	
	private MqttClientConnector mqttClient = null;
	
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		ConfigUtil.getInstance();
		this.mqttClient = new MqttClientConnector();
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
	}
	
	// test methods
	
	/**
	 * Test basic connect/disconnect performance.
	 * This establishes baseline overhead for connection operations.
	 */
	@Test
	public void testConnectAndDisconnect()
	{
		long startMillis = System.currentTimeMillis();
		
		assertTrue(this.mqttClient.connectClient());
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertTrue(this.mqttClient.disconnectClient());
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long endMillis = System.currentTimeMillis();
		long elapsedMillis = endMillis - startMillis;
		
		_Logger.info("Connect and Disconnect: " + elapsedMillis + " ms");
	}
	
	/**
	 * Test publish performance with QoS 0 (Fire and Forget).
	 */
	@Test
	public void testPublishQoS0()
	{
		execTestPublish(MAX_TEST_RUNS, 0);
	}
	
	/**
	 * Test publish performance with QoS 1 (At Least Once).
	 */
	@Test
	public void testPublishQoS1()
	{
		execTestPublish(MAX_TEST_RUNS, 1);
	}
	
	/**
	 * Test publish performance with QoS 2 (Exactly Once).
	 */
	@Test
	public void testPublishQoS2()
	{
		execTestPublish(MAX_TEST_RUNS, 2);
	}
	
	// private methods
	
	/**
	 * Execute publish performance test.
	 * 
	 * @param maxTestRuns Number of messages to publish
	 * @param qos QoS level (0, 1, or 2)
	 */
	private void execTestPublish(int maxTestRuns, int qos)
	{
		assertTrue(this.mqttClient.connectClient());
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		SensorData sensorData = new SensorData();
		String payload = DataUtil.getInstance().sensorDataToJson(sensorData);
		
		long startMillis = System.currentTimeMillis();
		
		for (int sequenceNo = 0; sequenceNo < maxTestRuns; sequenceNo++) {
			this.mqttClient.publishMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, payload, qos);
		}
		
		long endMillis = System.currentTimeMillis();
		long elapsedMillis = endMillis - startMillis;
		
		assertTrue(this.mqttClient.disconnectClient());
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		_Logger.info("Publish message - QoS " + qos + " [" + maxTestRuns + "]: " + elapsedMillis + " ms");
	}
	
}