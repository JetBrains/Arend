package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

public abstract class InferenceVariable implements Variable {
  private final String myName;
  private Expression myType;
  private InferenceReferenceExpression myReference;

  public InferenceVariable(String name, Expression type) {
    myName = name;
    myType = type;
  }

  @Override
  public String getName() {
    return myName;
  }

  public Expression getType() {
    return myType;
  }

  public void setType(Expression type) {
    myType = type;
  }

  public InferenceReferenceExpression getReference() {
    return myReference;
  }

  public void setReference(InferenceReferenceExpression reference) {
    myReference = reference;
  }

  public abstract Abstract.SourceNode getSourceNode();

  public abstract void reportErrorInfer(ErrorReporter errorReporter, Expression... candidates);
  public abstract void reportErrorLevelInfer(ErrorReporter errorReporter, Level... candidates);
  public abstract void reportErrorMismatch(ErrorReporter errorReporter, Expression expectedType, Type actualType, Expression candidate);

  @Override
  public String toString() {
    return "?" + myName;
  }
}
