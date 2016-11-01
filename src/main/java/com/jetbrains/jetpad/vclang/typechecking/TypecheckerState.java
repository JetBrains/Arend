package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.GlobalInstancePool;

public interface TypecheckerState {
  void record(Abstract.Definition def, Definition res);

  void record(Abstract.ClassView classView, ClassView res);

  void record(Abstract.ClassViewField classViewField, ClassView res);

  Definition getTypechecked(Abstract.Definition def);

  ClassView getClassView(Abstract.ClassView classView);

  ClassView getClassView(Abstract.ClassViewField classViewField);

  GlobalInstancePool getInstancePool();
}
