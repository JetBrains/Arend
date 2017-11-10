package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.caching.serialization.*;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CacheManager<SourceIdT extends SourceId> {
  private final PersistenceProvider<SourceIdT> myPersistenceProvider;
  private final CacheStorageSupplier<SourceIdT> myCacheSupplier;
  private final SourceVersionTracker<SourceIdT> myVersionTracker;
  private final DefinitionLocator<SourceIdT> myDefLocator;

  private final LocalizedTypecheckerState<SourceIdT> myTcState;
  private final Set<SourceIdT> myStubsLoaded = new HashSet<>();

  public CacheManager(PersistenceProvider<SourceIdT> persistenceProvider, CacheStorageSupplier<SourceIdT> cacheSupplier,
                      DefinitionLocator<SourceIdT> defLocator, SourceVersionTracker<SourceIdT> versionTracker) {
    myPersistenceProvider = persistenceProvider;
    myCacheSupplier = cacheSupplier;
    myVersionTracker = versionTracker;
    myDefLocator = defLocator;
    myTcState = new LocalizedTypecheckerState<>(defLocator);
  }

  public TypecheckerState getTypecheckerState() {
    return myTcState;
  }

  public Set<SourceIdT> getCachedModules() {
    return myTcState.getCachedModules();
  }

  /**
   * Load persisted cache for a source.
   * <p>
   * Also loads caches for all the dependencies of the requested source.
   * <p>
   * It is assumed that abstract source of this module is available, as well as abstract sources of all the modules
   * that this one refers to (which is probably automatically true by the time you have the source of this module
   * loaded as all the references will have been resolved).
   *
   * @param sourceId  ID of the source to load cache of
   * @param module    root class (module) loaded from the provided source
   *
   * @return <code>true</code> if loading succeeded;
   *         <code>false</code> otherwise.
   * @throws CacheLoadingException if an <code>IOException</code> occurs.
   */
  public boolean loadCache(@Nonnull SourceIdT sourceId, @Nonnull GlobalReferable module) throws CacheLoadingException {
    if (!sourceId.equals(myDefLocator.sourceOf(module))) throw new IllegalArgumentException();
    return loadCache(sourceId);
  }

  private boolean loadCache(@Nonnull SourceIdT sourceId) throws CacheLoadingException {
    if (myStubsLoaded.contains(sourceId)) return true;

    LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState = myTcState.getLocal(sourceId);

    InputStream cacheStream = myCacheSupplier.getCacheInputStream(sourceId);
    if (cacheStream == null) return false;

    try {
      try {
        GZIPInputStream compressedCacheStream = new GZIPInputStream(cacheStream);
        readModule(sourceId, localState, ModuleProtos.Module.parseFrom(compressedCacheStream));
        compressedCacheStream.close();
      } catch (IOException e) {
        throw new CacheLoadingException(sourceId, e);
      }
    } catch (CacheLoadingException e) {
      myStubsLoaded.remove(sourceId);
      // TODO: is this enough?
      myTcState.wipe(sourceId);
      throw e;
    }
    localState.sync();
    return true;
  }

  private void readModule(SourceIdT sourceId, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState, ModuleProtos.Module moduleProto) throws CacheLoadingException {
    if (!myVersionTracker.ensureLoaded(sourceId, moduleProto.getVersion())) {
      throw new CacheLoadingException(sourceId, "Source has changed");
    }

    try {
      DefinitionStateDeserialization<SourceIdT> defStateDeserialization = new DefinitionStateDeserialization<>(sourceId, myPersistenceProvider);
      defStateDeserialization.readStubs(moduleProto.getDefinitionState(), localState);
      myStubsLoaded.add(sourceId);
      ReadCalltargets calltargets = new ReadCalltargets(sourceId, moduleProto.getReferredDefinitionList());
      defStateDeserialization.fillInDefinitions(moduleProto.getDefinitionState(), localState, calltargets);
    } catch (DeserializationError deserializationError) {
      throw new CacheLoadingException(sourceId, deserializationError);
    }
  }

  class ReadCalltargets implements CalltargetProvider {
    private final List<Definition> myCalltargets = new ArrayList<>();

    ReadCalltargets(SourceIdT sourceId, List<ModuleProtos.Module.DefinitionReference> refDefProtos) throws CacheLoadingException {
      for (ModuleProtos.Module.DefinitionReference proto : refDefProtos) {
        final SourceIdT targetSourceId;
        if (!proto.getSourceUrl().isEmpty()) {
          URI uri = URI.create(proto.getSourceUrl());
          targetSourceId = myPersistenceProvider.getModuleId(uri);
          if (targetSourceId == null) {
            throw new CacheLoadingException(sourceId, "Unresolvable source URI: " + uri);
          }
          boolean targetLoaded = loadCache(targetSourceId);
          if (!targetLoaded) {
            throw new CacheLoadingException(sourceId, "Dependency does not support persistence: ");
          }
        } else {
          targetSourceId = sourceId;
        }
        GlobalReferable absDef = myPersistenceProvider.getFromId(targetSourceId, proto.getDefinitionId());
        Definition typechecked = myTcState.getLocal(targetSourceId).getTypechecked(absDef);
        if (typechecked == null) {
          throw new CacheLoadingException(sourceId, "Referred definition was not in cache");
        }
        myCalltargets.add(typechecked);
      }
    }

    @Override
    public Definition getCalltarget(int index) {
      return myCalltargets.get(index);
    }
  }


  /**
   * Persist cache for a source.
   * <p>
   * Also persists caches for all the dependencies of the requested source.
   * <p>
   * Definitions persisted are those and only those that were typechecked and can potentially be referred by compiled references.
   *
   * @param sourceId  ID of the source to persist cache of
   *
   * @return <code>true</code> if persisting succeeded;
   *         <code>false</code> otherwise.
   * @throws CachePersistenceException if an <code>IOException</code> occurs or if a dependency cannot be persisted.
   */
  public boolean persistCache(@Nonnull SourceIdT sourceId) throws CachePersistenceException {
    LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState = myTcState.getLocal(sourceId);
    if (!localState.isOutOfSync()) {
      return true;
    }

    OutputStream cacheStream = myCacheSupplier.getCacheOutputStream(sourceId);
    if (cacheStream == null) {
      return false;
    }

    try {
      try {
        GZIPOutputStream compressedCacheStream = new GZIPOutputStream(cacheStream);
        writeModule(sourceId, localState).writeTo(compressedCacheStream);
        compressedCacheStream.close();
      } catch (IOException e) {
        throw new CachePersistenceException(sourceId, e);
      }
    } catch (CachePersistenceException e) {
      localState.unsync();
      throw e;
    }
    return true;
  }

  private ModuleProtos.Module writeModule(SourceIdT sourceId, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState localState) throws CachePersistenceException {
    ModuleProtos.Module.Builder out = ModuleProtos.Module.newBuilder();
    final WriteCalltargets calltargets = new WriteCalltargets(sourceId);
    // Serialize the module first in order to populate the call-target registry
    DefinitionStateSerialization defStateSerialization = new DefinitionStateSerialization(myPersistenceProvider, calltargets);
    out.setDefinitionState(defStateSerialization.writeDefinitionState(localState));
    localState.sync();
    // now write the call-target registry
    out.addAllReferredDefinition(calltargets.write());

    out.setVersion(myVersionTracker.getCurrentVersion(sourceId));
    return out.build();
  }

  class WriteCalltargets implements CalltargetIndexProvider {
    private final LinkedHashMap<Definition, Integer> myCalltargets = new LinkedHashMap<>();
    private final SourceId mySourceId;

    WriteCalltargets(SourceId sourceId) {
      mySourceId = sourceId;
    }

    @Override
    public int getDefIndex(Definition definition) {
      return myCalltargets.computeIfAbsent(definition, k -> myCalltargets.size());
    }

    private List<ModuleProtos.Module.DefinitionReference> write() throws CachePersistenceException {
      List<ModuleProtos.Module.DefinitionReference> out = new ArrayList<>();
      for (Definition calltarget : myCalltargets.keySet()) {
        ModuleProtos.Module.DefinitionReference.Builder entry = ModuleProtos.Module.DefinitionReference.newBuilder();
        SourceIdT targetSourceId = myDefLocator.sourceOf(calltarget.getReferable());
        if (!mySourceId.equals(targetSourceId)) {
          boolean targetPersisted = persistCache(targetSourceId);
          if (!targetPersisted) {
            throw new CachePersistenceException(mySourceId, "Dependency does not support persistence " + targetSourceId);
          }
          entry.setSourceUrl(myPersistenceProvider.getUri(targetSourceId).toString());
        }
        entry.setDefinitionId(myPersistenceProvider.getIdFor(calltarget.getReferable()));
        out.add(entry.build());
      }
      return out;
    }
  }
}
