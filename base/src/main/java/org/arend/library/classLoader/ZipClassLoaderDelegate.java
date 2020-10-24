package org.arend.library.classLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipClassLoaderDelegate implements ClassLoaderDelegate {
  public ZipFile zipFile;
  private final File myFile;
  private final String myPrefix;

  public ZipClassLoaderDelegate(File file, ZipFile zipFile, String prefix) {
    myFile = file;
    this.zipFile = zipFile;
    myPrefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";
  }

  private byte[] readClass(ZipFile zipFile, String name) throws ClassNotFoundException {
    ZipEntry entry = zipFile.getEntry(myPrefix + name.replace('.', '/') + ".class");
    if (entry == null) {
      return null;
    }

    try (InputStream stream = zipFile.getInputStream(entry)) {
      return stream.readAllBytes();
    } catch (IOException e) {
      throw new ClassNotFoundException("An exception happened during loading of class " + name, e);
    }
  }

  @Override
  public byte[] findClass(String name) throws ClassNotFoundException {
    if (zipFile != null) {
      return readClass(zipFile, name);
    }

    try (ZipFile zipFile = new ZipFile(myFile)) {
      return readClass(zipFile, name);
    } catch (IOException e) {
      throw new ClassNotFoundException("Cannot open zip file '" + myFile.getName() + "'", e);
    }
  }

  @Override
  public String toString() {
    return myFile.getName();
  }
}
