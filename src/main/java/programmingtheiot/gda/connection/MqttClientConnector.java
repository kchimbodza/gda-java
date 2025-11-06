/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */
package programmingtheiot.gda.connection;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.common.SimpleCertManagementUtil;

/**
 * MQTT client connector for publishing and subscribing to topics.
 * Implements both IPubSubClient and MqttCallbackExtended interfaces.
 */
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// params
	
	private boolean useAsyncClient = false;

	private MqttClient           mqttClient = null;
	private MqttConnectOptions   connOpts = null;
	private MemoryPersistence    persistence = null;
	private IDataMessageListener dataMsgListener = null;
	private IConnectionListener  connListener = null;

	private String  clientID = null;
	private String  brokerAddr = null;
	private String  host = ConfigConst.DEFAULT_HOST;
	private String  protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
	private int     port = ConfigConst.DEFAULT_MQTT_PORT;
	private int     brokerKeepAlive = ConfigConst.DEFAULT_KEEP_ALIVE;
	
	// constructors
	
	/**
	 * Default constructor that initializes MQTT client parameters
	 * using the [Mqtt.GatewayService] section.
	 */
	public MqttClientConnector()
	{
		this(ConfigConst.MQTT_GATEWAY_SERVICE);
	}
	
	/**
	 * Constructor that accepts a boolean flag for cloud gateway configuration.
	 * 
	 * @param useCloudGatewayConfig If true, uses Cloud.GatewayService; otherwise uses Mqtt.GatewayService
	 */
	public MqttClientConnector(boolean useCloudGatewayConfig)
	{
		this(useCloudGatewayConfig ? ConfigConst.CLOUD_GATEWAY_SERVICE : ConfigConst.MQTT_GATEWAY_SERVICE);
	}
	
	/**
	 * Constructor that initializes MQTT client parameters
	 * using a custom configuration section.
	 * 
	 * @param configSectionName The configuration section to use
	 */
	public MqttClientConnector(String configSectionName)
	{
		super();
		
		this.persistence = new MemoryPersistence();
		this.connOpts = new MqttConnectOptions();
		
		initClientParameters(configSectionName);
		
		this.clientID = MqttClient.generateClientId();
		
		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOpts.setCleanSession(false);
		this.connOpts.setAutomaticReconnect(true);
		
		this.brokerAddr = this.protocol + "://" + this.host + ":" + this.port;
	}
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		try {
			if (this.mqttClient == null) {
				this.mqttClient = new MqttClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
			}
			
			if (! this.mqttClient.isConnected()) {
				_Logger.info("MQTT client connecting to broker: " + this.brokerAddr);
				this.mqttClient.connect(this.connOpts);
				return true;
			} else {
				_Logger.warning("MQTT client already connected to broker: " + this.brokerAddr);
			}
		} catch (MqttException e) {
			_Logger.log(Level.SEVERE, "Failed to connect MQTT client to broker.", e);
		}
		
		return false;
	}
	
	@Override
	public boolean disconnectClient()
	{
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				_Logger.info("Disconnecting MQTT client from broker: " + this.brokerAddr);
				this.mqttClient.disconnect();
				return true;
			}
		} catch (MqttException e) {
			_Logger.log(Level.SEVERE, "Failed to disconnect MQTT client from broker.", e);
		}
		
		return false;
	}
	
	public boolean isConnected()
	{
		if (this.mqttClient != null) {
			return this.mqttClient.isConnected();
		}
		
		return false;
	}
	
	@Override
	public boolean publishMessage(ResourceNameEnum resource, String msg, int qos)
	{
		if (resource != null) {
			String topicName = resource.getResourceName();
			return publishMessage(topicName, msg.getBytes(), qos);
		}
		
		return false;
	}
	
	/**
	 * Protected method to publish message with byte payload to a specific topic.
	 * 
	 * @param topicName The topic name
	 * @param payload The message payload as bytes
	 * @param qos Quality of Service level
	 * @return True if successful, false otherwise
	 */
	protected boolean publishMessage(String topicName, byte[] payload, int qos)
	{
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				this.mqttClient.publish(topicName, payload, qos, false);
				return true;
			} else {
				_Logger.severe("Failed to publish message to topic: " + topicName + " - Client is not connected (32104)");
				return false;
			}
		} catch (MqttException e) {
			_Logger.log(Level.SEVERE, "Failed to publish message to topic: " + topicName, e);
			return false;
		}
	}
	
	@Override
	public boolean subscribeToTopic(ResourceNameEnum resource, int qos)
	{
		if (resource != null) {
			String topicName = resource.getResourceName();
			return subscribeToTopic(topicName, qos);
		}
		
		return false;
	}
	
	/**
	 * Protected method to subscribe to a specific topic.
	 * 
	 * @param topicName The topic name
	 * @param qos Quality of Service level
	 * @return True if successful, false otherwise
	 */
	protected boolean subscribeToTopic(String topicName, int qos)
	{
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				this.mqttClient.subscribe(topicName, qos);
				_Logger.info("Successfully subscribed to topic: " + topicName);
				return true;
			} else {
				_Logger.warning("MQTT client not connected. Cannot subscribe to topic: " + topicName);
				return false;
			}
		} catch (MqttException e) {
			_Logger.log(Level.WARNING, "Failed to subscribe to topic: " + topicName, e);
			return false;
		}
	}
	
	/**
	 * Protected method to subscribe to a topic with custom message listener.
	 * 
	 * @param topicName The topic name
	 * @param qos Quality of Service level
	 * @param listener Custom message listener
	 * @return True if successful, false otherwise
	 */
	protected boolean subscribeToTopic(String topicName, int qos, IMqttMessageListener listener)
	{
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				this.mqttClient.subscribe(topicName, qos, listener);
				_Logger.info("Successfully subscribed to topic: " + topicName);
				return true;
			} else {
				_Logger.warning("MQTT client not connected. Cannot subscribe to topic: " + topicName);
				return false;
			}
		} catch (MqttException e) {
			_Logger.log(Level.WARNING, "Failed to subscribe to topic: " + topicName, e);
			return false;
		}
	}
	
	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum resource)
	{
		if (resource != null) {
			String topicName = resource.getResourceName();
			return unsubscribeFromTopic(topicName);
		}
		
		return false;
	}
	
	/**
	 * Protected method to unsubscribe from a specific topic.
	 * 
	 * @param topicName The topic name
	 * @return True if successful, false otherwise
	 */
	protected boolean unsubscribeFromTopic(String topicName)
	{
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				this.mqttClient.unsubscribe(topicName);
				_Logger.info("Successfully unsubscribed from topic: " + topicName);
				return true;
			} else {
				_Logger.warning("MQTT client not connected. Cannot unsubscribe from topic: " + topicName);
				return false;
			}
		} catch (MqttException e) {
			_Logger.log(Level.WARNING, "Failed to unsubscribe from topic: " + topicName, e);
			return false;
		}
	}
	
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		if (listener != null) {
			_Logger.info("Setting connection listener.");
			this.connListener = listener;
			return true;
		} else {
			_Logger.warning("No connection listener specified. Ignoring.");
		}
		
		return false;
	}
	
	// callbacks
	
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("MQTT connection successful (is reconnect = " + reconnect + "). Broker: " + serverURI);
		
		// Notify connection listener if set
		if (this.connListener != null) {
			this.connListener.onConnect();
		}
	}
	
	@Override
	public void connectionLost(Throwable cause)
	{
		_Logger.log(Level.WARNING, "MQTT connection lost.", cause);
		
		// Notify connection listener of disconnect
		if (this.connListener != null) {
			this.connListener.onDisconnect();
		}
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{
		//_Logger.info("Delivered MQTT message with ID: " + token.getMessageId());
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception
	{
		_Logger.info("MQTT message arrived on topic: '" + topic + "'");
		
		if (this.dataMsgListener != null) {
			String payload = new String(message.getPayload());
			
			// Create a ResourceNameEnum for the topic
			ResourceNameEnum topicEnum = ResourceNameEnum.getEnumFromValue(topic);
			
			if (topicEnum != null) {
				this.dataMsgListener.handleIncomingMessage(topicEnum, payload);
			} else {
				_Logger.warning("Unknown topic received: " + topic);
			}
		}
	}
	
	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters to be used for the connection.
	 * 
	 * @param configSectionName The name of the configuration section to use
	 */
	private void initClientParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.host = configUtil.getProperty(configSectionName, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		this.port = configUtil.getInteger(configSectionName, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
		this.brokerKeepAlive = configUtil.getInteger(configSectionName, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
		
		boolean useSecurePort = configUtil.getBoolean(configSectionName, ConfigConst.ENABLE_CRYPT_KEY);
		
		if (useSecurePort) {
			int securePort = configUtil.getInteger(configSectionName, ConfigConst.SECURE_PORT_KEY, ConfigConst.DEFAULT_MQTT_SECURE_PORT);
			this.port = securePort;
			this.protocol = "ssl";
		}
		
		this.useAsyncClient = configUtil.getBoolean(configSectionName, "useAsyncClient");
		
		initSecureConnectionParameters(configSectionName);
		initCredentialConnectionParameters(configSectionName);
		
		this.brokerAddr = this.protocol + "://" + this.host + ":" + this.port;
		_Logger.info("Using URL for broker conn: " + this.brokerAddr);
	}
	
	/**
	 * Called by initClientParameters() to enable encryption and load certificates.
	 * 
	 * @param configSectionName The name of the configuration section
	 */
	private void initSecureConnectionParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		String certFile = configUtil.getProperty(configSectionName, ConfigConst.CERT_FILE_KEY);
		boolean enableCrypt = configUtil.getBoolean(configSectionName, ConfigConst.ENABLE_CRYPT_KEY);
		
		if (enableCrypt) {
			_Logger.info("Configuring TLS...");
			
			if (certFile != null) {
				_Logger.info("PEM file valid. Using secure connection: " + certFile);
				
				SSLSocketFactory sslSocketFactory = SimpleCertManagementUtil.getInstance().loadCertificate(certFile);
				
				if (sslSocketFactory != null) {
					_Logger.info("Certificate / key file exists: " + certFile);
					this.connOpts.setSocketFactory(sslSocketFactory);
					_Logger.info("TLS enabled.");
				} else {
					_Logger.warning("Failed to load certificate. TLS not enabled.");
				}
			} else {
				_Logger.warning("Certificate file not specified. TLS will not be used.");
			}
		}
	}
	
	/**
	 * Called by initClientParameters() to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		String credFile = configUtil.getProperty(configSectionName, ConfigConst.CRED_FILE_KEY);
		boolean enableAuth = configUtil.getBoolean(configSectionName, ConfigConst.ENABLE_AUTH_KEY);
		
		if (enableAuth) {
			_Logger.info("Checking if credentials file exists and us loadable...");
			
			if (credFile != null) {
				try {
					Properties credentials = new Properties();
					FileInputStream fileInput = new FileInputStream(credFile);
					credentials.load(fileInput);
					fileInput.close();
					
					String userToken = credentials.getProperty(ConfigConst.USER_NAME_TOKEN_KEY);
					
					if (userToken != null && !userToken.isEmpty()) {
						this.connOpts.setUserName(userToken);
						this.connOpts.setPassword(userToken.toCharArray());
						
						_Logger.info("Credentials now set.");
					} else {
						_Logger.warning("User token not found in credentials file.");
					}
				} catch (Exception e) {
					_Logger.log(Level.WARNING, "Failed to load credentials from file: " + credFile, e);
				}
			} else {
				_Logger.warning("Credentials file not specified.");
			}
		}
	}
}