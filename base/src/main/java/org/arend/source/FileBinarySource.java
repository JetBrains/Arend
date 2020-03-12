package org.arend.source;

import org.arend.ext.module.ModulePath;
import org.arend.library.SourceLibrary;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileBinarySource extends StreamBinarySource {
  private final Path myFile;
  private final ModulePath myModulePath;

  /**
   * Creates a new {@code FileBinarySource} from a path to the base directory and a path to the source.
   *
   * @param basePath    a path to the base directory.
   * @param modulePath  a path to the source.
   */
  public FileBinarySource(Path basePath, ModulePath modulePath) {
    myFile = FileUtils.binaryFile(basePath, modulePath);
    myModulePath = modulePath;
  }

  @NotNull
  @Override
  public ModulePath getModulePath() {
    return myModulePath;
  }

  @Nullable
  @Override
  protected InputStream getInputStream() throws IOException {
    return Files.newInputStream(myFile);
  }

  @Nullable
  @Override
  protected OutputStream getOutputStream() throws IOException {
    Files.createDirectories(myFile.getParent());
    return Files.newOutputStream(myFile, StandardOpenOption.CREATE);
  }

  @Override
  public long getTimeStamp() {
    try {
      return Files.getLastModifiedTime(myFile).toMillis();
    } catch (IOException e) {
      return 0;
    }
  }

  @Override
  public boolean isAvailable() {
    return Files.exists(myFile);
  }

  @Override
  public boolean delete(SourceLibrary library) {
    try {
      Files.deleteIfExists(myFile);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
