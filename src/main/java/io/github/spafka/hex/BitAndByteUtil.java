package io.github.spafka.hex;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BitAndByteUtil {

    private static final char[] bcdLookup = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public short getShort(byte[] b, int index) {
        return (short) (((b[index + 0] << 8) | b[index + 1] & 0xFF));
    }

    public int getInt(byte[] bb, int index) {
        return (int) ((((bb[index + 3] & 0xff) << 24) | ((bb[index + 2] & 0xff) << 16) | ((bb[index + 1] & 0xff) << 8)
                | ((bb[index + 0] & 0xff) << 0)));
    }

    public int byteToShort(byte[] bytes, int offset) {
        int high = bytes[offset];
        int low = bytes[offset + 1];
        return (high << 8 & 0xFF00) | (low & 0xFF);
    }

    /**
     * 16进制字符串转字节数组
     *
     * @param hexString
     *            16进制字符串
     * @return 字节数组
     */
    public byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase().replace(" ", "");
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));

        }
        return d;
    }

    public byte hexStringToByte(String hexString) {

        hexString = hexString.toUpperCase().replace(" ", "");
        char[] hexChars = hexString.toCharArray();
        return 	(byte) (charToByte(hexChars[0]) << 4 | charToByte(hexChars[0 + 1]));



    }

    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 字节转二进制字符串
     *
     * @param b
     *            字节
     * @return 二进制字符串
     */
    public String byteToBit(byte b) {

        return "" + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1) + (byte) ((b >> 5) & 0x1)
                + (byte) ((b >> 4) & 0x1) + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1) + (byte) ((b >> 1) & 0x1)
                + (byte) ((b >> 0) & 0x1);
    }

    /**
     * 二进制字符串转字节
     *
     * @param bitString
     *            二进制字符串
     * @return byte
     */
    public byte BitToByte(String bitString) {
        int re, len;
        if (null == bitString) {
            return 0;
        }
        len = bitString.length();
        StringBuffer sb = new StringBuffer();
        if (len < 8 && len > 4) {
            for (int i = 0; i < 8 - len; i++) {
                sb.append("0");
            }
            sb.append(bitString);
            bitString = sb.toString();
        } else if (len < 4) {
            for (int i = 0; i < 4 - len; i++) {
                sb.append("0");
            }
            sb.append(bitString);
            bitString = sb.toString();
        } else {

            sb.append(bitString);
            bitString = sb.toString();
        }

        if (len == 8) {// 8 bit处理
            if (bitString.charAt(0) == '0') {// 正数
                re = Integer.parseInt(bitString, 2);
            } else {// 负数
                re = Integer.parseInt(bitString, 2) - 256;
            }
        } else {// 4 bit处理
            re = Integer.parseInt(bitString, 2);
        }
        return (byte) re;
    }

    /**
     * 二进制字符串转int
     *
     * @param bitString
     *            二进制字符串
     * @return int
     */
    public int BitToInt(String bitString) {
        int re, len;
        if (null == bitString) {
            return 0;
        }

        len = bitString.length();
        if (len == 8) {// 8 bit处理
            if (bitString.charAt(0) == '0') {// 正数
                re = Integer.parseInt(bitString, 2);
            } else {// 负数
                re = Integer.parseInt(bitString, 2) & 0xff;
            }
        } else {// 4 bit处理
            try {
                re = Integer.parseInt(bitString, 2);
            } catch (Exception e) {
                return 1;
            }

        }
        return re;
    }

    public long bitToLong(String bitString) {
        long re = 0;
        try {
            if (bitString.length() < 64) {
                re = Long.parseLong(bitString, 2);
                return re;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * 有符号二进制数据转Int
     *
     * @param bitString
     * @return
     */
    public int signedBinaryToInt(String bitString) {
        int re, len;
        if (null == bitString) {
            return 0;
        }
        if (bitString.charAt(0) == '0') {// 正数
            re = Integer.parseInt(bitString, 2);
        } else {// 负数
            re = Integer.parseInt(bitString.substring(1), 2) * -1;
        }

        return re;
    }

    /**
     * 二进制字符串转16进制字符串
     *
     * @param bitString
     *            bitString
     * @return 16进制字符串
     */
    public String binaryString2hexString(String bitString) {
        if (bitString == null || bitString.equals("") || bitString.length() % 8 != 0)
            return null;
        StringBuffer tmp = new StringBuffer();
        int iTmp = 0;
        for (int i = 0; i < bitString.length(); i += 4) {
            iTmp = 0;
            for (int j = 0; j < 4; j++) {
                iTmp += Integer.parseInt(bitString.substring(i + j, i + j + 1)) << (4 - j - 1);
            }
            tmp.append(Integer.toHexString(iTmp));
        }
        return tmp.toString();
    }

    /**
     * 将4字节的byte数组转成int值
     *
     * @param bytes
     *            将4字节的byte数组
     * @return int
     */
    public int byteArray2int(byte[] bytes) {
        byte[] a = new byte[4];
        int i = a.length - 1, j = bytes.length - 1;
        for (; i >= 0; i--, j--) {// 从b的尾部(即int值的低位)开始copy数据
            if (j >= 0)
                a[i] = bytes[j];
            else
                a[i] = 0;// 如果b.length不足4,则将高位补0
        }
        int v0 = (a[0] & 0xff) << 24;// &0xff将byte值无差异转成int,避免Java自动类型提升后,会保留高位的符号位
        int v1 = (a[1] & 0xff) << 16;
        int v2 = (a[2] & 0xff) << 8;
        int v3 = (a[3] & 0xff);
        return v0 + v1 + v2 + v3;
    }

    /**
     * 字节数组转为long
     *
     * @param bytes
     *            字节数组
     * @return long
     */
    public long byteArray2long(byte[] bytes) {
        long l = 0;
        l = (0xffL & (long) bytes[0]) | (0xff00L & ((long) bytes[1] << 8)) | (0xff0000L & ((long) bytes[2] << 16))
                | (0xff000000L & ((long) bytes[3] << 24)) | (0xff00000000L & ((long) bytes[4] << 32))
                | (0xff0000000000L & ((long) bytes[5] << 40)) | (0xff000000000000L & ((long) bytes[6] << 48))
                | (0xff00000000000000L & ((long) bytes[7] << 56));
        return l;

    }

    /**
     * 整型转换为4位字节数组
     *
     * @param intValue
     *            数值
     * @return 4位字节数组
     */
    public byte[] int2Byte(int intValue) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (intValue >> 8 * (3 - i) & 0xFF);
        }
        return b;
    }

    /**
     * long转换为8字节数组
     *
     * @param longValue
     *            数值
     * @return 8字节数组
     */
    public static byte[] long2Bytes(long longValue) {
        byte[] byteNum = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((longValue >> offset) & 0xff);
        }
        return byteNum;
    }

    /**
     * 十六进制字符串转化为字符串
     *
     * @param hex
     *            十六进制字符串
     * @return 字符串
     */
    public String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            String output = hex.substring(i, (i + 2));
            int decimal = Integer.parseInt(output, 16);
            sb.append((char) decimal);
            temp.append(decimal);
        }
        return sb.toString();
    }

    /**
     * 字节数组转16进制字符串 以空格分割
     *
     * @param bytes
     *            字节数组
     * @return 16进制字符串
     */
    public String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex.toUpperCase() + " ");
        }
        return sb.toString();
    }

    /**
     * 字节数组转16进制字符串 以空格分割
     *
     * @param bcd
     * @return 16进制字符串
     */
    public final String bytesToHexStr(byte[] bcd) {
        StringBuffer s = new StringBuffer(bcd.length * 2);

        for (int i = 0; i < bcd.length; i++) {
            s.append(bcdLookup[(bcd[i] >>> 4) & 0x0f]);
            s.append(bcdLookup[bcd[i] & 0x0f] + " ");
        }

        return s.toString();
    }

    /**
     * 字节数组转16进制字符串
     *
     * @param bytes
     *            字节数组
     * @return 16进制字符串
     */
    public static String byte2hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    public int hex2int(String str) {
        int a = 0;
        if (null != str && str.length() > 0) {
            a = Integer.parseInt(str, 16);
        }
        return a;
    }

    public long hex2long(String str) {
        long a = 0L;
        if (null != str && str.length() > 0) {
            a = Long.parseLong(str, 16);
        }
        return a;
    }

    public static float byte2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }

    /**
     *
     * 将二进制字符串转化为有符号的十进制数
     *
     * @param binaryString
     * @return
     */
    public int binToSignedNumber(String binaryString) {
        int resoult = 0;
        if (binaryString.trim().charAt(0) == '0') {
            resoult = Integer.parseInt(binaryString, 2);
        } else {
            String calutor = binaryAdd(binaryString, getMinusNum(binaryString.length()), false);
            String oppo = getOpposit(calutor);
            char[] num = oppo.toCharArray();
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < num.length; i++) {
                sb.append(num[i]);
            }
            long result = Long.parseLong(sb.toString(), 2);
            resoult = Integer.parseInt("-" + result);
        }
        return resoult;
    }

    /**
     * 从补码获得到反码需要-1，这个是求出这个-1的补码
     *
     * @param binaryLength
     *            要求给出这个补码的长度
     * @return
     */
    private static String getMinusNum(int binaryLength) {
        String minNum = "0";
        for (int i = 0; i < binaryLength - 1; i++) {
            minNum = minNum + "1";
        }
        return minNum;
    }

    /**
     * 两个二进制数相加。
     *
     * @param bnum1
     *            第一个二进制数 字符串
     * @param bnum2
     *            第二个二进制数 字符串
     * @param overflow
     *            是否运行溢出。
     * @return 返回相加结果
     */
    private static String binaryAdd(String bnum1, String bnum2, boolean overflow) {
        boolean flag = false;
        char[] bb1 = bnum1.toCharArray();
        char[] bb2 = bnum2.toCharArray();
        int maxLength = (bb1.length >= bb2.length ? bb1.length : bb2.length);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < maxLength; i++) {
            // 万一长度不相等的两个二进制数 怎么办
            if (i > bb1.length - 1 || i > bb2.length - 1) {
                System.out.println("字符串长度不一报警！");
                throw new AbstractMethodError("对与长度不相等的二进制数相加，还有待实现！");
            }
            if (flag) {
                // 说明上一位有进位
                int temp1 = Integer.parseInt(bb1[bb1.length - i - 1] + "");
                int temp2 = Integer.parseInt(bb2[bb2.length - i - 1] + "");
                // 因为上次有进位，所以这次相加时候 要加1 并得到的结果重置flag 得到这次是否有进位
                flag = (temp1 + temp2 + 1 > 1 ? true : false);

                if (flag) {
                    // 有进位 可能是0 也可能是1
                    result.append(temp1 + temp2 + 1 == 2 ? '0' : '1');
                } else {
                    // 无进位 但要加上上一次的进位1
                    result.append('1');
                }

            } else {
                // 上一没有进位
                int temp1 = Integer.parseInt(bb1[bb1.length - i - 1] + "");
                int temp2 = Integer.parseInt(bb2[bb2.length - i - 1] + "");
                flag = (temp1 + temp2 > 1 ? true : false);
                if (flag) {
                    // 有进位
                    result.append('0');
                } else {
                    // 无进位
                    int temp3 = (temp1 + temp2);
                    result.append(temp3 == 0 ? '0' : '1');
                }
            }

        }
        // 是否可以溢出
        if (overflow && flag && bb1.length == bb2.length) {
            result.append('1');
        }
        return result.reverse().toString();
    }

    /**
     * 获取反码
     *
     * @param binary
     *            要被转换的二进制
     * @return
     */
    private static String getOpposit(String binary) {
        char[] binArray = binary.toCharArray();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < binary.length(); i++) {
            if (binArray[i] == '0') {
                sb.append(1);
            } else {
                sb.append(0);
            }
        }
        return sb.toString();
    }

    /**
     * 十六进制转十进制
     * @param num
     * @return
     */
    public static Integer get10HexNum(String num){
        return Integer.parseInt(num,16);
    }


    /**
     * 倒序字符串
     * @param old
     * @return
     */
    public static String reverseOrder(String old){
        return new StringBuffer(old).reverse().toString();
    }




}


