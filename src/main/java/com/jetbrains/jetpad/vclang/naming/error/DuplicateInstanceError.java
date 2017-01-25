package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class DuplicateInstanceError extends NamingError {
  public final Abstract.ClassViewInstance instance1;
  public final Abstract.ClassViewInstance instance2;

  public DuplicateInstanceError(Level level, Abstract.ClassViewInstance instance1, Abstract.ClassViewInstance instance2) {
    super(level, "Instance of '" + instance2.getClassView().getName() + "' for '" + instance2.getClassifyingDefinition().getName() + "' is already defined", instance2);
    this.instance1 = instance1;
    this.instance2 = instance2;
  }

  public DuplicateInstanceError(Abstract.ClassViewInstance instance1, Abstract.ClassViewInstance instance2) {
    this(Level.ERROR, instance1, instance2);
  }
}
