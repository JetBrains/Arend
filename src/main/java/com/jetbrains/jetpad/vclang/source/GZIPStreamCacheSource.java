package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPStreamCacheSource extends StreamCacheSource {
  private final StreamCacheSource mySource;

  /**
   * Creates a new {@code GZIPStreamCacheSource} from a specified source.
   * @param source  the input source.
   */
  public GZIPStreamCacheSource(StreamCacheSource source) {
    mySource = source;
  }

  @Nullable
  @Override
  protected InputStream getInputStream(ErrorReporter errorReporter) {
    try (InputStream stream = mySource.getInputStream(errorReporter)) {
      return stream == null ? null : new GZIPInputStream(stream);
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, mySource.getModulePath()));
      return null;
    }
  }

  @Nullable
  @Override
  protected OutputStream getOutputStream(ErrorReporter errorReporter) {
    try (OutputStream stream = mySource.getOutputStream(errorReporter)) {
      return stream == null ? null : new GZIPOutputStream(stream);
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, mySource.getModulePath()));
      return null;
    }
  }

  @Nonnull
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
}
