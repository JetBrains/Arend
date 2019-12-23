package org.arend.ext;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileClassLoader extends ClassLoader {
  private final Path myRoot;

  public FileClassLoader(@Nonnull Path root) {
    myRoot = root;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    try {
      byte[] bytes = Files.readAllBytes(myRoot.resolve(name.replace('.', File.separatorChar) + ".class"));
      return defineClass(name, bytes, 0, bytes.length);
    } catch (IOException e) {
      throw new ClassNotFoundException("An exception happened during loading of class " + name, e);
    }
  }
}
