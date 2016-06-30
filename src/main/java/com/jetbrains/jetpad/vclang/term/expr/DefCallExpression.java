package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.HashSet;

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

  public boolean isPolymorphic() { return myDefinition.isPolymorphic(); }

  public LevelSubstitution getPolyParamsSubst() { return myPolyParamsSubst; }

  public abstract Expression applyThis(Expression thisExpr);

  public Definition getPolyDefinition() { return myDefinition; }

  public Definition getDefinition() {
    return myNonPolyDefinition;
  }

  /*public LevelExpression getPolyParamValueByType(String typeName) {
    for (Binding var : myPolyParamsSubst.getDomain()) {
      if (var.getType().toDefCall().getDefinition().getResolvedName().getFullName().equals(typeName)) {
        return myPolyParamsSubst.get(var);
      }
    }
    return null;
  }/**/

  public TypeUniverse getUniverse() {
    return myDefinition.getUniverse().subst(myPolyParamsSubst);
  }

  public DefCallExpression applyLevelSubst(LevelSubstitution subst) {
    myPolyParamsSubst = myPolyParamsSubst.compose(subst, new HashSet<>(myDefinition.getPolyParams()));
    myNonPolyDefinition = myDefinition.substPolyParams(myPolyParamsSubst);
    return this;
  }

  public static boolean compare(DefCallExpression defCall1, DefCallExpression defCall2, Equations.CMP cmp, Equations equations) {
    return defCall1.myDefinition == defCall2.myDefinition &&
            CompareVisitor.compare(equations, cmp, defCall1.getType(), defCall2.getType(), null);
  } /**/

  @Override
  public Expression getType() {
    return myNonPolyDefinition.getType(); //myDefinition.getType().subst(myPolyParamsSubst);
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
