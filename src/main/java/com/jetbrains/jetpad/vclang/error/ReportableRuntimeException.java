package com.jetbrains.jetpad.vclang.error;

public abstract class ReportableRuntimeException extends RuntimeException {
  public abstract GeneralError toError();
}
