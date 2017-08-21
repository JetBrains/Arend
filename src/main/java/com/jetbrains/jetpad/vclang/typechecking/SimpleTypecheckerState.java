package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.HashMap;
import java.util.Map;

public class SimpleTypecheckerState implements TypecheckerState {
  private final Map<GlobalReferable, Definition> myTypechecked;

  public SimpleTypecheckerState() {
    myTypechecked = new HashMap<>();
  }

  public SimpleTypecheckerState(SimpleTypecheckerState state) {
    myTypechecked = new HashMap<>(state.myTypechecked);
  }

  @Override
  public void record(GlobalReferable def, Definition res) {
    myTypechecked.put(def, res);
  }

  @Override
  public Definition getTypechecked(GlobalReferable def) {
    assert def != null;
    return myTypechecked.get(def);
  }

  @Override
  public void reset(GlobalReferable def) {
    myTypechecked.remove(def);
  }

  @Override
  public void reset() {
    myTypechecked.clear();
  }
}
