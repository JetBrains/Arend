package org.arend.frontend.source;

import org.arend.ext.module.ModulePath;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileRawSource extends StreamRawSource {
  private final ZipFile myFile;
  private final ZipEntry myEntry;

  public ZipFileRawSource(ModulePath modulePath, ZipFile file, ZipEntry entry) {
    super(modulePath, false);
    myFile = file;
    myEntry = entry;
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
  protected @NotNull InputStream getInputStream() throws IOException {
    return myFile.getInputStream(myEntry);
  }
}
