package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements InstancePool {
  static private class Pair {
    final Expression key;
    final ClassReferable classRef;
    final Expression value;

    public Pair(Expression key, ClassReferable classRef, Expression value) {
      this.key = key;
      this.classRef = classRef;
      this.value = value;
    }
  }

  private final List<Pair> myPool = new ArrayList<>();

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Equations equations, Concrete.SourceNode sourceNode) {
    return getInstance(classifyingExpression, classRef);
  }

  private Expression getInstance(Expression classifyingExpression, TCClassReferable classRef) {
    for (Pair pair : myPool) {
      if (pair.classRef == classRef && pair.key.equals(classifyingExpression)) {
        return pair.value;
      }
    }
    return null;
  }

  public Expression addInstance(Expression classifyingExpression, TCClassReferable classRef, Expression instance) {
    Expression oldInstance = getInstance(classifyingExpression, classRef);
    if (oldInstance != null) {
      return oldInstance;
    } else {
      myPool.add(new Pair(classifyingExpression, classRef, instance));
      return null;
    }
  }
}
