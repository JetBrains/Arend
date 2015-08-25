package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Namespace;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class HasErrors extends TypeCheckingError {
  public HasErrors(Namespace namespace, String name, Abstract.SourceNode expression) {
    super(namespace, name + " has errors", expression, null);
  }

  public HasErrors(Namespace namespace, Utils.Name name, Abstract.SourceNode expression) {
    this(namespace, name.getPrefixName(), expression);
  }

  @Override
  public String toString() {
    return printPosition() + getMessage();
  }
}
