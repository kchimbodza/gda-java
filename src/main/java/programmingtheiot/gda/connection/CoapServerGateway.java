/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * You may find it more helpful to your design to adjust the
 * functionality, constants and interfaces (if there are any)
 * provided within in order to meet the needs of your specific
 * Programming the Internet of Things project.
 */
package programmingtheiot.gda.connection;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.californium.core.config.CoapConfig;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.gda.connection.handlers.GenericCoapResourceHandler;
import programmingtheiot.gda.connection.handlers.GetActuatorCommandResourceHandler;
import programmingtheiot.gda.connection.handlers.UpdateSystemPerformanceResourceHandler;
import programmingtheiot.gda.connection.handlers.UpdateTelemetryResourceHandler;

/**
 * CoAP server gateway for handling CDA requests.
 */
public class CoapServerGateway {
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CoapServerGateway.class.getName());
	
	// Static configuration initializers for Californium > 3.8.0
	static {
		CoapConfig.register();
		UdpConfig.register();
	}
	
	// params
	
	private CoapServer coapServer = null;
	private IDataMessageListener dataMsgListener = null;
	
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param dataMsgListener The data message listener (DeviceDataManager)
	 */
	public CoapServerGateway(IDataMessageListener dataMsgListener) {
		super();
		
		this.dataMsgListener = dataMsgListener;
		
		initServer();
	}
		
	// public methods
	
	/**
	 * Adds a resource to the CoAP server based on ResourceNameEnum
	 * 
	 * @param resourceType The resource name enumeration
	 * @param endName Optional endpoint name (can be null)
	 * @param resource The resource handler to add
	 */
	public void addResource(ResourceNameEnum resourceType, String endName, Resource resource) {
		if (resourceType != null && resource != null) {
			// Break out the hierarchy of names and build the resource
			// handler generation(s) as needed
			createAndAddResourceChain(resourceType, resource);
		}
	}
	
	/**
	 * Checks if the server has a resource with the given name
	 * 
	 * @param name The resource name to check
	 * @return true if resource exists, false otherwise
	 */
	public boolean hasResource(String name) {
		if (this.coapServer != null && name != null) {
			return (this.coapServer.getRoot().getChild(name) != null);
		}
		
		return false;
	}
	
	/**
	 * Sets or updates the data message listener
	 * 
	 * @param listener The data message listener
	 */
	public void setDataMessageListener(IDataMessageListener listener) {
		if (listener != null) {
			this.dataMsgListener = listener;
		}
	}
	
	/**
	 * Starts the CoAP server
	 * 
	 * @return true if started successfully, false otherwise
	 */
	public boolean startServer() {
		try {
			if (this.coapServer != null) {
				this.coapServer.start();
				
				// Add message tracer for logging
				for (Endpoint ep : this.coapServer.getEndpoints()) {
					ep.addInterceptor(new MessageTracer());
				}
				
				_Logger.info("CoAP server started successfully.");
				return true;
			} else {
				_Logger.warning("CoAP server START failed. Not yet initialized.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to start CoAP server.", e);
		}
		
		return false;
	}
	
	/**
	 * Stops the CoAP server
	 * 
	 * @return true if stopped successfully, false otherwise
	 */
	public boolean stopServer() {
		try {
			if (this.coapServer != null) {
				this.coapServer.stop();
				
				_Logger.info("CoAP server stopped successfully.");
				return true;
			} else {
				_Logger.warning("CoAP server STOP failed. Not yet initialized.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to stop CoAP server.", e);
		}
		
		return false;
	}
	
	
	// private methods
	
	/**
	 * Creates a resource chain based on the ResourceNameEnum
	 * The chain represents the path hierarchy (e.g., PIOT/ConstrainedDevice/sensor)
	 * 
	 * @param resourceType The resource name enumeration
	 * @param resource The actual resource handler to add at the end of the chain
	 */
	private void createAndAddResourceChain(ResourceNameEnum resourceType, Resource resource) {
		_Logger.info("Adding server resource handler chain: " + resourceType.getResourceName());
		
		List<String> resourceNames = resourceType.getResourceNameChain();
		Queue<String> queue = new ArrayBlockingQueue<>(resourceNames.size());
		
		queue.addAll(resourceNames);
		
		// Check if we have a parent resource (start with root)
		Resource parentResource = this.coapServer.getRoot();
		
		// Process the first name in the chain (should be "PIOT")
		String rootName = queue.poll();
		Resource rootResource = (parentResource != null) ? parentResource.getChild(rootName) : null;
		
		if (rootResource == null) {
			rootResource = new CoapResource(rootName);
			this.coapServer.add(rootResource);
		}
		
		parentResource = rootResource;
		
		// Process remaining names in the chain
		while (!queue.isEmpty()) {
			// Get the next resource name
			String resourceName = queue.poll();
			Resource nextResource = parentResource.getChild(resourceName);
			
			if (nextResource == null) {
				// If this is the last name in the chain, use the provided resource handler
				if (queue.isEmpty()) {
					nextResource = resource;
					nextResource.setName(resourceName);
				} else {
					// Otherwise, create an intermediate CoapResource
					nextResource = new CoapResource(resourceName);
				}
				
				parentResource.add(nextResource);
			}
			
			parentResource = nextResource;
		}
		
		_Logger.info("Resource handler chain added: " + resourceType.getResourceName());
	}
	
	/**
	 * Initializes the CoAP server and default resources
	 * 
	 * @param resources Optional resources to initialize with
	 */
	private void initServer(ResourceNameEnum ...resources) {
		// Create the CoAP server instance
		this.coapServer = new CoapServer();
		
		// Initialize default resource handlers
		initDefaultResources();
		
		_Logger.info("CoAP server initialized with default resources.");
	}
	
	/**
	 * Initialize default resource handlers for the CoAP server
	 */
	private void initDefaultResources() {
		// Create and register GetActuatorCommandResourceHandler (Observable)
		GetActuatorCommandResourceHandler getActuatorCmdResourceHandler =
			new GetActuatorCommandResourceHandler(
				ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE.getResourceType());
		
		// Register this handler as an actuator listener with DeviceDataManager
		if (this.dataMsgListener != null) {
			this.dataMsgListener.setActuatorDataListener(null, getActuatorCmdResourceHandler);
		}
		
		addResource(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, null, getActuatorCmdResourceHandler);
		
		// Create and register UpdateTelemetryResourceHandler (for SensorData)
		UpdateTelemetryResourceHandler updateTelemetryResourceHandler =
			new UpdateTelemetryResourceHandler(
				ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceType());
		
		updateTelemetryResourceHandler.setDataMessageListener(this.dataMsgListener);
		
		addResource(
			ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, null, updateTelemetryResourceHandler);
		
		// Create and register UpdateSystemPerformanceResourceHandler
		UpdateSystemPerformanceResourceHandler updateSystemPerformanceResourceHandler =
			new UpdateSystemPerformanceResourceHandler(
				ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceType());
		
		updateSystemPerformanceResourceHandler.setDataMessageListener(this.dataMsgListener);
		
		addResource(
			ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, null, updateSystemPerformanceResourceHandler);
		
		_Logger.info("Default resource handlers initialized.");
	}
}