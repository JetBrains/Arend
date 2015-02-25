package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class IndexExpression extends Expression {
  public final static int PREC = 11;

  private final int index;

  public IndexExpression(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  @Override
  public void prettyPrint(PrintStream stream, List<String> names, int prec) {
    assert index < names.size();
    stream.print(names.get(names.size() - 1 - index));
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof IndexExpression)) return false;
    IndexExpression other = (IndexExpression)o;
    return index == other.index;
  }

  @Override
  public String toString() {
    return "<" + index + ">";
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitIndex(this);
  }
}
