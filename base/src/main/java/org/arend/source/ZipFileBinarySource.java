package org.arend.source;

import org.arend.ext.module.ModulePath;
import org.arend.library.SourceLibrary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileBinarySource extends StreamBinarySource {
  private final ModulePath myModulePath;
  private final ZipFile myFile;
  private final ZipEntry myEntry;

  public ZipFileBinarySource(ModulePath modulePath, ZipFile file, ZipEntry entry) {
    myModulePath = modulePath;
    myFile = file;
    myEntry = entry;
  }

  @Override
  public @NotNull ModulePath getModulePath() {
    return myModulePath;
  }

  @Override
  public long getTimeStamp() {
    return 0;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  protected @Nullable InputStream getInputStream() throws IOException {
    return myFile.getInputStream(myEntry);
  }

  @Override
  protected @Nullable OutputStream getOutputStream() {
    return null;
  }

  @Override
  public boolean delete(SourceLibrary library) {
    return false;
  }
}
