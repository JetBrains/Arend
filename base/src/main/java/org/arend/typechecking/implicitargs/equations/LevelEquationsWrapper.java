package org.arend.typechecking.implicitargs.equations;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;

public class LevelEquationsWrapper implements Equations {
  private final Equations myEquations;

  public LevelEquationsWrapper(Equations equations) {
    myEquations = equations;
  }

  @Override
  public boolean addEquation(Expression expr1, Expression expr2, Expression type, CMP cmp, Concrete.SourceNode sourceNode, InferenceVariable stuckVar1, InferenceVariable stuckVar2) {
    return false;
  }

  @Override
  public boolean solve(Expression expr1, Expression expr2, Expression type, CMP cmp, Concrete.SourceNode sourceNode) {
    return false;
  }

  @Override
  public boolean solve(InferenceVariable var, Expression expr) {
    return false;
  }

  @Override
  public boolean addEquation(Level level1, Level level2, CMP cmp, Concrete.SourceNode sourceNode) {
    return myEquations.addEquation(level1, level2, cmp, sourceNode);
  }

  @Override
  public LevelEquationsSolver makeLevelEquationsSolver() {
    return myEquations.makeLevelEquationsSolver();
  }

  @Override
  public void finalizeEquations(LevelSubstitution levelSubstitution, Concrete.SourceNode sourceNode) {
    myEquations.finalizeEquations(levelSubstitution, sourceNode);
  }

  @Override
  public boolean addVariable(InferenceLevelVariable var) {
    return myEquations.addVariable(var);
  }

  @Override
  public void bindVariables(InferenceLevelVariable pVar, InferenceLevelVariable hVar) {
    myEquations.bindVariables(pVar, hVar);
  }

  @Override
  public boolean remove(Equation equation) {
    return false;
  }

  @Override
  public void solveEquations() {
    myEquations.solveEquations();
  }

  @Override
  public boolean supportsLevels() {
    return true;
  }

  @Override
  public boolean supportsExpressions() {
    return false;
  }
}
