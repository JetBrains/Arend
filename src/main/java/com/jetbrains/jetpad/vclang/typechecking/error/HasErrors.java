package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class HasErrors extends TypeCheckingError {
  public HasErrors(String name, Abstract.SourceNode expression) {
    super(name + " has errors", expression);
  }

  @Override
  public String toString() {
    return printHeader() + getMessage();
  }
}
