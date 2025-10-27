/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */

package programmingtheiot.integration.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.app.DeviceDataManager;

/**
 * Test case for DeviceDataManager humidity actuation logic.
 * Tests the threshold crossing detection and actuation event generation.
 * 
 * PIOT-GDA-10-003
 */
public class DeviceDataManagerSimpleCdaActuationTest
{
	private static final Logger _Logger = 
		Logger.getLogger(DeviceDataManagerSimpleCdaActuationTest.class.getName());
	
	private DeviceDataManager devDataMgr = null;
	
	@Before
	public void setUp()
	{
		this.devDataMgr = new DeviceDataManager();
	}
	
	@After
	public void tearDown()
	{
		if (this.devDataMgr != null) {
			this.devDataMgr.stopManager();
		}
	}
	
	@Test
	public void testSendActuationEventsToCda()
	{
		_Logger.info("Starting humidity threshold actuation test...");
		
		// Start the manager
		devDataMgr.startManager();
		
		ConfigUtil cfgUtil = ConfigUtil.getInstance();
		
		// Get configuration values
		float nominalVal = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
		float lowVal     = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
		float highVal    = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");
		int   delay      = cfgUtil.getInteger(ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");
		
		_Logger.info("Configuration loaded:");
		_Logger.info("  Nominal humidity: " + nominalVal + "%");
		_Logger.info("  Floor threshold: " + lowVal + "%");
		_Logger.info("  Ceiling threshold: " + highVal + "%");
		_Logger.info("  Time delay threshold: " + delay + " seconds");
		
		// Test Sequence 1: Test nominal -> low -> nominal -> high -> nominal
		_Logger.info("\n=== Test Sequence 1: Humidity Threshold Crossing ===");
		generateAndProcessHumiditySensorDataSequence(
			devDataMgr, nominalVal, lowVal, highVal, delay);
		
		_Logger.info("\nTest completed successfully.");
	}
	
	/**
	 * Generates and processes a sequence of humidity sensor readings
	 * to test threshold crossing logic.
	 * 
	 * @param ddm The DeviceDataManager instance
	 * @param nominalVal The nominal humidity value
	 * @param lowVal The low (floor) threshold value
	 * @param highVal The high (ceiling) threshold value
	 * @param delay The delay in seconds between readings
	 */
	private void generateAndProcessHumiditySensorDataSequence(
		DeviceDataManager ddm, float nominalVal, float lowVal, float highVal, int delay)
	{
		SensorData sd = new SensorData();
		sd.setName("Humidity Sensor");
		sd.setLocationID("constraineddevice001");
		sd.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		
		_Logger.info("\n--- Phase 1: Send nominal readings (no actuation) ---");
		
		// Send two nominal readings - no actuation should occur
		_Logger.info("Sending nominal reading 1: " + nominalVal + "%");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		_Logger.info("Sending nominal reading 2: " + nominalVal + "%");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		_Logger.info("\n--- Phase 2: Send LOW readings (should trigger ON actuation) ---");
		
		// Send low reading - triggers timer start
		_Logger.info("Sending LOW reading 1 (floor-2): " + (lowVal - 2) + "%");
		sd.setValue(lowVal - 2);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		_Logger.info("Waiting " + (delay + 1) + " seconds for time threshold...");
		waitForSeconds(delay + 1);
		
		// Send another low reading after time threshold - triggers ON command
		_Logger.info("Sending LOW reading 2 (floor-1): " + (lowVal - 1) + "%");
		sd.setValue(lowVal - 1);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		_Logger.info("Actuation ON event should have been sent to CDA");
		waitForSeconds(2);
		
		_Logger.info("\n--- Phase 3: Send readings returning to nominal ---");
		
		// Send reading between floor and ceiling (nominal range)
		_Logger.info("Sending reading returning to nominal range: " + (lowVal + 1) + "%");
		sd.setValue(lowVal + 1);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		_Logger.info("Waiting " + (delay + 1) + " seconds...");
		waitForSeconds(delay + 1);
		
		// Send nominal reading - triggers OFF command
		_Logger.info("Sending nominal reading: " + nominalVal + "%");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		_Logger.info("Actuation OFF event should have been sent to CDA");
		waitForSeconds(2);
		
		_Logger.info("\n--- Phase 4: Send HIGH readings (should trigger OFF actuation) ---");
		
		// Send high reading - triggers timer start
		_Logger.info("Sending HIGH reading 1 (ceiling+2): " + (highVal + 2) + "%");
		sd.setValue(highVal + 2);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		_Logger.info("Waiting " + (delay + 1) + " seconds for time threshold...");
		waitForSeconds(delay + 1);
		
		// Send another high reading after time threshold - triggers OFF command
		_Logger.info("Sending HIGH reading 2 (ceiling+1): " + (highVal + 1) + "%");
		sd.setValue(highVal + 1);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		_Logger.info("Actuation OFF event should have been sent to CDA");
		waitForSeconds(2);
	}
	
	/**
	 * Helper method to wait for a specified number of seconds.
	 * 
	 * @param seconds The number of seconds to wait
	 */
	private void waitForSeconds(int seconds)
	{
		try {
			_Logger.fine("Sleeping for " + seconds + " seconds...");
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			_Logger.log(Level.WARNING, "Thread interrupted during sleep", e);
		}
	}
}
