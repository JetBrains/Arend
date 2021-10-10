package org.arend.typechecking.instance.pool;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.instance.InstanceSearchParameters;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.List;

public interface InstancePool {
  TypecheckingResult getInstance(Expression classifyingExpression, Expression expectedType, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData);
  Concrete.Expression getInstance(Expression classifyingExpression, InstanceSearchParameters parameters, Concrete.SourceNode sourceNode, RecursiveInstanceHoleExpression recursiveData);
  InstancePool subst(ExprSubstitution substitution);
  InstancePool getLocalInstancePool();
  Expression addLocalInstance(Expression classifyingExpression, ClassDefinition classDef, Expression instance);
  List<?> getLocalInstances();
  InstancePool copy(CheckTypeVisitor typechecker);
}
