package util;

public class DtoVars {

    public static <T> T getVariant(T x, T y) {
        if (x == null) {
            return y;
        } else {
            return x;
        }
    }
}