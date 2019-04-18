package io.guthub.spafka.test;

import io.github.spafka.hex.BitAndByteUtil;
import io.github.spafka.hex.XorUtils;
import io.guthub.spafka.test.dto.DataItem_1EDTO;
import io.guthub.spafka.test.dto.DataItem_1EDecoder;
import io.guthub.spafka.test.dto.GPSDTO;
import io.guthub.spafka.test.dto.GPSDecoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Arrays;

@Slf4j
public class BitAndByteUtilTest {

    String gps = "48 15 20 10 06 13 12 58 9B BA E3 03 00 00 00 00 00 00 00 00 00 00 00";
    GPSDecoder gpsDecoder = new GPSDecoder();
    BitAndByteUtil b2b = new BitAndByteUtil();
    @Test
    public void test() {


        byte[] bytes = b2b.hexStringToBytes(gps);

        GPSDTO decode = gpsDecoder.decode(bytes);

        System.out.println();


    }



    @Test
    public void test2() {

        byte[] bytes = b2b.hexStringToBytes("0B 15 20 03 30 1E 0C 5C B6 D4 3D 56 00 00 3F 06 DB");
        DataItem_1EDTO dto = DataItem_1EDecoder.decode(bytes);


        byte[] body = dto.getBody();

        byte[] bytes2 = b2b.hexStringToBytes("0B 15 20 03 30 1E 0C 5C B6 D4 3D 56 00 00 3F 16 DB");
        DataItem_1EDTO dto2 = DataItem_1EDecoder.decode(bytes2);

        byte[] body2 = dto2.getBody();


        byte[] bytes1 = XorUtils.xorWhichPositionChanged(body, body2, 27);

        System.out.println(Arrays.toString(bytes1));

    }



}

