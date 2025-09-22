/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.system;

import java.util.logging.Logger;
import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Simple system performance manager.
 */
public class SystemPerformanceManager
{
    // static
    private static final Logger _Logger = Logger.getLogger(SystemPerformanceManager.class.getName());
    
    // private var's
    private String locationID = ConfigConst.NOT_SET;
    private IDataMessageListener dataMsgListener = null;
    
    private SystemCpuUtilTask sysCpuUtilTask = null;
    private SystemMemUtilTask sysMemUtilTask = null;
    
    // constructors
    
    /**
     * Default.
     */
    public SystemPerformanceManager()
    {
        this.locationID =
            ConfigUtil.getInstance().getProperty(
                ConfigConst.GATEWAY_DEVICE, ConfigConst.DEVICE_LOCATION_ID_KEY, ConfigConst.NOT_SET);
        
        this.sysCpuUtilTask = new SystemCpuUtilTask();
        this.sysMemUtilTask = new SystemMemUtilTask();
        
        _Logger.info("Created SystemPerformanceManager instance.");
    }
    
    // public methods
    
    public void handleTelemetry()
    {
        float cpuUtil = this.sysCpuUtilTask.getTelemetryValue();
        float memUtil = this.sysMemUtilTask.getTelemetryValue();
        
        _Logger.info("CPU utilization: " + cpuUtil + ", Mem utilization: " + memUtil);
        
        SystemPerformanceData spd = new SystemPerformanceData();
        spd.setLocationID(this.locationID);
        spd.setCpuUtilization(cpuUtil);
        spd.setMemoryUtilization(memUtil);
        
        if (this.dataMsgListener != null) {
            this.dataMsgListener.handleSystemPerformanceMessage(
                ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE, spd);
        }
    }
    
    public void setDataMessageListener(IDataMessageListener listener)
    {
        if (listener != null) {
            this.dataMsgListener = listener;
        }
    }
    
    public void startManager()
    {
        _Logger.info("SystemPerformanceManager started.");
    }
    
    public void stopManager()
    {
        _Logger.info("SystemPerformanceManager stopped.");
    }
}