package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final ClassView myClassView;
  private final ClassDefinition myClassDef;

  public TypeClassInferenceVariable(String name, Expression type, ClassView classView, Abstract.SourceNode sourceNode) {
    super(name, type, sourceNode);
    myClassView = classView;
    myClassDef = null;
  }

  public TypeClassInferenceVariable(String name, Expression type, ClassDefinition classDef, Abstract.SourceNode sourceNode) {
    super(name, type, sourceNode);
    myClassView = null;
    myClassDef = classDef;
  }

  public ClassView getClassView() {
    return myClassView;
  }

  public ClassDefinition getClassDefinition() {
    return myClassDef;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, TypeMax actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.typeClass(), expectedType, actualType, getSourceNode(), candidate);
  }
}
