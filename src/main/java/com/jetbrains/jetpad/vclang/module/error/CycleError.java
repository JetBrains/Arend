package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

import java.util.Collection;
import java.util.List;

public class CycleError extends GeneralError {
  private final List<ModuleID> myCycle;

  public CycleError(List<ModuleID> cycle) {
    super(null, "modules dependencies form a cycle: ");
    myCycle = cycle;
  }

  public Collection<? extends ModuleID> getCycle() {
    return myCycle;
  }

  @Override
  public String toString() {
    String msg = printHeader() + getMessage();
    for (ModuleID moduleID : myCycle) {
      msg += moduleID.getModulePath() + " - ";
    }
    return msg + myCycle.get(0);
  }
}
