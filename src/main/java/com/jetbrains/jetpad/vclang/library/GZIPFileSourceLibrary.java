package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.error.ExceptionError;
import com.jetbrains.jetpad.vclang.source.FileCacheSource;
import com.jetbrains.jetpad.vclang.source.GZIPStreamCacheSource;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GZIPFileSourceLibrary extends UnmodifiableSourceLibrary {
  private final Path myBasePath;

  /**
   * Creates a new {@code UnmodifiableFileSourceLibrary}
   *
   * @param basePath          a path from which files will be taken.
   * @param name              the name of this library.
   * @param typecheckerState  a typechecker state in which the result of loading of cached modules will be stored.
   */
  public GZIPFileSourceLibrary(Path basePath, String name, TypecheckerState typecheckerState) {
    super(name, typecheckerState);
    myBasePath = basePath;
  }

  @Nullable
  @Override
  public Source getCacheSource(ModulePath modulePath) {
    return new GZIPStreamCacheSource(new FileCacheSource(myBasePath, modulePath));
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    List<ModulePath> modules = new ArrayList<>();
    try {
      Files.walkFileTree(myBasePath, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.getFileName().toString().endsWith(FileUtils.SERIALIZED_EXTENSION)) {
            ModulePath modulePath = FileUtils.modulePath(myBasePath.relativize(file), FileUtils.SERIALIZED_EXTENSION);
            if (modulePath != null) {
              modules.add(modulePath);
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      errorReporter.report(new ExceptionError(e, getName()));
      return null;
    }

    return new LibraryHeader(modules, Collections.emptyList());
  }
}
