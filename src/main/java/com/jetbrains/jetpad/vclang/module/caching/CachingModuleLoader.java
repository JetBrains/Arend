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
    return load(sourceId, myUseCache);
  }

  protected Abstract.ClassDefinition load(SourceIdT sourceId, boolean useCache) {
    Abstract.ClassDefinition result = myAbstractLoaded.get(sourceId);
    if (result == null) {
      result = mySourceLoader.load(sourceId);
      if (result != null) {
        myAbstractLoaded.put(sourceId, result);
      }
    }

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
    final WriteCalltargets calltargets = new WriteCalltargets(sourceId);
    // Serialize the module first to populate the call-target registry
    DefinitionStateSerialization defStateSerialization = new DefinitionStateSerialization(myPersistenceProvider, calltargets);
    out.setDefinitionState(defStateSerialization.writeDefinitionState(localState));
    // now write the call-target registry
    out.addAllReferredDefinition(calltargets.write());
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
    DefinitionStateDeserialization<SourceIdT> defStateDeserialization = new DefinitionStateDeserialization<>(sourceId, myPersistenceProvider);
    defStateDeserialization.readStubs(moduleProto.getDefinitionState(), state);
    myStubsLoaded.add(sourceId);
    ReadCalltargets calltargets = new ReadCalltargets(sourceId, moduleProto.getReferredDefinitionList());
    defStateDeserialization.fillInDefinitions(moduleProto.getDefinitionState(), state, calltargets);
    return true;
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

    private List<ModuleProtos.Module.DefinitionReference> write() {
      List<ModuleProtos.Module.DefinitionReference> out = new ArrayList<>();
      for (Definition calltarget : myCalltargets.keySet()) {
        ModuleProtos.Module.DefinitionReference.Builder entry = ModuleProtos.Module.DefinitionReference.newBuilder();
        SourceIdT targetSourceId = myDefLocator.sourceOf(calltarget.getAbstractDefinition());
        if (!mySourceId.equals(targetSourceId)) {
          boolean targetPersisted = false;
          try {
            targetPersisted = persistModule(targetSourceId);
          } catch (IOException ignored) {
          }
          if (!targetPersisted) {
            throw new IllegalStateException("Can't persist a referred module");  // TODO[serial]: report proper error
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

    ReadCalltargets(SourceIdT sourceId, List<ModuleProtos.Module.DefinitionReference> refDefProtos) {
      for (ModuleProtos.Module.DefinitionReference proto : refDefProtos) {
        try {
          final SourceIdT targetSourceId;
          if (!proto.getSourceUrl().isEmpty()) {
            URL url = new URL(proto.getSourceUrl());
            targetSourceId = myPersistenceProvider.getModuleId(url);
            if (targetSourceId == null) {
              throw new IllegalStateException();  // TODO[serial]: report
            }
            if (!myStubsLoaded.contains(targetSourceId)) {
              load(targetSourceId);
            }
          } else {
            targetSourceId = sourceId;
          }
          Abstract.Definition absDef = myPersistenceProvider.getFromId(targetSourceId, proto.getDefinitionId());
          Definition typechecked = myTcState.getLocal(targetSourceId).getTypechecked(absDef);
          if (typechecked == null) {
            throw new IllegalStateException();
          }
          myCalltargets.add(typechecked);
        } catch (MalformedURLException e) {
          throw new IllegalStateException();  // TODO[serial]: report
        }
      }
    }

    @Override
    protected Definition getDef(int index) {
      return myCalltargets.get(index);
    }
  }
}
