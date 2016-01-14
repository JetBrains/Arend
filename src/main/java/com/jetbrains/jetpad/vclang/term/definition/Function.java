package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public interface Function {
  ElimTreeNode getElimTree();
  DependentLink getParameters();
  Expression getResultType();
  ClassDefinition getThisClass();

  class Helper {
    public static Expression getFunctionType(Function function) {
      assert function.getResultType() != null;
      if (function.getParameters() == null) {
        return function.getResultType();
      }
      Map<Binding, Expression> substs = new HashMap<>();
      DependentLink params = function.getParameters().subst(substs);
      return Pi(params, function.getResultType().subst(substs));
    }
  }
}
