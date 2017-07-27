package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;

public class TermLineDoc extends LineDoc {
  private final Expression myTerm;
  private String myText;

  public TermLineDoc(Expression term) {
    myTerm = term;
  }

  public Expression getTerm() {
    return myTerm;
  }

  public String getText() {
    if (myText == null) {
      StringBuilder builder = new StringBuilder();
      myTerm.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0); // TODO: print in a single line
      myText = builder.toString();
    }
    return myText;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTermLine(this, params);
  }

  @Override
  public int getWidth() {
    return getText().length();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
