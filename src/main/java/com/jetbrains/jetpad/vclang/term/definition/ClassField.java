package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class ClassField extends Definition {
  private DependentLink myThisParameter;
  private Expression myType;
  private SortMax mySorts;

  public ClassField(String name, Abstract.Definition.Precedence precedence, Expression type, ClassDefinition thisClass, DependentLink thisParameter) {
    super(name, precedence);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
    hasErrors(type == null || type.toError() != null);
  }

  public void updateSort(SortMax sorts, ExprSubstitution substitution) {
    Expression expr1 = myType.subst(substitution).normalize(NormalizeVisitor.Mode.WHNF);
    SortMax sorts1 = null;
    if (expr1.toOfType() != null) {
      sorts1 = expr1.toOfType().getExpression().getType().toSorts();
    }
    if (sorts1 == null) {
      sorts1 = expr1.getType().toSorts();
    }

    sorts.add(sorts1);
    assert sorts1 != null;
  }

  public DependentLink getThisParameter() {
    return myThisParameter;
  }

  public Expression getBaseType() {
    return myType;
  }

  public SortMax getSorts() {
    return mySorts;
  }

  public void setSorts(SortMax sorts) {
    mySorts = sorts;
  }

  @Override
  public Expression getType() {
    return myType == null ? null : Pi(myThisParameter, myType);
  }

  @Override
  public FieldCallExpression getDefCall() {
    return FieldCall(this);
  }

  public void setBaseType(Expression type) {
    myType = type;
  }
}
