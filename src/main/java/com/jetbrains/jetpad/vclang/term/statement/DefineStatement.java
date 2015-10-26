package com.jetbrains.jetpad.vclang.term.statement;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;

public class DefineStatement implements Abstract.DefineStatement {
  private final Abstract.Definition myDefinition;
  private final boolean myStatic;

  public DefineStatement(Abstract.Definition definition, boolean isStatic) {
    myDefinition = definition;
    myStatic = isStatic;
  }

  @Override
  public Abstract.Definition getParent() {
    return null;
  }

  @Override
  public boolean isStatic() {
    return myStatic;
  }

  @Override
  public Abstract.Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public <P, R> R accept(AbstractStatementVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDefine(this, params);
  }
}
