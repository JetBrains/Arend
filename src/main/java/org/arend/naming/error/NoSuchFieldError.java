package org.arend.naming.error;

import org.arend.term.concrete.Concrete;

public class NoSuchFieldError extends NamingError {
  public final String name;

  public NoSuchFieldError(String name, Concrete.SourceNode cause) {
    super("No such field: " + name, cause);
    this.name = name;
  }
}
