package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ClassCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class ClassField extends Definition {
  private ClassDefinition myThisClass;
  private Expression myBaseType;

  public ClassField(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, Expression type, ClassDefinition thisClass) {
    super(parentNamespace, name, precedence);
    myBaseType = type;
    myThisClass = thisClass;
    hasErrors(false);
  }

  @Override
  public Expression getType() {
    return Pi("\\this", ClassCall(myThisClass), myBaseType);
  }

  public Expression getBaseType() {
    return myBaseType;
  }

  public void setBaseType(Expression baseType) {
    myBaseType = baseType;
  }

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public void setThisClass(ClassDefinition thisClass) {
    myThisClass = thisClass;
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    throw new IllegalStateException();
  }
}
