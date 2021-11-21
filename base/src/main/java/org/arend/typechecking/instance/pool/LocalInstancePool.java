package org.arend.typechecking.instance.pool;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.instance.InstanceSearchParameters;
import org.arend.naming.reference.CoreReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public class LocalInstancePool implements InstancePool {
  static class InstanceData {
    final Expression key;
    final ClassDefinition classDef;
    final Expression value;

    InstanceData(Expression key, ClassDefinition classDef, Expression value) {
      this.key = key;
      this.classDef = classDef;
      this.value = value;
    }
  }

  private final CheckTypeVisitor myTypechecker;
  private final List<InstanceData> myPool;

  private LocalInstancePool(CheckTypeVisitor typechecker, List<InstanceData> pool) {
    myTypechecker = typechecker;
    myPool = pool;
  }

  public LocalInstancePool(CheckTypeVisitor typechecker) {
    myTypechecker = typechecker;
    myPool = new ArrayList<>();
  }

  enum FieldSearchParameters { ALL, FIELDS_ONLY, NOT_FIELDS }

  @Override
  public TypecheckingResult findInstance(Expression classifyingExpression, Expression expectedType, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData, Definition currentDef) {
    return findInstance(classifyingExpression, expectedType, parameters, sourceNode, currentDef, FieldSearchParameters.ALL);
  }

  TypecheckingResult findInstance(Expression classifyingExpression, Expression expectedType, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, Definition currentDef, FieldSearchParameters fieldSearch) {
    Expression result = findInstance(classifyingExpression, parameters, currentDef, fieldSearch);
    if (result == null) {
      return null;
    }
    if (expectedType == null) {
      return new TypecheckingResult(result, null);
    }

    Expression actualType = result.getType();
    if (actualType == null) {
      TypecheckingError error = new TypecheckingError("Cannot infer the type of the instance", sourceNode);
      myTypechecker.getErrorReporter().report(error);
      ErrorExpression errorExpr = new ErrorExpression(error);
      return new TypecheckingResult(errorExpr, errorExpr);
    }

    if (!CompareVisitor.compare(myTypechecker.getEquations(), CMP.LE, actualType, expectedType, Type.OMEGA, sourceNode)) {
      TypecheckingError error = new TypeMismatchError(expectedType, actualType, sourceNode);
      myTypechecker.getErrorReporter().report(error);
      ErrorExpression errorExpr = new ErrorExpression(error);
      return new TypecheckingResult(errorExpr, errorExpr);
    }

    return new TypecheckingResult(result, actualType);
  }

  @Override
  public Concrete.Expression findInstance(Expression classifyingExpression, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData, Definition currentDef) {
    return findInstance(classifyingExpression, parameters, sourceNode, currentDef, FieldSearchParameters.ALL);
  }

  Concrete.Expression findInstance(Expression classifyingExpression, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, Definition currentDef, FieldSearchParameters fieldSearch) {
    Expression result = findInstance(classifyingExpression, parameters, currentDef, fieldSearch);
    return result == null ? null : new Concrete.ReferenceExpression(sourceNode, new CoreReferable(null, new TypecheckingResult(result, null)));
  }

  @Override
  public LocalInstancePool subst(ExprSubstitution substitution) {
    LocalInstancePool result = new LocalInstancePool(myTypechecker);
    for (InstanceData data : myPool) {
      result.myPool.add(new InstanceData(data.key == null ? null : data.key.subst(substitution), data.classDef, data.value.subst(substitution)));
    }
    return result;
  }

  @Override
  public InstancePool getLocalInstancePool() {
    return this;
  }

  private Expression findInstance(Expression classifyingExpression, InstanceSearchParameters parameters, Definition currentDef, FieldSearchParameters fieldSearch) {
    if (!parameters.searchLocal()) {
      return null;
    }
    for (int i = myPool.size() - 1; i >= 0; i--) {
      InstanceData instanceData = myPool.get(i);
      if (fieldSearch != FieldSearchParameters.ALL) {
        boolean isField = instanceData.value instanceof FieldCallExpression && (!(currentDef instanceof ClassDefinition) || ((FieldCallExpression) instanceData.value).getDefinition().getParentClass() != currentDef);
        if (fieldSearch == FieldSearchParameters.FIELDS_ONLY && !isField || fieldSearch == FieldSearchParameters.NOT_FIELDS && isField) {
          continue;
        }
      }
      if (parameters.testClass(instanceData.classDef) && (instanceData.key == classifyingExpression || instanceData.key != null && classifyingExpression != null && Expression.compare(instanceData.key, classifyingExpression, null, CMP.EQ)) && parameters.testLocalInstance(instanceData.value)) {
        return instanceData.value;
      }
    }
    return null;
  }

  @Override
  public Expression addLocalInstance(Expression classifyingExpression, ClassDefinition classDef, Expression instance) {
    myPool.add(new InstanceData(classifyingExpression, classDef, instance));
    return null;
  }

  @Override
  public List<InstanceData> getLocalInstances() {
    return myPool;
  }

  @Override
  public LocalInstancePool copy(CheckTypeVisitor typechecker) {
    return typechecker == myTypechecker ? this : new LocalInstancePool(typechecker, myPool);
  }
}
