package org.arend.library.classLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileClassLoaderDelegate implements ClassLoaderDelegate {
  private final Path myRoot;

  public FileClassLoaderDelegate(Path root) {
    myRoot = root;
  }

  @Override
  public byte[] findClass(String name) throws ClassNotFoundException {
    Path file = myRoot.resolve(name.replace('.', File.separatorChar) + ".class");
    try {
      return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
    } catch (IOException e) {
      throw new ClassNotFoundException("An exception happened during loading of class " + name, e);
    }
  }

  @Override
  public String toString() {
    return myRoot.toString();
  }
}
