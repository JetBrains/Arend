package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.core.expr.Expression;

public class TermDoc extends CachingDoc {
  private final Expression myTerm;

  TermDoc(Expression term) {
    myTerm = term;
  }

  public Expression getTerm() {
    return myTerm;
  }

  @Override
  protected String getString() {
    StringBuilder builder = new StringBuilder();
    myTerm.prettyPrint(builder, true);
    return builder.toString();
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
