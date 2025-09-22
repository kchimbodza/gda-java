package programmingtheiot.gda.system;

import java.lang.management.ManagementFactory;
import programmingtheiot.common.ConfigConst;

public class SystemCpuUtilTask extends BaseSystemUtilTask
{
    public SystemCpuUtilTask()
    {
        super(ConfigConst.CPU_UTIL_NAME, ConfigConst.CPU_UTIL_TYPE);
    }
    
    @Override
    public float getTelemetryValue()
    {
        double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        if (cpuLoad >= 0) {
            return (float) (cpuLoad * 100.0 / Runtime.getRuntime().availableProcessors());
        }
        return 25.0f; // Default test value
    }
}