package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.ext.error.LocalError;
import org.arend.ext.instance.SubclassSearchParameters;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.inference.RecursiveInstanceInferenceError;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;
import org.arend.typechecking.result.TypecheckingResult;

import java.util.Set;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final ClassDefinition myClassDef;
  private final boolean myExactCompare;
  private final boolean myOnlyLocal;
  private Expression myClassifyingExpression;
  private final RecursiveInstanceHoleExpression myRecursiveInstanceHoleExpression;
  private final Definition myDefinition;

  public TypeClassInferenceVariable(String name, Expression type, ClassDefinition classDef, boolean exactCompare, boolean onlyLocal, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveInstanceHoleExpression, Definition definition, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myClassDef = classDef;
    myExactCompare = exactCompare;
    myOnlyLocal = onlyLocal;
    myRecursiveInstanceHoleExpression = recursiveInstanceHoleExpression;
    myDefinition = definition;
  }

  public ClassDefinition getClassDefinition() {
    return myClassDef;
  }

  @Override
  public boolean compareClassCallsExactly() {
    return myExactCompare;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return candidates.length == 0 ? new RecursiveInstanceInferenceError(myClassDef.getReferable(), myClassifyingExpression, getSourceNode(), myRecursiveInstanceHoleExpression) : new RecursiveInstanceInferenceError(null, myClassDef.getReferable(), getSourceNode(), myRecursiveInstanceHoleExpression, candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new RecursiveInstanceInferenceError(myClassDef.getReferable(), expectedType, actualType, getSourceNode(), candidate, myRecursiveInstanceHoleExpression);
  }

  public Expression getInstance(InstancePool pool, Expression classifyingExpression, Expression expectedType, Concrete.SourceNode sourceNode) {
    TypecheckingResult result = (myOnlyLocal ? pool.getLocalInstancePool() : pool).findInstance(classifyingExpression, expectedType, new SubclassSearchParameters(myClassDef), sourceNode, myRecursiveInstanceHoleExpression, myDefinition);
    if (result == null && myClassifyingExpression == null) {
      myClassifyingExpression = classifyingExpression;
    }
    return result == null ? null : result.expression;
  }
}
