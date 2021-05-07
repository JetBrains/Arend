package org.arend.typechecking.implicitargs.equations;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;

public interface Equations {
  boolean addEquation(Expression expr1, Expression expr2, Expression type, CMP cmp, Concrete.SourceNode sourceNode, InferenceVariable stuckVar1, InferenceVariable stuckVar2, boolean normalize);
  default boolean addEquation(Expression expr1, Expression expr2, Expression type, CMP cmp, Concrete.SourceNode sourceNode, InferenceVariable stuckVar1, InferenceVariable stuckVar2) {
    return addEquation(expr1, expr2, type, cmp, sourceNode, stuckVar1, stuckVar2, true);
  }
  boolean solve(Expression expr1, Expression expr2, Expression type, CMP cmp, Concrete.SourceNode sourceNode);
  boolean solve(InferenceVariable var, Expression expr);
  void solveLowerBounds(InferenceVariable var);
  boolean addEquation(Level level1, Level level2, CMP cmp, Concrete.SourceNode sourceNode);
  boolean addVariable(InferenceLevelVariable var);
  void bindVariables(InferenceLevelVariable pVar, InferenceLevelVariable hVar);
  boolean remove(Equation equation);
  Boolean solveInstance(TypeClassInferenceVariable variable, FieldCallExpression fieldCall, Expression expr);
  void solveEquations();
  LevelEquationsSolver makeLevelEquationsSolver();
  void finalizeEquations(LevelSubstitution levelSubstitution, Concrete.SourceNode sourceNode);
  boolean supportsLevels();
  boolean supportsExpressions();
  void saveState(TypecheckerState state);
  void loadState(TypecheckerState state);
}
