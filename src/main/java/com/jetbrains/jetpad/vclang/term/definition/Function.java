package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public interface Function {
  ElimTreeNode getElimTree();
  DependentLink getParameters();
  Expression getResultType();
  ClassDefinition getThisClass();

  class Helper {
    public static Expression getFunctionType(Function function) {
      assert function.getResultType() != null;
      if (!function.getParameters().hasNext()) {
        return function.getResultType();
      }
      Substitution subst = new Substitution();
      DependentLink params = DependentLink.Helper.subst(function.getParameters(), subst);
      return Pi(params, function.getResultType().subst(subst));
    }
  }
}
