package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.GlobalInstancePool;

import java.util.HashMap;
import java.util.Map;

public class SimpleTypecheckerState implements TypecheckerState {
  private final Map<Abstract.Definition, Definition> myTypechecked;
  private final GlobalInstancePool myInstancePool = new GlobalInstancePool();

  public SimpleTypecheckerState() {
    myTypechecked = new HashMap<>();
  }

  public SimpleTypecheckerState(SimpleTypecheckerState state) {
    myTypechecked = new HashMap<>(state.myTypechecked);
  }

  @Override
  public void record(Abstract.Definition def, Definition res) {
    myTypechecked.put(def, res);
  }

  @Override
  public Definition getTypechecked(Abstract.Definition def) {
    assert def != null;
    return myTypechecked.get(def);
  }

  @Override
  public GlobalInstancePool getInstancePool() {
    return myInstancePool;
  }

  public void clear() {
    myTypechecked.clear();
    myInstancePool.clear();
  }
}
