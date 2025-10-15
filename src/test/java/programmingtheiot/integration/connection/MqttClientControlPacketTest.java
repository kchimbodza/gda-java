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

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.*;
import programmingtheiot.gda.connection.*;

/**
 * Test class to generate all 14 MQTT 3.1.1 Control Packets for Wireshark analysis.
 * 
 * This test is designed to generate:
 * 1. CONNECT, 2. CONNACK, 3. PUBLISH, 4. PUBACK, 5. PUBREC, 
 * 6. PUBREL, 7. PUBCOMP, 8. SUBSCRIBE, 9. SUBACK, 
 * 10. UNSUBSCRIBE, 11. UNSUBACK, 12. PINGREQ, 13. PINGRESP, 14. DISCONNECT
 */
public class MqttClientControlPacketTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientControlPacketTest.class.getName());
	
	
	// member var's
	
	private MqttClientConnector mqttClient = null;
	
	
	// test setup methods
	
	@Before
	public void setUp() throws Exception
	{
		this.mqttClient = new MqttClientConnector();
	}
	
	@After
	public void tearDown() throws Exception
	{
		// Ensure clean disconnect
		if (this.mqttClient != null) {
			try {
				this.mqttClient.disconnectClient();
			} catch (Exception e) {
				// Ignore
			}
		}
	}
	
	// test methods
	
	/**
	 * Comprehensive test to generate all 14 MQTT 3.1.1 Control Packets in sequence.
	 * 
	 * Packet Types Generated:
	 * 1. CONNECT - Client connection request
	 * 2. CONNACK - Server connection acknowledgment
	 * 3. PUBLISH - Publish message (QoS 0, 1, 2)
	 * 4. PUBACK - Publish acknowledgment (QoS 1)
	 * 5. PUBREC - Publish received (QoS 2, part 1)
	 * 6. PUBREL - Publish release (QoS 2, part 2)
	 * 7. PUBCOMP - Publish complete (QoS 2, part 3)
	 * 8. SUBSCRIBE - Client subscribe request
	 * 9. SUBACK - Server subscribe acknowledgment
	 * 10. UNSUBSCRIBE - Client unsubscribe request
	 * 11. UNSUBACK - Server unsubscribe acknowledgment
	 * 12. PINGREQ - Ping request
	 * 13. PINGRESP - Ping response
	 * 14. DISCONNECT - Client disconnect
	 */
	@Test
	public void testAllMqttControlPackets()
	{
		_Logger.info("\n" + "=".repeat(70));
		_Logger.info("STARTING COMPREHENSIVE MQTT CONTROL PACKET TEST");
		_Logger.info("=".repeat(70));
		
		try {
			// Prepare test data
			ActuatorData actuatorData = new ActuatorData();
			actuatorData.setCommand(5);
			actuatorData.setName("TestActuator");
			
			SensorData sensorData = new SensorData();
			sensorData.setValue(25.5f);
			sensorData.setName("TestSensor");
			
			SystemPerformanceData sysPerfData = new SystemPerformanceData();
			
			DataUtil dataUtil = DataUtil.getInstance();
			String actuatorPayload = dataUtil.actuatorDataToJson(actuatorData);
			String sensorPayload = dataUtil.sensorDataToJson(sensorData);
			String sysPerfPayload = dataUtil.systemPerformanceDataToJson(sysPerfData);
			
			// =====================================================================
			// STEP 1: CONNECT and CONNACK
			// =====================================================================
			_Logger.info("\n>>> STEP 1: Generating CONNECT and CONNACK packets");
			this.mqttClient.connectClient();
			Thread.sleep(3000);
			_Logger.info("✓ CONNECT and CONNACK packets should be captured");
			
			// =====================================================================
			// STEP 2: QoS 0 - SUBSCRIBE, SUBACK, PUBLISH, UNSUBSCRIBE, UNSUBACK
			// =====================================================================
			_Logger.info("\n>>> STEP 2: Testing QoS 0 (PUBLISH only)");
			
			// SUBSCRIBE and SUBACK
			_Logger.info("Subscribing to sensor topic (QoS 0)...");
			this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, 0);
			Thread.sleep(2000);
			_Logger.info("✓ SUBSCRIBE and SUBACK packets should be captured");
			
			// PUBLISH (QoS 0 - no acknowledgment)
			_Logger.info("Publishing to sensor topic (QoS 0)...");
			this.mqttClient.publishMessage(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, sensorPayload, 0);
			Thread.sleep(3000);
			_Logger.info("✓ PUBLISH (QoS 0) packet should be captured");
			
			// UNSUBSCRIBE and UNSUBACK
			_Logger.info("Unsubscribing from sensor topic...");
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
			Thread.sleep(2000);
			_Logger.info("✓ UNSUBSCRIBE and UNSUBACK packets should be captured");
			
			// =====================================================================
			// STEP 3: QoS 1 - SUBSCRIBE, SUBACK, PUBLISH, PUBACK, UNSUBSCRIBE, UNSUBACK
			// =====================================================================
			_Logger.info("\n>>> STEP 3: Testing QoS 1 (PUBLISH + PUBACK)");
			
			// SUBSCRIBE and SUBACK (QoS 1)
			_Logger.info("Subscribing to actuator topic (QoS 1)...");
			this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, 1);
			Thread.sleep(2000);
			_Logger.info("✓ SUBSCRIBE and SUBACK packets should be captured");
			
			// PUBLISH and PUBACK (QoS 1)
			_Logger.info("Publishing to actuator topic (QoS 1)...");
			this.mqttClient.publishMessage(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, actuatorPayload, 1);
			Thread.sleep(3000);
			_Logger.info("✓ PUBLISH and PUBACK packets should be captured");
			
			// UNSUBSCRIBE and UNSUBACK
			_Logger.info("Unsubscribing from actuator topic...");
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE);
			Thread.sleep(2000);
			_Logger.info("✓ UNSUBSCRIBE and UNSUBACK packets should be captured");
			
			// =====================================================================
			// STEP 4: QoS 2 - SUBSCRIBE, SUBACK, PUBLISH, PUBREC, PUBREL, PUBCOMP
			// =====================================================================
			_Logger.info("\n>>> STEP 4: Testing QoS 2 (PUBLISH + PUBREC + PUBREL + PUBCOMP)");
			
			// SUBSCRIBE and SUBACK (QoS 2)
			_Logger.info("Subscribing to system performance topic (QoS 2)...");
			this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE, 2);
			Thread.sleep(2000);
			_Logger.info("✓ SUBSCRIBE and SUBACK packets should be captured");
			
			// PUBLISH, PUBREC, PUBREL, PUBCOMP (QoS 2 four-way handshake)
			_Logger.info("Publishing to system performance topic (QoS 2)...");
			this.mqttClient.publishMessage(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE, sysPerfPayload, 2);
			Thread.sleep(5000);  // QoS 2 requires more time for 4-way handshake
			_Logger.info("✓ PUBLISH, PUBREC, PUBREL, and PUBCOMP packets should be captured");
			
			// UNSUBSCRIBE and UNSUBACK
			_Logger.info("Unsubscribing from system performance topic...");
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE);
			Thread.sleep(2000);
			_Logger.info("✓ UNSUBSCRIBE and UNSUBACK packets should be captured");
			
			// =====================================================================
			// STEP 5: PINGREQ and PINGRESP (KeepAlive)
			// =====================================================================
			_Logger.info("\n>>> STEP 5: Waiting for KeepAlive PING packets");
			_Logger.info("Note: PINGREQ/PINGRESP will be generated automatically by the client");
			_Logger.info("      based on the keepAlive interval (default: 60 seconds)");
			_Logger.info("Waiting 70 seconds to ensure PING packets are generated...");
			
			// Wait for keepalive to trigger (default is usually 60 seconds)
			for (int i = 0; i < 7; i++) {
				Thread.sleep(10000);
				_Logger.info("  ... waiting (" + ((i + 1) * 10) + "/70 seconds)");
			}
			
			_Logger.info("✓ PINGREQ and PINGRESP packets should be captured");
			
			// =====================================================================
			// STEP 6: DISCONNECT
			// =====================================================================
			_Logger.info("\n>>> STEP 6: Generating DISCONNECT packet");
			this.mqttClient.disconnectClient();
			Thread.sleep(2000);
			_Logger.info("✓ DISCONNECT packet should be captured");
			
			// =====================================================================
			// TEST COMPLETE
			// =====================================================================
			_Logger.info("\n" + "=".repeat(70));
			_Logger.info("ALL 14 MQTT CONTROL PACKETS SHOULD NOW BE CAPTURED");
			_Logger.info("=".repeat(70));
			_Logger.info("\nPacket Summary:");
			_Logger.info("  1. CONNECT    ✓");
			_Logger.info("  2. CONNACK    ✓");
			_Logger.info("  3. PUBLISH    ✓ (QoS 0, 1, 2)");
			_Logger.info("  4. PUBACK     ✓ (QoS 1)");
			_Logger.info("  5. PUBREC     ✓ (QoS 2)");
			_Logger.info("  6. PUBREL     ✓ (QoS 2)");
			_Logger.info("  7. PUBCOMP    ✓ (QoS 2)");
			_Logger.info("  8. SUBSCRIBE  ✓ (QoS 0, 1, 2)");
			_Logger.info("  9. SUBACK     ✓ (QoS 0, 1, 2)");
			_Logger.info(" 10. UNSUBSCRIBE ✓");
			_Logger.info(" 11. UNSUBACK   ✓");
			_Logger.info(" 12. PINGREQ    ✓");
			_Logger.info(" 13. PINGRESP   ✓");
			_Logger.info(" 14. DISCONNECT ✓");
			_Logger.info("=".repeat(70) + "\n");
			
		} catch (Exception e) {
			_Logger.severe("Test failed with exception: " + e.getMessage());
			e.printStackTrace();
			fail("Exception occurred during test: " + e.getMessage());
		}
	}
	
	/**
	 * Legacy test method - kept for backwards compatibility
	 * Use testAllMqttControlPackets() instead for comprehensive packet capture
	 */
	@Test
	public void testConnectAndDisconnect()
	{
		_Logger.info("=== Testing Connect and Disconnect ===");
		
		try {
			// Connect (generates CONNECT and CONNACK)
			this.mqttClient.connectClient();
			Thread.sleep(3000);
			
			// Disconnect (generates DISCONNECT)
			this.mqttClient.disconnectClient();
			Thread.sleep(2000);
			
			_Logger.info("✓ Connect and Disconnect test completed");
		} catch (Exception e) {
			_Logger.severe("Test failed: " + e.getMessage());
			fail("Exception occurred: " + e.getMessage());
		}
	}
	
	/**
	 * Legacy test method - kept for backwards compatibility
	 * Use testAllMqttControlPackets() instead for comprehensive packet capture
	 */
	@Test
	public void testServerPing()
	{
		_Logger.info("=== Testing Server Ping (KeepAlive) ===");
		
		try {
			this.mqttClient.connectClient();
			
			_Logger.info("Waiting for keepalive ping (60+ seconds)...");
			Thread.sleep(70000);
			
			this.mqttClient.disconnectClient();
			Thread.sleep(2000);
			
			_Logger.info("✓ Server Ping test completed");
		} catch (Exception e) {
			_Logger.severe("Test failed: " + e.getMessage());
			fail("Exception occurred: " + e.getMessage());
		}
	}
	
	/**
	 * Legacy test method - kept for backwards compatibility
	 * Use testAllMqttControlPackets() instead for comprehensive packet capture
	 */
	@Test
	public void testPubSub()
	{
		_Logger.info("=== Testing Pub/Sub with All QoS Levels ===");
		
		try {
			this.mqttClient.connectClient();
			Thread.sleep(2000);
			
			// Test data
			ActuatorData actuatorData = new ActuatorData();
			actuatorData.setCommand(5);
			
			SensorData sensorData = new SensorData();
			sensorData.setValue(25.5f);
			
			SystemPerformanceData sysPerfData = new SystemPerformanceData();
			
			DataUtil dataUtil = DataUtil.getInstance();
			String actuatorPayload = dataUtil.actuatorDataToJson(actuatorData);
			String sensorPayload = dataUtil.sensorDataToJson(sensorData);
			String sysPerfPayload = dataUtil.systemPerformanceDataToJson(sysPerfData);
			
			// QoS 0
			_Logger.info("--- Testing QoS 0 ---");
			this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, 0);
			Thread.sleep(2000);
			this.mqttClient.publishMessage(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, sensorPayload, 0);
			Thread.sleep(3000);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
			Thread.sleep(2000);
			
			// QoS 1
			_Logger.info("--- Testing QoS 1 ---");
			this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, 1);
			Thread.sleep(2000);
			this.mqttClient.publishMessage(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, actuatorPayload, 1);
			Thread.sleep(3000);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE);
			Thread.sleep(2000);
			
			// QoS 2
			_Logger.info("--- Testing QoS 2 ---");
			this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE, 2);
			Thread.sleep(2000);
			this.mqttClient.publishMessage(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE, sysPerfPayload, 2);
			Thread.sleep(5000);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_MGMT_STATUS_MSG_RESOURCE);
			Thread.sleep(2000);
			
			this.mqttClient.disconnectClient();
			Thread.sleep(2000);
			
			_Logger.info("✓ Pub/Sub test completed");
		} catch (Exception e) {
			_Logger.severe("Test failed: " + e.getMessage());
			fail("Exception occurred: " + e.getMessage());
		}
	}
}