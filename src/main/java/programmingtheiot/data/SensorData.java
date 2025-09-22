package programmingtheiot.data;

import java.io.Serializable;
import programmingtheiot.common.ConfigConst;

public class SensorData extends BaseIotData implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private float value = ConfigConst.DEFAULT_VAL;
    private int sensorType = ConfigConst.DEFAULT_SENSOR_TYPE;
    
    public SensorData()
    {
        super();
    }
    
    public SensorData(int sensorType)
    {
        super();
        this.sensorType = sensorType;
    }
    
    public float getValue()
    {
        return this.value;
    }
    
    public int getSensorType()
    {
        return this.sensorType;
    }
    
    public void setValue(float val)
    {
        super.updateTimeStamp();
        this.value = val;
    }
    
    public void setSensorType(int sensorType)
    {
        super.updateTimeStamp();
        this.sensorType = sensorType;
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(',');
        sb.append(ConfigConst.VALUE_PROP).append('=').append(this.getValue());
        return sb.toString();
    }
    
    protected void handleUpdateData(BaseIotData data)
    {
        if (data instanceof SensorData) {
            SensorData sData = (SensorData) data;
            this.setValue(sData.getValue());
            this.setSensorType(sData.getSensorType());
        }
    }
}