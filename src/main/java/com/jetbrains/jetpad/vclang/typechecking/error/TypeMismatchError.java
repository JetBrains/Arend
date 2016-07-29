package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.ArrayList;

public class TypeMismatchError extends TypeCheckingError {
  public final PrettyPrintable expected;
  public final PrettyPrintable actual;

  public TypeMismatchError(Abstract.Definition definition, PrettyPrintable expected, PrettyPrintable actual, Abstract.Expression expression) {
    super(definition, "Type mismatch", expression);
    this.expected = expected;
    this.actual = actual;
  }
}
