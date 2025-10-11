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

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;

/**
 * Resource handler for SensorData (telemetry) updates from CDA.
 * Handles PUT requests to update sensor telemetry data.
 */
public class UpdateTelemetryResourceHandler extends CoapResource {
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(UpdateTelemetryResourceHandler.class.getName());
	
	// params
	
	private IDataMessageListener dataMsgListener = null;
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param resourceName The name of the resource
	 */
	public UpdateTelemetryResourceHandler(String resourceName) {
		super(resourceName);
	}
	
	// public methods
	
	/**
	 * Sets the data message listener for callbacks to DeviceDataManager
	 * 
	 * @param listener The data message listener
	 */
	public void setDataMessageListener(IDataMessageListener listener) {
		if (listener != null) {
			this.dataMsgListener = listener;
		}
	}
	
	@Override
	public void handleDELETE(CoapExchange context) {
		_Logger.info("handleDELETE called for SensorData resource: " + super.getName());
		
		context.accept();
		context.respond(ResponseCode.DELETED, "SensorData resource deleted");
	}
	
	@Override
	public void handleGET(CoapExchange context) {
		_Logger.info("handleGET called for SensorData resource: " + super.getName());
		
		context.accept();
		
		// For now, return a simple response
		// In future, could retrieve cached SensorData
		context.respond(ResponseCode.CONTENT, "SensorData resource: " + super.getName());
	}
	
	@Override
	public void handlePOST(CoapExchange context) {
		_Logger.info("handlePOST called for SensorData resource: " + super.getName());
		
		ResponseCode code = ResponseCode.NOT_ACCEPTABLE;
		
		context.accept();
		
		if (this.dataMsgListener != null) {
			try {
				String jsonData = new String(context.getRequestPayload());
				
				SensorData sensorData =
					DataUtil.getInstance().jsonToSensorData(jsonData);
				
				this.dataMsgListener.handleSensorMessage(
					ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData);
				
				code = ResponseCode.CREATED;
			} catch (Exception e) {
				_Logger.warning("Failed to handle POST request. Message: " + e.getMessage());
				code = ResponseCode.BAD_REQUEST;
			}
		} else {
			_Logger.info("No callback listener for request. Ignoring POST.");
			code = ResponseCode.CONTINUE;
		}
		
		String msg = "Create sensor data request handled: " + super.getName();
		context.respond(code, msg);
	}
	
	@Override
	public void handlePUT(CoapExchange context) {
		_Logger.info("handlePUT called for SensorData resource: " + super.getName());
		
		ResponseCode code = ResponseCode.NOT_ACCEPTABLE;
		
		context.accept();
		
		if (this.dataMsgListener != null) {
			try {
				String jsonData = new String(context.getRequestPayload());
				
				SensorData sensorData =
					DataUtil.getInstance().jsonToSensorData(jsonData);
				
				// Delegate to the data message listener (DeviceDataManager)
				this.dataMsgListener.handleSensorMessage(
					ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sensorData);
				
				code = ResponseCode.CHANGED;
			} catch (Exception e) {
				_Logger.warning("Failed to handle PUT request. Message: " + e.getMessage());
				code = ResponseCode.BAD_REQUEST;
			}
		} else {
			_Logger.info("No callback listener for request. Ignoring PUT.");
			code = ResponseCode.CONTINUE;
		}
		
		String msg = "Update sensor data request handled: " + super.getName();
		context.respond(code, msg);
	}
}