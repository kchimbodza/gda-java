/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */
package programmingtheiot.gda.connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * CloudClientConnector implements ICloudClient and IConnectionListener.
 * Delegates MQTT work to MqttClientConnector for communicating with cloud service providers.
 * Handles LED actuation events from cloud service via LedEnablementMessageListener.
 */
public class CloudClientConnector implements ICloudClient, IConnectionListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnector.class.getName());
	
	// params
	
	private String topicPrefix = "";
	private MqttClientConnector mqttClient = null;
	private IDataMessageListener dataMsgListener = null;
	
	// TODO: set to either 0 or 1, depending on which is preferred for your implementation
	private int qosLevel = 1;
	
	// constructors
	
	/**
	 * Default constructor that initializes CloudClientConnector with
	 * cloud service configuration from PiotConfig.props.
	 */
	public CloudClientConnector()
	{
		super();
		
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.topicPrefix =
			configUtil.getProperty(ConfigConst.CLOUD_GATEWAY_SERVICE, ConfigConst.BASE_TOPIC_KEY);
		
		if (this.topicPrefix == null) {
			this.topicPrefix = "/";
		} else {
			if (! this.topicPrefix.endsWith("/")) {
				this.topicPrefix += "/";
			}
		}
		
		_Logger.info("CloudClientConnector initialized. Topic Prefix: " + this.topicPrefix);
	}
	
	// public methods - ICloudClient implementation
	
	@Override
	public boolean connectClient()
	{
		if (this.mqttClient == null) {
			this.mqttClient = new MqttClientConnector(true);
			this.mqttClient.setConnectionListener(this);
			this.mqttClient.setDataMessageListener(this.dataMsgListener);
		}
		
		_Logger.info("Cloud client connecting to MQTT broker...");
		return this.mqttClient.connectClient();
	}
	
	@Override
	public boolean disconnectClient()
	{
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			_Logger.info("Cloud client disconnecting from MQTT broker...");
			return this.mqttClient.disconnectClient();
		}
		
		_Logger.warning("Cloud client is not connected. Ignoring disconnect request.");
		return false;
	}
	
	/**
	 * Checks if the cloud client is currently connected to the broker.
	 * 
	 * @return True if connected, False otherwise
	 */
	public boolean isConnected()
	{
		if (this.mqttClient != null) {
			return this.mqttClient.isConnected();
		}
		
		return false;
	}
	
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data)
	{
		if (resource != null && data != null) {
			String jsonData = DataUtil.getInstance().sensorDataToTimeAndValueJson(data);
			// Use data.getName() to create variable-specific topic
			String topicName = createTopicName(resource.getDeviceName(), data.getName());
			
			_Logger.fine("Publishing sensor data to cloud: " + topicName + " with value: " + data.getValue());
			
			return publishMessageToCloud(topicName, jsonData);
		}
		
		return false;
	}
	
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data)
	{
		if (resource != null && data != null) {
			String jsonData = DataUtil.getInstance().systemPerformanceDataToTimeAndValueJson(data);
			String topicName = createTopicName(resource);
			
			_Logger.fine("Publishing system performance data to cloud: " + topicName);
			
			return publishMessageToCloud(topicName, jsonData);
		}
		
		return false;
	}
	
	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, ActuatorData data)
	{
	    if (resource != null && data != null) {
	        String jsonData = DataUtil.getInstance().actuatorDataToTimeAndValueJson(data);
	        String topicName = createTopicName(resource);
	        
	        _Logger.fine("Publishing actuator data to cloud: " + topicName);
	        
	        return publishMessageToCloud(topicName, jsonData);
	    }
	    
	    return false;
	}
	
	@Override
	public boolean subscribeToCloudEvents(ResourceNameEnum resource)
	{
		if (resource != null) {
			String topicName = createTopicName(resource);
			
			_Logger.info("Subscribing to cloud events on topic: " + topicName);
			
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				return this.mqttClient.subscribeToTopic(topicName, this.qosLevel);
			}
		}
		
		return false;
	}
	
	@Override
	public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource)
	{
		if (resource != null) {
			String topicName = createTopicName(resource);
			
			_Logger.info("Unsubscribing from cloud events on topic: " + topicName);
			
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				return this.mqttClient.unsubscribeFromTopic(topicName);
			}
		}
		
		return false;
	}
	
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			
			if (this.mqttClient != null) {
				return this.mqttClient.setDataMessageListener(listener);
			}
			
			return true;
		}
		
		return false;
	}
	
	// public methods - IConnectionListener implementation
	
	@Override
	public void onConnect()
	{
		_Logger.info("Handling CSP subscriptions and device topic provisioninig...");
		
		// Create LED enablement message listener
		LedEnablementMessageListener ledListener = new LedEnablementMessageListener(this.dataMsgListener);
		
		// Topic may not exist yet, so create a 'response' actuation event with invalid value
		// This will create the relevant topic if it doesn't yet exist, which ensures
		// the message listener (if coded correctly) will log a message but ignore the
		// actuation command and NOT pass it onto the IDataMessageListener instance
		ActuatorData ad = new ActuatorData();
		ad.setAsResponse();
		ad.setName(ConfigConst.LED_ACTUATOR_NAME);
		ad.setValue((float) -1.0);
		
		String ledTopic = createTopicName(ledListener.getResource().getDeviceName(), ad.getName());
		
		String adJson = DataUtil.getInstance().actuatorDataToTimeAndValueJson(ad);
		
		this.publishMessageToCloud(ledTopic, adJson);
		
		if (this.mqttClient != null) {
			this.mqttClient.subscribeToTopic(ledTopic, this.qosLevel, ledListener);
		}
	}
	
	@Override
	public void onDisconnect()
	{
		_Logger.info("MQTT client disconnected. Nothing else to do.");
	}
	
	// private methods
	
	/**
	 * Publishes a message to the cloud broker.
	 * 
	 * @param topicName The full topic name
	 * @param payload The JSON payload
	 * @return True if successful, false otherwise
	 */
	private boolean publishMessageToCloud(String topicName, String payload)
	{
		try {
			_Logger.finest("Publishing payload value(s) to CSP: " + topicName);
			
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				this.mqttClient.publishMessage(topicName, payload.getBytes(), this.qosLevel);
				return true;
			}
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to publish message to CSP: " + topicName, e);
		}
		
		return false;
	}
	
	/**
	 * Creates a cloud topic name from a ResourceNameEnum.
	 * Format: {topicPrefix}{deviceName}/{resourceType}
	 * 
	 * @param resource The ResourceNameEnum
	 * @return The formatted cloud topic name
	 */
	private String createTopicName(ResourceNameEnum resource)
	{
		return createTopicName(resource.getDeviceName(), resource.getResourceType());
	}
	
	/**
	 * Creates a cloud topic name from resource and item name.
	 * Format: {createTopicName(resource)}-{itemName}
	 * 
	 * @param resource The ResourceNameEnum
	 * @param itemName The item name
	 * @return The formatted cloud topic name
	 */
	private String createTopicName(ResourceNameEnum resource, String itemName)
	{
		return (createTopicName(resource) + "-" + itemName).toLowerCase();
	}
	
	/**
	 * Creates a cloud topic name from device name and resource type.
	 * Format: {topicPrefix}{deviceName}/{resourceType}
	 * 
	 * @param deviceName The device name
	 * @param resourceType The resource type
	 * @return The formatted cloud topic name
	 */
	private String createTopicName(String deviceName, String resourceType)
	{
		StringBuilder buf = new StringBuilder();
		
		if (deviceName != null && deviceName.trim().length() > 0) {
			buf.append(this.topicPrefix).append(deviceName);
		}
		
		if (resourceType != null && resourceType.trim().length() > 0) {
			buf.append('/').append(resourceType);
		}
		
		return buf.toString().toLowerCase();
	}
	
	// Inner class: LedEnablementMessageListener
	
	/**
	 * Inner class that handles incoming LED actuation events from cloud service.
	 * Listens on the LED topic and forwards ActuatorData to DeviceDataManager.
	 */
	private class LedEnablementMessageListener implements IMqttMessageListener
	{
		private IDataMessageListener dataMsgListener = null;
		
		private ResourceNameEnum resource = ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE;
		
		private int    typeID   = ConfigConst.LED_ACTUATOR_TYPE;
		private String itemName = ConfigConst.LED_ACTUATOR_NAME;
		
		/**
		 * Constructor that stores the data message listener.
		 * 
		 * @param dataMsgListener The listener to forward messages to
		 */
		LedEnablementMessageListener(IDataMessageListener dataMsgListener)
		{
			this.dataMsgListener = dataMsgListener;
		}
		
		/**
		 * Returns the resource enum associated with this listener.
		 * 
		 * @return The ResourceNameEnum
		 */
		public ResourceNameEnum getResource()
		{
			return this.resource;
		}
		
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception
		{
			try {
				String jsonData = new String(message.getPayload());
				
				ActuatorData actuatorData =
					DataUtil.getInstance().jsonToActuatorData(jsonData);
				
				actuatorData.setLocationID(ConfigConst.CONSTRAINED_DEVICE);
				actuatorData.setTypeID(this.typeID);
				actuatorData.setName(this.itemName);
				
				int val = (int) actuatorData.getValue();
				
				switch (val) {
					case ConfigConst.ON_COMMAND:
						_Logger.info("Received LED enablement message [ON].");
						actuatorData.setStateData("LED switching ON");
						break;
						
					case ConfigConst.OFF_COMMAND:
						_Logger.info("Received LED enablement message [OFF].");
						actuatorData.setStateData("LED switching OFF");
						break;
						
					default:
						return;
				}
				
				if (this.dataMsgListener != null) {
					jsonData = DataUtil.getInstance().actuatorDataToJson(actuatorData);
					
					this.dataMsgListener.handleIncomingMessage(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, jsonData);
				}
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to convert message payload to ActuatorData.", e);
			}
		}
	}
}