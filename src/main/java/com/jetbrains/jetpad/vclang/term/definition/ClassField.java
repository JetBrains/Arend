package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.LevelSubstVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class ClassField extends Definition {
  private DependentLink myThisParameter;
  private Expression myType;

  public ClassField(String name, Abstract.Definition.Precedence precedence, Expression type, ClassDefinition thisClass, DependentLink thisParameter) {
    super(name, precedence);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
    hasErrors(type == null || type.toError() != null);
  }

  public ClassField(String name, Abstract.Definition.Precedence precedence, Expression type, ClassDefinition thisClass, DependentLink thisParameter, Sort universe) {
    super(name, precedence, universe);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
    hasErrors(type == null || type.toError() != null);
  }

  public Sort updateSort(Sort sort, ExprSubstitution substitution) {
    Expression expr1 = myType.subst(substitution).normalize(NormalizeVisitor.Mode.WHNF);
    UniverseExpression expr = null;
    if (expr1.toOfType() != null) {
      // TODO [sorts]
      Expression expr2 = (Expression) expr1.toOfType().getExpression().getType();
      if (expr2 != null) {
        expr = expr2.normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
      }
    }
    if (expr == null) {
      // TODO [sorts]
      expr = ((Expression) expr1.getType()).normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
    }

    Sort fieldUniverse = expr != null ? expr.getSort() : getSort();
    sort = sort.max(fieldUniverse);
    assert expr != null;
    return sort;
  }

  public DependentLink getThisParameter() {
    return myThisParameter;
  }

  public void setThisParameter(DependentLink thisParameter) {
    myThisParameter = thisParameter;
  }

  public Expression getBaseType() {
    return myType;
  }

  @Override
  public Expression getType() {
    return myType == null ? null : Pi(myThisParameter, myType);
  }

  @Override
  public FieldCallExpression getDefCall() {
    return FieldCall(this);
  }

  @Override
  public ClassField substPolyParams(LevelSubstitution subst) {
    if (!isPolymorphic()) {
      return this;
    }
    return new ClassField(getName(), getPrecedence(), LevelSubstVisitor.subst(myType, subst), getThisClass(),
            myThisParameter, getSort().subst(subst));
  }

  public void setBaseType(Expression type) {
    myType = type;
  }
}
