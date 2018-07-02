package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements InstancePool {
  static private class Triple {
    final Expression key;
    final TCClassReferable classRef;
    final Expression value;

    Triple(Expression key, TCClassReferable classRef, Expression value) {
      this.key = key;
      this.classRef = classRef;
      this.value = value;
    }
  }

  private final List<Triple> myPool = new ArrayList<>();

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, boolean isField, Equations equations, Concrete.SourceNode sourceNode) {
    return getInstance(classifyingExpression, classRef, isField);
  }

  private Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, boolean isField) {
    for (Triple triple : myPool) {
      if ((isField ? triple.classRef == classRef : triple.classRef.getUnderlyingTypecheckable() == classRef.getUnderlyingTypecheckable()) && triple.key.equals(classifyingExpression)) {
        return triple.value;
      }
    }
    return null;
  }

  public Expression addInstance(Expression classifyingExpression, TCClassReferable classRef, Expression instance) {
    Expression oldInstance = getInstance(classifyingExpression, classRef, true);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new Triple(classifyingExpression, classRef, instance));
      return null;
    }
  }
}
