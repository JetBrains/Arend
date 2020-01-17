package org.arend.typechecking.implicitargs.equations;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.expr.Expression;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;

public class DummyEquations implements Equations {
  private static final DummyEquations INSTANCE = new DummyEquations();

  private DummyEquations() {}

  public static DummyEquations getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean addEquation(Expression expr1, Expression expr2, ExpectedType type, CMP cmp, Concrete.SourceNode sourceNode, InferenceVariable stuckVar1, InferenceVariable stuckVar2) {
    return false;
  }

  @Override
  public boolean solve(Expression expr1, Expression expr2, ExpectedType type, CMP cmp, Concrete.SourceNode sourceNode) {
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
  public LevelSubstitution solve(Concrete.SourceNode sourceNode) {
    return LevelSubstitution.EMPTY;
  }

  @Override
  public boolean isDummy() {
    return true;
  }
}
