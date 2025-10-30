/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.data;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import com.google.gson.Gson;

/**
 * Utility class for converting data objects to/from JSON using Gson.
 */
public class DataUtil
{
	// static
	
	private static final Logger _Logger = Logger.getLogger(DataUtil.class.getName());
	private static final DataUtil _Instance = new DataUtil();

	public static final DataUtil getInstance()
	{
		return _Instance;
	}
	
	// constructors
	
	private DataUtil()
	{
		super();
	}
	
	// public methods
	
	public String actuatorDataToJson(ActuatorData data)
	{
		String jsonData = null;
		
		if (data != null) {
			Gson gson = new Gson();
			jsonData = gson.toJson(data);
		}
		
		return jsonData;
	}
	
	public ActuatorData jsonToActuatorData(String jsonData)
	{
		ActuatorData data = null;
		
		if (jsonData != null && jsonData.trim().length() > 0) {
			Gson gson = new Gson();
			data = gson.fromJson(jsonData, ActuatorData.class);
		}
		
		return data;
	}
	
	public String actuatorDataToTimeAndValueJson(ActuatorData data)
	{
		String jsonData = null;
		
		if (data != null) {
			Gson gson = new Gson();
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(data);
			jsonData = gson.toJson(tvData);
		}
		
		return jsonData;
	}
	
	public String sensorDataToJson(SensorData data)
	{
		String jsonData = null;
		
		if (data != null) {
			Gson gson = new Gson();
			jsonData = gson.toJson(data);
		}
		
		return jsonData;
	}
	
	public SensorData jsonToSensorData(String jsonData)
	{
		SensorData data = null;
		
		if (jsonData != null && jsonData.trim().length() > 0) {
			Gson gson = new Gson();
			data = gson.fromJson(jsonData, SensorData.class);
		}
		
		return data;
	}
	
	public String sensorDataToTimeAndValueJson(SensorData data)
	{
		String jsonData = null;
		
		if (data != null) {
			Gson gson = new Gson();
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(data);
			jsonData = gson.toJson(tvData);
		}
		
		return jsonData;
	}
	
	// ADD THIS METHOD TO YOUR DataUtil CLASS 
	// Insert it after sensorDataToTimeAndValueJson() method

	/**
	 * Converts SystemPerformanceData to TimeAndValuePayloadData JSON format.
	 * Used for publishing to cloud services like Ubidots.
	 * 
	 * @param data The SystemPerformanceData instance
	 * @return JSON string in TimeAndValuePayloadData format
	 */
	public String systemPerformanceDataToTimeAndValueJson(SystemPerformanceData data)
	{
		String jsonData = null;
		
		if (data != null) {
			Gson gson = new Gson();
			TimeAndValuePayloadData tvData = new TimeAndValuePayloadData(data);
			jsonData = gson.toJson(tvData);
		}
		
		return jsonData;
	}
	
	public String systemPerformanceDataToJson(SystemPerformanceData data)
	{
		String jsonData = null;
		
		if (data != null) {
			Gson gson = new Gson();
			jsonData = gson.toJson(data);
		}
		
		return jsonData;
	}
	
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
		SystemPerformanceData data = null;
		
		if (jsonData != null && jsonData.trim().length() > 0) {
			Gson gson = new Gson();
			data = gson.fromJson(jsonData, SystemPerformanceData.class);
		}
		
		return data;
	}
	
	public String systemStateDataToJson(SystemStateData data)
	{
		String jsonData = null;
		
		if (data != null) {
			Gson gson = new Gson();
			jsonData = gson.toJson(data);
		}
		
		return jsonData;
	}
	
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
		SystemStateData data = null;
		
		if (jsonData != null && jsonData.trim().length() > 0) {
			Gson gson = new Gson();
			data = gson.fromJson(jsonData, SystemStateData.class);
		}
		
		return data;
	}
}