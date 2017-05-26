package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class CycleInConditions extends LocalTypeCheckingError {
  public final List<Abstract.Definition> cycle;

  /* This is the right constructor for this error

  public CycleInConditions(List<Abstract.Constructor> cycle) {
    super("Conditions form a cycle");
    this.cycle = cycle;
  }
  */

  @Deprecated
  public CycleInConditions(List<Constructor> cycle) {
    super("Conditions form a cycle: " + toMessage(cycle), null);
    this.cycle = null;
  }

  private static String toMessage(List<Constructor> cycle) {
    StringBuilder message = new StringBuilder();
    for (Constructor constructor : cycle) {
        message.append(constructor.getName()).append(" - ");
    }
    message.append(cycle.get(0).getName());
    return message.toString();
  }
}
