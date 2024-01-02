package org.arend.ext.util;

public class StringUtils {
  public static String number(int n, String r) {
    return n + r + (n == 1 ? "" : "s");
  }

  public static String suffix(int n) {
    if (n >= 10 && n < 20) {
      return "th";
    }
    return switch (n % 10) {
      case 1 -> "st";
      case 2 -> "nd";
      case 3 -> "rd";
      default -> "th";
    };
  }

  public static String ordinal(int n) {
    return n + suffix(n);
  }
}
