package org.arend.source;

import org.arend.ext.module.ModulePath;
import org.arend.library.SourceLibrary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPStreamBinarySource extends StreamBinarySource {
  private final StreamBinarySource mySource;

  /**
   * Creates a new {@code GZIPStreamBinarySource} from a specified source.
   * @param source  the input source.
   */
  public GZIPStreamBinarySource(StreamBinarySource source) {
    mySource = source;
  }

  @Nullable
  @Override
  protected InputStream getInputStream() throws IOException {
    InputStream stream = mySource.getInputStream();
    return stream == null ? null : new GZIPInputStream(stream);
  }

  @Nullable
  @Override
  protected OutputStream getOutputStream() throws IOException {
    OutputStream stream = mySource.getOutputStream();
    return stream == null ? null : new GZIPOutputStream(stream);
  }

  @NotNull
  @Override
  public ModulePath getModulePath() {
    return mySource.getModulePath();
  }

  @Override
  public long getTimeStamp() {
    return mySource.getTimeStamp();
  }

  @Override
  public boolean isAvailable() {
    return mySource.isAvailable();
  }

  @Override
  public boolean delete(SourceLibrary library) {
    return mySource.delete(library);
  }
}
