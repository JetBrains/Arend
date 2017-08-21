package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
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
  public void record(GlobalReferable def, Definition res) {
    getLocal(def).record(def, res);
  }

  @Override
  public Definition getTypechecked(GlobalReferable def) {
    return getLocal(def).getTypechecked(def);
  }

  public Set<SourceIdT> getCachedModules() {
    return myStates.keySet();
  }

  public LocalTypecheckerState getLocal(@Nonnull SourceIdT sourceId) {
    return myStates.computeIfAbsent(sourceId, k -> new LocalTypecheckerState());
  }

  private LocalTypecheckerState getLocal(GlobalReferable def) {
    SourceIdT sourceId = myDefLocator.sourceOf(def);
    if (sourceId == null) {
      throw new IllegalArgumentException();
    }
    return getLocal(sourceId);
  }

  public void wipe(SourceIdT sourceId) {
    myStates.remove(sourceId);
  }

  @Override
  public void reset(Abstract.Definition def) {
    getLocal(def).reset(def);
  }

  @Override
  public void reset() {
    myStates.clear();
  }


  public class LocalTypecheckerState {
    private boolean myIsOutOfSync = false;
    private final Map<GlobalReferable, Definition> myDefinitions = new HashMap<>();

    public void record(GlobalReferable def, Definition res) {
      if (myDefinitions.put(def, res) != res) {
        myIsOutOfSync = true;
      }
    }

    public void reset(GlobalReferable def) {
      if (myDefinitions.remove(def) != null) {
        myIsOutOfSync = true;
      }
    }

    public Definition getTypechecked(GlobalReferable def) {
      assert def != null;
      return myDefinitions.get(def);
    }

    public Set<GlobalReferable> getTypecheckedDefinitions() {
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
