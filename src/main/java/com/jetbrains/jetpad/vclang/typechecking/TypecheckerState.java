package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.GlobalInstancePool;

import java.util.HashMap;
import java.util.Map;

public class TypecheckerState {
  private final Map<Abstract.Definition, Definition> myTypechecked;
  private final Map<Abstract.Definition, ClassView> myClassViews;
  private final GlobalInstancePool myInstancePool = new GlobalInstancePool();

  public TypecheckerState() {
    myTypechecked = new HashMap<>();
    myClassViews = new HashMap<>();
  }

  public TypecheckerState(TypecheckerState state) {
    myTypechecked = new HashMap<>(state.myTypechecked);
    myClassViews = new HashMap<>(state.myClassViews);
  }

  public void record(Abstract.Definition def, Definition res) {
    myTypechecked.put(def, res);
  }

  public void record(Abstract.ClassView classView, ClassView res) {
    myClassViews.put(classView, res);
  }

  public void record(Abstract.ClassViewField classViewField, ClassView res) {
    myClassViews.put(classViewField, res);
  }

  public Definition getTypechecked(Abstract.Definition def) {
    assert def != null;
    Abstract.Definition definition = def instanceof Abstract.ClassView ? ((Abstract.ClassView) def).getUnderlyingClassDefCall().getReferent() : def;
    if (definition == null) {
      throw new IllegalStateException("Internal error: class view " + def + " was not resolved");
    }
    return myTypechecked.get(definition);
  }

  public ClassView getClassView(Abstract.ClassView classView) {
    assert classView != null;
    ClassView result = myClassViews.get(classView);
    if (result == null) {
      throw new IllegalStateException("Internal error: class view " + classView + " was not type checked");
    }
    return result;
  }

  public ClassView getClassView(Abstract.ClassViewField classViewField) {
    assert classViewField != null;
    ClassView result = myClassViews.get(classViewField);
    if (result == null) {
      throw new IllegalStateException("Internal error: cannot find class view for " + classViewField);
    }
    return result;
  }

  public GlobalInstancePool getInstancePool() {
    return myInstancePool;
  }
}
