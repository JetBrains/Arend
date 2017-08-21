package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class DuplicateInstanceError<T> extends NamingError<T> {
  public final Abstract.ClassViewInstance instance1;
  public final Abstract.ClassViewInstance instance2;

  public DuplicateInstanceError(Level level, Abstract.ClassViewInstance instance1, Concrete.ClassViewInstance<T> instance2) {
    super(level, "Instance of '" + instance2.getClassView().getReferent().getName() + "' for '" + instance2.getClassifyingDefinition().getName() + "' is already defined", instance2);
    this.instance1 = instance1;
    this.instance2 = instance2;
  }
}
