package org.arend.frontend.source;

import org.arend.ext.module.ModulePath;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileRawSource extends StreamRawSource {
  private final Path myFile;

  /**
   * Creates a new {@code FileRawSource} from a path to the base directory and a path to the source.
   *
   * @param basePath    a path to the base directory.
   * @param modulePath  a path to the source.
   * @param inTests     true if the source is located in the test directory.
   */
  public FileRawSource(Path basePath, ModulePath modulePath, boolean inTests) {
    super(modulePath, inTests);
    myFile = FileUtils.sourceFile(basePath, modulePath);
  }

  @NotNull
  @Override
  protected InputStream getInputStream() throws IOException {
    return Files.newInputStream(myFile);
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
