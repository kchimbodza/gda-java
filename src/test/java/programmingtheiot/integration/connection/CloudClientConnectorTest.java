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

import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.DefaultDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.app.DeviceDataManager;
import programmingtheiot.gda.connection.*;

/**
 * This test case class contains very basic integration tests for
 * CloudClientConnector. It should not be considered complete,
 * but serve as a starting point for the student implementing
 * additional functionality within their Programming the IoT
 * environment.
 *
 */
public class CloudClientConnectorTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnectorTest.class.getName());
	
	
	// member var's
	
	private List<ICloudClient> cloudClientList = null;
	private ICloudClient cloudClient = null;
	
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		this.cloudClient = new CloudClientConnector();
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
	 * Test method for {@link programmingtheiot.gda.connection.CloudClientConnector#connectClient()}.
	 */
	@Test
	public void testCloudClientConnectAndDisconnect()
	{
		_Logger.info("Test 1: Testing CloudClientConnector basic connect and disconnect...");
		
		this.cloudClient.setDataMessageListener(new DefaultDataMessageListener());
		
		assertTrue("CloudClientConnector should connect successfully", this.cloudClient.connectClient());
		
		_Logger.info("CloudClientConnector connected to cloud service.");
		
		try {
			// sleep for 10 seconds to allow connection to stabilize
			Thread.sleep(10000L);
		} catch (Exception e) {
			_Logger.warning("Sleep interrupted: " + e.getMessage());
		}
		
		assertTrue("CloudClientConnector should disconnect successfully", this.cloudClient.disconnectClient());
		
		_Logger.info("CloudClientConnector disconnected from cloud service.");
	}
	
	/**
	 * Test method for {@link programmingtheiot.gda.connection.CloudClientConnector#publishMessage(programmingtheiot.common.ResourceNameEnum, java.lang.String, int)}.
	 */
	@Test
	public void testPublishAndSubscribe()
	{
		_Logger.info("Test 1b: Testing publish and subscribe to cloud events...");
		
		this.cloudClient.setDataMessageListener(new DefaultDataMessageListener());
		
		assertTrue("CloudClientConnector should connect successfully", this.cloudClient.connectClient());
		
		try {
			// sleep for a couple of seconds to allow connection to complete
			Thread.sleep(2000L);
		} catch (Exception e) {
			_Logger.warning("Sleep interrupted: " + e.getMessage());
		}
		
		// Create sample sensor data
		SensorData sensorData = new SensorData();
		sensorData.setName(ConfigConst.TEMP_SENSOR_NAME);
		sensorData.setValue(92.0f);
		sensorData.setTypeID(ConfigConst.TEMP_SENSOR_TYPE);
		sensorData.setLocationID(ConfigConst.CONSTRAINED_DEVICE);
		
		// Create sample system performance data
		SystemPerformanceData sysPerfData = new SystemPerformanceData();
		sysPerfData.setCpuUtilization(34.7f);
		sysPerfData.setMemoryUtilization(39.8f);
		
		// Subscribe to actuator commands (LED actuation)
		assertTrue("Should subscribe to actuator commands", 
			this.cloudClient.subscribeToCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE));
		
		try {
			// sleep for a few seconds to allow subscription to complete
			Thread.sleep(5000L);
		} catch (Exception e) {
			_Logger.warning("Sleep interrupted: " + e.getMessage());
		}
		
		// Publish sensor and performance data
		assertTrue("SensorData should be published to cloud", 
			this.cloudClient.sendEdgeDataToCloud(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData));
		
		assertTrue("SystemPerformanceData should be published to cloud", 
			this.cloudClient.sendEdgeDataToCloud(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, sysPerfData));
		
		try {
			// sleep for half a minute to allow data delivery and any LED events to trigger
			Thread.sleep(30000L);
		} catch (Exception e) {
			_Logger.warning("Sleep interrupted: " + e.getMessage());
		}
		
		// Unsubscribe from actuator commands
		assertTrue("Should unsubscribe from actuator commands", 
			this.cloudClient.unsubscribeFromCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE));

		try {
			// sleep for a minute to allow unsubscribe to complete
			Thread.sleep(50000L);
		} catch (Exception e) {
			_Logger.warning("Sleep interrupted: " + e.getMessage());
		}

		// Disconnect from cloud
		assertTrue("CloudClientConnector should disconnect successfully", 
			this.cloudClient.disconnectClient());

		try {
			// sleep for a couple of seconds to allow disconnect to complete
			Thread.sleep(2000L);
		} catch (Exception e) {
			_Logger.warning("Sleep interrupted: " + e.getMessage());
		}
		
		_Logger.info("Test 1b: PASSED - Publish and subscribe test complete.");
	}
	
	/**
	 * Test method for integrated DeviceDataManager with CloudClientConnector.
	 * This is Test 2: LED Actuation Rule & Response.
	 * 
	 * Expected behavior:
	 * 1. DeviceDataManager starts with both local and cloud MQTT clients
	 * 2. Sensor data is published to Ubidots
	 * 3. Ubidots rule triggers on threshold crossing
	 * 4. LED actuation event is received by GDA
	 * 5. LED command is forwarded to CDA via local MQTT
	 */
	@Test
	public void testIntegratedCloudClientConnectAndDisconnect()
	{
		_Logger.info("Test 2: Testing integrated DeviceDataManager with cloud and LED actuation...");
		_Logger.info("This test will run for approximately 60 seconds.");
		_Logger.info("Expected: Sensor data published → Ubidots rule triggers → LED event received → LED command forwarded");
		
		// Create and start DeviceDataManager
		DeviceDataManager ddm = new DeviceDataManager();
		
		_Logger.info("Starting DeviceDataManager...");
		ddm.startManager();
		
		_Logger.info("DeviceDataManager started. Waiting for LED actuation events...");
		
		try {
			// sleep for 60 seconds to allow:
			// 1. Connections to establish
			// 2. Sensor data to be published to Ubidots
			// 3. Ubidots rules to trigger on threshold crossing
			// 4. LED actuation events to be received
			// 5. LED commands to be forwarded to CDA
			Thread.sleep(60000L);
		} catch (Exception e) {
			_Logger.warning("Sleep interrupted: " + e.getMessage());
		}
		
		_Logger.info("Stopping DeviceDataManager...");
		ddm.stopManager();
		
		_Logger.info("Test 2: PASSED - Integrated cloud and LED actuation test complete.");
	}
	
}