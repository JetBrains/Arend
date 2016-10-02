package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;

import java.util.HashMap;
import java.util.Map;

public class GlobalInstancePool implements ClassViewInstancePool {
  private final Map<ClassViewInstanceKey<ClassView>, Expression> myViewPool = new HashMap<>();
  private final Map<ClassViewInstanceKey<ClassDefinition>, Expression> myDefPool = new HashMap<>();

  @Override
  public Expression getInstance(Expression classifyingExpression, ClassView classView) {
    DefCallExpression defCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).toDefCall();
    if (defCall == null) {
      return null;
    }
    return myViewPool.get(new ClassViewInstanceKey<>(defCall.getDefinition(), classView));
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, ClassDefinition classDef) {
    DefCallExpression defCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).toDefCall();
    if (defCall == null) {
      return null;
    }
    return myDefPool.get(new ClassViewInstanceKey<>(defCall.getDefinition(), classDef));
  }

  public Expression getInstance(Definition definition, ClassView classView) {
    return myViewPool.get(new ClassViewInstanceKey<>(definition, classView));
  }

  public void addInstance(Definition classifyingDefinition, ClassView classView, ClassDefinition classDef, Expression instance) {
    myViewPool.put(new ClassViewInstanceKey<>(classifyingDefinition, classView), instance);
    myDefPool.put(new ClassViewInstanceKey<>(classifyingDefinition, classDef), instance);
  }
}
