package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class ClassField extends Definition {
  private DependentLink myThisParameter;
  private Expression myType;
  private SortMax mySorts;

  public ClassField(Abstract.ClassField abstractDef, Expression type, ClassDefinition thisClass, DependentLink thisParameter) {
    super(abstractDef);
    myThisParameter = thisParameter;
    myType = type;
    setThisClass(thisClass);
    hasErrors(type == null || type.toError() != null);
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
  public boolean typeHasErrors() {
    return myType == null;
  }

  @Override
  public Expression getType(LevelSubstitution polyParams) {
    return myType == null ? null : Pi(myThisParameter, myType.subst(polyParams));
  }

  @Override
  public DefCallExpression getDefCall(LevelSubstitution polyParams) {
    return new FieldCallExpression(this, null);
  }

  @Override
  public Expression getDefCall(LevelSubstitution polyParams, List<Expression> args) {
    assert args.size() == 1;
    return FieldCall(this, args.get(0));
  }

  @Override
  public int getNumberOfParameters() {
    return 1;
  }

  public void setBaseType(Expression type) {
    myType = type;
  }
}
