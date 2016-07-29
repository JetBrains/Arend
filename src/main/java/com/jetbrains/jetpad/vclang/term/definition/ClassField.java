package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.LevelSubstVisitor;

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

  public ClassField(String name, Abstract.Definition.Precedence precedence, Expression type, ClassDefinition thisClass, DependentLink thisParameter, TypeUniverse universe) {
    super(name, precedence, universe);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
    hasErrors(type == null || type.toError() != null);
  }

  public TypeUniverse updateUniverse(TypeUniverse universe, ExprSubstitution substitution) {
    Expression expr1 = myType.subst(substitution).normalize(NormalizeVisitor.Mode.WHNF);
    UniverseExpression expr = null;
    if (expr1.toOfType() != null) {
      Expression expr2 = expr1.toOfType().getExpression().getType();
      if (expr2 != null) {
        expr = expr2.normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
      }
    }
    if (expr == null) {
      expr = expr1.getType().normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
    }

    TypeUniverse fieldUniverse = expr != null ? expr.getUniverse() : getUniverse();
    universe = universe.max(fieldUniverse);
    assert expr != null;
    return universe;
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
            myThisParameter, getUniverse().subst(subst));
  }

  public void setBaseType(Expression type) {
    myType = type;
  }
}
