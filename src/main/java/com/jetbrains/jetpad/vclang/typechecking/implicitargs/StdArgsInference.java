package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

public class StdArgsInference extends ExplicitImplicitArgsInference {
  public StdArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }
}
