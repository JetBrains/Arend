package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.serialization.*;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class CachingModuleLoader<SourceIdT extends SourceId> extends SourceModuleLoader<SourceIdT> {
  private final SourceModuleLoader<SourceIdT> mySourceLoader;
  private final PersistenceProvider<SourceIdT> myPersistenceProvider;
  private final CacheStorageSupplier<SourceIdT> myCacheSupplier;
  private final DefinitionLocator<SourceIdT> myDefLocator;
  private final boolean myUseCache;
  private final LocalizedTypecheckerState<SourceIdT> myTcState;
  private final Map<SourceIdT, Abstract.ClassDefinition> myAbstractLoaded = new HashMap<>();
  private final Set<SourceIdT> myStubsLoaded = new HashSet<>();

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
    return loadWithResult(sourceId).definition;
  }

  public Result loadWithResult(SourceIdT sourceIdT) {
    return load(sourceIdT, myUseCache);
  }

  public Result load(SourceIdT sourceId, boolean useCache) {
    Abstract.ClassDefinition result = myAbstractLoaded.get(sourceId);
    if (result == null) {
      result = mySourceLoader.load(sourceId);
      if (result != null) {
        myAbstractLoaded.put(sourceId, result);
      }
    }

    if (result != null && useCache) {
      try {
        boolean cacheLoaded = loadCache(sourceId);
        return Result.Cache(result, cacheLoaded);
      } catch (CacheLoadingException e) {
        // TODO: TC state is now potentially corrupted, clean it up properly
        myStubsLoaded.remove(sourceId);
        myTcState.wipe(sourceId);
        return Result.CacheError(result, e);
      }
    } else {
      return Result.NoCache(result);
    }
  }

  public TypecheckerState getTypecheckerState() {
    return myTcState;
  }

  public Set<SourceIdT> getCachedModules() {
    return myTcState.getCachedModules();
  }

  @Deprecated
  public void hackForceModuleSync(SourceIdT sourceId) {
    myTcState.getLocal(sourceId).sync();
  }

  public boolean persistModule(SourceIdT sourceId) throws IOException, CachePersistenceException {
    LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState = myTcState.getLocal(sourceId);
    if (!localState.isOutOfSync()) {
      return true;
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

  private ModuleProtos.Module writeModule(SourceIdT sourceId, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState) throws CachePersistenceException {
    ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();
    final WriteCalltargets calltargets = new WriteCalltargets(sourceId);
    // Serialize the module first to populate the call-target registry
    DefinitionStateSerialization defStateSerialization = new DefinitionStateSerialization(myPersistenceProvider, calltargets);
    out.setDefinitionState(defStateSerialization.writeDefinitionState(localState));
    // now write the call-target registry
    out.addAllReferredDefinition(calltargets.write());
    return out.build();
  }

  private boolean loadCache(SourceIdT sourceId) throws CacheLoadingException {
    InputStream cacheStream = myCacheSupplier.getCacheInputStream(sourceId);
    if (cacheStream == null) {
      return false;
    }
    try {
      LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState = myTcState.getLocal(sourceId);
      ModuleProtos.Module moduleProto = ModuleProtos.Module.parseFrom(cacheStream);
      loadCachedState(sourceId, localState, moduleProto);
      return true;
    } catch (IOException e) {
      throw new CacheLoadingException(sourceId, e);
    }
  }

  private void loadCachedState(SourceIdT sourceId, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState, ModuleProtos.Module moduleProto) throws CacheLoadingException {
    DefinitionStateDeserialization<SourceIdT> defStateDeserialization = new DefinitionStateDeserialization<>(sourceId, myPersistenceProvider);
    try {
      defStateDeserialization.readStubs(moduleProto.getDefinitionState(), localState);
      myStubsLoaded.add(sourceId);
      ReadCalltargets calltargets = new ReadCalltargets(sourceId, moduleProto.getReferredDefinitionList());
      defStateDeserialization.fillInDefinitions(moduleProto.getDefinitionState(), localState, calltargets);
    } catch (DeserializationError deserializationError) {
      throw new CacheLoadingException(sourceId, deserializationError);
    }
  }

  public static class Result {
    public final Abstract.ClassDefinition definition;
    public final boolean cacheLoaded;
    public final CacheLoadingException exception;

    private Result(Abstract.ClassDefinition definition, boolean cacheLoaded, CacheLoadingException exception) {
      this.definition = definition;
      this.cacheLoaded = cacheLoaded;
      this.exception = exception;
    }

    private static Result NoCache(Abstract.ClassDefinition definition) {
      return new Result(definition, false, null);
    }
    private static Result Cache(Abstract.ClassDefinition definition, boolean cacheLoaded) {
      return new Result(definition, cacheLoaded, null);
    }
    private static Result CacheError(Abstract.ClassDefinition definition, CacheLoadingException e) {
      return new Result(definition, false, e);
    }
  }


  class WriteCalltargets implements CalltargetIndexProvider {
    private final LinkedHashMap<Definition, Integer> myCalltargets = new LinkedHashMap<>();
    private final SourceId mySourceId;

    WriteCalltargets(SourceId sourceId) {
      mySourceId = sourceId;
    }

    @Override
    public int getDefIndex(Definition definition) {
      Integer index = myCalltargets.get(definition);
      if (index == null) {
        index = myCalltargets.size();
        myCalltargets.put(definition, index);
      }
      return index;
    }

    private List<ModuleProtos.Module.DefinitionReference> write() throws CachePersistenceException {
      List<ModuleProtos.Module.DefinitionReference> out = new ArrayList<>();
      for (Definition calltarget : myCalltargets.keySet()) {
        ModuleProtos.Module.DefinitionReference.Builder entry = ModuleProtos.Module.DefinitionReference.newBuilder();
        SourceIdT targetSourceId = myDefLocator.sourceOf(calltarget.getAbstractDefinition());
        if (!mySourceId.equals(targetSourceId)) {
          boolean targetPersisted = false;
          try {
            targetPersisted = persistModule(targetSourceId);
          } catch (IOException e) {
            throw new CachePersistenceException(mySourceId, e);
          }
          if (!targetPersisted) {
            throw new CachePersistenceException(mySourceId, "Dependency cannot be persisted: " + targetSourceId);
          }
          entry.setSourceUrl(myPersistenceProvider.getUrl(targetSourceId).toString());
        }
        entry.setDefinitionId(myPersistenceProvider.getIdFor(calltarget.getAbstractDefinition()));
        out.add(entry.build());
      }
      return out;
    }
  }

  class ReadCalltargets extends CalltargetProvider.BaseCalltargetProvider implements CalltargetProvider {
    private final List<Definition> myCalltargets = new ArrayList<>();

    ReadCalltargets(SourceIdT sourceId, List<ModuleProtos.Module.DefinitionReference> refDefProtos) throws CacheLoadingException {
      for (ModuleProtos.Module.DefinitionReference proto : refDefProtos) {
          final SourceIdT targetSourceId;
          if (!proto.getSourceUrl().isEmpty()) {
            try {
              URL url = new URL(proto.getSourceUrl());
              targetSourceId = myPersistenceProvider.getModuleId(url);
              if (targetSourceId == null) {
                throw new CacheLoadingException(sourceId, "Unresolvable source URL: " + url);
              }
              if (!myStubsLoaded.contains(targetSourceId)) {
                Result result = load(targetSourceId, true);
                if (!result.cacheLoaded) {
                  String reason = result.exception == null ? "no cache for " + targetSourceId : "" + result.exception;
                  throw new CacheLoadingException(sourceId, "can’t load dependency cache: " + reason);
                }
              }
            } catch (MalformedURLException e) {
              throw new CacheLoadingException(sourceId, "Malformed source URL (" + e.getMessage() + "): " + proto.getSourceUrl());
            }
          } else {
            targetSourceId = sourceId;
          }
          Abstract.Definition absDef = myPersistenceProvider.getFromId(targetSourceId, proto.getDefinitionId());
          Definition typechecked = myTcState.getLocal(targetSourceId).getTypechecked(absDef);
          if (typechecked == null) {
            throw new CacheLoadingException(sourceId, "Referred definition was not in cache");
          }
          myCalltargets.add(typechecked);
      }
    }

    @Override
    protected Definition getDef(int index) {
      return myCalltargets.get(index);
    }
  }
}
