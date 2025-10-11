/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;

/**
 * Observable resource handler for ActuatorData commands.
 * Allows GDA to push actuator commands to CDA via CoAP OBSERVE.
 */
public class GetActuatorCommandResourceHandler extends CoapResource
	implements IActuatorDataListener {
	
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GetActuatorCommandResourceHandler.class.getName());
	
	// params
	
	private ActuatorData actuatorData = null;
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param resourceName The name of the resource
	 */
	public GetActuatorCommandResourceHandler(String resourceName) {
		super(resourceName);
		
		// Initialize with default ActuatorData
		this.actuatorData = new ActuatorData();
		
		// Set the resource to be observable
		super.setObservable(true);
		
		_Logger.info("Created observable actuator command resource handler: " + resourceName);
	}
	
	// public methods
	
	/**
	 * Callback method for actuator data updates from DeviceDataManager.
	 * When actuator data is updated, all observing clients are notified.
	 * 
	 * @param data The updated actuator data
	 * @return true if update was successful, false otherwise
	 */
	@Override
	public boolean onActuatorDataUpdate(ActuatorData data) {
		if (data != null && this.actuatorData != null) {
			this.actuatorData.updateData(data);
			
			// Notify all connected clients (observers)
			super.changed();
			
			_Logger.fine("Actuator data updated for URI: " + super.getURI() + 
				": Data value = " + this.actuatorData.getValue());
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Handle GET requests for actuator command data.
	 * Returns the current actuator data as JSON.
	 */
	@Override
	public void handleGET(CoapExchange context) {
		_Logger.info("handleGET called for ActuatorCommand resource: " + super.getName());
		
		// Validate context
		if (context == null) {
			_Logger.warning("CoapExchange context is null. Ignoring GET request.");
			return;
		}
		
		// Accept the request
		context.accept();
		
		// Convert the locally stored ActuatorData to JSON
		String jsonData = 
			DataUtil.getInstance().actuatorDataToJson(this.actuatorData);
		
		_Logger.fine("Returning actuator data as JSON: " + jsonData);
		
		// Send response with actuator data
		context.respond(ResponseCode.CONTENT, jsonData);
	}
}