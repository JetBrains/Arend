package com.jetbrains.jetpad.vclang.frontend.storage;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.parser.ParseSource;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Prelude;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PreludeStorage implements Storage<PreludeStorage.SourceId> {
  // It is difficult to handle this in a uniform way due to differences in OS file systems
  // (and bugs in JDK), therefore all that is left is to be careful in keeping all these paths synced.
  private static final Path BASE_PATH = Paths.get("lib", "Prelude");
  public static final Path SOURCE_PATH = FileStorage.sourceFile(BASE_PATH);
  public static final Path CACHE_PATH = FileStorage.cacheFile(BASE_PATH);
  public static final String SOURCE_RESOURCE_PATH = "/lib/" + SOURCE_PATH.getFileName();
  public static final String CACHE_RESOURCE_PATH = "/lib/" + CACHE_PATH.getFileName();

  public static final ModulePath PRELUDE_MODULE_PATH = new ModulePath("Prelude");
  public final SourceId preludeSourceId = new SourceId();
  private final NameResolver myNameResolver;

  public PreludeStorage(NameResolver nameResolver) {
    myNameResolver = nameResolver;
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    if (sourceId != preludeSourceId) return null;
    return Prelude.class.getResourceAsStream(CACHE_RESOURCE_PATH);
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    // Prelude cache is generated during build and stored as a resource,
    // therefore PreludeStorage does not support serialization of Prelude in runtime.
    return null;
  }

  @Override
  public SourceId locateModule(@Nonnull ModulePath modulePath) {
    if (modulePath.getParent().toList().isEmpty() && modulePath.getName().equals("Prelude")) {
      return preludeSourceId;
    } else {
      return null;
    }
  }

  @Override
  public boolean isAvailable(@Nonnull SourceId sourceId) {
    return sourceId == preludeSourceId;
  }

  @Override
  public LoadResult loadSource(@Nonnull SourceId sourceId, @Nonnull ErrorReporter errorReporter) {
    try {
      if (sourceId != preludeSourceId) return null;
      InputStream stream = Prelude.class.getResourceAsStream(SOURCE_RESOURCE_PATH);
      if (stream == null) {
        throw new IllegalStateException("Prelude source resource not found");
      }
      Group result = new ParseSource(preludeSourceId, new InputStreamReader(stream, StandardCharsets.UTF_8)) {}.load(errorReporter, null, new EmptyScope(), myNameResolver);
      return LoadResult.make(result, 1);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public long getAvailableVersion(@Nonnull SourceId sourceId) {
    if (sourceId != preludeSourceId) return 0;
    return 1;  // TODO: no really
  }


  public static class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private SourceId() {}

    @Override
    public ModulePath getModulePath() {
      return PRELUDE_MODULE_PATH;
    }

    @Override
    public String toString() {
      return "PRELUDE";
    }
  }
}
