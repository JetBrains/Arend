package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;

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
    myTerm.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
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
