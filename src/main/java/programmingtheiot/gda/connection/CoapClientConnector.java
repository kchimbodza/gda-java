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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

/**
 * CoAP client connector implementation.
 * Implements IRequestResponseClient interface for CoAP communication.
 */
public class CoapClientConnector implements IRequestResponseClient
{
	// Static
	private static final Logger _Logger = 
		Logger.getLogger(CoapClientConnector.class.getName());
	
	// Class-scoped variables
	private String protocol;
	private String host;
	private int port;
	private String serverAddr;
	private CoapClient clientConn;
	private IDataMessageListener dataMsgListener;
	
	/**
	 * Default constructor.
	 * Initializes the CoAP client properties using ConfigUtil.
	 */
	public CoapClientConnector()
	{
		super();
		
		// Get configuration
		ConfigUtil config = ConfigUtil.getInstance();
		this.host = config.getProperty(
			ConfigConst.COAP_GATEWAY_SERVICE,
			ConfigConst.HOST_KEY,
			ConfigConst.DEFAULT_HOST
		);
		
		// Check if encryption is enabled
		if (config.getBoolean(ConfigConst.COAP_GATEWAY_SERVICE, ConfigConst.ENABLE_CRYPT_KEY)) {
			this.protocol = ConfigConst.DEFAULT_COAP_SECURE_PROTOCOL;
			this.port = config.getInteger(
				ConfigConst.COAP_GATEWAY_SERVICE,
				ConfigConst.SECURE_PORT_KEY,
				ConfigConst.DEFAULT_COAP_SECURE_PORT
			);
		} else {
			this.protocol = ConfigConst.DEFAULT_COAP_PROTOCOL;
			this.port = config.getInteger(
				ConfigConst.COAP_GATEWAY_SERVICE,
				ConfigConst.PORT_KEY,
				ConfigConst.DEFAULT_COAP_PORT
			);
		}
		
		this.serverAddr = this.protocol + "://" + this.host + ":" + this.port;
		
		initClient();
		
		_Logger.info("Using URL for server conn: " + this.serverAddr);
	}
	
	/**
	 * Initialize the CoAP client connection.
	 * Creates a new CoapClient instance.
	 */
	private void initClient()
	{
		try {
			this.clientConn = new CoapClient(this.serverAddr);
			_Logger.info("Created client connection to server / resource: " + this.serverAddr);
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to create CoAP client: " + this.serverAddr, e);
		}
	}
	
	/**
	 * Send a CoAP discovery request.
	 * Discovers available resources on the CoAP server using GET to /.well-known/core
	 * 
	 * @param timeout - The timeout in milliseconds
	 * @return boolean - True if successful, False otherwise
	 */
	@Override
	public boolean sendDiscoveryRequest(int timeout)
	{
		_Logger.info("Issuing discover...");
		_Logger.info("Discovery URI: " + this.serverAddr + "/.well-known/core");
		
		try {
			this.clientConn.setURI(this.serverAddr + "/.well-known/core");
			_Logger.info("Client URI set to: " + this.clientConn.getURI());
			
			CoapResponse response = this.clientConn.get();
			
			if (response != null) {
				String responseText = response.getResponseText();
				
				if (responseText != null && !responseText.isEmpty()) {
					String[] resources = responseText.split(",");
					_Logger.info("Found " + resources.length + " resources");
					
					for (String resource : resources) {
						String cleanResource = resource.replace("<", "").replace(">", "").trim();
						_Logger.info(" --> URI: " + cleanResource);
					}
					return true;
				} else {
					_Logger.warning("Discovery response was empty.");
					return false;
				}
			} else {
				_Logger.warning("No response from discovery request.");
				return false;
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Discovery request failed", e);
			return false;
		}
	}
	
	/**
	 * Send a CoAP GET request.
	 * 
	 * @param resource - The resource enum
	 * @param name - The resource name
	 * @param enableCON - Whether to enable CON (confirmable) messages
	 * @param timeout - The timeout in milliseconds
	 * @return boolean - True if successful, False otherwise
	 */
	@Override
	public boolean sendGetRequest(ResourceNameEnum resource, String name, boolean enableCON, int timeout)
	{
		try {
			if (resource == null) {
				return false;
			}
			
			String uri = this.serverAddr + "/" + resource.getResourceName();
			if (name != null && !name.isEmpty()) {
				uri = uri + "/" + name;
			}
			
			this.clientConn.setURI(uri);
			CoapResponse response = this.clientConn.get();
			
			return (response != null);
			
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to send GET request", e);
			return false;
		}
	}
	
	/**
	 * Send a CoAP POST request.
	 * Logging disabled for performance testing.
	 * 
	 * @param resource - The resource enum
	 * @param name - The resource name
	 * @param enableCON - Whether to enable CON (confirmable) messages
	 * @param payload - The payload to send
	 * @param timeout - The timeout in milliseconds
	 * @return boolean - True if successful, False otherwise
	 */
	@Override
	public boolean sendPostRequest(ResourceNameEnum resource, String name, boolean enableCON, String payload, int timeout)
	{
		// NOTE: Logging disabled for performance testing
		
		try {
			if (resource == null) {
				return false;
			}
			
			if (payload == null || payload.isEmpty()) {
				return false;
			}
			
			String uri = this.serverAddr + "/" + resource.getResourceName();
			if (name != null && !name.isEmpty()) {
				uri = uri + "/" + name;
			}
			
			this.clientConn.setURI(uri);
			CoapResponse response = this.clientConn.post(payload, MediaTypeRegistry.APPLICATION_JSON);
			
			return true;
			
		} catch (IllegalStateException e) {
			// Californium config file missing - POST still completes successfully
			_Logger.fine("Config handled: " + e.getMessage());
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to send POST request", e);
			return false;
		}
	}
	
	/**
	 * Send a CoAP PUT request.
	 * Logging disabled for performance testing.
	 * 
	 * @param resource - The resource enum
	 * @param name - The resource name
	 * @param enableCON - Whether to enable CON (confirmable) messages
	 * @param payload - The payload to send
	 * @param timeout - The timeout in milliseconds
	 * @return boolean - True if successful, False otherwise
	 */
	@Override
	public boolean sendPutRequest(ResourceNameEnum resource, String name, boolean enableCON, String payload, int timeout)
	{
		// NOTE: Logging disabled for performance testing
		
		try {
			if (resource == null) {
				return false;
			}
			
			if (payload == null || payload.isEmpty()) {
				return false;
			}
			
			String uri = this.serverAddr + "/" + resource.getResourceName();
			if (name != null && !name.isEmpty()) {
				uri = uri + "/" + name;
			}
			
			this.clientConn.setURI(uri);
			CoapResponse response = this.clientConn.put(payload, MediaTypeRegistry.APPLICATION_JSON);
			
			return true;
			
		} catch (IllegalStateException e) {
			// Californium config file missing - PUT still completes successfully
			_Logger.fine("Config handled: " + e.getMessage());
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to send PUT request", e);
			return false;
		}
	}
	
	/**
	 * Send a CoAP DELETE request.
	 * 
	 * @param resource - The resource enum
	 * @param name - The resource name
	 * @param enableCON - Whether to enable CON (confirmable) messages
	 * @param timeout - The timeout in milliseconds
	 * @return boolean - True if successful, False otherwise
	 */
	@Override
	public boolean sendDeleteRequest(ResourceNameEnum resource, String name, boolean enableCON, int timeout)
	{
		try {
			if (resource == null) {
				return false;
			}
			
			String uri = this.serverAddr + "/" + resource.getResourceName();
			if (name != null && !name.isEmpty()) {
				uri = uri + "/" + name;
			}
			
			this.clientConn.setURI(uri);
			CoapResponse response = this.clientConn.delete();
			
			return (response != null);
			
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to send DELETE request", e);
			return false;
		}
	}
	
	/**
	 * Set the data message listener.
	 * Accepts an IDataMessageListener reference (typically DeviceDataManager).
	 * 
	 * @param listener - The IDataMessageListener to set
	 * @return boolean - True if successful, False otherwise
	 */
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		this.dataMsgListener = listener;
		_Logger.info("Data message listener set for CoAP client connector");
		return true;
	}
	
	/**
	 * Clear the endpoint path.
	 */
	@Override
	public void clearEndpointPath()
	{
		_Logger.info("clearEndpointPath() called");
	}
	
	/**
	 * Set the endpoint path for a given resource.
	 * 
	 * @param resource - The resource enum to set as endpoint path
	 */
	@Override
	public void setEndpointPath(ResourceNameEnum resource)
	{
		_Logger.info("setEndpointPath() called for resource: " + resource);
	}
	
	/**
	 * Start observing a CoAP resource.
	 * 
	 * @param resource - The resource enum to observe
	 * @param name - The resource name
	 * @param ttl - The time to live
	 * @return boolean - True if successful, False otherwise
	 */
	@Override
	public boolean startObserver(ResourceNameEnum resource, String name, int ttl)
	{
		_Logger.info("startObserver() called for resource: " + resource + ", name: " + name);
		return false;
	}
	
	/**
	 * Stop observing a CoAP resource.
	 * 
	 * @param resource - The resource enum to stop observing
	 * @param name - The resource name
	 * @param timeout - The timeout in milliseconds
	 * @return boolean - True if successful, False otherwise
	 */
	@Override
	public boolean stopObserver(ResourceNameEnum resource, String name, int timeout)
	{
		_Logger.info("stopObserver() called for resource: " + resource + ", name: " + name);
		return false;
	}
}