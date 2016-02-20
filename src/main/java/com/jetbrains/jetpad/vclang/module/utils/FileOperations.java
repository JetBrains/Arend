package com.jetbrains.jetpad.vclang.module.utils;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileOperations {
  public static final String EXTENSION = ".vc";
  public static final String SERIALIZED_EXTENSION = ".vcc";

  public static File getFile(File dir, ModulePath modulePath) {
    return modulePath.getParent() == null ? dir : new File(getFile(dir, modulePath.getParent()), modulePath.getName());
  }

  public static File getFile(File dir, ModulePath modulePath, String ext) {
    return new File(getFile(dir, modulePath.getParent()), modulePath.getName() + ext);
  }

  public static String getExtFileName(File file, String extension) {
    String name = file.getName();
    if (name.endsWith(extension)) {
      return name.substring(0, name.length() - extension.length());
    } else {
      return null;
    }
  }

  public static byte[] calcSha256(File file) {
    if (!file.exists()) {
      return new byte[0];
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
      byte[] dataBytes = new byte[1024];
      int nread;
      while ((nread = stream.read(dataBytes)) != -1) {
        md.update(dataBytes, 0, nread);
      }
      return md.digest();
    } catch (NoSuchAlgorithmException | IOException e) {
      return new byte[0];
    }
  }

  public static String sha256ToStr(byte[] sha256) {
    StringBuilder sb = new StringBuilder();
    for (byte b : sha256) {
      sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
    }
    return sb.toString();
  }

  public static byte[] strToSha256(String str) {
    if (str.length() != 64)
      return null;
    byte[] result = new byte[str.length() / 2];
    for (int i = 0; i < str.length(); i += 2) {
      if (!Character.isLetterOrDigit(str.charAt(i)) || !Character.isLetterOrDigit(str.charAt(i + 1))) {
        return null;
      }
      try {
        int intVal = Integer.valueOf(str.substring(i, i + 2));
        result[i / 2] = (byte) intVal;
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return result;
  }
}
