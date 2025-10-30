/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */
package programmingtheiot.data;

import programmingtheiot.common.ConfigConst;

/**
 * Simple data container for cloud services that require only a value and timestamp.
 * Some IoT cloud services rely on their own specific data format where the context
 * (device name, variable name, etc.) is pre-configured or embedded in the topic.
 * This class provides a simplified JSON payload containing just the value and timestamp.
 * 
 */
public class TimeAndValuePayloadData
{
	private float value = ConfigConst.DEFAULT_VAL;
	private long timeStamp = System.currentTimeMillis();
	
	// constructors
	
	/**
	 * Default constructor.
	 */
	public TimeAndValuePayloadData()
	{
		super();
	}
	
	/**
	 * Constructor that extracts value and timestamp from SensorData.
	 * 
	 * @param data The SensorData instance
	 */
	public TimeAndValuePayloadData(SensorData data)
	{
		super();
		
		if (data != null) {
			this.value = data.getValue();
			// TimeStamp is stored as String in BaseIotData, convert to long
			try {
				this.timeStamp = Long.parseLong(data.getTimeStamp());
			} catch (Exception e) {
				this.timeStamp = System.currentTimeMillis();
			}
		}
	}
	
	/**
	 * Constructor that extracts value and timestamp from ActuatorData.
	 * 
	 * @param data The ActuatorData instance
	 */
	public TimeAndValuePayloadData(ActuatorData data)
	{
		super();
		
		if (data != null) {
			this.value = data.getValue();
			// TimeStamp is stored as String in BaseIotData, convert to long
			try {
				this.timeStamp = Long.parseLong(data.getTimeStamp());
			} catch (Exception e) {
				this.timeStamp = System.currentTimeMillis();
			}
		}
	}
	
	/**
	 * Constructor that extracts value and timestamp from SystemPerformanceData.
	 * 
	 * @param data The SystemPerformanceData instance
	 */
	public TimeAndValuePayloadData(SystemPerformanceData data)
	{
		super();
		
		if (data != null) {
			// For SystemPerformanceData, we'll use a representative value
			// You may want to modify this to use a specific metric
			this.value = data.getCpuUtilization();
			// TimeStamp is stored as String in BaseIotData, convert to long
			try {
				this.timeStamp = Long.parseLong(data.getTimeStamp());
			} catch (Exception e) {
				this.timeStamp = System.currentTimeMillis();
			}
		}
	}
	
	// public methods
	
	/**
	 * Get the value.
	 * 
	 * @return The float value
	 */
	public float getValue()
	{
		return this.value;
	}
	
	/**
	 * Set the value.
	 * 
	 * @param value The float value to set
	 */
	public void setValue(float value)
	{
		this.value = value;
	}
	
	/**
	 * Get the timestamp.
	 * 
	 * @return The timestamp in milliseconds
	 */
	public long getTimeStamp()
	{
		return this.timeStamp;
	}
	
	/**
	 * Set the timestamp.
	 * 
	 * @param timeStamp The timestamp in milliseconds
	 */
	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	
	@Override
	public String toString()
	{
		return "TimeAndValuePayloadData [value=" + this.value + ", timeStamp=" + this.timeStamp + "]";
	}
}