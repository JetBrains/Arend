package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileCacheSource extends StreamCacheSource {
  private final Path myFile;
  private final ModulePath myModulePath;

  /**
   * Creates a new {@code FileCacheSource} from a path to the base directory and a path to the source.
   *
   * @param basePath    a path to the base directory.
   * @param modulePath  a path to the source.
   */
  public FileCacheSource(Path basePath, ModulePath modulePath) {
    myFile = FileUtils.cacheFile(basePath, modulePath);
    myModulePath = modulePath;
  }

  @Nonnull
  @Override
  public ModulePath getModulePath() {
    return myModulePath;
  }

  @Nullable
  @Override
  protected InputStream getInputStream(ErrorReporter errorReporter) {
    try {
      return Files.newInputStream(myFile);
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, myModulePath));
      return null;
    }
  }

  @Nullable
  @Override
  protected OutputStream getOutputStream(ErrorReporter errorReporter) {
    try {
      return Files.newOutputStream(myFile);
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, myModulePath));
      return null;
    }
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
