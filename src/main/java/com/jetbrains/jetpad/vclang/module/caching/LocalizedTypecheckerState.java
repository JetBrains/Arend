package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocalizedTypecheckerState<SourceIdT extends SourceId> implements TypecheckerState {
  private final DefinitionLocator<SourceIdT> myDefLocator;
  private final Map<SourceIdT, LocalTypecheckerState> myStates = new HashMap<>();

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

  public Set<SourceIdT> getCachedModules() {
    return myStates.keySet();
  }

  public LocalTypecheckerState getLocal(@Nonnull SourceIdT sourceId) {
    return myStates.computeIfAbsent(sourceId, k -> new LocalTypecheckerState());
  }

  private LocalTypecheckerState getLocal(Abstract.Definition def) {
    SourceIdT sourceId = myDefLocator.sourceOf(def);
    if (sourceId == null) {
      throw new IllegalArgumentException();
    }
    return getLocal(sourceId);
  }

  public void wipe(SourceIdT sourceId) {
    myStates.remove(sourceId);
  }


  public class LocalTypecheckerState {
    private boolean myIsOutOfSync = false;
    private final Map<Abstract.Definition, Definition> myDefinitions = new HashMap<>();

    public void record(Abstract.Definition def, Definition res) {
      if (myDefinitions.put(def, res) != res) {
        myIsOutOfSync = true;
      }
    }

    public Definition getTypechecked(Abstract.Definition def) {
      assert def != null;
      return myDefinitions.get(def);
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

    void unsync() {
      myIsOutOfSync = true;
    }
  }
}
