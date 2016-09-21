package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;

import java.util.HashMap;
import java.util.Map;

public class GlobalInstancePool implements ClassViewInstancePool {
  private final Map<ClassViewInstanceKey, Expression> myPool = new HashMap<>();

  @Override
  public Expression getInstance(Expression classifyingExpression, ClassView classView) {
    DefCallExpression defCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).getFunction().toDefCall();
    if (defCall == null) {
      return null;
    }
    return myPool.get(new ClassViewInstanceKey(defCall.getDefinition(), classView));
  }

  public Expression getInstance(Definition definition, ClassView classView) {
    return myPool.get(new ClassViewInstanceKey(definition, classView));
  }

  public void addInstance(Definition classifyingDefinition, ClassView classView, Expression instance) {
    myPool.put(new ClassViewInstanceKey(classifyingDefinition, classView), instance);
  }
}
