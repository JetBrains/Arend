package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

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

  public LevelExpression getPolyParamValueByType(String typeName) {
    for (Binding var : myPolyParamsSubst.getDomain()) {
      if (var.getType().toDefCall().getDefinition().getResolvedName().getFullName().equals(typeName)) {
        return myPolyParamsSubst.get(var);
      }
    }
    return null;
  }

  public void applyLevelSubst(LevelSubstitution subst) {
    myPolyParamsSubst = myPolyParamsSubst.compose(subst);
    myNonPolyDefinition = myDefinition.substPolyParams(myPolyParamsSubst);
  }

  public static boolean compare(DefCallExpression defCall1, DefCallExpression defCall2, Equations.CMP cmp, Equations equations) {
    return defCall1.myDefinition == defCall2.myDefinition &&
            CompareVisitor.compare(equations, cmp, defCall1.myNonPolyDefinition.getType(), defCall2.myNonPolyDefinition.getType(), null);
  } /**/

  @Override
  public Expression getType() {
    return myNonPolyDefinition.getType();
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
