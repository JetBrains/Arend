package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelArguments;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;

public class ClassField extends Definition {
  private DependentLink myThisParameter;
  private Expression myType;

  public ClassField(Abstract.ClassField abstractDef, ClassDefinition thisClass) {
    this(abstractDef, null, thisClass, null);
  }

  public ClassField(Abstract.ClassField abstractDef, Expression type, ClassDefinition thisClass, DependentLink thisParameter) {
    super(abstractDef);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
  }

  @Override
  public String getName() {
    return getAbstractDefinition() != null ? super.getName() : getThisClass().getName() + "::\\parent";
  }

  @Override
  public Abstract.ClassField getAbstractDefinition() {
    return (Abstract.ClassField) super.getAbstractDefinition();
  }

  @Override
  public DependentLink getParameters() {
    return myThisParameter;
  }

  public DependentLink getThisParameter() {
    return myThisParameter;
  }

  public void setThisParameter(DependentLink thisParameter) {
    assert myThisParameter == null;
    myThisParameter = thisParameter;
  }

  public Expression getBaseType() {
    return myType;
  }

  @Override
  public boolean typeHasErrors() {
    return myType == null;
  }

  @Override
  public TypeCheckingStatus hasErrors() {
    return myType == null || myType.toError() != null ? TypeCheckingStatus.HAS_ERRORS : TypeCheckingStatus.NO_ERRORS;
  }

  @Override
  public void hasErrors(TypeCheckingStatus status) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Expression getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution polySubst = polyArguments.toLevelSubstitution(this);
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myThisParameter, subst, polySubst)));
    return myType == null ? null : myType.subst(subst, polySubst);
  }

  @Override
  public DefCallExpression getDefCall(LevelArguments polyArguments) {
    return new FieldCallExpression(this, null);
  }

  @Override
  public Expression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    assert args.size() == 1;
    return FieldCall(this, args.get(0));
  }

  public void setBaseType(Expression type) {
    assert myType != null;
    myType = type;
  }
}
