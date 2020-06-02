package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.expr.Expression;
import org.arend.ext.error.LocalError;
import org.arend.ext.instance.SubclassSearchParameters;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;
import org.arend.typechecking.result.TypecheckingResult;

import java.util.Set;

public class TypeClassInferenceVariable extends InferenceVariable {
  private final ClassDefinition myClassDef;
  private final boolean myOnlyLocal;
  private Expression myClassifyingExpression;
  private final RecursiveInstanceHoleExpression myRecursiveInstanceHoleExpression;

  public TypeClassInferenceVariable(String name, Expression type, ClassDefinition classDef, boolean onlyLocal, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveInstanceHoleExpression, Set<Binding> bounds) {
    super(name, type, sourceNode, bounds);
    myClassDef = classDef;
    myOnlyLocal = onlyLocal;
    myRecursiveInstanceHoleExpression = recursiveInstanceHoleExpression;
  }

  public ClassDefinition getClassDefinition() {
    return myClassDef;
  }

  @Override
  public LocalError getErrorInfer(Expression... candidates) {
    return candidates.length == 0 ? new InstanceInferenceError(myClassDef.getReferable(), myClassifyingExpression, getSourceNode(), myRecursiveInstanceHoleExpression) : new InstanceInferenceError(myClassDef.getReferable(), getSourceNode(), myRecursiveInstanceHoleExpression, candidates);
  }

  @Override
  public LocalError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new InstanceInferenceError(myClassDef.getReferable(), expectedType, actualType, getSourceNode(), candidate, myRecursiveInstanceHoleExpression);
  }

  public Expression getInstance(InstancePool pool, Expression classifyingExpression, Expression expectedType, Concrete.SourceNode sourceNode) {
    TypecheckingResult result = (myOnlyLocal ? pool.getLocalInstancePool() : pool).getInstance(classifyingExpression, expectedType, new SubclassSearchParameters(myClassDef), sourceNode, myRecursiveInstanceHoleExpression);
    if (result == null && myClassifyingExpression == null) {
      myClassifyingExpression = classifyingExpression;
    }
    return result == null ? null : result.expression;
  }
}
