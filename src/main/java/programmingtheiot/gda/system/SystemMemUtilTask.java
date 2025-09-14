/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.system;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;

/**
 * SystemMemUtilTask implementation for retrieving memory utilization.
 */
public class SystemMemUtilTask extends BaseSystemUtilTask
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(SystemMemUtilTask.class.getName());
	
	// constructors
	
	/**
	 * Default constructor.
	 */
	public SystemMemUtilTask()
	{
		super(ConfigConst.MEM_UTIL_NAME, ConfigConst.MEM_UTIL_TYPE);
	}
	
	// public methods
	
	@Override
	public float getTelemetryValue()
	{
		MemoryUsage memUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		double memUtil = ((double) memUsage.getUsed() / (double) memUsage.getMax()) * 100.0d;
		
		_Logger.info("Memory utilization: " + memUtil);
		
		return (float) memUtil;
	}
}