package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;
import java.util.Map;

public class SimpleTypecheckerState implements TypecheckerState {
  private final Map<Abstract.GlobalReferableSourceNode, Definition> myTypechecked;

  public SimpleTypecheckerState() {
    myTypechecked = new HashMap<>();
  }

  public SimpleTypecheckerState(SimpleTypecheckerState state) {
    myTypechecked = new HashMap<>(state.myTypechecked);
  }

  @Override
  public void record(Abstract.GlobalReferableSourceNode def, Definition res) {
    myTypechecked.put(def, res);
  }

  @Override
  public Definition getTypechecked(Abstract.GlobalReferableSourceNode def) {
    assert def != null;
    return myTypechecked.get(def);
  }

  @Override
  public void reset(Abstract.Definition def) {
    myTypechecked.remove(def);
  }

  @Override
  public void reset() {
    myTypechecked.clear();
  }
}
