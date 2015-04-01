package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.visitor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Expression implements PrettyPrintable, Abstract.Expression {
  public abstract <T> T accept(ExpressionVisitor<? extends T> visitor);

  @Override
  public void setWellTyped(Expression wellTyped) {}

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    accept(new PrettyPrintVisitor(builder, new ArrayList<String>()), 0);
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Expression)) return false;
    List<CompareVisitor.Equation> result = compare(this, (Expression) obj, CompareVisitor.CMP.EQ);
    return result != null && result.size() == 0;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, int prec) {
    accept(new PrettyPrintVisitor(builder, names), prec);
  }

  public final Expression liftIndex(int from, int on) {
    return on == 0 ? this : accept(new LiftIndexVisitor(from, on));
  }

  public final Expression subst(Expression substExpr, int from) {
    return accept(new SubstVisitor(substExpr, from));
  }

  public final Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(new NormalizeVisitor(mode));
  }

  public final CheckTypeVisitor.OKResult checkType(Map<String, Definition> globalContext, List<Definition> localContext, Expression expectedType, List<TypeCheckingError> errors) {
    return new CheckTypeVisitor(globalContext, localContext, errors).checkType(this, expectedType);
  }

  public static List<CompareVisitor.Equation> compare(Abstract.Expression expr1, Abstract.Expression expr2, CompareVisitor.CMP cmp) {
    CompareVisitor visitor = new CompareVisitor(cmp, new ArrayList<CompareVisitor.Equation>());
    Boolean result = expr1.accept(visitor, expr2);
    return result ? visitor.equations() : null;
  }
}
