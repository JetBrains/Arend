package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.HashMap;
import java.util.Map;

public class GlobalInstancePool implements ClassViewInstancePool {
  private final Map<Definition, Expression> myPool = new HashMap<>();

  @Override
  public Expression getInstance(Expression classifyingExpression) {
    DefCallExpression defCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).getFunction().toDefCall();
    if (defCall == null) {
      return null;
    }
    return myPool.get(defCall.getDefinition());
  }

  public Expression getInstance(Definition definition) {
    return myPool.get(definition);
  }

  public void addInstance(Definition classifyingDefinition, Expression instance) {
    myPool.put(classifyingDefinition, instance);
  }
}
