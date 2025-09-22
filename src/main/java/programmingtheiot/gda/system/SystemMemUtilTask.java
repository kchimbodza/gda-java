package programmingtheiot.gda.system;

import programmingtheiot.common.ConfigConst;

public class SystemMemUtilTask extends BaseSystemUtilTask
{
    public SystemMemUtilTask()
    {
        super(ConfigConst.MEM_UTIL_NAME, ConfigConst.MEM_UTIL_TYPE);
    }
    
    @Override
    public float getTelemetryValue()
    {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        float memUtil = (float) (usedMemory * 100.0 / totalMemory);
        return memUtil;
    }
}