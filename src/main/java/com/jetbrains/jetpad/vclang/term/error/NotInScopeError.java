package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class NotInScopeError extends TypeCheckingError {
  private final String myName;

  public NotInScopeError(Definition parent, Abstract.PrettyPrintableSourceNode expression, String name) {
    super(parent, "Not in scope", expression, null);
    myName = name;
  }

  public NotInScopeError(Definition parent, Abstract.PrettyPrintableSourceNode expression, Utils.Name name) {
    this(parent, expression, name.getPrefixName());
  }

  @Override
  public String toString() {
    return printPosition() + getMessage() + ": " + myName;
  }
}
