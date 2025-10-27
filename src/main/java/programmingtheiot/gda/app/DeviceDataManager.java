/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.app;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.SystemStateData;
import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;
import programmingtheiot.gda.system.SystemPerformanceManager;

/**
 * DeviceDataManager - the heart and soul of the GDA.
 * 
 * This class implements IDataMessageListener to handle callbacks from
 * various system components including sensors, actuators, and system
 * performance monitoring.
 */
public class DeviceDataManager implements IDataMessageListener
{
    // static
    private static final Logger _Logger = Logger.getLogger(DeviceDataManager.class.getName());
    
    // private var's
    private boolean enableMqttClient = true;
    private boolean enableCoapServer = false;
    private boolean enableCloudClient = false;
    private boolean enablePersistenceClient = false;
    private boolean enableSystemPerf = false;
    
    private MqttClientConnector mqttClient = null;
    private IPubSubClient cloudClient = null;
    private CoapServerGateway coapServer = null;
    private SystemPerformanceManager sysPerfMgr = null;
    private IActuatorDataListener actuatorDataListener = null;
    
    // ===== Humidity Threshold Crossing Variables (PIOT-GDA-10-003) =====
    
    private ActuatorData latestHumidifierActuatorData = null;
    private ActuatorData latestHumidifierActuatorResponse = null;
    private SensorData latestHumiditySensorData = null;
    private OffsetDateTime latestHumiditySensorTimeStamp = null;
    
    private boolean handleHumidityChangeOnDevice = false;
    private int lastKnownHumidifierCommand = ConfigConst.OFF_COMMAND;
    
    // Configuration parameters for humidity control
    private long humidityMaxTimePastThreshold = 300; // seconds
    private float nominalHumiditySetting = 40.0f;
    private float triggerHumidifierFloor = 30.0f;
    private float triggerHumidifierCeiling = 50.0f;
    
    // constructors
    public DeviceDataManager()
    {
        super();
        
        ConfigUtil configUtil = ConfigUtil.getInstance();
        
        this.enableMqttClient =
            configUtil.getBoolean(
                ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
        
        this.enableCoapServer =
            configUtil.getBoolean(
                ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
        
        this.enableCloudClient =
            configUtil.getBoolean(
                ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
        
        this.enablePersistenceClient =
            configUtil.getBoolean(
                ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
        
        // Load humidity threshold configuration (PIOT-GDA-10-003)
        this.handleHumidityChangeOnDevice = 
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, "handleHumidityChangeOnDevice");
        
        this.humidityMaxTimePastThreshold = 
            configUtil.getInteger(ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");
        
        this.nominalHumiditySetting = 
            configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
        
        this.triggerHumidifierFloor = 
            configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
        
        this.triggerHumidifierCeiling = 
            configUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");
        
        // Validate humidity threshold timing
        if (this.humidityMaxTimePastThreshold < 10 || this.humidityMaxTimePastThreshold > 7200) {
            _Logger.warning("Invalid humidityMaxTimePastThreshold value. Using default 300 seconds.");
            this.humidityMaxTimePastThreshold = 300;
        }
        
        initManager();
    }
    
    // public methods
    public void startManager()
    {
        _Logger.info("Starting DeviceDataManager...");
        
        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.startManager();
        }
        
        // UPDATED: implement this for Lab Module 7
        if (this.mqttClient != null && this.enableMqttClient) {
            if (this.mqttClient.connectClient()) {
                _Logger.info("Successfully connected MQTT client to broker.");
                
                // Add necessary subscriptions
                int qos = ConfigConst.DEFAULT_QOS;
                
                // TODO: check the return value for each and take appropriate action
                
                // IMPORTANT NOTE: The 'subscribeToTopic()' method calls shown
                // below will be moved to MqttClientConnector.connectComplete()
                // in Lab Module 10. For now, they can remain here.
                this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
            } else {
                _Logger.severe("Failed to connect MQTT client to broker.");
                
                // TODO: take appropriate action
            }
        }
        
        if (this.coapServer != null && this.enableCoapServer) {
        	if (this.coapServer.startServer()) {
                _Logger.info("CoAP server started.");
            } else {
                _Logger.severe("Failed to start CoAP server. Check log file for details.");
            }
        }
        
        if (this.cloudClient != null && this.enableCloudClient) {
            // TODO: implement this in Lab Module 10
            // boolean success = this.cloudClient.connectClient();
            // _Logger.info("Cloud client connection attempt: " + success);
        }
        
        _Logger.info("DeviceDataManager started successfully.");
    }
    
    public void stopManager()
    {
        _Logger.info("DeviceDataManager is stopping...");
        
        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.stopManager();
        }
        
        // UPDATED: implement this for Lab Module 7
        if (this.mqttClient != null) {
            // add necessary un-subscribes
            
            // TODO: check the return value for each and take appropriate action
            
            // NOTE: The unsubscribeFromTopic() method calls below should match with
            // the subscribeToTopic() method calls from startManager(). Also, the
            // unsubscribe logic below can be moved to MqttClientConnector's
            // disconnectClient() call PRIOR to actually disconnecting from
            // the MQTT broker.
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);
            
            if (this.mqttClient.disconnectClient()) {
                _Logger.info("Successfully disconnected MQTT client from broker.");
            } else {
                _Logger.severe("Failed to disconnect MQTT client from broker.");
                
                // TODO: take appropriate action
            }
        }
        
        if (this.coapServer != null && this.enableCoapServer) {
        	if (this.coapServer.stopServer()) {
                _Logger.info("CoAP server stopped.");
            } else {
                _Logger.severe("Failed to stop CoAP server. Check log file for details.");
            }
        }
        
        if (this.cloudClient != null && this.enableCloudClient) {
            // TODO: implement this in Lab Module 10
            // boolean success = this.cloudClient.disconnectClient();
            // _Logger.info("Cloud client disconnection attempt: " + success);
        }
        
        _Logger.info("DeviceDataManager stopped successfully.");
    }
    
    @Override
    public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
    {
        if (data != null) {
            _Logger.info("Handling actuator response: " + data.getName());
            
            // Optional: Call data analysis method (as mentioned in Kanban instructions)
            this.handleIncomingDataAnalysis(resourceName, data);
            
            if (data.hasError()) {
                _Logger.warning("Error flag set for ActuatorData instance.");
            }
            
            return true;
        } else {
            return false;
        }
    }
    
    // NOTE: This method is not part of Lab Module 05 Kanban requirements
    // Adding minimal implementation for interface compliance
    @Override
    public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
    {
        // TODO: This will be implemented in a future lab module
        _Logger.info("ActuatorCommandRequest received - not implemented in Lab Module 05");
        return false;
    }
    
    @Override
    public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
    {
        if (msg != null) {
            _Logger.info("Handling incoming generic message: " + msg);
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
    {
        if (data != null) {
            _Logger.info("Handling sensor message: " + data.getName());
            
            if (data.hasError()) {
                _Logger.warning("Error flag set for SensorData instance.");
            }
            
            // Invoke humidity threshold analysis (PIOT-GDA-10-003)
            this.handleIncomingDataAnalysis(resourceName, data);
            
            // Optional: Convert to JSON and handle upstream transmission
            String jsonData = DataUtil.getInstance().sensorDataToJson(data);
            this.handleUpstreamTransmission(resourceName, jsonData, 0);
            
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
    {
        if (data != null) {
            _Logger.info("Handling system performance message: " + data.getName());
            
            if (data.hasError()) {
                _Logger.warning("Error flag set for SystemPerformanceData instance.");
            }
            
            // Optional: Convert to JSON and handle upstream transmission
            String jsonData = DataUtil.getInstance().systemPerformanceDataToJson(data);
            this.handleUpstreamTransmission(resourceName, jsonData, 0);
            
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public void setActuatorDataListener(String name, IActuatorDataListener listener)
    {
        _Logger.info("setActuatorDataListener called for: " + name + " - not implemented in Lab Module 05");
        
        if (listener != null) {
            // For now, just ignore 'name' - if you need more than one listener,
            // you can use 'name' to create a map of listener instances
            this.actuatorDataListener = listener;
            
            _Logger.info("Actuator data listener registered: " + 
                (name != null ? name : "default"));
        }
    }
    
    // private methods
    private void initManager()
    {
        ConfigUtil configUtil = ConfigUtil.getInstance();
        
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);
        
        if (this.enableSystemPerf) {
            this.sysPerfMgr = new SystemPerformanceManager();
            this.sysPerfMgr.setDataMessageListener(this);
        }
        
        // NOTE: This is new - creating the MQTT client connector instance
        if (this.enableMqttClient) {
            this.mqttClient = new MqttClientConnector();
            
            // NOTE: The next line isn't technically needed until Lab Module 10
            this.mqttClient.setDataMessageListener(this);
        }
        
        if (this.enableCoapServer) {
        	this.coapServer = new CoapServerGateway(this);
            _Logger.info("CoAP server initialized.");
        }
        
        if (this.enableCloudClient) {
            // TODO: implement this in Lab Module 10
            // this.cloudClient = new CloudClientConnector();
        }
        
        if (this.enablePersistenceClient) {
            // TODO: implement this as an optional exercise in Lab Module 5
            // this.persistenceClient = new RedisPersistenceAdapter();
        }
    }
    
    /**
     * Analyzes incoming sensor data for threshold crossing events.
     * Routes to appropriate handler based on sensor type.
     * (PIOT-GDA-10-003)
     * 
     * @param resource The resource name
     * @param data The sensor data
     */
    private void handleIncomingDataAnalysis(ResourceNameEnum resource, SensorData data)
    {
        if (data.getTypeID() == ConfigConst.HUMIDITY_SENSOR_TYPE) {
            handleHumiditySensorAnalysis(resource, data);
        }
    }
    
    /**
     * Analyzes humidity sensor data for threshold crossings.
     * Generates actuation events based on configured thresholds.
     * (PIOT-GDA-10-003)
     * 
     * @param resource The resource name
     * @param data The humidity sensor data
     */
    private void handleHumiditySensorAnalysis(ResourceNameEnum resource, SensorData data)
    {
        if (!handleHumidityChangeOnDevice) {
            return;
        }
        
        _Logger.fine("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());
        
        boolean isLow = data.getValue() < this.triggerHumidifierFloor;
        boolean isHigh = data.getValue() > this.triggerHumidifierCeiling;
        
        if (isLow || isHigh) {
            _Logger.fine("Humidity data from CDA exceeds nominal range.");
            
            if (this.latestHumiditySensorData == null) {
                // First threshold crossing detected - start timer
                this.latestHumiditySensorData = data;
                this.latestHumiditySensorTimeStamp = getDateTimeFromData(data);
                
                _Logger.fine("Starting humidity nominal exception timer. Waiting for seconds: " + 
                    this.humidityMaxTimePastThreshold);
                
                return;
            } else {
                // Check time delta since last reading
                OffsetDateTime currentHumiditySensorTimeStamp = getDateTimeFromData(data);
                long diffSeconds = ChronoUnit.SECONDS.between(
                    this.latestHumiditySensorTimeStamp, currentHumiditySensorTimeStamp);
                
                _Logger.fine("Checking Humidity value exception time delta: " + diffSeconds);
                
                if (diffSeconds >= this.humidityMaxTimePastThreshold) {
                    // Time threshold crossed - send actuation command
                    ActuatorData actuatorCmd = new ActuatorData();
                    actuatorCmd.setName(ConfigConst.HUMIDIFIER_ACTUATOR_NAME);
                    actuatorCmd.setLocationID(data.getLocationID());
                    actuatorCmd.setTypeID(ConfigConst.HUMIDIFIER_ACTUATOR_TYPE);
                    actuatorCmd.setValue(this.nominalHumiditySetting);
                    
                    if (isLow) {
                        actuatorCmd.setCommand(ConfigConst.ON_COMMAND);
                    } else if (isHigh) {
                        actuatorCmd.setCommand(ConfigConst.OFF_COMMAND);
                    }
                    
                    _Logger.info("Humidity exceptional value reached. Sending actuation event to CDA: " + actuatorCmd);
                    
                    this.lastKnownHumidifierCommand = actuatorCmd.getCommand();
                    sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, actuatorCmd);
                    
                    // Reset state
                    this.latestHumidifierActuatorData = actuatorCmd;
                    this.latestHumiditySensorData = null;
                    this.latestHumiditySensorTimeStamp = null;
                }
            }
        } else if (this.lastKnownHumidifierCommand == ConfigConst.ON_COMMAND) {
            // Humidity returned to nominal - turn off humidifier
            if (this.latestHumidifierActuatorData != null) {
                if (data.getValue() >= this.nominalHumiditySetting) {
                    this.latestHumidifierActuatorData.setCommand(ConfigConst.OFF_COMMAND);
                    
                    _Logger.info("Humidity nominal value reached. Sending OFF actuation event to CDA: " + 
                        this.latestHumidifierActuatorData);
                    
                    sendActuatorCommandtoCda(
                        ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestHumidifierActuatorData);
                    
                    // Reset state
                    this.lastKnownHumidifierCommand = this.latestHumidifierActuatorData.getCommand();
                    this.latestHumidifierActuatorData = null;
                    this.latestHumiditySensorData = null;
                    this.latestHumiditySensorTimeStamp = null;
                } else {
                    _Logger.fine("Humidifier is still on. Not yet at nominal levels (OK).");
                }
            }
        }
    }
    
    /**
     * Sends actuator command to CDA via MQTT or CoAP.
     * (PIOT-GDA-10-003)
     * 
     * @param resource The resource to send to
     * @param data The ActuatorData command
     */
    private void sendActuatorCommandtoCda(ResourceNameEnum resource, ActuatorData data)
    {
        // Send via MQTT if enabled
        if (this.mqttClient != null && this.enableMqttClient) {
            String jsonData = DataUtil.getInstance().actuatorDataToJson(data);
            
            if (this.mqttClient.publishMessage(resource, jsonData, ConfigConst.DEFAULT_QOS)) {
                _Logger.info("Published ActuatorData command from GDA to CDA: " + data.getCommand());
            } else {
                _Logger.warning("Failed to publish ActuatorData command from GDA to CDA: " + data.getCommand());
            }
        }
        
        // TODO: Send via CoAP if enabled
    }
    
    /**
     * Extracts OffsetDateTime from SensorData's ISO 8601 timestamp.
     * (PIOT-GDA-10-003)
     * 
     * @param data The IoT data object
     * @return The OffsetDateTime or current time if parsing fails
     */
    private OffsetDateTime getDateTimeFromData(SensorData data)
    {
        OffsetDateTime odt = null;
        
        try {
            odt = OffsetDateTime.parse(data.getTimeStamp());
        } catch (Exception e) {
            _Logger.warning("Failed to extract ISO 8601 timestamp from IoT data. Using local current time.");
            odt = OffsetDateTime.now();
        }
        
        return odt;
    }
    
    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
    {
        _Logger.fine("Analyzing incoming actuator data for resource: " + resourceName.name());
        
        if (data != null) {
            if (data.isResponseFlagEnabled()) {
                // TODO: This is a response - handle accordingly
                _Logger.info("Actuator response received: " + data.getName());
            } else {
                // This is a command - forward to the actuator listener
                if (this.actuatorDataListener != null) {
                    this.actuatorDataListener.onActuatorDataUpdate(data);
                }
            }
        }
    }
    
    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
    {
        _Logger.fine("Analyzing incoming system state data for resource: " + resourceName.name());
        
        // TODO: Implement command interpretation and handling logic
        // This is a command the GDA should interpret and handle internally
    }
    
    private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
    {
        _Logger.fine("Handling upstream transmission for resource: " + resourceName.name());
        
        // TODO: This will eventually publish to the cloud service
        // Will be implemented in Part 03 - Connectivity
        // The qos (Quality of Service) parameter will be used for MQTT publishing
        
        return true;
    }
}