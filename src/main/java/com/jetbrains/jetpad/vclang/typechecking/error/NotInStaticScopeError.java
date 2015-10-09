package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public class NotInStaticScopeError extends TypeCheckingError {
  private final String myName;

  public NotInStaticScopeError(ResolvedName resolvedName, Abstract.SourceNode expression, String name) {
    super(resolvedName, null, expression, null);
    myName = name;
  }

  public NotInStaticScopeError(Abstract.SourceNode expression, String name) {
    super(null, expression, null);
    myName = name;
  }

  public NotInStaticScopeError(ResolvedName resolvedName, Abstract.SourceNode expression, Name name) {
    this(resolvedName, expression, name.getPrefixName());
  }

  public NotInStaticScopeError(Abstract.SourceNode expression, Name name) {
    this(expression, name.getPrefixName());
  }

  @Override
  public String toString() {
    return printHeader() + "'" + myName + "' is not static";
  }
}
