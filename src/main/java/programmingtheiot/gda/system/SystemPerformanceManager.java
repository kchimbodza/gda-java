/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.system;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private SystemDiskUtilTask sysDiskUtilTask = null;
    
    private ScheduledExecutorService schedExecSvc = null;
    private int pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
    
    // constructors
    
    /**
     * Default.
     */
    public SystemPerformanceManager()
    {
        this.locationID =
            ConfigUtil.getInstance().getProperty(
                ConfigConst.GATEWAY_DEVICE, ConfigConst.DEVICE_LOCATION_ID_KEY, ConfigConst.NOT_SET);
        
        this.pollRate = 
            ConfigUtil.getInstance().getInteger(
                ConfigConst.GATEWAY_DEVICE, ConfigConst.POLL_CYCLES_KEY, ConfigConst.DEFAULT_POLL_CYCLES);
        
        this.sysCpuUtilTask = new SystemCpuUtilTask();
        this.sysMemUtilTask = new SystemMemUtilTask();
        this.sysDiskUtilTask = new SystemDiskUtilTask();
        
        this.schedExecSvc = Executors.newScheduledThreadPool(1);
        
        _Logger.info("Created SystemPerformanceManager instance.");
    }
    
    // public methods
    
    public void handleTelemetry()
    {
        float cpuUtil = this.sysCpuUtilTask.getTelemetryValue();
        float memUtil = this.sysMemUtilTask.getTelemetryValue();
        float diskUtil = this.sysDiskUtilTask.getTelemetryValue();
        
        _Logger.info("CPU utilization: " + cpuUtil + ", Mem utilization: " + memUtil + ", Disk utilization: " + diskUtil);
        
        SystemPerformanceData spd = new SystemPerformanceData();
        spd.setLocationID(this.locationID);
        spd.setCpuUtilization(cpuUtil);
        spd.setMemoryUtilization(memUtil);
        spd.setDiskUtilization(diskUtil);
        
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
        _Logger.info("SystemPerformanceManager is starting...");
        
        if (this.schedExecSvc != null) {
            this.schedExecSvc.scheduleAtFixedRate(
                this::handleTelemetry, 0L, this.pollRate, TimeUnit.SECONDS);
        }
    }
    
    public void stopManager()
    {
        if (this.schedExecSvc != null) {
            this.schedExecSvc.shutdown();
        }
        
        _Logger.info("SystemPerformanceManager is stopped.");
    }
}