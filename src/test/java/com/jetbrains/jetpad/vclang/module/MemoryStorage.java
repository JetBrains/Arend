package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.namespace.ModuleRegistry;
import com.jetbrains.jetpad.vclang.frontend.parser.ParseSource;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MemoryStorage implements Storage<MemoryStorage.SourceId> {
  private final Map<ModulePath, String> mySources = new HashMap<>();
  private final Map<SourceId, ByteArrayOutputStream> myCaches = new HashMap<>();
  private final ModuleRegistry myModuleRegistry;
  private Scope myGlobalScope = new EmptyScope();
  private final NameResolver myNameResolver;

  public MemoryStorage(ModuleRegistry moduleRegistry, NameResolver nameResolver) {
    myModuleRegistry = moduleRegistry;
    myNameResolver = nameResolver;
  }

  public void setPreludeNamespace(Namespace ns) {
    myGlobalScope = new NamespaceScope(ns);
  }

  public SourceId add(ModulePath modulePath, String source) {
    String old = mySources.put(modulePath, source);
    assert old == null;
    return locateModule(modulePath);
  }

  public void remove(ModulePath modulePath) {
    String old = mySources.remove(modulePath);
    assert old != null;
  }

  @Override
  public SourceId locateModule(@Nonnull ModulePath modulePath) {
    String data = mySources.get(modulePath);
    return data != null ? new SourceId(modulePath, data) : null;
  }

  @Override
  public boolean isAvailable(@Nonnull SourceId sourceId) {
    String myData = mySources.get(sourceId.getModulePath());
    //noinspection StringEquality
    return myData == sourceId.myData;
  }

  @Override
  public Abstract.ClassDefinition loadSource(@Nonnull SourceId sourceId, @Nonnull ErrorReporter errorReporter) {
    try {
      return isAvailable(sourceId) ? new ParseSource(sourceId, new StringReader(sourceId.myData)) {}.load(errorReporter, myModuleRegistry, myGlobalScope, myNameResolver) : null;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
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
