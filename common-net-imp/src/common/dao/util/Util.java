package common.dao.util;

public class Util {

    public static void printSection(String s) {
        s = "-[ " + s + " ]";
        while (s.length() < 79) {
            s = "=" + s;
        }
        System.out.println(s);
    }
}