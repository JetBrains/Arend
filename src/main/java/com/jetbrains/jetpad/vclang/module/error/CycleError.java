package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

import java.util.Collection;
import java.util.List;

public class CycleError extends GeneralError {
  private final List<Namespace> myCycle;

  public CycleError(List<Namespace> cycle) {
    super(null, "modules dependencies form a cycle: ");
    myCycle = cycle;
  }

  public Collection<? extends Namespace> getCycle() {
    return myCycle;
  }

  @Override
  public String toString() {
    String msg = printHeader() + getMessage();
    for (Namespace namespace : myCycle) {
      msg += namespace + " - ";
    }
    return msg + myCycle.get(0);
  }
}
