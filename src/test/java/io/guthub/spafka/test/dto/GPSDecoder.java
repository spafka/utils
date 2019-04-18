package io.guthub.spafka.test.dto;

public class GPSDecoder extends BaseDecoder {


    public GPSDTO decode(byte[] b) {

        GPSDTO dto = new GPSDTO();
        StringBuilder dataBit = new StringBuilder();

        byte[] dataHead = new byte[DecoderDefine.BASE_HEAD_LENGTH];
        byte[] dataBody = new byte[DecoderDefine.GPS_BODY_LENGTH];
        System.arraycopy(b, 0, dataHead, 0, DecoderDefine.BASE_HEAD_LENGTH);
        System.arraycopy(b, DecoderDefine.BASE_HEAD_LENGTH, dataBody, 0, DecoderDefine.GPS_BODY_LENGTH);

        setCommon(dto, dataHead, b);

        for (int i = 0; i < dataBody.length; i++) {

            dataBit.append(b2b.byteToBit(dataBody[i]));

        }

        dto.setGpsok(b2b.BitToInt(dataBit.substring(5, 6)));
        dto.setEastern(b2b.BitToInt(dataBit.substring(6, 7)));
        dto.setSourthern(b2b.BitToInt(dataBit.substring(7, 8)));
        dto.setLng(b2b.BitToInt(dataBit.substring(8, 40)));
        dto.setLat(b2b.BitToInt(dataBit.substring(40, 72)));
        String tmp = dataBit.substring(72, 88);
        int alt = 0;
        if (tmp.charAt(0) == '1') {
            alt = ((~Integer.parseInt(b2b.binaryString2hexString(tmp), 16) & 0xffff) + 1) * -1;
        } else {
            alt = b2b.BitToInt(dataBit.substring(72, 88));
        }
        dto.setAltitude(alt);
        return dto;

    }

}
