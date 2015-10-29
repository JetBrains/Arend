package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class ClassField extends Definition {
  private final Expression myType;

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
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    throw new IllegalStateException();
  }
}
