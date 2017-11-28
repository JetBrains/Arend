package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.List;

public class ClassField extends Definition {
  private TypedDependentLink myThisParameter;
  private Expression myType;

  public ClassField(GlobalReferable referable, ClassDefinition thisClass) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    setThisClass(thisClass);
  }

  public ClassField(GlobalReferable referable, Expression type, ClassDefinition thisClass, TypedDependentLink thisParameter) {
    super(referable, TypeCheckingStatus.NO_ERRORS);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
  }

  @Override
  public String getName() {
    return getReferable() != null ? super.getName() : getThisClass().getName() + ".\\parent"; // TODO[classes]
  }

  @Override
  public DependentLink getParameters() {
    return myThisParameter;
  }

  public TypedDependentLink getThisParameter() {
    return myThisParameter;
  }

  public void setThisParameter(TypedDependentLink thisParameter) {
    assert myThisParameter == null;
    myThisParameter = thisParameter;
  }

  public Expression getBaseType(Sort sortArgument) {
    return myType.subst(sortArgument.toLevelSubstitution());
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (myType == null) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution polySubst = sortArgument.toLevelSubstitution();
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myThisParameter, subst, polySubst)));
    return myType.subst(subst, polySubst);
  }

  @Override
  public Expression getDefCall(Sort sortArgument, Expression thisExpr, List<Expression> args) {
    return FieldCallExpression.make(this, thisExpr);
  }

  public void setBaseType(Expression type) {
    assert myType == null;
    myType = type;
  }
}
