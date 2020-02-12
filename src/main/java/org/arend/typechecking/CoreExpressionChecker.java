package org.arend.typechecking;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.inference.BaseInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.StdLevelSubstitution;
import org.arend.ext.core.elimtree.CoreBranchKey;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.naming.reference.FieldReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.CoreErrorWrapper;
import org.arend.typechecking.error.local.FieldsImplementationError;
import org.arend.typechecking.implicitargs.equations.Equations;

import java.util.*;

public class CoreExpressionChecker implements ExpressionVisitor<Expression, Expression> {
  private ErrorReporter myErrorReporter;
  private final Set<Binding> myContext;
  private final Equations myEquations;
  private final Concrete.SourceNode mySourceNode;

  public CoreExpressionChecker(ErrorReporter errorReporter, Set<Binding> context, Equations equations, Concrete.SourceNode sourceNode) {
    myErrorReporter = errorReporter;
    myContext = context;
    myEquations = equations;
    mySourceNode = sourceNode;
  }

  ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  void setErrorReporter(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  void clear() {
    myContext.clear();
  }

  public Expression check(Expression expectedType, Expression actualType, Expression expression) {
    if (expectedType != null && !CompareVisitor.compare(myEquations, CMP.LE, actualType, expectedType, Type.OMEGA, mySourceNode)) {
      myErrorReporter.report(CoreErrorWrapper.make(new TypeMismatchError(expectedType, actualType, mySourceNode), expression));
      return null;
    }
    return actualType;
  }

  private boolean checkList(List<? extends Expression> args, DependentLink parameters, ExprSubstitution substitution, LevelSubstitution levelSubst) {
    for (Expression arg : args) {
      if (arg.accept(this, parameters.getTypeExpr().subst(substitution, levelSubst)) == null) {
        return false;
      }
      substitution.add(parameters, arg);
      parameters = parameters.getNext();
    }
    return true;
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Expression expectedType) {
    LevelSubstitution levelSubst = expr.getSortArgument().toLevelSubstitution();
    ExprSubstitution substitution = new ExprSubstitution();
    List<? extends Expression> args = expr.getDefCallArguments();
    DependentLink parameters = expr.getDefinition().getParameters();
    // If the sort argument is \\Prop, the first argument can be a set of any level
    if (expr.getDefinition() == Prelude.PATH_INFIX && expr.getSortArgument().isProp()) {
      if (args.get(0).accept(this, new UniverseExpression(new Sort(Level.INFINITY, new Level(0)))) == null) {
        return null;
      }
      substitution.add(parameters, args.get(0));
      args = args.subList(1, args.size());
      parameters = parameters.getNext();
    }
    return checkList(args, parameters, substitution, levelSubst) ? check(expectedType, expr.getDefinition().getResultType().subst(substitution, levelSubst), expr) : null;
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Expression expectedType) {
    LevelSubstitution levelSubst = expr.getSortArgument().toLevelSubstitution();
    ExprSubstitution substitution = new ExprSubstitution();
    return checkList(expr.getDataTypeArguments(), expr.getDefinition().getDataTypeParameters(), substitution, levelSubst) && checkList(expr.getDefCallArguments(), expr.getDefinition().getParameters(), substitution, levelSubst) ? check(expectedType, expr.getDefinition().getDataTypeExpression(expr.getSortArgument(), expr.getDataTypeArguments()), expr) : null;
  }

  @Override
  public Expression visitDataCall(DataCallExpression expr, Expression expectedType) {
    LevelSubstitution levelSubst = expr.getSortArgument().toLevelSubstitution();
    ExprSubstitution substitution = new ExprSubstitution();
    List<? extends Expression> args = expr.getDefCallArguments();
    DependentLink parameters = expr.getDefinition().getParameters();
    // If the sort argument is \\Prop, the first argument can be a set of any level
    if (expr.getDefinition() == Prelude.PATH && expr.getSortArgument().isProp()) {
      if (args.get(0).accept(this, parameters.getTypeExpr().subst(new StdLevelSubstitution(Level.INFINITY, new Level(-1)))) == null) {
        return null;
      }
      substitution.add(parameters, args.get(0));
      args = args.subList(1, args.size());
      parameters = parameters.getNext();
    }
    return checkList(args, parameters, substitution, levelSubst) ? check(expectedType, new UniverseExpression(expr.getDefinition().getSort().subst(levelSubst)), expr) : null;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Expression expectedType) {
    PiExpression type = expr.getDefinition().getType(expr.getSortArgument());
    Expression argType = expr.getArgument().accept(this, type.getParameters().getTypeExpr());
    if (argType == null) {
      return null;
    }

    Expression actualType = null;
    ClassCallExpression argClassCall = argType.normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
    if (argClassCall != null && !(expr.getArgument() instanceof FieldCallExpression)) {
      Expression impl = argClassCall.getImplementation(expr.getDefinition(), expr.getArgument());
      if (impl != null) {
        actualType = impl.getType();
      }
    }
    if (actualType == null) {
      PiExpression overriddenType = argClassCall == null ? null : argClassCall.getDefinition().getOverriddenType(expr.getDefinition(), expr.getSortArgument());
      actualType = (overriddenType == null ? type : overriddenType).applyExpression(expr.getArgument());
    }
    return check(expectedType, actualType, expr);
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Expression expectedType) {
    if (!addBinding(expr.getThisBinding(), expr)) {
      return null;
    }
    Expression thisExpr = new ReferenceExpression(expr.getThisBinding());
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      if (entry.getValue().accept(this, entry.getKey().getType(expr.getSortArgument()).applyExpression(thisExpr)) == null) {
        return null;
      }
    }
    myContext.remove(expr.getThisBinding());

    Integer level = expr.getDefinition().getUseLevel(expr.getImplementedHere(), expr.getThisBinding());
    if (level == null || level != -1) {
      for (ClassField field : expr.getDefinition().getFields()) {
        if (!expr.isImplemented(field)) {
          Sort sort = field.getType(expr.getSortArgument()).applyExpression(thisExpr).getSortOfType();
          if (sort == null) {
            myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the type of field '" + field.getName() + "'", mySourceNode), expr));
            return null;
          }
          if (sort.isProp()) {
            continue;
          }
          if (!(Level.compare(sort.getPLevel(), expr.getSort().getPLevel(), CMP.LE, myEquations, mySourceNode) && (level != null && sort.getHLevel().isClosed() && sort.getHLevel().getConstant() <= level || Level.compare(sort.getHLevel(), expr.getSort().getHLevel(), CMP.LE, myEquations, mySourceNode)))) {
            myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("The sort " + sort + " of field '" + field.getName() + "' does not fit into the expected sort " + expr.getSort(), mySourceNode), expr));
            return null;
          }
        }
      }
    }

    if (expr.getUniverseKind().ordinal() < expr.getDefinition().getUniverseKind().ordinal()) {
      for (ClassField field : expr.getDefinition().getFields()) {
        if (expr.getUniverseKind().ordinal() < field.getUniverseKind().ordinal() && !expr.isImplemented(field)) {
          myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Field '" + field.getName() + "' has universes, but the class call does not have them", mySourceNode), expr));
          return null;
        }
      }
    }

    return check(expectedType, new UniverseExpression(expr.getSort()), expr);
  }

  @Override
  public Expression visitApp(AppExpression expr, Expression expectedType) {
    Expression funType = expr.getFunction().accept(this, null);
    if (funType == null) {
      return null;
    }

    funType = funType.normalize(NormalizationMode.WHNF);
    PiExpression piType = funType.cast(PiExpression.class);
    if (piType == null) {
      myErrorReporter.report(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a pi type"), funType, mySourceNode), expr.getFunction()));
      return null;
    }

    return expr.getArgument().accept(this, piType.getParameters().getTypeExpr()) == null ? null : piType.applyExpression(expr.getArgument());
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Expression expectedType) {
    if (!myContext.contains(expr.getBinding())) {
      myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Variable '" + expr.getBinding().getName() + "' is not bound", mySourceNode), expr));
      return null;
    }
    return check(expectedType, expr.getBinding().getTypeExpr(), expr);
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Expression expectedType) {
    if (expr.getSubstExpression() != null) {
      return expr.getSubstExpression().accept(this, expectedType);
    }
    BaseInferenceVariable infVar = expr.getVariable();
    for (Binding bound : infVar.getBounds()) {
      if (!myContext.contains(bound)) {
        myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Variable '" + bound.getName() + "' is not bound", mySourceNode), expr));
        return null;
      }
    }
    return check(expectedType, infVar.getType(), expr);
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Expression expectedType) {
    return expr.getSubstExpression().accept(this, expectedType);
  }

  boolean addBinding(Binding binding, Expression expr) {
    if (!myContext.add(binding)) {
      TypecheckingError error = new TypecheckingError("Binding '" + binding.getName() + "' is already bound", mySourceNode);
      myErrorReporter.report(CoreErrorWrapper.make(error, expr));
      return false;
    }
    return true;
  }

  void removeBinding(Binding binding) {
    myContext.remove(binding);
  }

  boolean checkDependentLink(DependentLink link, Expression type, Expression expr) {
    for (; link.hasNext(); link = link.getNext()) {
      if (!addBinding(link, expr)) {
        return false;
      }
      if (link instanceof TypedDependentLink && link.getTypeExpr().accept(this, type) == null) {
        return false;
      }
    }
    return true;
  }

  Sort checkDependentLink(DependentLink link, Expression expr) {
    Sort result = Sort.PROP;
    for (; link.hasNext(); link = link.getNext()) {
      if (!addBinding(link, expr)) {
        return null;
      }
      if (link instanceof TypedDependentLink) {
        Expression type = link.getTypeExpr().accept(this, Type.OMEGA);
        if (type == null) {
          return null;
        }
        Sort sort = type.toSort();
        result = sort == null ? null : result.max(sort);
        if (result == null) {
          myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the sort of type", null), link.getTypeExpr()));
          return null;
        }
      }
    }

    return result;
  }

  boolean addDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      if (!addBinding(link, null)) {
        return false;
      }
    }
    return true;
  }

  void freeDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      myContext.remove(link);
    }
  }

  @Override
  public Expression visitLam(LamExpression expr, Expression expectedType) {
    boolean ok = checkDependentLink(expr.getParameters(), new UniverseExpression(new Sort(expr.getResultSort().getPLevel(), Level.INFINITY)), expr);
    Expression type = ok ? expr.getBody().accept(this, null) : null;
    freeDependentLink(expr.getParameters());
    return type != null ? check(expectedType, new PiExpression(expr.getResultSort(), expr.getParameters(), type), expr) : null;
  }

  @Override
  public Expression visitPi(PiExpression expr, Expression expectedType) {
    UniverseExpression type = new UniverseExpression(expr.getResultSort());
    boolean ok = checkDependentLink(expr.getParameters(), new UniverseExpression(new Sort(expr.getResultSort().getPLevel(), Level.INFINITY)), expr);
    ok = ok && expr.getCodomain().accept(this, type) != null;
    freeDependentLink(expr.getParameters());
    return ok ? check(expectedType, type, expr) : null;
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Expression expectedType) {
    UniverseExpression type = new UniverseExpression(expr.getSort());
    boolean ok = checkDependentLink(expr.getParameters(), type, expr);
    freeDependentLink(expr.getParameters());
    return ok ? check(expectedType, type, expr) : null;
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Expression expectedType) {
    if (expr.isOmega()) {
      myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Universes of the infinity level are not allowed", mySourceNode), expr));
      return null;
    }
    return check(expectedType, new UniverseExpression(expr.getSort().succ()), expr);
  }

  @Override
  public Expression visitError(ErrorExpression expr, Expression expectedType) {
    if (expr.isError()) {
      myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Unknown error", mySourceNode), expr));
      return null;
    }
    return expectedType != null ? expectedType : expr.getExpression() == null ? expr : new ErrorExpression(null, expr.isGoal());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Expression expectedType) {
    return visitSigma(expr.getSigmaType(), null) != null && checkList(expr.getFields(), expr.getSigmaType().getParameters(), new ExprSubstitution(), LevelSubstitution.EMPTY) ? check(expectedType, expr.getSigmaType(), expr) : null;
  }

  @Override
  public Expression visitProj(ProjExpression expr, Expression expectedType) {
    Expression type = expr.getExpression().accept(this, null);
    if (type == null) {
      return null;
    }

    type = type.normalize(NormalizationMode.WHNF);
    SigmaExpression sigmaExpr = type.cast(SigmaExpression.class);
    if (sigmaExpr == null) {
      myErrorReporter.report(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a sigma type"), type, mySourceNode), expr.getExpression()));
      return null;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink param = sigmaExpr.getParameters();
    for (int i = 0; true; i++) {
      if (!param.hasNext()) {
        myErrorReporter.report(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a sigma type with " + (expr.getField() + 1) + " parameter" + (expr.getField() == 0 ? "" : "s")), sigmaExpr, mySourceNode), expr.getExpression()));
        return null;
      }
      if (i == expr.getField()) {
        break;
      }
      substitution.add(param, ProjExpression.make(expr.getExpression(), i));
      param = param.getNext();
    }

    return check(expectedType, param.getTypeExpr().subst(substitution), expr);
  }

  boolean checkCocoverage(ClassCallExpression classCall) {
    if (classCall.getDefinition().getNumberOfNotImplementedFields() == classCall.getImplementedHere().size()) {
      return true;
    }

    List<FieldReferable> fields = new ArrayList<>();
    for (ClassField field : classCall.getDefinition().getFields()) {
      if (!classCall.isImplemented(field)) {
        fields.add(field.getReferable());
      }
    }
    if (!fields.isEmpty()) {
      myErrorReporter.report(CoreErrorWrapper.make(new FieldsImplementationError(false, classCall.getDefinition().getReferable(), fields, mySourceNode), classCall));
    }
    return false;
  }

  @Override
  public Expression visitNew(NewExpression expr, Expression expectedType) {
    ClassCallExpression classCall = expr.getType();
    return visitClassCall(classCall, null) != null && checkCocoverage(classCall) ? check(expectedType, classCall, expr) : null;
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Expression expectedType) {
    Expression type = expr.getExpression().accept(this, null);
    if (type == null) {
      return null;
    }

    Sort sortArg = type.getSortOfType();
    if (sortArg == null) {
      myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the sort of the type of the expression", mySourceNode), expr.getExpression()));
      return null;
    }

    List<Expression> args = new ArrayList<>(3);
    args.add(type);
    args.add(expr.getExpression());
    args.add(expr.eval());
    return check(expectedType, new FunCallExpression(Prelude.PATH_INFIX, sortArg, args), expr);
  }

  @Override
  public Expression visitLet(LetExpression expr, Expression expectedType) {
    for (LetClause clause : expr.getClauses()) {
      if (!addBinding(clause, expr)) {
        return null;
      }
      if (clause.getExpression().accept(this, null) == null) {
        return null;
      }
    }
    Expression type = expr.getExpression().accept(this, expectedType);
    myContext.removeAll(expr.getClauses());
    return type;
  }

  Integer checkLevelProof(Expression proof, Expression type) {
    Expression proofType = proof.accept(this, null);
    if (proofType == null) {
      return null;
    }

    List<SingleDependentLink> params = new ArrayList<>();
    FunCallExpression codomain = proofType.getPiParameters(params, false).toEquality();
    if (codomain == null || params.isEmpty() || params.size() % 2 == 1) {
      myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("\\level has wrong format", mySourceNode), proof));
      return null;
    }

    for (int i = 0; i < params.size(); i += 2) {
      if (!CompareVisitor.compare(myEquations, CMP.EQ, type, params.get(i).getTypeExpr(), Type.OMEGA, mySourceNode) || !CompareVisitor.compare(myEquations, CMP.EQ, type, params.get(i + 1).getTypeExpr(), Type.OMEGA, mySourceNode)) {
        myErrorReporter.report(CoreErrorWrapper.make(new TypecheckingError("\\level has wrong format", mySourceNode), proof));
        return null;
      }

      List<Expression> args = new ArrayList<>();
      args.add(type);
      args.add(new ReferenceExpression(params.get(i)));
      args.add(new ReferenceExpression(params.get(i + 1)));
      type = new FunCallExpression(Prelude.PATH_INFIX, Sort.PROP, args);
    }

    return params.size() / 2 - 2;
  }

  private boolean checkElimTree(ElimTree elimTree, Expression expr) {
    boolean ok = checkDependentLink(elimTree.getParameters(), Type.OMEGA, expr);
    if (elimTree instanceof LeafElimTree) {
      // TODO[lang_ext]: Check the type of the expression
      ok = ok && ((LeafElimTree) elimTree).getExpression().accept(this, null) != null;
    } else {
      // TODO[lang_ext]: Check the coverage
      for (Map.Entry<CoreBranchKey, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        ok = ok && checkElimTree(entry.getValue(), expr);
      }
    }
    freeDependentLink(elimTree.getParameters());
    return ok;
  }

  boolean checkElimTree(ElimTree elimTree, Expression expr, boolean isSFunc) {
    // TODO[lang_ext]: Check coverage
    // TODO[lang_ext]: Check conditions
    // TODO[lang_ext]: Check isSCase
    return checkElimTree(elimTree, expr);
  }

  @Override
  public Expression visitCase(CaseExpression expr, Expression expectedType) {
    ExprSubstitution substitution = new ExprSubstitution();
    boolean ok = checkDependentLink(expr.getParameters(), Type.OMEGA, expr);
    ok = ok && checkList(expr.getArguments(), expr.getParameters(), substitution, LevelSubstitution.EMPTY);
    ok = ok && expr.getResultType().accept(this, Type.OMEGA) != null;

    if (ok && expr.getResultTypeLevel() != null) {
      ok = checkLevelProof(expr.getResultTypeLevel(), expr.getResultType()) != null;
    }

    freeDependentLink(expr.getParameters());
    if (!ok) {
      return null;
    }

    return checkElimTree(expr.getElimTree(), expr, expr.isSCase()) ? check(expectedType, expr.getResultType().subst(substitution), expr) : null;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Expression expectedType) {
    if (expr.getTypeOf().accept(this, Type.OMEGA) == null) {
      return null;
    }
    Expression type = expr.getExpression().accept(this, expr.getTypeOf());
    return type == null ? null : check(expectedType, type, expr);
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Expression expectedType) {
    return check(expectedType, new DataCallExpression(Prelude.NAT, Sort.PROP, Collections.emptyList()), expr);
  }
}
