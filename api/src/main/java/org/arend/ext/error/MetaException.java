package org.arend.ext.error;

public class MetaException extends RuntimeException {
  public final TypecheckingError error;

  public MetaException(TypecheckingError error) {
    this.error = error;
  }
}
