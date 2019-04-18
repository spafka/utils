package io.guthub.spafka.test.dto;


public class DecoderDefine {

    public static final String ITEMTYPE_CAN = "11";

    public static final String ITEMTYPE_SENSOR = "12";

    public static final String ITEMTYPE_GPS = "13";

    public static final String ITEMTYPE_CALL = "14";

    public static int CAN_HEAD_LENGTH = 14;

    public static int CAN_BODY_LENGTH = 8;

    public static int BASE_HEAD_LENGTH = 12;

    public static int GPS_BODY_LENGTH = 11;

    public static int CALL_BODY_LENGTH = 3;

    public static int SENSOR_BODY_LENGTH = 14;

    public static int CDD1A_BODY_LENGTH = 7;

    public static int SDD1B_BODY_LENGTH = 5;

    public static int DGD1C_BODY_LENGTH = 12;

    public static int BCM1D_BODY_LENGTH = 3;

    public static int BCM1E_BODY_LENGTH = 5;

    public static int AC1F_BODY_LENGTH = 9;

    public static int TPMS20_BODY_LENGTH = 12;

    public static int FM21_BODY_LENGTH = 7;

    public static int CAN22_BODY_LENGTH = 14;
    //电动汽车
    public static int EV23_BODY_LENGTH = 18;

}
