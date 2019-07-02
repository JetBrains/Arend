package org.arend.typechecking.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.implicitargs.equations.Equations;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class DumbTypechecker extends BaseTypechecker implements ConcreteExpressionVisitor<ExpectedType, CheckTypeVisitor.Result>, ConcreteDefinitionVisitor<Void, Void> {
  private final TypecheckerState myState;
  private final Map<Referable, Binding> myContext;
  private Concrete.Definition myDefinition;
  private boolean myRecursive;

  public DumbTypechecker(TypecheckerState state, LocalErrorReporter errorReporter) {
    myState = state;
    myContext = new HashMap<>();
    this.errorReporter = errorReporter;
  }

  public void setCurrentDefinition(Concrete.Definition definition) {
    myDefinition = definition;
    myRecursive = false;
  }

  public boolean isRecursive() {
    return myRecursive;
  }

  @Override
  protected Type checkType(Concrete.Expression expr, ExpectedType expectedType, boolean isFinal) {
    // TODO
    return null;
  }

  @Override
  protected void addBinding(@Nullable Referable referable, Binding binding) {
    if (referable != null) {
      myContext.put(referable, binding);
    }
  }

  @Override
  protected Definition getTypechecked(TCReferable referable) {
    return myState.getTypechecked(referable);
  }

  @Override
  protected Integer getExpressionLevel(DependentLink link, Expression type, Expression expr, Equations equations, Concrete.SourceNode sourceNode) {
    // TODO
    return null;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
    setCurrentDefinition(def);
    // TODO
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    setCurrentDefinition(def);
    // TODO
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    setCurrentDefinition(def);
    // TODO
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitApp(Concrete.AppExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitReference(Concrete.ReferenceExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitThis(Concrete.ThisExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitInferenceReference(Concrete.InferenceReferenceExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitLam(Concrete.LamExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitPi(Concrete.PiExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitUniverse(Concrete.UniverseExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitHole(Concrete.HoleExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitGoal(Concrete.GoalExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitTuple(Concrete.TupleExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitSigma(Concrete.SigmaExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitBinOpSequence(Concrete.BinOpSequenceExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitCase(Concrete.CaseExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitProj(Concrete.ProjExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitClassExt(Concrete.ClassExtExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitNew(Concrete.NewExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitLet(Concrete.LetExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitNumericLiteral(Concrete.NumericLiteral expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitTyped(Concrete.TypedExpression expr, ExpectedType params) {
    return null;
  }
}
