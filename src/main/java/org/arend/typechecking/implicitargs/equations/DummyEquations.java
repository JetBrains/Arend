package org.arend.typechecking.implicitargs.equations;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelSubstitution;
import org.arend.term.concrete.Concrete;

public class DummyEquations implements Equations {
  private static final DummyEquations INSTANCE = new DummyEquations();

  private DummyEquations() {}

  public static DummyEquations getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp, Concrete.SourceNode sourceNode, InferenceVariable stuckVar) {
    return false;
  }

  @Override
  public boolean solve(Expression type, Expression expr, CMP cmp, Concrete.SourceNode sourceNode) {
    return false;
  }

  @Override
  public boolean add(Level expr1, Level expr2, CMP cmp, Concrete.SourceNode sourceNode) {
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
  public void remove(Equation equation) {

  }

  @Override
  public LevelSubstitution solve(Concrete.SourceNode sourceNode) {
    return LevelSubstitution.EMPTY;
  }
}
