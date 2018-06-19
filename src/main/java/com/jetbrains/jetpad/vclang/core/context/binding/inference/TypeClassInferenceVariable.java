package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;
import com.jetbrains.jetpad.vclang.typechecking.instance.pool.InstancePool;

import java.util.Set;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final TCClassReferable myClassRef;

  public TypeClassInferenceVariable(String name, Expression type, TCClassReferable classRef, Concrete.SourceNode sourceNode, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myClassRef = classRef;
  }

  public TCClassReferable getClassReferable() {
    return myClassRef;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), getSourceNode(), candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), expectedType, actualType, getSourceNode(), candidate);
  }

  public Expression getInstance(InstancePool pool, Expression classifyingExpression) {
    return pool.getInstance(classifyingExpression, myClassRef);
  }
}
