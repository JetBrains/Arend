package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class DuplicateName extends NamingError {
  public DuplicateName(Abstract.ReferableSourceNode referable) {
    super(Level.WARNING, "Duplicate name: " + referable.getName(), referable);
  }

  public Abstract.ReferableSourceNode getReferable() {
    return (Abstract.ReferableSourceNode) cause;
  }
}
