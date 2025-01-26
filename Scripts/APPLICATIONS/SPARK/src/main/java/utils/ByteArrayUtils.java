package utils;

public class ByteArrayUtils {

    public static int byteArrayToInt(byte[] b, int offset){
        return   (b[3+offset] & 0xFF) << 24 |
                (b[2+offset] & 0xFF) << 16 |
                (b[1+offset] & 0xFF) << 8 |
                (b[0+offset] & 0xFF);
    }

    public static int byteArrayToInt(byte[] b){
        return byteArrayToInt(b, 0);
    }

    public static float byteArrayToFloat(byte[] b){
        return byteArrayToFloat(b, 0);
    }

    public static float byteArrayToFloat(byte[] b, int offset) {
        return Float.intBitsToFloat(byteArrayToInt(b, offset));
    }

    public static byte[] floatToByteArray(float a) {
        return intToByteArray(Float.floatToIntBits(a));
    }

    public static byte[] intToByteArray(int a)
    {
        return new byte[] {
            (byte) ((a ) & 0xFF),
            (byte) ((a >> 8) & 0xFF),
            (byte) ((a >> 16) & 0xFF),
            (byte) ((a >> 24) & 0xFF)
        };
    }

    public static Double sumFloatArrays(byte[] _2) {
        double sum = 0;
        int position = 0;
        for(int i=0; i<(_2.length/4); i++) {
            sum += (double) ByteArrayUtils.byteArrayToFloat(_2, position);
            position+=4;
        }
        return sum;
    }

    public static Long sumIntegerArrays(byte[] _2) {
        long sum = 0;
        int position = 0;
        for(int i=0; i<(_2.length/4); i++) {
            sum += (long) ByteArrayUtils.byteArrayToInt(_2, position);
            position+=4;
        }
        return sum;
    }

    public static long byteArrayToLong(byte[] b, int offset){
        return ((long) b[7+offset] << 56)
             | ((long) b[6+offset] & 0xff) << 48
             | ((long) b[5+offset] & 0xff) << 40
             | ((long) b[4+offset] & 0xff) << 32
             | ((long) b[3+offset] & 0xff) << 24
             | ((long) b[2+offset] & 0xff) << 16
             | ((long) b[1+offset] & 0xff) << 8
             | ((long) b[0+offset] & 0xff);
    }

    public static double byteArrayToDouble(byte[] b, int offset) {
        return Double.longBitsToDouble(byteArrayToLong(b, offset));
    }

}
