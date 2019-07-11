package org.arend.typechecking.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.TypeMismatchError;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Decision;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DumbTypechecker extends BaseTypechecker implements ConcreteExpressionVisitor<ExpectedType, CheckTypeVisitor.Result>, ConcreteDefinitionVisitor<Void, Void> {
  private final TypecheckerState myState;
  private final Map<Referable, Binding> myContext;
  private Concrete.Definition myConcreteDefinition;
  private Definition myDefinition;
  private boolean myRecursive;

  public DumbTypechecker(TypecheckerState state, LocalErrorReporter errorReporter) {
    myState = state;
    myContext = new HashMap<>();
    this.errorReporter = errorReporter;
  }

  public void setCurrentDefinition(Concrete.Definition concreteDefinition, Definition definition) {
    myConcreteDefinition = concreteDefinition;
    myDefinition = definition;
    myRecursive = false;
  }

  public boolean isRecursive() {
    return myRecursive;
  }

  @Override
  protected CheckTypeVisitor.Result finalCheckExpr(Concrete.Expression expr, ExpectedType expectedType, boolean returnExpectedType) {
    CheckTypeVisitor.Result result = checkExpr(expr, expectedType);
    if (returnExpectedType && result != null && expectedType instanceof Expression) {
      result.type = (Expression) expectedType;
    }
    return result;
  }

  private CheckTypeVisitor.Result checkExpr(Concrete.Expression expr, ExpectedType expectedType) {
    return expr == null ? null : expr.accept(this, expectedType);
  }

  @Override
  protected CheckTypeVisitor.Result finalize(CheckTypeVisitor.Result result, Expression expectedType, Concrete.SourceNode sourceNode) {
    return result;
  }

  private CheckTypeVisitor.Result checkResultExpr(Expression expectedType, CheckTypeVisitor.Result result, Concrete.Expression expr) {
    if (new CompareVisitor(DummyEquations.getInstance(), Equations.CMP.LE, expr).normalizedCompare(result.type, expectedType)) { // TODO
      return result;
    }

    if (!result.type.isError()) {
      errorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
    }
    return null;
  }

  @Override
  protected Type checkType(Concrete.Expression expr, ExpectedType expectedType, boolean isFinal) {
    if (expr == null) {
      return null;
    }

    if (expectedType instanceof Expression) {
      if (((Expression) expectedType).getStuckInferenceVariable() != null) {
        expectedType = ExpectedType.OMEGA;
      }
    }

    CheckTypeVisitor.Result result = expr.accept(this, expectedType);
    if (result == null) {
      return null;
    }
    if (result.expression instanceof Type) {
      return (Type) result.expression;
    }

    UniverseExpression universe = result.type.checkedCast(UniverseExpression.class);
    return new TypeExpression(result.expression, universe == null ? Sort.UNKNOWN : universe.getSort());
  }

  @Override
  public void addBinding(@Nullable Referable referable, Binding binding) {
    if (referable != null) {
      myContext.put(referable, binding);
    }
  }

  @Override
  protected Definition getTypechecked(TCReferable referable) {
    return myState.getTypechecked(referable);
  }

  @Override
  protected boolean isDumb() {
    return true;
  }

  @Override
  protected CheckTypeVisitor.Result typecheckClassExt(List<? extends Concrete.ClassFieldImpl> classFieldImpls, ExpectedType expectedType, Expression implExpr, ClassCallExpression classCallExpr, Set<ClassField> pseudoImplemented, Concrete.Expression expr) {
    // TODO
    return null;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
    FunctionDefinition funcDef = new FunctionDefinition(def.getData());
    setCurrentDefinition(def, funcDef);
    typecheckFunctionHeader(funcDef, def, null, true);

    Concrete.FunctionBody body = def.getBody();
    if (body instanceof Concrete.ElimFunctionBody) {
      // TODO
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      if (def.getResultType() != null) {
        Referable typeRef = def.getResultType().getUnderlyingReferable();
        if (typeRef instanceof ClassReferable) {
          typecheckCoClauses(funcDef, def, def.getResultType(), def.getResultTypeLevel(), body.getClassFieldImpls());
        } else {
          finalCheckExpr(def.getResultType(), def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA, false);
        }
      }
    } else {
      CheckTypeVisitor.Result result = finalCheckExpr(((Concrete.TermFunctionBody) body).getTerm(), funcDef.getResultType(), false);
      if (result != null && def.getResultType() == null && def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA && !(result.expression instanceof NewExpression)) {
        Expression type = result.type.getType(false);
        if (type != null && (type instanceof UniverseExpression && !((UniverseExpression) type).getSort().isProp() || !(type instanceof UniverseExpression) && type.isWHNF() == Decision.YES)) {
          errorReporter.report(new TypeMismatchError(new UniverseExpression(Sort.PROP), type, def));
        }
      }
    }

    checkElimBody(def);
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    setCurrentDefinition(def, new DataDefinition(def.getData()));
    // TODO
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    setCurrentDefinition(def, new ClassDefinition(def.getData()));
    // TODO
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitApp(Concrete.AppExpression expr, ExpectedType params) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result visitReference(Concrete.ReferenceExpression expr, ExpectedType params) {
    if (expr.getReferent() == myConcreteDefinition.getData()) {
      myConcreteDefinition.setRecursive(true);
    }
    // TODO
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
