package org.arend.library.classLoader;

public interface ClassLoaderDelegate {
  byte[] findClass(String name) throws ClassNotFoundException;
}
