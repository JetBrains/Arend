package org.arend.core.expr.type;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.ErrorReporter;

public class TypeExpression implements Type {
  private final Expression myType;
  private final Sort mySort;

  public TypeExpression(Expression type, Sort sort) {
    myType = type;
    mySort = sort;
  }

  @Override
  public Expression getExpr() {
    return myType;
  }

  @Override
  public Sort getSortOfType() {
    return mySort;
  }

  @Override
  public Type subst(SubstVisitor substVisitor) {
    if (substVisitor.isEmpty()) {
      return this;
    }
    Expression expr = myType.subst(substVisitor);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort.subst(substVisitor.getLevelSubstitution()));
  }

  @Override
  public Type strip(ErrorReporter errorReporter) {
    Expression expr = myType.strip(errorReporter);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort);
  }

  @Override
  public Type normalize(NormalizeVisitor.Mode mode) {
    Expression expr = myType.normalize(mode);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort);
  }
}
