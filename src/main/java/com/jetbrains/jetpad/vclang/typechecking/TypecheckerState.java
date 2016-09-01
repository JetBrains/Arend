package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.util.HashMap;
import java.util.Map;

public class TypecheckerState {
  private final Map<Abstract.Definition, Definition> myTypechecked;

  public TypecheckerState() {
    myTypechecked = new HashMap<>();
  }

  public TypecheckerState(TypecheckerState state) {
    myTypechecked = new HashMap<>(state.myTypechecked);
  }

  public void record(Abstract.Definition def, Definition res) {
    myTypechecked.put(def, res);
  }

  public Definition getTypechecked(Abstract.Definition def) {
    return myTypechecked.get(def);
  }
}
