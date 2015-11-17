package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FieldCall;

public class ClassField extends Definition {
  private Expression myType;

  public ClassField(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, Expression type, ClassDefinition thisClass) {
    super(parentNamespace, name, precedence);
    myType = type;
    setThisClass(thisClass);
    hasErrors(false);
  }

  @Override
  public Expression getBaseType() {
    return myType;
  }

  @Override
  public FieldCallExpression getDefCall() {
    return FieldCall(this);
  }

  public void setBaseType(Expression type) {
    myType = type;
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    throw new IllegalStateException();
  }
}
