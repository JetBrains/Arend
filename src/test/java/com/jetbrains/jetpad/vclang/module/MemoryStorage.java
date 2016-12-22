package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.frontend.parser.ParseSource;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MemoryStorage implements SourceSupplier<MemoryStorage.SourceId>, CacheStorageSupplier<MemoryStorage.SourceId> {
  private final Map<ModulePath, String> mySources = new HashMap<>();
  private final Map<SourceId, ByteArrayOutputStream> myCaches = new HashMap<>();

  public void add(ModulePath modulePath, String source) {
    String old = mySources.put(modulePath, source);
    assert old == null;
  }

  public void remove(ModulePath modulePath) {
    String old = mySources.remove(modulePath);
    assert old != null;
  }

  @Override
  public SourceId locateModule(ModulePath modulePath) {
    String data = mySources.get(modulePath);
    return data != null ? new SourceId(modulePath, data) : null;
  }

  @Override
  public boolean isAvailable(SourceId sourceId) {
    String myData = mySources.get(sourceId.getModulePath());
    //noinspection StringEquality
    return myData == sourceId.myData;
  }

  @Override
  public Abstract.ClassDefinition loadSource(SourceId sourceId, ErrorReporter errorReporter) throws IOException {
    return isAvailable(sourceId) ? new ParseSource(sourceId, new StringReader(sourceId.myData)) {}.load(errorReporter) : null;
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    ByteArrayOutputStream stream = myCaches.get(sourceId);
    return stream != null ? new ByteArrayInputStream(stream.toByteArray()) : null;
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    myCaches.put(sourceId, stream);
    return stream;
  }

  public class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private final ModulePath myPath;
    private final String myData;

    private SourceId(ModulePath path, String data) {
      myPath = path;
      myData = data;
    }

    @Override
    public ModulePath getModulePath() {
      return myPath;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SourceId sourceId = (SourceId) o;

      return myPath != null ? myPath.equals(sourceId.myPath) : sourceId.myPath == null;
    }

    @Override
    public int hashCode() {
      return myPath != null ? myPath.hashCode() : 0;
    }
  }
}
