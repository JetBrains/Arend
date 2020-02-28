package org.arend.typechecking.doubleChecker;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.inference.BaseInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.ElimBindingVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.FreeVariablesCollector;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.StdLevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.naming.reference.FieldReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.patternmatching.ElimTypechecking;
import org.arend.typechecking.patternmatching.PatternTypechecking;
import org.arend.util.Pair;

import java.util.*;

public class CoreExpressionChecker implements ExpressionVisitor<Expression, Expression> {
  private final Set<Binding> myContext;
  private final Equations myEquations;
  private final Concrete.SourceNode mySourceNode;

  public CoreExpressionChecker(Set<Binding> context, Equations equations, Concrete.SourceNode sourceNode) {
    myContext = context;
    myEquations = equations;
    mySourceNode = sourceNode;
  }

  void clear() {
    myContext.clear();
  }

  public Expression check(Expression expectedType, Expression actualType, Expression expression) {
    if (expectedType != null && !CompareVisitor.compare(myEquations, CMP.LE, actualType, expectedType, Type.OMEGA, mySourceNode)) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(expectedType, actualType, mySourceNode), expression));
    }
    return actualType;
  }

  private void checkList(List<? extends Expression> args, DependentLink parameters, ExprSubstitution substitution, LevelSubstitution levelSubst) {
    for (Expression arg : args) {
      arg.accept(this, parameters.getTypeExpr().subst(substitution, levelSubst));
      substitution.add(parameters, arg);
      parameters = parameters.getNext();
    }
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Expression expectedType) {
    LevelSubstitution levelSubst = expr.getSortArgument().toLevelSubstitution();
    ExprSubstitution substitution = new ExprSubstitution();
    List<? extends Expression> args = expr.getDefCallArguments();
    DependentLink parameters = expr.getDefinition().getParameters();
    // If the sort argument is \\Prop, the first argument can be a set of any level
    if (expr.getDefinition() == Prelude.PATH_INFIX && expr.getSortArgument().isProp()) {
      args.get(0).accept(this, new UniverseExpression(new Sort(Level.INFINITY, new Level(0))));
      substitution.add(parameters, args.get(0));
      args = args.subList(1, args.size());
      parameters = parameters.getNext();
    }
    checkList(args, parameters, substitution, levelSubst);
    return check(expectedType, expr.getDefinition().getResultType().subst(substitution, levelSubst), expr);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Expression expectedType) {
    LevelSubstitution levelSubst = expr.getSortArgument().toLevelSubstitution();
    ExprSubstitution substitution = new ExprSubstitution();
    checkList(expr.getDataTypeArguments(), expr.getDefinition().getDataTypeParameters(), substitution, levelSubst);
    checkList(expr.getDefCallArguments(), expr.getDefinition().getParameters(), substitution, levelSubst);
    return check(expectedType, expr.getDefinition().getDataTypeExpression(expr.getSortArgument(), expr.getDataTypeArguments()), expr);
  }

  @Override
  public Expression visitDataCall(DataCallExpression expr, Expression expectedType) {
    LevelSubstitution levelSubst = expr.getSortArgument().toLevelSubstitution();
    ExprSubstitution substitution = new ExprSubstitution();
    List<? extends Expression> args = expr.getDefCallArguments();
    DependentLink parameters = expr.getDefinition().getParameters();
    // If the sort argument is \\Prop, the first argument can be a set of any level
    if (expr.getDefinition() == Prelude.PATH && expr.getSortArgument().isProp()) {
      args.get(0).accept(this, parameters.getTypeExpr().subst(new StdLevelSubstitution(Level.INFINITY, new Level(-1))));
      substitution.add(parameters, args.get(0));
      args = args.subList(1, args.size());
      parameters = parameters.getNext();
    }
    checkList(args, parameters, substitution, levelSubst);
    return check(expectedType, new UniverseExpression(expr.getDefinition().getSort().subst(levelSubst)), expr);
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Expression expectedType) {
    PiExpression type = expr.getDefinition().getType(expr.getSortArgument());
    Expression argType = expr.getArgument().accept(this, type.getParameters().getTypeExpr());

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
    addBinding(expr.getThisBinding(), expr);
    Expression thisExpr = new ReferenceExpression(expr.getThisBinding());
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      entry.getValue().accept(this, entry.getKey().getType(expr.getSortArgument()).applyExpression(thisExpr));
    }
    myContext.remove(expr.getThisBinding());

    Integer level = expr.getDefinition().getUseLevel(expr.getImplementedHere(), expr.getThisBinding());
    if (level == null || level != -1) {
      for (ClassField field : expr.getDefinition().getFields()) {
        if (!expr.isImplemented(field)) {
          Sort sort = field.getType(expr.getSortArgument()).applyExpression(thisExpr).getSortOfType();
          if (sort == null) {
            throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the type of field '" + field.getName() + "'", mySourceNode), expr));
          }
          if (sort.isProp()) {
            continue;
          }
          if (!(Level.compare(sort.getPLevel(), expr.getSort().getPLevel(), CMP.LE, myEquations, mySourceNode) && (level != null && sort.getHLevel().isClosed() && sort.getHLevel().getConstant() <= level || Level.compare(sort.getHLevel(), expr.getSort().getHLevel(), CMP.LE, myEquations, mySourceNode)))) {
            throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("The sort " + sort + " of field '" + field.getName() + "' does not fit into the expected sort " + expr.getSort(), mySourceNode), expr));
          }
        }
      }
    }

    if (expr.getUniverseKind().ordinal() < expr.getDefinition().getUniverseKind().ordinal()) {
      for (ClassField field : expr.getDefinition().getFields()) {
        if (expr.getUniverseKind().ordinal() < field.getUniverseKind().ordinal() && !expr.isImplemented(field)) {
          throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Field '" + field.getName() + "' has universes, but the class call does not have them", mySourceNode), expr));
        }
      }
    }

    return check(expectedType, new UniverseExpression(expr.getSort()), expr);
  }

  @Override
  public Expression visitApp(AppExpression expr, Expression expectedType) {
    Expression funType = expr.getFunction().accept(this, null).normalize(NormalizationMode.WHNF);
    PiExpression piType = funType.cast(PiExpression.class);
    if (piType == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a pi type"), funType, mySourceNode), expr.getFunction()));
    }

    expr.getArgument().accept(this, piType.getParameters().getTypeExpr());
    return piType.applyExpression(expr.getArgument());
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Expression expectedType) {
    if (!myContext.contains(expr.getBinding())) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Variable '" + expr.getBinding().getName() + "' is not bound", mySourceNode), expr));
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
        throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Variable '" + bound.getName() + "' is not bound", mySourceNode), expr));
      }
    }
    return check(expectedType, infVar.getType(), expr);
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Expression expectedType) {
    return expr.getSubstExpression().accept(this, expectedType);
  }

  void addBinding(Binding binding, Expression expr) {
    if (!myContext.add(binding)) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Binding '" + binding.getName() + "' is already bound", mySourceNode), expr));
    }
  }

  void removeBinding(Binding binding) {
    myContext.remove(binding);
  }

  void checkDependentLink(DependentLink link, Expression type, Expression expr) {
    for (; link.hasNext(); link = link.getNext()) {
      addBinding(link, expr);
      if (link instanceof TypedDependentLink) {
        link.getTypeExpr().accept(this, type);
      }
    }
  }

  Sort checkDependentLink(DependentLink link, Expression expr) {
    Sort result = Sort.PROP;
    for (; link.hasNext(); link = link.getNext()) {
      addBinding(link, expr);
      if (link instanceof TypedDependentLink) {
        Sort sort = link.getTypeExpr().accept(this, Type.OMEGA).toSort();
        result = sort == null ? null : result.max(sort);
        if (result == null) {
          throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the sort of type", null), link.getTypeExpr()));
        }
      }
    }

    return result;
  }

  void addDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      addBinding(link, null);
    }
  }

  void freeDependentLink(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      myContext.remove(link);
    }
  }

  @Override
  public Expression visitLam(LamExpression expr, Expression expectedType) {
    checkDependentLink(expr.getParameters(), new UniverseExpression(new Sort(expr.getResultSort().getPLevel(), Level.INFINITY)), expr);
    Expression type = expr.getBody().accept(this, null);
    freeDependentLink(expr.getParameters());
    return check(expectedType, new PiExpression(expr.getResultSort(), expr.getParameters(), type), expr);
  }

  @Override
  public Expression visitPi(PiExpression expr, Expression expectedType) {
    UniverseExpression type = new UniverseExpression(expr.getResultSort());
    checkDependentLink(expr.getParameters(), new UniverseExpression(new Sort(expr.getResultSort().getPLevel(), Level.INFINITY)), expr);
    expr.getCodomain().accept(this, type);
    freeDependentLink(expr.getParameters());
    return check(expectedType, type, expr);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Expression expectedType) {
    UniverseExpression type = new UniverseExpression(expr.getSort());
    checkDependentLink(expr.getParameters(), type, expr);
    freeDependentLink(expr.getParameters());
    return check(expectedType, type, expr);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Expression expectedType) {
    if (expr.isOmega()) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Universes of the infinity level are not allowed", mySourceNode), expr));
    }
    return check(expectedType, new UniverseExpression(expr.getSort().succ()), expr);
  }

  @Override
  public Expression visitError(ErrorExpression expr, Expression expectedType) {
    if (expr.isError()) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Unknown error", mySourceNode), expr));
    }
    return expectedType != null ? expectedType : expr.getExpression() == null ? expr : new ErrorExpression(null, expr.isGoal());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Expression expectedType) {
    visitSigma(expr.getSigmaType(), null);
    checkList(expr.getFields(), expr.getSigmaType().getParameters(), new ExprSubstitution(), LevelSubstitution.EMPTY);
    return check(expectedType, expr.getSigmaType(), expr);
  }

  @Override
  public Expression visitProj(ProjExpression expr, Expression expectedType) {
    Expression type = expr.getExpression().accept(this, null).normalize(NormalizationMode.WHNF);
    SigmaExpression sigmaExpr = type.cast(SigmaExpression.class);
    if (sigmaExpr == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a sigma type"), type, mySourceNode), expr.getExpression()));
    }

    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink param = sigmaExpr.getParameters();
    for (int i = 0; true; i++) {
      if (!param.hasNext()) {
        throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a sigma type with " + (expr.getField() + 1) + " parameter" + (expr.getField() == 0 ? "" : "s")), sigmaExpr, mySourceNode), expr.getExpression()));
      }
      if (i == expr.getField()) {
        break;
      }
      substitution.add(param, ProjExpression.make(expr.getExpression(), i));
      param = param.getNext();
    }

    return check(expectedType, param.getTypeExpr().subst(substitution), expr);
  }

  void checkCocoverage(ClassCallExpression classCall) {
    if (classCall.getDefinition().getNumberOfNotImplementedFields() == classCall.getImplementedHere().size()) {
      return;
    }

    List<FieldReferable> fields = new ArrayList<>();
    for (ClassField field : classCall.getDefinition().getFields()) {
      if (!classCall.isImplemented(field)) {
        fields.add(field.getReferable());
      }
    }
    if (!fields.isEmpty()) {
      throw new CoreException(CoreErrorWrapper.make(new FieldsImplementationError(false, classCall.getDefinition().getReferable(), fields, mySourceNode), classCall));
    }
  }

  @Override
  public Expression visitNew(NewExpression expr, Expression expectedType) {
    ClassCallExpression classCall = expr.getType();
    visitClassCall(classCall, null);
    checkCocoverage(classCall);
    return check(expectedType, classCall, expr);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Expression expectedType) {
    Expression type = expr.getExpression().accept(this, null);
    Sort sortArg = type.getSortOfType();
    if (sortArg == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the sort of the type of the expression", mySourceNode), expr.getExpression()));
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
      addBinding(clause, expr);
      clause.getExpression().accept(this, null);
    }
    Expression type = expr.getExpression().accept(this, expectedType);
    myContext.removeAll(expr.getClauses());
    return type;
  }

  Integer checkLevelProof(Expression proof, Expression type) {
    Expression proofType = proof.accept(this, null);

    List<SingleDependentLink> params = new ArrayList<>();
    FunCallExpression codomain = proofType.getPiParameters(params, false).toEquality();
    if (codomain == null || params.isEmpty() || params.size() % 2 == 1) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("\\level has wrong format", mySourceNode), proof));
    }

    for (int i = 0; i < params.size(); i += 2) {
      if (!CompareVisitor.compare(myEquations, CMP.EQ, type, params.get(i).getTypeExpr(), Type.OMEGA, mySourceNode) || !CompareVisitor.compare(myEquations, CMP.EQ, type, params.get(i + 1).getTypeExpr(), Type.OMEGA, mySourceNode)) {
        throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("\\level has wrong format", mySourceNode), proof));
      }

      List<Expression> args = new ArrayList<>();
      args.add(type);
      args.add(new ReferenceExpression(params.get(i)));
      args.add(new ReferenceExpression(params.get(i + 1)));
      type = new FunCallExpression(Prelude.PATH_INFIX, Sort.PROP, args);
    }

    return params.size() / 2 - 2;
  }

  private boolean checkElimPattern(Expression type, Pattern pattern, DependentLink firstBinding, ExprSubstitution idpSubst, List<Pair<Expression,Expression>> types, Expression errorExpr) {
    if (pattern instanceof BindingPattern) {
      Expression actualType = pattern.getFirstBinding().getTypeExpr();
      if (pattern.getFirstBinding() instanceof TypedDependentLink) {
        actualType.accept(this, Type.OMEGA);
      }
      types.add(new Pair<>(actualType, type));
      addBinding(pattern.getFirstBinding(), errorExpr);
      return true;
    }

    if (pattern instanceof ConstructorPattern && pattern.getDefinition() == Prelude.IDP) {
      FunCallExpression equality = type.toEquality();
      if (equality == null || !(type instanceof DataCallExpression)) {
        throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(type, DocFactory.text("_ = _"), mySourceNode), errorExpr));
      }
      Expression left = equality.getDefCallArguments().get(1).normalize(NormalizationMode.WHNF);
      Expression right = equality.getDefCallArguments().get(2).normalize(NormalizationMode.WHNF);
      ReferenceExpression refExprLeft = left.cast(ReferenceExpression.class);
      ReferenceExpression refExprRight = right.cast(ReferenceExpression.class);
      Binding refLeft = refExprLeft == null ? null : refExprLeft.getBinding();
      Binding refRight = refExprRight == null ? null : refExprRight.getBinding();
      if (refLeft == null && refRight == null) {
        throw new CoreException(CoreErrorWrapper.make(new IdpPatternError(IdpPatternError.noVariable(), (DataCallExpression) type, mySourceNode), errorExpr));
      }

      Binding var = null;
      for (DependentLink link = firstBinding; link.hasNext(); link = link.getNext()) {
        if (link == refLeft) {
          var = link;
        } else if (link == refRight) {
          var = link;
        }
      }
      if (var == null) {
        throw new CoreException(CoreErrorWrapper.make(new IdpPatternError(IdpPatternError.noParameter(), (DataCallExpression) type, mySourceNode), errorExpr));
      }
      Expression otherExpr = ElimBindingVisitor.elimBinding(var == refLeft ? right : left, var);
      if (otherExpr == null) {
        throw new CoreException(CoreErrorWrapper.make(new IdpPatternError(IdpPatternError.variable(var.getName()), (DataCallExpression) type, mySourceNode), errorExpr));
      }

      Set<Binding> freeVars = FreeVariablesCollector.getFreeVariables(otherExpr);
      Binding banVar = null;
      List<DependentLink> params = DependentLink.Helper.toList(firstBinding);
      for (int i = params.size() - 1; i >= 0; i--) {
        DependentLink paramLink = params.get(i);
        if (paramLink == var) {
          break;
        }
        if (freeVars.contains(paramLink)) {
          banVar = paramLink;
        }
        if (paramLink instanceof TypedDependentLink && banVar != null && paramLink.getTypeExpr().findBinding(var)) {
          throw new CoreException(CoreErrorWrapper.make(new IdpPatternError(IdpPatternError.subst(var.getName(), paramLink.getName(), banVar.getName()), null, mySourceNode), errorExpr));
        }
      }

      myContext.remove(var);
      idpSubst.add(var, otherExpr);
      return true;
    }

    if (pattern instanceof ConstructorPattern && pattern.getDefinition() == null) {
      if (type instanceof SigmaExpression) {
        return checkElimPatterns(((SigmaExpression) type).getParameters(), pattern.getSubPatterns(), new ExprSubstitution(), firstBinding, idpSubst, types, errorExpr, null);
      } else if (type instanceof ClassCallExpression) {
        return checkElimPatterns(((ClassCallExpression) type).getClassFieldParameters(), pattern.getSubPatterns(), new ExprSubstitution(), firstBinding, idpSubst, types, errorExpr, null);
      } else {
        throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a sigma type or a class call"), type, mySourceNode), errorExpr));
      }
    }

    if (!(type instanceof DataCallExpression)) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a data type"), type, mySourceNode), errorExpr));
    }
    DataCallExpression dataCall = (DataCallExpression) type;

    if (pattern == EmptyPattern.INSTANCE) {
      List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
      if (conCalls == null) {
        throw new CoreException(CoreErrorWrapper.make(new ImpossibleEliminationError(dataCall, mySourceNode), errorExpr));
      }
      if (!conCalls.isEmpty()) {
        throw new CoreException(CoreErrorWrapper.make(new DataTypeNotEmptyError(dataCall, DataTypeNotEmptyError.getConstructors(conCalls), mySourceNode), errorExpr));
      }
      return false;
    }

    assert pattern instanceof ConstructorPattern;
    ConstructorPattern conPattern = (ConstructorPattern) pattern;
    if (!(conPattern.getDefinition() instanceof Constructor)) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Expected a constructor", mySourceNode), errorExpr));
    }

    List<ConCallExpression> conCalls = new ArrayList<>(1);
    if (!dataCall.getMatchedConCall((Constructor) conPattern.getDefinition(), conCalls)) {
      throw new CoreException(CoreErrorWrapper.make(new ImpossibleEliminationError(dataCall, mySourceNode), errorExpr));
    }
    if (conCalls.isEmpty()) {
      throw new CoreException(CoreErrorWrapper.make(new DataTypeNotEmptyError(dataCall, DataTypeNotEmptyError.getConstructors(conCalls), mySourceNode), errorExpr));
    }

    ConCallExpression conCall = conCalls.get(0);
    return checkElimPatterns(DependentLink.Helper.subst(conCall.getDefinition().getParameters(), new ExprSubstitution().add(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments())), pattern.getSubPatterns(), new ExprSubstitution(), firstBinding, idpSubst, types, errorExpr, null);
  }

  private boolean checkElimPatterns(DependentLink parameters, List<? extends Pattern> patterns, ExprSubstitution substitution, DependentLink firstBinding, ExprSubstitution idpSubst, List<Pair<Expression,Expression>> types, Expression errorExpr, List<ExpressionPattern> exprPatterns) {
    boolean noEmpty = true;
    for (Pattern pattern : patterns) {
      if (!parameters.hasNext()) {
        throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Too many patterns", mySourceNode), errorExpr));
      }
      Expression type = parameters.getTypeExpr().subst(substitution).normalize(NormalizationMode.WHNF).getUnderlyingExpression();
      if (noEmpty) {
        ExprSubstitution varSubst = new ExprSubstitution();
        if (!checkElimPattern(type, pattern, firstBinding, varSubst, types, errorExpr)) {
          if (exprPatterns == null) {
            return false;
          }
          noEmpty = false;
        }
        substitution.addSubst(varSubst);
        idpSubst.addSubst(varSubst);
      }
      ExpressionPattern exprPattern = pattern.toExpressionPattern(type);
      if (exprPattern == null) {
        throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot convert pattern", mySourceNode), errorExpr));
      }
      if (exprPatterns != null) {
        exprPatterns.add(exprPattern);
      }
      Expression expression = exprPattern.toExpression();
      if (expression != null) {
        substitution.add(parameters, expression);
      }
      parameters = parameters.getNext();
    }

    if (parameters.hasNext()) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Not enough patterns", mySourceNode), errorExpr));
    }

    return noEmpty;
  }

  private DependentLink checkStitchedPatterns(Collection<? extends Pattern> patterns, DependentLink link, Expression errorExpr) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof BindingPattern) {
        if (((BindingPattern) pattern).getBinding() != link) {
          throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("", mySourceNode), errorExpr));
        }
        link = link.getNext();
      } else if (pattern instanceof ConstructorPattern) {
        link = checkStitchedPatterns(pattern.getSubPatterns(), link, errorExpr);
      } else if (pattern != EmptyPattern.INSTANCE) {
        throw new IllegalStateException();
      }
    }
    return link;
  }

  void checkElimBody(ElimBody elimBody, DependentLink parameters, Expression type, Integer level, Expression errorExpr, boolean isSFunc, PatternTypechecking.Mode mode) {
    List<ElimClause<ExpressionPattern>> exprClauses = new ArrayList<>();
    for (ElimClause<Pattern> clause : elimBody.getClauses()) {
      DependentLink firstBinding = Pattern.getFirstBinding(clause.getPatterns());
      checkStitchedPatterns(clause.getPatterns(), firstBinding, errorExpr);

      ExprSubstitution substitution = new ExprSubstitution();
      ExprSubstitution idpSubst = new ExprSubstitution();
      List<Pair<Expression,Expression>> types = new ArrayList<>();
      List<ExpressionPattern> exprPatterns = new ArrayList<>();
      exprClauses.add(new ElimClause<>(exprPatterns, clause.getExpression()));
      boolean noEmpty = checkElimPatterns(parameters, clause.getPatterns(), substitution, firstBinding, idpSubst, types, errorExpr, exprPatterns);
      for (Pair<Expression,Expression> pair : types) {
        Expression expectedType = pair.proj2.subst(idpSubst);
        if (!new CompareVisitor(myEquations, CMP.EQ, mySourceNode).normalizedCompare(expectedType, pair.proj1.normalize(NormalizationMode.WHNF), Type.OMEGA)) {
          throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(expectedType, pair.proj1, mySourceNode), errorExpr));
        }
      }
      if (noEmpty) {
        if (clause.getExpression() != null) {
          substitution.addSubst(idpSubst);
          clause.getExpression().accept(this, type.subst(substitution));
        } else {
          throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("The right hand side cannot be omitted without absurd pattern", mySourceNode), errorExpr));
        }
      }

      freeDependentLink(firstBinding);
    }

    if (level == null) {
      DefCallExpression defCall = type.cast(DefCallExpression.class);
      if (defCall != null) {
        level = defCall.getUseLevel();
      } else {
        defCall = type.getPiParameters(null, false).cast(DefCallExpression.class);
        if (defCall != null) {
          level = defCall.getUseLevel();
        }
      }
    }

    Sort sort = type.getSortOfType();
    ErrorReporter errorReporter = new MyErrorReporter(errorExpr);
    ElimBody newBody = new ElimTypechecking(errorReporter, myEquations, type, mode, level, sort != null ? sort.getHLevel() : Level.INFINITY, 0, isSFunc, null, mySourceNode).typecheckElim(exprClauses, parameters);
    if (newBody == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot check the body", mySourceNode), errorExpr));
    }
    if (!new CompareVisitor(myEquations, CMP.EQ, mySourceNode).compare(elimBody.getElimTree(), newBody.getElimTree())) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("The elim tree of the body is incorrect", mySourceNode), errorExpr));
    }

    // TODO[lang_ext]: Check conditions
  }

  private static class MyErrorReporter implements ErrorReporter {
    final Expression errorExpr;

    private MyErrorReporter(Expression errorExpr) {
      this.errorExpr = errorExpr;
    }

    @Override
    public void report(GeneralError error) {
      throw new CoreException(CoreErrorWrapper.make(error, errorExpr));
    }
  }

  @Override
  public Expression visitCase(CaseExpression expr, Expression expectedType) {
    ExprSubstitution substitution = new ExprSubstitution();
    checkDependentLink(expr.getParameters(), Type.OMEGA, expr);
    checkList(expr.getArguments(), expr.getParameters(), substitution, LevelSubstitution.EMPTY);
    expr.getResultType().accept(this, Type.OMEGA);

    Integer level = expr.getResultTypeLevel() == null ? null : checkLevelProof(expr.getResultTypeLevel(), expr.getResultType());

    freeDependentLink(expr.getParameters());
    checkElimBody(expr.getElimBody(), expr.getParameters(), expr.getResultType(), level, expr, expr.isSCase(), PatternTypechecking.Mode.CASE);
    return check(expectedType, expr.getResultType().subst(substitution), expr);
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Expression expectedType) {
    expr.getTypeOf().accept(this, Type.OMEGA);
    return check(expectedType, expr.getExpression().accept(this, expr.getTypeOf()), expr);
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Expression expectedType) {
    return check(expectedType, new DataCallExpression(Prelude.NAT, Sort.PROP, Collections.emptyList()), expr);
  }
}
