package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.util.HashMap;
import java.util.Map;

public class TypecheckerState {
  private final Map<Abstract.Definition, Definition> myTypechecked = new HashMap<>();

  public void record(Abstract.Definition def, Definition res) {
    myTypechecked.put(def, res);
  }

  public Definition getTypechecked(Abstract.Definition def) {
    assert def != null;
    Abstract.Definition definition = def instanceof Abstract.ClassView ? ((Abstract.ClassView) def).getUnderlyingClass() : def;
    if (definition == null) {
      throw new IllegalStateException("Internal error: class view " + def + " was not resolved");
    }
    return myTypechecked.get(definition);
  }
}
