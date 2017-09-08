package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

public class NotInScopeError extends ReferenceError {
  public NotInScopeError(Referable referable) {
    super("Cannot resolve reference: " + referable.textRepresentation(), referable);
  }
}
