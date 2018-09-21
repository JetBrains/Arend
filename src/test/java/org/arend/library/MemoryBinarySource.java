package org.arend.library;

import org.arend.module.ModulePath;
import org.arend.source.StreamBinarySource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MemoryBinarySource extends StreamBinarySource {
  private final ModulePath myModulePath;
  private ByteArrayOutputStream myOutputStream;

  public MemoryBinarySource(ModulePath modulePath) {
    myModulePath = modulePath;
  }

  @Nullable
  @Override
  protected InputStream getInputStream() {
    return new ByteArrayInputStream(myOutputStream.toByteArray());
  }

  @Nullable
  @Override
  protected OutputStream getOutputStream() {
    if (myOutputStream == null) {
      myOutputStream = new ByteArrayOutputStream();
    }
    return myOutputStream;
  }

  @Nonnull
  @Override
  public ModulePath getModulePath() {
    return myModulePath;
  }

  @Override
  public long getTimeStamp() {
    return 0;
  }

  @Override
  public boolean isAvailable() {
    return myOutputStream != null;
  }
}
