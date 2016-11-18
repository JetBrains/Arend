package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.GlobalInstancePool;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocalizedTypecheckerState<SourceIdT extends SourceId> implements TypecheckerState {
  private final DefinitionLocator<SourceIdT> myDefLocator;
  private final Map<SourceIdT, LocalTypecheckerState> myStates = new HashMap<>();
  private final GlobalInstancePool myInstancePool = new GlobalInstancePool();

  public LocalizedTypecheckerState(DefinitionLocator<SourceIdT> defLocator) {
    myDefLocator = defLocator;
  }

  @Override
  public void record(Abstract.Definition def, Definition res) {
    getLocal(def).record(def, res);
  }

  @Override
  public Definition getTypechecked(Abstract.Definition def) {
    return getLocal(def).getTypechecked(def);
  }

  @Override
  public GlobalInstancePool getInstancePool() {
    return myInstancePool;
  }

  public void useCache(SourceIdT sourceId, LocalTypecheckerState localState) {
    return;
  }

  public Set<SourceIdT> getCachedModules() {
    return myStates.keySet();
  }

  public LocalTypecheckerState getLocal(SourceIdT sourceId) {
    if (sourceId == null) throw new IllegalArgumentException();
    LocalTypecheckerState state = myStates.get(sourceId);
    if (state == null) {
      state = new LocalTypecheckerState();
      myStates.put(sourceId, state);
    }
    return state;
  }

  private LocalTypecheckerState getLocal(Abstract.Definition def) {
    return getLocal(myDefLocator.sourceOf(def));
  }


  public class LocalTypecheckerState implements TypecheckerState {
    private boolean myIsOutOfSync = false;
    private final Map<Abstract.Definition, Definition> myDefinitions = new HashMap<>();

    @Override
    public void record(Abstract.Definition def, Definition res) {
      if (myDefinitions.put(def, res) != res) {
        myIsOutOfSync = true;
      }
    }

    // FIXME: DRY
    @Override
    public Definition getTypechecked(Abstract.Definition def) {
      assert def != null;
      Abstract.Definition definition = def;
      while (definition instanceof Abstract.ClassView) {
        definition = ((Abstract.ClassView) definition).getUnderlyingClassDefCall().getReferent();
      }
      if (definition instanceof Abstract.ClassViewField) {
        definition = ((Abstract.ClassViewField) definition).getUnderlyingField();
      }
      if (definition == null) {
        throw new IllegalStateException("Internal error: " + def + " was not resolved");
      }
      return myDefinitions.get(definition);
    }

    @Override
    public GlobalInstancePool getInstancePool() {
      throw new UnsupportedOperationException();
    }

    public Set<Abstract.Definition> getTypecheckedDefinitions() {
      return myDefinitions.keySet();
    }

    public boolean isOutOfSync() {
      return myIsOutOfSync;
    }

    public void sync() {
      myIsOutOfSync = false;
    }
  }
}
