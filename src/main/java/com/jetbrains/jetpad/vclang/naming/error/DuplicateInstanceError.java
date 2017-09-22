package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

public class DuplicateInstanceError extends NamingError {
  public final Concrete.Instance instance1;
  public final Concrete.Instance instance2;

  public DuplicateInstanceError(Level level, Concrete.Instance instance1, Concrete.Instance instance2) {
    super(level, "Instance of '" + instance2.getClassView().getReferent().textRepresentation() + "' for '" + instance2.getClassifyingDefinition().textRepresentation() + "' is already defined", instance2);
    this.instance1 = instance1;
    this.instance2 = instance2;
  }
}
