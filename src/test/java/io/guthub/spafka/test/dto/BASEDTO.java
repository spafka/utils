package io.guthub.spafka.test.dto;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@ToString(callSuper = false,exclude = {"hexMetaData","body","itemLength","ecu","vin"})
@Data
public class BASEDTO implements Serializable {



    public String ecu;
    public String itemType;
    public int itemLength;
    public String seconds1970;
    public String microSecond;
    public String vin;
    public String hexMetaData;
    public String deviceId;
    private byte[] body;


}
