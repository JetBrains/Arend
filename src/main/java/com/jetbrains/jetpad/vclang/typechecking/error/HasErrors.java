package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;

public class HasErrors extends TypeCheckingError {
  public HasErrors(ResolvedName resolvedName, String name, Abstract.SourceNode expression) {
    super(resolvedName, name + " has errors", expression);
  }

  public HasErrors(String name, Abstract.SourceNode expression) {
    super(name + " has errors", expression);
  }

  public HasErrors(ResolvedName resolvedName, Name name, Abstract.SourceNode expression) {
    this(resolvedName, name.getPrefixName(), expression);
  }

  public HasErrors(Name name, Abstract.SourceNode expression) {
    this(name.getPrefixName(), expression);
  }

  @Override
  public String toString() {
    return printHeader() + getMessage();
  }
}
