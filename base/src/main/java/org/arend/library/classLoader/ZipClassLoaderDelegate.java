package org.arend.library.classLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipClassLoaderDelegate implements ClassLoaderDelegate {
  private final ZipFile myZipFile;
  private final String myPrefix;

  public ZipClassLoaderDelegate(ZipFile zipFile, String prefix) {
    myZipFile = zipFile;
    myPrefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";
  }

  @Override
  public byte[] findClass(String name) throws ClassNotFoundException {
    ZipEntry entry = myZipFile.getEntry(myPrefix + name.replace('.', '/') + ".class");
    if (entry == null) {
      return null;
    }

    try (InputStream stream = myZipFile.getInputStream(entry)) {
      return stream.readAllBytes();
    } catch (IOException e) {
      throw new ClassNotFoundException("An exception happened during loading of class " + name, e);
    }
  }

  @Override
  public String toString() {
    return myZipFile.getName();
  }
}
