package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.InstanceInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.instance.pool.InstancePool;

import java.util.Set;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final TCClassReferable myClassRef;
  private final boolean myField;

  public TypeClassInferenceVariable(String name, Expression type, TCClassReferable classRef, boolean isField, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myClassRef = classRef;
    myField = isField;
  }

  public TCClassReferable getClassReferable() {
    return myClassRef;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new InstanceInferenceError(myClassRef, getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new InstanceInferenceError(myClassRef, expectedType, actualType, getSourceNode(), candidate);
  }

  public Expression getInstance(InstancePool pool, Expression classifyingExpression, Equations equations, Concrete.SourceNode sourceNode) {
    return pool.getInstance(classifyingExpression, myClassRef, myField, equations, sourceNode);
  }
}
