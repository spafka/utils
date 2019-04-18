package io.guthub.spafka.test.dto;


import io.github.spafka.hex.BitAndByteUtil;

public strictfp class BaseDecoder {
    protected static BitAndByteUtil b2b = new BitAndByteUtil();

    public static void setCommon(BASEDTO dto, byte[] b, byte[] metaData) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < b.length; i++) {
            sb.append(b2b.byteToBit(b[i]));
        }
        dto.setHexMetaData(b2b.bytesToHexString(metaData));

        dto.setDeviceId(b2b.binaryString2hexString(sb.substring(0, 40)));

        dto.setItemType(Integer.toHexString(b2b.BitToInt(sb.substring(40, 48))));

        dto.setItemLength(b2b.BitToInt(sb.substring(48, 56)));

        dto.setSeconds1970(String.valueOf(b2b.bitToLong(sb.substring(56, 88))));

        String time = String.valueOf(b2b.BitToInt(sb.substring(88, 96)));

        dto.setMicroSecond(dealString(time));
    }

    private static String dealString(String str) {
        if (str.length() == 1) {
            str = "00" + str;
            return str;
        } else if (str.length() == 2) {
            str = "0" + str;
            return str;
        }
        return str;
    }

    // 这里只有 8位的说法

    public static int binaryToDecimal(String str) {
        int p = -1;
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != '1') {
                return Integer.parseInt(str, 2);
            }
        }

        if (str.length() <= 8) return p & 0xFF;
        if (str.length() <= 16) return p & 0xFFFF;
        if (str.length() <= 24) return p & 0xFFFFFF;
        if (str.length() <= 32) return p & 0xFFFFFFFF;
        return p;
    }
}
