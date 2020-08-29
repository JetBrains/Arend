package org.arend.typechecking.implicitargs.equations;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;

public class DummyEquations implements Equations {
  private static final DummyEquations INSTANCE = new DummyEquations();

  private DummyEquations() {}

  public static DummyEquations getInstance() {
    return INSTANCE;
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
  public boolean addEquation(Level expr1, Level expr2, CMP cmp, Concrete.SourceNode sourceNode) {
    return false;
  }

  @Override
  public boolean addVariable(InferenceLevelVariable var) {
    return false;
  }

  @Override
  public void bindVariables(InferenceLevelVariable pVar, InferenceLevelVariable hVar) {

  }

  @Override
  public boolean remove(Equation equation) {
    return false;
  }

  @Override
  public void solveEquations() {

  }

  @Override
  public LevelEquationsSolver makeLevelEquationsSolver() {
    return null;
  }

  @Override
  public void finalizeEquations(LevelSubstitution levelSubstitution, Concrete.SourceNode sourceNode) {

  }

  @Override
  public boolean supportsLevels() {
    return false;
  }

  @Override
  public boolean supportsExpressions() {
    return false;
  }

  @Override
  public void saveState(TypecheckerState state) {

  }

  @Override
  public void loadState(TypecheckerState state) {

  }
}
