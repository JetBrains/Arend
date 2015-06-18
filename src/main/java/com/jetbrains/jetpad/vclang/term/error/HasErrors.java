package com.jetbrains.jetpad.vclang.term.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class HasErrors extends TypeCheckingError {
  public HasErrors(String name, Abstract.SourceNode expression) {
    super(name + " has errors", expression, null);
  }

  @Override
  public String toString() {
    return printPosition() + getMessage();
  }
}
