package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;
  private LevelSubstitution myPolyParamsSubst;

  public DefCallExpression(Definition definition) {
    myDefinition = definition;
    myPolyParamsSubst = new LevelSubstitution();
  }

  public DefCallExpression(Definition definition, LevelSubstitution subst) {
    myDefinition = definition;
    myPolyParamsSubst = subst;
  }

  public boolean isPolymorphic() {
    return myDefinition.isPolymorphic();
  }

  public LevelSubstitution getPolyParamsSubst() {
    return myPolyParamsSubst;
  }

  public abstract Expression applyThis(Expression thisExpr);

  public Definition getDefinition() {
    return myDefinition;
  }

  public void setPolyParamsSubst(LevelSubstitution polyParams) {
    myPolyParamsSubst = polyParams;
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
