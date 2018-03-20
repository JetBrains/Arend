package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.source.StreamBinarySource;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PreludeResourceSource extends StreamBinarySource {
  public static final Path BASE_PATH = Paths.get("lib");
  private static final Path BINARY_PATH = FileUtils.binaryFile(BASE_PATH, Prelude.MODULE_PATH);
  private static final String BINARY_RESOURCE_PATH = "/lib/" + BINARY_PATH.getFileName();

  @Nullable
  @Override
  protected InputStream getInputStream() {
    return Prelude.class.getResourceAsStream(BINARY_RESOURCE_PATH);
  }

  @Nullable
  @Override
  protected OutputStream getOutputStream() {
    return null;
  }

  @Nonnull
  @Override
  public ModulePath getModulePath() {
    return Prelude.MODULE_PATH;
  }

  @Override
  public long getTimeStamp() {
    return 0;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
