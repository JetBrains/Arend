package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public class HasErrors extends TypeCheckingError {
  public HasErrors(Definition parent, String name, Abstract.SourceNode expression) {
    super(parent, name + " has errors", expression, null);
  }

  @Override
  public String toString() {
    return printPosition() + getMessage();
  }
}
