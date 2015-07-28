package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class HasErrors extends TypeCheckingError {
  public HasErrors(Definition parent, String name, Abstract.SourceNode expression) {
    super(parent, name + " has errors", expression, null);
  }

  public HasErrors(Definition parent, Utils.Name name, Abstract.SourceNode expression) {
    this(parent, name.getPrefixName(), expression);
  }

  @Override
  public String toString() {
    return printPosition() + getMessage();
  }
}
