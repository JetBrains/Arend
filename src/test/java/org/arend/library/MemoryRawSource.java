package org.arend.library;

import org.arend.module.ModulePath;
import org.arend.source.StreamRawSource;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MemoryRawSource extends StreamRawSource {
  private final ModulePath myModulePath;
  private final String myText;
  private final long myTimeStamp;

  public MemoryRawSource(ModulePath modulePath, String text, long timeStamp) {
    myModulePath = modulePath;
    myText = text;
    myTimeStamp = timeStamp;
  }

  public MemoryRawSource(ModulePath modulePath, String text) {
    this(modulePath, text, 0);
  }

  @Nonnull
  @Override
  protected InputStream getInputStream() {
    return new ByteArrayInputStream(myText.getBytes(StandardCharsets.UTF_8));
  }

  @Nonnull
  @Override
  public ModulePath getModulePath() {
    return myModulePath;
  }

  @Override
  public long getTimeStamp() {
    return myTimeStamp;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
