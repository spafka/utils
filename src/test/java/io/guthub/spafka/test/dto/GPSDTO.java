package io.guthub.spafka.test.dto;


import lombok.Data;
import lombok.ToString;

@ToString(callSuper = true)
@Data
public class GPSDTO extends BASEDTO{



    /**
     * gps�Ƿ���Ч
     */
    private int gpsok;

    /**
     * �������� 1-east;0-west
     */
    private int eastern;

    /**
     * �ϱ����� 1-sourth;0-north
     */
    private int sourthern;

    /**
     * ����
     */
    private int lat;

    /**
     * γ��
     */
    private int lng;

    /**
     * ���θ߶�
     */
    private int altitude;



}
