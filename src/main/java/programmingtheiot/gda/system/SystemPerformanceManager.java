/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */
package programmingtheiot.gda.system;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;

/**
 * SystemPerformanceManager for Lab Module 02 with full telemetry integration.
 */
public class SystemPerformanceManager
{
    // private variables
    private static final Logger _Logger = Logger.getLogger(SystemPerformanceManager.class.getName());
    
    private ScheduledExecutorService schedExecSvc = null;
    private SystemCpuUtilTask sysCpuUtilTask = null;
    private SystemMemUtilTask sysMemUtilTask = null;
    
    private String locationID = ConfigConst.GATEWAY_DEVICE;
    private Runnable taskRunner = null;
    private boolean isStarted = false;
    private int pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
    
    // constructors
    
    /**
     * Default constructor.
     */
    public SystemPerformanceManager()
    {
        super();
        
        this.pollRate = ConfigUtil.getInstance().getInteger(
            ConfigConst.GATEWAY_DEVICE,
            ConfigConst.POLL_CYCLES_KEY,
            ConfigConst.DEFAULT_POLL_CYCLES);
        
        if (this.pollRate <= 0) {
            this.pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
        }
        
        this.locationID = ConfigUtil.getInstance().getProperty(
            ConfigConst.GATEWAY_DEVICE,
            ConfigConst.DEVICE_LOCATION_ID_KEY,
            ConfigConst.GATEWAY_DEVICE);
        
        this.schedExecSvc = Executors.newScheduledThreadPool(1);
        this.sysCpuUtilTask = new SystemCpuUtilTask();
        this.sysMemUtilTask = new SystemMemUtilTask();
        
        this.taskRunner = () -> {
            this.handleTelemetry();
        };
    }
    
    // public methods
    
    public void handleTelemetry()
    {
        float cpuUtilPct = this.sysCpuUtilTask.getTelemetryValue();
        float memUtilPct = this.sysMemUtilTask.getTelemetryValue();
        
        _Logger.info("Handle telemetry results: cpuUtil=" + cpuUtilPct + ", memUtil=" + memUtilPct);
    }
    
    public void setDataMessageListener(IDataMessageListener listener)
    {
        // TODO: Will be implemented later
    }
    
    public void startManager()
    {
        _Logger.info("SystemPerformanceManager is starting...");
        
        if (!this.isStarted) {
            ScheduledFuture<?> futureTask = this.schedExecSvc.scheduleAtFixedRate(
                this.taskRunner, 0L, this.pollRate, TimeUnit.SECONDS);
            
            this.isStarted = true;
        }
    }
    
    public void stopManager()
    {
        _Logger.info("SystemPerformanceManager is stopped.");
        
        this.schedExecSvc.shutdown();
        this.isStarted = false;
    }
}