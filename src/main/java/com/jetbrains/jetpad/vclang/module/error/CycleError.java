package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

import java.util.Collection;
import java.util.List;

public class CycleError extends GeneralError {
  private final List<ResolvedName> myCycle;

  public CycleError(List<ResolvedName> cycle) {
    super(null, "modules dependencies form a cycle: ");
    myCycle = cycle;
  }

  public Collection<? extends ResolvedName> getCycle() {
    return myCycle;
  }

  @Override
  public String toString() {
    String msg = printHeader() + getMessage();
    for (ResolvedName name : myCycle) {
      msg += name + " - ";
    }
    return msg + myCycle.get(0);
  }
}
