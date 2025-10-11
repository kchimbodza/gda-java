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

import java.util.logging.Level;
import java.util.logging.Logger;

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
	 * @param resource The resource name enumeration
	 */
	public void addResource(ResourceNameEnum resource) {
		if (resource != null && this.coapServer != null) {
			// Create the resource chain for this resource
			Resource resourceChain = createResourceChain(resource);
			
			if (resourceChain != null) {
				this.coapServer.add(resourceChain);
				_Logger.info("Added CoAP resource: " + resource.getResourceName());
			}
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
				
				_Logger.info("CoAP server started successfully on port " + 
					this.coapServer.getEndpoints().get(0).getAddress().getPort());
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
	 * The chain represents the path hierarchy (e.g., PIOT/constrained-device/sensor)
	 * 
	 * @param resource The resource name enumeration
	 * @return The root resource of the chain
	 */
	private Resource createResourceChain(ResourceNameEnum resource) {
		if (resource != null) {
			// Get the resource name components (split by '/')
			// Example: "PIOT/constrained-device/sensor" -> ["PIOT", "constrained-device", "sensor"]
			String[] resourceNames = resource.getResourceName().split("/");
			
			// TODO: Implement in PIOT-GDA-08-002
			// For now, return null - we'll build the chain in the next exercise
			_Logger.info("Resource chain creation for: " + resource.getResourceName());
		}
		
		return null;
	}
	
	/**
	 * Initializes the CoAP server
	 * 
	 * @param resources Optional resources to initialize with
	 */
	private void initServer(ResourceNameEnum ...resources) {
		// Create the CoAP server instance
		this.coapServer = new CoapServer();
		
		_Logger.info("CoAP server initialized.");
		
		// If resources are provided, add them
		if (resources != null && resources.length > 0) {
			for (ResourceNameEnum resource : resources) {
				addResource(resource);
			}
		}
	}
}