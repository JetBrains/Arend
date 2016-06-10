package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;
  private Definition myNonPolyDefinition;
  private LevelSubstitution myPolyParamsSubst;

  public DefCallExpression(Definition definition) {
    myDefinition = definition;
    myNonPolyDefinition = definition;
    myPolyParamsSubst = new LevelSubstitution();
  }

  public DefCallExpression(Definition definition, LevelSubstitution subst) {
    myDefinition = definition;
    myNonPolyDefinition = definition.substPolyParams(subst);
    myPolyParamsSubst = subst;
  }

  public abstract Expression applyThis(Expression thisExpr);

  public Definition getDefinition() {
    return myNonPolyDefinition;
  }

  public LevelSubstitution getPolyParamsSubst() {
    return myPolyParamsSubst;
  }

  public LevelExpression getPolyParamValueByType(String typeName) {
    for (Binding var : myPolyParamsSubst.getDomain()) {
      if (var.getType().toDefCall().getDefinition().getResolvedName().getName().equals(typeName)) {
        return myPolyParamsSubst.get(var);
      }
    }
    return null;
  }

  public void applyLevelSubst(LevelSubstitution subst) {
    myPolyParamsSubst = subst.compose(myPolyParamsSubst);
    myNonPolyDefinition = myDefinition.substPolyParams(myPolyParamsSubst);
  }

  @Override
  public Expression getType() {
    return myDefinition.getType();
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
