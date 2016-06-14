package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.HashMap;
import java.util.Map;

public class TypecheckerState {
  private final Map<Abstract.Definition, Definition> myTypechecked = new HashMap<>();

  public void record(Abstract.Definition def, Definition res) {
    myTypechecked.put(def, res);
  }

  public Definition getTypechecked(Abstract.Definition def) {
    return myTypechecked.get(def);
  }

  public Definition getTypechecked(Referable ref) {
    final Definition res;
    if (ref instanceof Definition) {
      res = (Definition) ref;
    } else if (ref instanceof Abstract.Definition) {
      res = getTypechecked((Abstract.Definition) ref);
    } else {
      // FIXME[referable]
      throw new IllegalStateException();
    }
    return res;
  }
}
