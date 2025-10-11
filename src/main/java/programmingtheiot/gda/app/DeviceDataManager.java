/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.app;

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
import programmingtheiot.gda.connection.IRequestResponseClient;
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
        
        // TODO: This will be implemented in a future lab module
        // Future implementation will store the listener for routing actuator data updates
        // The listener has method: onActuatorDataUpdate(ActuatorData data)
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
    
    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
    {
        _Logger.fine("Analyzing incoming actuator data for resource: " + resourceName.name());
        
        // TODO: This will eventually publish back to the CDA using either MQTT or CoAP
        // Will be implemented in Part 03 - Connectivity
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