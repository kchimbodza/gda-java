/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.system;

import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;

/**
 * SystemCpuUtilTask implementation for retrieving CPU utilization.
 */
public class SystemCpuUtilTask extends BaseSystemUtilTask
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(SystemCpuUtilTask.class.getName());
	
	// constructors
	
	/**
	 * Default constructor.
	 */
	public SystemCpuUtilTask()
	{
		super(ConfigConst.CPU_UTIL_NAME, ConfigConst.CPU_UTIL_TYPE);
	}
	
	// public methods
	
	@Override
	public float getTelemetryValue()
	{
		double cpuUtil = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
		
		_Logger.info("CPU utilization: " + cpuUtil);
		
		return (float) cpuUtil;
	}
}