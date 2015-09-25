package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class NotInScopeError extends TypeCheckingError {
  private final String myName;

  public NotInScopeError(Namespace namespace, Abstract.SourceNode expression, String name) {
    super(namespace, "Not in scope", expression, null);
    myName = name;
  }

  public NotInScopeError(Abstract.SourceNode expression, String name) {
    super("Not in scope", expression, null);
    myName = name;
  }

  public NotInScopeError(Namespace namespace, Abstract.SourceNode expression, Utils.Name name) {
    this(namespace, expression, name.getPrefixName());
  }

  public NotInScopeError(Abstract.SourceNode expression, Utils.Name name) {
    this(expression, name.getPrefixName());
  }

  @Override
  public String toString() {
    return printHeader() + getMessage() + ": " + myName;
  }
}
