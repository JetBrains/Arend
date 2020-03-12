package org.arend.util;

public class StringFormat {
  // We use this hack as String.format is not emulated in GWT
  public static String rightPad(int width, String s) {
    if (s.length() >= width) {
      return s;
    } else {
      return s + new String(new char[width - s.length()]).replace('\0', ' ');
    }
  }

  public static String rightPad(int width, char c) {
    return rightPad(width, Character.toString(c));
  }
}
