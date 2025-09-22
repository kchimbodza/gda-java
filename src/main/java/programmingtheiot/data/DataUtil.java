/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.data;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Utility class for converting data objects to/from JSON using Gson.
 */
public class DataUtil
{
	// static
	private static final Logger _Logger = Logger.getLogger(DataUtil.class.getName());
	private static final DataUtil _Instance = new DataUtil();
	
	/**
	 * Returns the Singleton instance of this class.
	 * 
	 * @return DataUtil
	 */
	public static final DataUtil getInstance()
	{
		return _Instance;
	}
	
	// private var's
	private Gson gson = null;
	
	// constructors
	/**
	 * Default (private).
	 */
	private DataUtil()
	{
		super();
		
		// Configure Gson with pretty printing
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();
		
		_Logger.info("Created DataUtil instance.");
	}
	
	// public methods
	
	public String actuatorDataToJson(ActuatorData actuatorData)
	{
		if (actuatorData == null) {
			_Logger.warning("ActuatorData is null. Returning empty string.");
			return "";
		}
		
		String jsonData = this.gson.toJson(actuatorData);
		return jsonData;
	}
	
	public String sensorDataToJson(SensorData sensorData)
	{
		if (sensorData == null) {
			_Logger.warning("SensorData is null. Returning empty string.");
			return "";
		}
		
		String jsonData = this.gson.toJson(sensorData);
		return jsonData;
	}
	
	public String systemPerformanceDataToJson(SystemPerformanceData sysPerfData)
	{
		if (sysPerfData == null) {
			_Logger.warning("SystemPerformanceData is null. Returning empty string.");
			return "";
		}
		
		String jsonData = this.gson.toJson(sysPerfData);
		return jsonData;
	}
	
	public String systemStateDataToJson(SystemStateData sysStateData)
	{
		if (sysStateData == null) {
			_Logger.warning("SystemStateData is null. Returning empty string.");
			return "";
		}
		
		String jsonData = this.gson.toJson(sysStateData);
		return jsonData;
	}
	
	public ActuatorData jsonToActuatorData(String jsonData)
	{
		if (jsonData == null || jsonData.trim().isEmpty()) {
			_Logger.warning("JSON data is empty or null. Returning null.");
			return null;
		}
		
		try {
			ActuatorData actuatorData = this.gson.fromJson(jsonData, ActuatorData.class);
			return actuatorData;
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to convert JSON to ActuatorData: " + e.getMessage(), e);
			return null;
		}
	}
	
	public SensorData jsonToSensorData(String jsonData)
	{
		if (jsonData == null || jsonData.trim().isEmpty()) {
			_Logger.warning("JSON data is empty or null. Returning null.");
			return null;
		}
		
		try {
			SensorData sensorData = this.gson.fromJson(jsonData, SensorData.class);
			return sensorData;
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to convert JSON to SensorData: " + e.getMessage(), e);
			return null;
		}
	}
	
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
		if (jsonData == null || jsonData.trim().isEmpty()) {
			_Logger.warning("JSON data is empty or null. Returning null.");
			return null;
		}
		
		try {
			SystemPerformanceData sysPerfData = this.gson.fromJson(jsonData, SystemPerformanceData.class);
			return sysPerfData;
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to convert JSON to SystemPerformanceData: " + e.getMessage(), e);
			return null;
		}
	}
	
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
		if (jsonData == null || jsonData.trim().isEmpty()) {
			_Logger.warning("JSON data is empty or null. Returning null.");
			return null;
		}
		
		try {
			SystemStateData sysStateData = this.gson.fromJson(jsonData, SystemStateData.class);
			return sysStateData;
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Failed to convert JSON to SystemStateData: " + e.getMessage(), e);
			return null;
		}
	}
}