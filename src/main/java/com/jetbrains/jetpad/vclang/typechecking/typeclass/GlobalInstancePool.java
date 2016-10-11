package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;

import java.util.HashMap;
import java.util.Map;

public class GlobalInstancePool implements ClassViewInstancePool {
  private final Map<ClassViewInstanceKey<ClassView>, Expression> myViewPool = new HashMap<>();

  @Override
  public Expression getInstance(Expression classifyingExpression, ClassView classView) {
    DefCallExpression defCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).toDefCall();
    if (defCall == null) {
      return null;
    }
    return myViewPool.get(new ClassViewInstanceKey<>(defCall.getDefinition(), classView));
  }

  public Expression getInstance(Definition definition, ClassView classView) {
    return myViewPool.get(new ClassViewInstanceKey<>(definition, classView));
  }

  public void addInstance(Definition classifyingDefinition, ClassView classView, Expression instance) {
    myViewPool.put(new ClassViewInstanceKey<>(classifyingDefinition, classView), instance);
    myViewPool.put(new ClassViewInstanceKey<ClassView>(classifyingDefinition, null), instance);
  }
}
