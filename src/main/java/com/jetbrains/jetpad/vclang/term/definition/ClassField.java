package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class ClassField extends Definition {
  private final Expression myType;

  public ClassField(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, Expression type) {
    super(parentNamespace, name, precedence);
    myType = type;
  }

  public Expression getType() {
    return myType;
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    throw new IllegalStateException();
  }
}
