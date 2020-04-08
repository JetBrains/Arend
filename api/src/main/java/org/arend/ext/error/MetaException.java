package org.arend.ext.error;

/**
 * Exceptions thrown from meta definitions should extend this class.
 */
public class MetaException extends RuntimeException {
  public final TypecheckingError error;

  public MetaException(TypecheckingError error) {
    this.error = error;
  }
}
