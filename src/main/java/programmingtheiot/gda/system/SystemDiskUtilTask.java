/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.system;

import java.io.File;
import programmingtheiot.common.ConfigConst;

/**
 * System disk utilization task.
 */
public class SystemDiskUtilTask extends BaseSystemUtilTask
{
    // constructors
    
    public SystemDiskUtilTask()
    {
        super(ConfigConst.DISK_UTIL_NAME, ConfigConst.DISK_UTIL_TYPE);
    }
    
    // public methods
    
    @Override
    public float getTelemetryValue()
    {
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        
        if (totalSpace > 0) {
            return (float) (usedSpace * 100.0 / totalSpace);
        }
        
        return ConfigConst.DEFAULT_VAL;
    }
}
