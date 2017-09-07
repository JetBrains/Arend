package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

public class NotInScopeError<T> extends ReferenceError<T> {
  public NotInScopeError(Referable referable) {
    super("Cannot resolve reference: " + referable.textRepresentation(), referable);
  }
}
