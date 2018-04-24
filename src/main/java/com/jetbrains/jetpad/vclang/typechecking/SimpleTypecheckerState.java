package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;

import java.util.HashMap;
import java.util.Map;

public class SimpleTypecheckerState implements TypecheckerState {
  private final Map<GlobalReferable, Definition> myTypechecked;

  public SimpleTypecheckerState() {
    myTypechecked = new HashMap<>();
  }

  @Override
  public void record(TCReferable def, Definition res) {
    myTypechecked.put(def, res);
  }

  @Override
  public Definition getTypechecked(TCReferable def) {
    assert def != null;
    return myTypechecked.get(def);
  }

  @Override
  public void reset(TCReferable def) {
    myTypechecked.remove(def);
  }

  @Override
  public void reset() {
    myTypechecked.clear();
  }
}
