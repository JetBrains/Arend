package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class NotInStaticScopeError extends TypeCheckingError {
  private final String myName;

  public NotInStaticScopeError(Namespace namespace, Abstract.SourceNode expression, String name) {
    super(namespace, null, expression, null);
    myName = name;
  }

  public NotInStaticScopeError(Abstract.SourceNode expression, String name) {
    super(null, expression, null);
    myName = name;
  }

  public NotInStaticScopeError(Namespace namespace, Abstract.SourceNode expression, Utils.Name name) {
    this(namespace, expression, name.getPrefixName());
  }

  public NotInStaticScopeError(Abstract.SourceNode expression, Utils.Name name) {
    this(expression, name.getPrefixName());
  }

  @Override
  public String toString() {
    return printHeader() + "'" + myName + "' is not static";
  }
}
