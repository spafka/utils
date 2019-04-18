package io.github.spafka.hex;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class XorUtils {
   static BitAndByteUtil b2b = new BitAndByteUtil();

    public static String  xorAtFixPosit(@NonNull String s1, @NonNull String s2, int poisition) {

        Preconditions.checkState(s1.length() == s2.length());

        int length = s1.length();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {

            if (i != poisition) {
                sb.append("0");
            } else {
                if (s1.charAt(i) != s2.charAt(i)) {
                    sb.append(s2.charAt(i));
                }
            }

        }
        return sb.toString();
    }

    // 把8位字节拼成byte数组
    public static byte[] cheng8bitString2Bytes(String string) {

        Preconditions.checkState(string.length() % 8 == 0);

        int length = string.length() / 8;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            String substring = string.substring(i * 8, i * 8 + 8);
            bytes[i] = Byte.valueOf(substring, 2);
        }
        return bytes;
    }

    public static byte[] xorWhichPositionChanged(byte[] old, byte[] newz, int poisition) {
        return xorWhichPositionChanged(change(old), change(newz), poisition);
    }

    public static byte[] xorWhichPositionChanged(int[] old, int[] newz, int poisition) {

        String s1 = Arrays.stream(old).mapToObj(x -> b2b.byteToBit((byte) x)
        ).reduce((x, x2) -> x + x2).get();

        log.debug(s1);

        String s2 = Arrays.stream(newz).mapToObj(x -> b2b.byteToBit((byte) x)
        ).reduce((x, x2) -> x + x2).get();
        log.debug(s2);
        String s = xorAtFixPosit(s1, s2, poisition);

        byte[] bytes = cheng8bitString2Bytes(s);

        return bytes;

    }

    public static int[] change(@NonNull byte[] bz) {

        int[] ints = new int[bz.length];

        for (int i = 0; i < bz.length; i++) {
            ints[i] = bz[i];
        }
        return ints;
    }
}
