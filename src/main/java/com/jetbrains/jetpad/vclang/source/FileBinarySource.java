package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  @Nonnull
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
}
