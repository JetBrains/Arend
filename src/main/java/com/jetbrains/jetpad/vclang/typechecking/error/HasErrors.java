package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;

public class HasErrors extends TypeCheckingError {
  public HasErrors(Namespace namespace, String name, Abstract.SourceNode expression) {
    super(namespace, name + " has errors", expression, null);
  }

  public HasErrors(String name, Abstract.SourceNode expression) {
    super(name + " has errors", expression, null);
  }

  public HasErrors(Namespace namespace, Name name, Abstract.SourceNode expression) {
    this(namespace, name.getPrefixName(), expression);
  }

  public HasErrors(Name name, Abstract.SourceNode expression) {
    this(name.getPrefixName(), expression);
  }

  @Override
  public String toString() {
    return printHeader() + getMessage();
  }
}
