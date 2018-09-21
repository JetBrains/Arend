package org.arend.error;

public abstract class ReportableRuntimeException extends RuntimeException {
  public abstract GeneralError toError();
}
