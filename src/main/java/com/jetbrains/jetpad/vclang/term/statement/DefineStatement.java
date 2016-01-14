package com.jetbrains.jetpad.vclang.term.statement;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;

public class DefineStatement implements Abstract.DefineStatement {
  private final Abstract.Definition myDefinition;
  private final Abstract.DefineStatement.StaticMod myStaticMod;

  public DefineStatement(Abstract.Definition definition, Abstract.DefineStatement.StaticMod staticMod) {
    myDefinition = definition;
    myStaticMod = staticMod;
  }

  @Override
  public Abstract.Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public StaticMod getStaticMod() {
    return myStaticMod;
  }

  @Override
  public Abstract.Definition getParentDefinition() {
    throw new IllegalStateException();
  }

  @Override
  public <P, R> R accept(AbstractStatementVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDefine(this, params);
  }
}
