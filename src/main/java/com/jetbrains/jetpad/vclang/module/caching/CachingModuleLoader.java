package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.serialization.ModuleProtos;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class CachingModuleLoader<SourceIdT extends SourceId> extends SourceModuleLoader<SourceIdT> {
  private final SourceModuleLoader<SourceIdT> mySourceLoader;
  private final PersistenceProvider<SourceIdT> myPersistenceProvider;
  private final CacheStorageSupplier<SourceIdT> myCacheSupplier;
  private final DefinitionLocator<SourceIdT> myDefLocator;
  private final boolean myUseCache;
  private final LocalizedTypecheckerState<SourceIdT> myTcState;

  public CachingModuleLoader(SourceModuleLoader<SourceIdT> sourceLoader, PersistenceProvider<SourceIdT> persistenceProvider, CacheStorageSupplier<SourceIdT> cacheSupplier, DefinitionLocator<SourceIdT> defLocator, boolean useCache) {
    mySourceLoader = sourceLoader;
    myPersistenceProvider = persistenceProvider;
    myCacheSupplier = cacheSupplier;
    myDefLocator = defLocator;
    myUseCache = useCache;
    myTcState = new LocalizedTypecheckerState<>(defLocator);
  }

  @Override
  public SourceIdT locateModule(ModulePath modulePath) {
    return mySourceLoader.locateModule(modulePath);
  }

  @Override
  public Abstract.ClassDefinition load(SourceIdT sourceId) {
    return load(sourceId, myUseCache);
  }

  protected Abstract.ClassDefinition load(SourceIdT sourceId, boolean useCache) {
    Abstract.ClassDefinition result = mySourceLoader.load(sourceId);

    if (result != null && useCache) {
      LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState = loadCache(sourceId);
      if (localState != null) {
        myTcState.useCache(sourceId, localState);
      }
    }

    return result;
  }

  public TypecheckerState getTypecheckerState() {
    return myTcState;
  }

  public Set<SourceIdT> getCachedModules() {
    return myTcState.getCachedModules();
  }

  public boolean persistModule(SourceIdT sourceId) throws IOException {
    LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState = myTcState.getLocal(sourceId);
    if (!localState.isOutOfSync()) {
      return true;
    }
    if (sourceId.getModulePath().getName().equals("Prelude")) {
      return true; // FIXME: hack
    }

    OutputStream cacheStream = myCacheSupplier.getCacheOutputStream(sourceId);
    if (cacheStream == null) {
      return false;
    }

    writeModule(sourceId, localState).writeTo(cacheStream);

    cacheStream.close();
    localState.sync();
    return true;
  }

  private ModuleProtos.Module writeModule(SourceIdT sourceId, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState) {
    ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();
    // TODO: perform actual serialization
    return out.build();
  }

  private LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState loadCache(SourceIdT sourceId) {
    LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState = myTcState.getLocal(sourceId);
    if (!localState.getTypecheckedDefinitions().isEmpty()) {
      // TODO: properly assert that current state and new state are compatible
      return null;
    }
    InputStream cacheStream = myCacheSupplier.getCacheInputStream(sourceId);
    if (cacheStream == null) {
      // TODO[report]: report that loading/loaded smth from cache
      return null;
    }
    try {
      ModuleProtos.Module moduleProto = ModuleProtos.Module.parseFrom(cacheStream);
      loadCachedModule(sourceId, moduleProto, localState);
      return localState;
    } catch (IOException e) {
      return null;
    }
  }

  private boolean loadCachedModule(SourceIdT sourceId, ModuleProtos.Module moduleProto, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) {
    // TODO: actually load
    return false;
  }
}
