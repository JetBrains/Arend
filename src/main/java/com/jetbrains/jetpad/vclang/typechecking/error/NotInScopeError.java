package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public class NotInScopeError extends TypeCheckingError {
  private final String myName;

  public NotInScopeError(ResolvedName resolvedName, Abstract.SourceNode expression, String name) {
    super(resolvedName, "Not in scope", expression);
    myName = name;
  }

  public NotInScopeError(Abstract.SourceNode expression, String name) {
    super("Not in scope", expression);
    myName = name;
  }

  public NotInScopeError(ResolvedName resolvedName, Abstract.SourceNode expression, Name name) {
    this(resolvedName, expression, name.getPrefixName());
  }

  public NotInScopeError(Abstract.SourceNode expression, Name name) {
    this(expression, name.getPrefixName());
  }

  @Override
  public String toString() {
    return printHeader() + getMessage() + ": " + myName;
  }
}
