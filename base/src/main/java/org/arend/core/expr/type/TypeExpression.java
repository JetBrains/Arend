package org.arend.core.expr.type;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.ops.NormalizationMode;

public class TypeExpression implements Type {
  private final Expression myType;
  private Sort mySort;

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
    Expression expr = myType.accept(substVisitor, null);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort.subst(substVisitor.getLevelSubstitution()));
  }

  @Override
  public void subst(InPlaceLevelSubstVisitor substVisitor) {
    myType.accept(substVisitor, null);
    mySort = mySort.subst(substVisitor.getLevelSubstitution());
  }

  @Override
  public Type strip(StripVisitor visitor) {
    Expression expr = myType.accept(visitor, null);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort);
  }

  @Override
  public Type normalize(NormalizeVisitor visitor, NormalizationMode mode) {
    Expression expr = myType.accept(visitor, mode);
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, mySort);
  }
}
