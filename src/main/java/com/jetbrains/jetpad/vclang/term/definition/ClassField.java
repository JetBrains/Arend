package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.LevelSubstVisitor;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class ClassField extends Definition {
  private DependentLink myThisParameter;
  private Expression myType;

  public ClassField(ResolvedName rn, Abstract.Definition.Precedence precedence, Expression type, ClassDefinition thisClass, DependentLink thisParameter) {
    super(rn, precedence);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
    hasErrors(type == null);
  }

  public ClassField(ResolvedName rn, Abstract.Definition.Precedence precedence, Expression type, ClassDefinition thisClass, DependentLink thisParameter, TypeUniverse universe) {
    super(rn, precedence, universe);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
    hasErrors(type == null);
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
    return new ClassField(getResolvedName(), getPrecedence(), LevelSubstVisitor.subst(myType, subst), getThisClass(),
            myThisParameter, getUniverse().subst(subst));
  }

  public void setBaseType(Expression type) {
    myType = type;
  }
}
