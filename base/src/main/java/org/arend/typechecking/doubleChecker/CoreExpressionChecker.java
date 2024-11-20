package org.arend.typechecking.doubleChecker;

import org.arend.core.context.binding.*;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.param.*;
import org.arend.core.definition.*;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.*;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.*;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.naming.reference.FieldReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.patternmatching.ConditionsChecking;
import org.arend.typechecking.patternmatching.ElimTypechecking;
import org.arend.typechecking.patternmatching.ExtElimClause;
import org.arend.typechecking.patternmatching.PatternTypechecking;
import org.arend.util.SingletonList;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;

public class CoreExpressionChecker implements ExpressionVisitor<Expression, Expression> {
  private final Set<Binding> myContext;
  private final Equations myEquations;
  private final Concrete.SourceNode mySourceNode;
  private List<? extends LevelVariable> myPParameters;
  private List<? extends LevelVariable> myHParameters;
  private boolean myCheckLevelVariables;

  public CoreExpressionChecker(Set<Binding> context, Equations equations, Concrete.SourceNode sourceNode) {
    myContext = context;
    myEquations = equations;
    mySourceNode = sourceNode;
  }

  void clear() {
    if (myContext != null) myContext.clear();
    myPParameters = null;
    myHParameters = null;
    myCheckLevelVariables = false;
  }

  void setDefinition(Definition definition) {
    List<? extends LevelVariable> params = definition.getLevelParameters();
    int pNum = definition.getNumberOfPLevelParameters();
    myPParameters = params == null ? null : params.subList(0, pNum);
    myHParameters = params == null ? null : params.subList(pNum, params.size());
    myCheckLevelVariables = true;
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

  void checkLevels(Levels levels, Definition definition, Expression expr) {
    List<? extends Level> list = levels.toList();
    int pNum = definition.getNumberOfPLevelParameters();
    for (int i = 0; i < pNum; i++) {
      checkLevel(list.get(i), LevelVariable.LvlType.PLVL, expr);
    }
    for (int i = pNum; i < list.size(); i++) {
      checkLevel(list.get(i), LevelVariable.LvlType.HLVL, expr);
    }
  }

  private void checkBoxes(DefCallExpression defCall) {
    DependentLink param = defCall.getDefinition().getParameters();
    for (Expression arg : defCall.getDefCallArguments()) {
      if (param.isProperty() && !arg.isInstance(BoxExpression.class)) {
        throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Parameter '" + param.getName() + "' must be boxed", mySourceNode), defCall));
      }
      param = param.getNext();
    }
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Expression expectedType) {
    checkLevels(expr.getLevels(), expr.getDefinition(), expr);
    checkBoxes(expr);
    ExprSubstitution substitution = new ExprSubstitution();
    List<? extends Expression> args = expr.getDefCallArguments();
    checkList(args, expr.getDefinition().getParameters(), substitution, expr.getLevelSubstitution());
    var resultType = expr.getDefinition().getResultType().subst(substitution, expr.getMinimizedLevels().makeSubstitution(expr.getDefinition()));
    if (expr.getDefinition() == Prelude.MOD || expr.getDefinition() == Prelude.DIV_MOD) {
      Expression arg2 = args.get(1);
      IntegerExpression integer = arg2.cast(IntegerExpression.class);
      ConCallExpression conCall = arg2.cast(ConCallExpression.class);
      if (integer != null && !integer.isZero() || conCall != null && conCall.getDefinition() == Prelude.SUC) {
        resultType = expr.getDefinition() == Prelude.MOD ? ExpressionFactory.Fin(arg2) : ExpressionFactory.finDivModType(arg2);
      }
    }
    return check(expectedType, resultType, expr);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Expression expectedType) {
    checkLevels(expr.getLevels(), expr.getDefinition(), expr);
    checkBoxes(expr);
    if (expr.getDefinition() == Prelude.FIN_ZERO || expr.getDefinition() == Prelude.FIN_SUC) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("'Fin." + expr.getDefinition().getName() + "' is not allowed", mySourceNode), expr));
    }

    if (expr.getDefinition() == Prelude.SUC) {
      int sucs = 1;
      Expression expression = expr.getDefCallArguments().get(0);
      while (expression instanceof ConCallExpression && ((ConCallExpression) expression).getDefinition() == Prelude.SUC) {
        sucs++;
        expression = ((ConCallExpression) expression).getDefCallArguments().get(0);
      }
      DataCallExpression dataCall = expression.accept(this, null).normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
      if (dataCall != null && dataCall.getDefinition() == Prelude.FIN) {
        Expression arg = dataCall.getDefCallArguments().get(0);
        for (int i = 0; i < sucs; i++) {
          arg = Suc(arg);
        }
        return check(expectedType, DataCallExpression.make(dataCall.getDefinition(), dataCall.getLevels(), new SingletonList<>(arg)), expr);
      }
      return check(expectedType, Nat(), expr);
    }

    Expression result = null;
    Expression it = expr;
    do {
      ConCallExpression conCall = (ConCallExpression) it;
      LevelSubstitution levelSubst = conCall.getLevelSubstitution();
      ExprSubstitution substitution = new ExprSubstitution();
      checkList(conCall.getDataTypeArguments(), conCall.getDefinition().getDataTypeParameters(), substitution, levelSubst);
      Expression actualType = conCall.getDefinition().getDataTypeExpression(conCall.getLevels(), conCall.getDataTypeArguments());
      check(expectedType, actualType, conCall);
      if (result == null) {
        result = actualType;
      }

      int recursiveParam = conCall.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        checkList(conCall.getDefCallArguments(), conCall.getDefinition().getParameters(), substitution, levelSubst);
        return result;
      }

      DependentLink link = conCall.getDefinition().getParameters();
      for (int i = 0; i < conCall.getDefCallArguments().size(); i++) {
        if (i != recursiveParam) {
          Expression arg = conCall.getDefCallArguments().get(i);
          arg.accept(this, link.getTypeExpr().subst(substitution, levelSubst));
          substitution.add(link, arg);
        } else {
          expectedType = link.getTypeExpr().subst(substitution, levelSubst);
        }
        link = link.getNext();
      }

      it = conCall.getDefCallArguments().get(recursiveParam);
    } while (it instanceof ConCallExpression);

    it.accept(this, expectedType);
    return result;
  }

  @Override
  public Expression visitDataCall(DataCallExpression expr, Expression expectedType) {
    checkLevels(expr.getLevels(), expr.getDefinition(), expr);
    checkBoxes(expr);
    checkList(expr.getDefCallArguments(), expr.getDefinition().getParameters(), new ExprSubstitution(), expr.getLevelSubstitution());
    return check(expectedType, GetTypeVisitor.INSTANCE.visitDataCall(expr, null), expr);
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Expression expectedType) {
    Expression argType = expr.getArgument().accept(this, null).normalize(NormalizationMode.WHNF);
    ClassCallExpression argClassCall = argType.cast(ClassCallExpression.class);
    if (argClassCall == null || !argClassCall.getDefinition().isSubClassOf(expr.getDefinition().getParentClass())) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.refDoc(expr.getDefinition().getParentClass().getRef()), argType, mySourceNode), expr));
    }

    Expression actualType = null;
    Levels levels = GetTypeVisitor.MIN_INSTANCE.minimizeLevels(argClassCall);
    PiExpression overriddenType = argClassCall.getDefinition().getOverriddenType(expr.getDefinition(), levels);
    if (overriddenType != null) {
      actualType = overriddenType.applyExpression(expr.getArgument());
    }
    if (actualType == null) {
      actualType = expr.getDefinition().getResultType().subst(new ExprSubstitution(expr.getDefinition().getType().getParameters(), expr.getArgument()), argClassCall.getDefinition().castLevels(expr.getDefinition().getParentClass(), levels).makeSubstitution(expr.getDefinition()));
    }
    return check(expectedType, actualType, expr);
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Expression expectedType) {
    if (!(expr.getImplementedHere().size() <= 1 || expr.getImplementedHere() instanceof LinkedHashMap)) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Implementations in a classCall have wrong type: " + expr.getImplementedHere().getClass(), mySourceNode), expr));
    }
    checkLevels(expr.getLevels(), expr.getDefinition(), expr);
    addBinding(expr.getThisBinding(), expr);
    Expression thisExpr = new ReferenceExpression(expr.getThisBinding());
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      Expression type = expr.getDefinition().getFieldType(entry.getKey(), expr.getLevels(entry.getKey().getParentClass()), thisExpr);
      if (entry.getKey().isProperty() || entry.getKey().getResultTypeLevel() == -1) {
        if (entry.getValue() instanceof LamExpression) {
          checkLam((LamExpression) entry.getValue(), type, -1);
        } else if (entry.getValue() instanceof CaseExpression) {
          checkCase((CaseExpression) entry.getValue(), type, -1);
        } else {
          entry.getValue().accept(this, type);
        }
      } else {
        entry.getValue().accept(this, type);
      }
    }
    if (myContext != null) myContext.remove(expr.getThisBinding());

    Integer level = expr.getDefinition().getUseLevel(expr.getImplementedHere(), expr.getThisBinding(), true);
    if (level == null || level != -1) {
      for (ClassField field : expr.getDefinition().getNotImplementedFields()) {
        if (!expr.isImplementedHere(field)) {
          Sort sort = expr.getDefinition().getFieldType(field, expr.getLevels(field.getParentClass()), thisExpr).normalize(NormalizationMode.WHNF).getSortOfType();
          if (sort == null) {
            throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the type of field '" + field.getName() + "'", mySourceNode), expr));
          }
          if (sort.isProp()) {
            continue;
          }
          if (!(Level.compare(sort.getPLevel(), expr.getSortOfType().getPLevel(), CMP.LE, myEquations, mySourceNode) && (level != null && sort.getHLevel().isClosed() && sort.getHLevel().getConstant() <= level || Level.compare(sort.getHLevel(), expr.getSortOfType().getHLevel(), CMP.LE, myEquations, mySourceNode)))) {
            throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("The sort " + sort + " of field '" + field.getName() + "' does not fit into the expected sort " + expr.getSortOfType(), mySourceNode), expr));
          }
        }
      }
    }

    if (expr.getUniverseKind().ordinal() < expr.getDefinition().getUniverseKind().ordinal()) {
      for (ClassField field : expr.getDefinition().getNotImplementedFields()) {
        if (expr.getUniverseKind().ordinal() < field.getUniverseKind().ordinal() && !expr.isImplementedHere(field)) {
          throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Field '" + field.getName() + "' has universes, but the class call does not have them", mySourceNode), expr));
        }
      }
    }

    return check(expectedType, GetTypeVisitor.INSTANCE.visitClassCall(expr, null), expr);
  }

  @Override
  public Expression visitApp(AppExpression expr, Expression expectedType) {
    Expression funType = expr.getFunction().accept(this, null).normalize(NormalizationMode.WHNF);
    PiExpression piType = funType.cast(PiExpression.class);
    if (piType == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a pi type"), funType, mySourceNode), expr.getFunction()));
    }
    if (piType.getParameters().isExplicit() != expr.isExplicit()) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a pi type with " + (expr.isExplicit() ? "explicit" : "implicit") + " parameter"), piType, mySourceNode), expr.getFunction()));
    }

    expr.getArgument().accept(this, piType.getParameters().getTypeExpr());
    return check(expectedType, piType.applyExpression(expr.getArgument()), expr);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Expression expectedType) {
    if (expr.getBinding() instanceof PersistentEvaluatingBinding) {
      return ((PersistentEvaluatingBinding) expr.getBinding()).getExpression().accept(this, expectedType);
    }
    if (myContext != null && !myContext.contains(expr.getBinding())) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Variable '" + expr.getBinding().getName() + "' is not bound", mySourceNode), expr));
    }
    return check(expectedType, expr.getBinding().getTypeExpr(), expr);
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Expression expectedType) {
    if (expr.getSubstExpression() != null) {
      return expr.getSubstExpression().accept(this, expectedType);
    }
    InferenceVariable infVar = expr.getVariable();
    for (Binding bound : infVar.getBounds()) {
      if (myContext != null && !myContext.contains(bound)) {
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
    if (binding != UnusedIntervalDependentLink.INSTANCE && !(myContext == null || myContext.add(binding))) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Binding '" + binding.getName() + "' is already bound", mySourceNode), expr));
    }
  }

  void removeBinding(Binding binding) {
    if (myContext != null) myContext.remove(binding);
  }

  Sort checkDependentLinkWithResult(DependentLink link, Expression type, Expression expr) {
    Sort result = Sort.PROP;
    for (; link.hasNext(); link = link.getNext()) {
      addBinding(link, expr);
      if (link instanceof TypedDependentLink) {
        Expression paramType = link.getTypeExpr().accept(this, type);
        if (result != null) {
          Sort sort = paramType.toSort();
          result = sort == null ? null : result.max(sort);
        }
      }
    }
    return result;
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
      if (myContext != null) myContext.remove(link);
    }
  }

  private void compareSort(Sort expected, Sort actual, Expression expr) {
    if (!Sort.compare(actual, expected, CMP.LE, myEquations, mySourceNode)) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError("Sort mismatch", new UniverseExpression(expected), new UniverseExpression(actual), mySourceNode), expr));
    }
  }

  private Expression checkLam(LamExpression expr, Expression expectedType, Integer level) {
    checkDependentLink(expr.getParameters(), Type.OMEGA, expr);
    Expression type;
    if (expr.getBody() instanceof LamExpression) {
      type = checkLam((LamExpression) expr.getBody(), null, level);
    } else if (expr.getBody() instanceof CaseExpression) {
      type = checkCase((CaseExpression) expr.getBody(), null, level);
    } else {
      type = expr.getBody().accept(this, null);
    }
    Sort sort = type.normalize(NormalizationMode.WHNF).getSortOfType();
    if (sort == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot infer sort", mySourceNode), expr));
    }
    freeDependentLink(expr.getParameters());
    Level pLevel = sort.isProp() ? new Level(0) : expr.getParameters().getType().getSortOfType().getPLevel().max(sort.getPLevel());
    if (pLevel == null) {
      checkSort(expr.getResultSort(), expr);
      compareSort(expr.getResultSort(), new Sort(expr.getParameters().getType().getSortOfType().getPLevel(), expr.getResultSort().getHLevel()), expr);
      compareSort(expr.getResultSort(), sort, expr);
    }
    return check(expectedType, new PiExpression(pLevel == null ? expr.getResultSort() : new Sort(pLevel, sort.getHLevel()), expr.getParameters(), type), expr);
  }

  @Override
  public Expression visitLam(LamExpression expr, Expression expectedType) {
    return checkLam(expr, expectedType, null);
  }

  @Override
  public Expression visitPi(PiExpression expr, Expression expectedType) {
    checkSort(expr.getResultSort(), expr);
    UniverseExpression type = new UniverseExpression(expr.getResultSort());
    Sort sort1 = checkDependentLinkWithResult(expr.getParameters(), expr.getResultSort().isProp() ? null : new UniverseExpression(new Sort(expr.getResultSort().getPLevel(), Level.INFINITY)), expr);
    Sort sort2 = expr.getCodomain().accept(this, type).toSort();
    freeDependentLink(expr.getParameters());

    Expression actualType;
    if (sort1 != null && sort1.isProp() || sort2 != null && sort2.isProp()) {
      actualType = new UniverseExpression(sort2);
    } else {
      Level maxPLevel = sort1 == null || sort2 == null ? null : sort1.getPLevel().max(sort2.getPLevel());
      actualType = maxPLevel == null ? type : new UniverseExpression(new Sort(maxPLevel, sort2.getHLevel()));
    }
    return check(expectedType, actualType, expr);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Expression expectedType) {
    checkSort(expr.getSort(), expr);
    UniverseExpression type = new UniverseExpression(expr.getSort());
    Sort sort = checkDependentLinkWithResult(expr.getParameters(), type, expr);
    freeDependentLink(expr.getParameters());

    for (DependentLink param = expr.getParameters(); param.hasNext(); param = param.getNext()) {
      if (param.isProperty() && !param.getType().getSortOfType().isProp()) {
        throw new CoreException(CoreErrorWrapper.make(new LevelMismatchError(LevelMismatchError.TargetKind.SIGMA_FIELD, param.getType().getSortOfType(), mySourceNode), expr));
      }
    }

    return check(expectedType, sort == null ? type : new UniverseExpression(sort), expr);
  }

  private void checkLevel(Level level, LevelVariable.LvlType type, Expression expr) {
    LevelVariable var = level.getVar();
    if (var == null) return;
    if (var.getType() != type) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text(type.toString()), DocFactory.text(var.getType().toString()), mySourceNode), expr));
    }
    if (!myCheckLevelVariables) return;
    List<? extends LevelVariable> params = type == LevelVariable.LvlType.HLVL ? myHParameters : myPParameters;
    if (params != null && params.isEmpty() || var instanceof ParamLevelVariable && (params == null || !params.contains(var))) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Variable '" + var + "' is not defined", mySourceNode), expr));
    }
  }

  private void checkSort(Sort sort, Expression expr) {
    checkLevel(sort.getPLevel(), LevelVariable.LvlType.PLVL, expr);
    checkLevel(sort.getHLevel(), LevelVariable.LvlType.HLVL, expr);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Expression expectedType) {
    if (expr.isOmega()) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Universes of the infinity level are not allowed", mySourceNode), expr));
    }
    Sort sort = expr.getSort();
    if (sort.getHLevel().isProp() && !(sort.getPLevel().isClosed() && sort.getPLevel().getConstant() == 0)) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("p-level of \\Prop is not 0", mySourceNode), expr));
    }
    checkSort(sort, expr);
    return check(expectedType, new UniverseExpression(sort.succ()), expr);
  }

  @Override
  public Expression visitError(ErrorExpression expr, Expression expectedType) {
    if (expr.isError()) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Unknown error", mySourceNode), expr));
    }
    return expr.getExpression() == null ? expr : expr.replaceExpression(null);
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
      substitution.add(param, ProjExpression.make(expr.getExpression(), i, param.isProperty()));
      param = param.getNext();
    }

    return check(expectedType, param.getTypeExpr().subst(substitution), expr);
  }

  void checkCocoverage(ClassCallExpression classCall) {
    if (classCall.getDefinition().getNumberOfNotImplementedFields() == classCall.getImplementedHere().size()) {
      return;
    }

    List<FieldReferable> fields = new ArrayList<>();
    for (ClassField field : classCall.getDefinition().getNotImplementedFields()) {
      if (!classCall.isImplementedHere(field)) {
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
    Sort sort = type.getSortOfType();
    if (sort == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot infer the sort of the type of the expression", mySourceNode), expr.getExpression()));
    }

    List<Expression> args = new ArrayList<>(3);
    args.add(type);
    args.add(expr.getExpression());
    Expression evaluated = expr.eval();
    if (evaluated == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Expression does not evaluate", mySourceNode), expr.getExpression()));
    }

    args.add(evaluated);
    return check(expectedType, FunCallExpression.make(Prelude.PATH_INFIX, new LevelPair(sort.getPLevel(), sort.getHLevel()), args), expr);
  }

  @Override
  public Expression visitBox(BoxExpression expr, Expression expectedType) {
    Expression type = expr.getType().accept(this, Type.OMEGA);
    Sort sort = type.toSort();
    if (sort == null || !sort.isProp()) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError("The type of a boxed expression doe not live in \\Prop", new UniverseExpression(Sort.PROP), type.getType(), mySourceNode), expr));
    }
    return check(expectedType, expr.getType(), expr);
  }

  @Override
  public Expression visitLet(LetExpression expr, Expression expectedType) {
    for (HaveClause clause : expr.getClauses()) {
      clause.getExpression().accept(this, null);
      addBinding(clause, expr);
    }
    Expression type = expr.getExpression().accept(this, expectedType);
    if (myContext != null) expr.getClauses().forEach(myContext::remove);
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
      type = FunCallExpression.make(Prelude.PATH_INFIX, LevelPair.PROP, args);
    }

    return params.size() / 2 - 2;
  }

  private boolean checkElimPattern(Expression type, Pattern pattern, List<Binding> newBindings, ExprSubstitution idpSubst, ExprSubstitution patternSubst, ExprSubstitution reversePatternSubst, Expression errorExpr) {
    if (pattern instanceof BindingPattern) {
      Expression actualType = pattern.getFirstBinding().getTypeExpr();
      if (pattern.getFirstBinding() instanceof TypedDependentLink) {
        actualType.accept(this, Type.OMEGA);
      }
      Binding newBinding = new TypedBinding(pattern.getFirstBinding().getName(), type);
      newBindings.add(newBinding);
      patternSubst.add(pattern.getFirstBinding(), new ReferenceExpression(newBinding));
      reversePatternSubst.add(newBinding, new ReferenceExpression(pattern.getFirstBinding()));
      addBinding(pattern.getFirstBinding(), errorExpr);
      return true;
    }

    if (pattern instanceof ConstructorPattern && pattern.getDefinition() == Prelude.IDP) {
      FunCallExpression equality = type.toEquality();
      if (equality == null || !(type instanceof DataCallExpression)) {
        throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(type, DocFactory.text("_ = _"), mySourceNode), errorExpr));
      }
      Expression left = equality.getDefCallArguments().get(1).subst(patternSubst).normalize(NormalizationMode.WHNF).subst(patternSubst);
      Expression right = equality.getDefCallArguments().get(2).subst(patternSubst).normalize(NormalizationMode.WHNF).subst(patternSubst);
      ReferenceExpression refExprLeft = left.cast(ReferenceExpression.class);
      ReferenceExpression refExprRight = right.cast(ReferenceExpression.class);
      Binding refLeft = refExprLeft == null ? null : refExprLeft.getBinding();
      Binding refRight = refExprRight == null ? null : refExprRight.getBinding();
      if (refLeft == null && refRight == null) {
        throw new CoreException(CoreErrorWrapper.make(new IdpPatternError(null, IdpPatternError.noVariable(), (DataCallExpression) type, mySourceNode), errorExpr));
      }

      Binding var = null;
      for (Binding binding : newBindings) {
        if (binding == refLeft) {
          var = binding;
        } else if (binding == refRight) {
          var = binding;
        }
      }
      if (var == null) {
        throw new CoreException(CoreErrorWrapper.make(new IdpPatternError(null, IdpPatternError.noParameter(), (DataCallExpression) type, mySourceNode), errorExpr));
      }
      Expression otherExpr = ElimBindingVisitor.elimBinding(var == refLeft ? right : left, var);
      if (otherExpr == null) {
        throw new CoreException(CoreErrorWrapper.make(new IdpPatternError(null, IdpPatternError.variable(var.getName()), (DataCallExpression) type, mySourceNode), errorExpr));
      }

      Set<Binding> freeVars = FreeVariablesCollector.getFreeVariables(otherExpr);
      Binding banVar = null;
      for (int i = newBindings.size() - 1; i >= 0; i--) {
        Binding binding = newBindings.get(i);
        if (binding == var) {
          break;
        }
        if (freeVars.contains(binding)) {
          banVar = binding;
        }
        if (banVar != null && binding.getTypeExpr().findBinding(var)) {
          throw new CoreException(CoreErrorWrapper.make(new IdpPatternError(null, IdpPatternError.subst(var.getName(), binding.getName(), banVar.getName()), null, mySourceNode), errorExpr));
        }
      }

      var = ((ReferenceExpression) reversePatternSubst.get(var)).getBinding();
      if (myContext != null) myContext.remove(var);
      idpSubst.add(var, otherExpr.subst(reversePatternSubst));
      return true;
    }

    if (pattern instanceof ConstructorPattern && pattern.getConstructor() == null) {
      if (type instanceof SigmaExpression) {
        return checkElimPatterns(((SigmaExpression) type).getParameters(), pattern.getSubPatterns(), new ExprSubstitution(), newBindings, idpSubst, patternSubst, reversePatternSubst, errorExpr, null);
      } else if (type instanceof ClassCallExpression) {
        return checkElimPatterns(((ClassCallExpression) type).getClassFieldParameters(), pattern.getSubPatterns(), new ExprSubstitution(), newBindings, idpSubst, patternSubst, reversePatternSubst, errorExpr, null);
      } else {
        throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a sigma type or a class call"), type, mySourceNode), errorExpr));
      }
    }

    if (pattern instanceof ConstructorPattern && pattern.getConstructor() instanceof DConstructor constructor) {
      if (constructor != Prelude.EMPTY_ARRAY && constructor != Prelude.ARRAY_CONS) {
        throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Expected either '" + Prelude.EMPTY_ARRAY.getName() + "' or '" + Prelude.ARRAY_CONS.getName() + "'", mySourceNode), errorExpr));
      }
      if (!(type instanceof ClassCallExpression classCall && classCall.getDefinition() == Prelude.DEP_ARRAY)) {
        throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(new ClassCallExpression(Prelude.DEP_ARRAY, LevelPair.STD), type, mySourceNode), errorExpr));
      }
      Expression length = classCall.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
      if (length != null) length = length.normalize(NormalizationMode.WHNF);
      if (length != null && !(length instanceof IntegerExpression || length instanceof ConCallExpression && ((ConCallExpression) length).getDefinition() == Prelude.SUC)) {
        throw new CoreException(CoreErrorWrapper.make(new ImpossibleEliminationError(classCall, mySourceNode), errorExpr));
      }
      Boolean isEmpty = length == null ? null : length instanceof IntegerExpression && ((IntegerExpression) length).isZero();
      if (isEmpty != null && isEmpty != (constructor == Prelude.EMPTY_ARRAY)) {
        throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text(Prelude.DEP_ARRAY.getName() + " " + (isEmpty ? "0" : "(" + Prelude.SUC + " _)")), type, mySourceNode), errorExpr));
      }
      return checkElimPatterns(constructor.getArrayParameters(classCall), pattern.getSubPatterns(), new ExprSubstitution(), newBindings, idpSubst, patternSubst, reversePatternSubst, errorExpr, null);
    }

    if (!(type instanceof DataCallExpression dataCall)) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.text("a data type"), type, mySourceNode), errorExpr));
    }

    if (pattern instanceof EmptyPattern) {
      List<ConCallExpression> conCalls = dataCall.getMatchedConstructors();
      if (conCalls == null) {
        throw new CoreException(CoreErrorWrapper.make(new ImpossibleEliminationError(dataCall, mySourceNode, null, null, null, null, null), errorExpr));
      }
      if (!conCalls.isEmpty()) {
        throw new CoreException(CoreErrorWrapper.make(new DataTypeNotEmptyError(dataCall, DataTypeNotEmptyError.getConstructors(conCalls), mySourceNode), errorExpr));
      }
      return false;
    }

    assert pattern instanceof ConstructorPattern;
    var conPattern = (ConstructorPattern<?>) pattern;
    if (!(conPattern.getDefinition() instanceof Constructor)) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Expected a constructor", mySourceNode), errorExpr));
    }

    List<ConCallExpression> conCalls = new ArrayList<>(1);
    if (!dataCall.getMatchedConCall((Constructor) conPattern.getDefinition(), conCalls)) {
      throw new CoreException(CoreErrorWrapper.make(new ImpossibleEliminationError(dataCall, mySourceNode, null, null, null, null, null), errorExpr));
    }
    if (conCalls.isEmpty()) {
      throw new CoreException(CoreErrorWrapper.make(new DataTypeNotEmptyError(dataCall, DataTypeNotEmptyError.getConstructors(conCalls), mySourceNode), errorExpr));
    }

    ConCallExpression conCall = conCalls.get(0);
    return checkElimPatterns(DependentLink.Helper.subst(conCall.getDefinition().getParameters(), new ExprSubstitution().add(conCall.getDefinition().getDataTypeParameters(), conCall.getDataTypeArguments())), pattern.getSubPatterns(), new ExprSubstitution(), newBindings, idpSubst, patternSubst, reversePatternSubst, errorExpr, null);
  }

  private boolean checkElimPatterns(DependentLink parameters, List<? extends Pattern> patterns, ExprSubstitution substitution, List<Binding> newBindings, ExprSubstitution idpSubst, ExprSubstitution patternSubst, ExprSubstitution reversePatternSubst, Expression errorExpr, List<ExpressionPattern> exprPatterns) {
    boolean noEmpty = true;
    for (Pattern pattern : patterns) {
      if (!parameters.hasNext()) {
        throw new CoreException(CoreErrorWrapper.make(new CertainTypecheckingError(CertainTypecheckingError.Kind.TOO_MANY_PATTERNS, mySourceNode), errorExpr));
      }
      Expression type = parameters.getTypeExpr().subst(substitution);
      List<FunCallExpression> typeConstructorFunCalls = new ArrayList<>();
      if (pattern instanceof ConstructorPattern) {
        type = PatternTypechecking.unfoldType(type, typeConstructorFunCalls);
      } else {
        type = type.normalize(NormalizationMode.WHNF);
      }
      if (noEmpty) {
        ExprSubstitution varSubst = new ExprSubstitution();
        if (!checkElimPattern(type, pattern, newBindings, varSubst, patternSubst, reversePatternSubst, errorExpr)) {
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
        for (int i = typeConstructorFunCalls.size() - 1; i >= 0; i--) {
          expression = TypeConstructorExpression.match(typeConstructorFunCalls.get(i), expression);
        }
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
      if (pattern instanceof BindingPattern || pattern instanceof EmptyPattern) {
        if (pattern.getFirstBinding() != link || pattern instanceof BindingPattern && pattern.getBinding() != pattern.getFirstBinding()) {
          throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("", mySourceNode), errorExpr));
        }
        link = link.getNext();
      } else if (pattern instanceof ConstructorPattern) {
        link = checkStitchedPatterns(pattern.getSubPatterns(), link, errorExpr);
      } else {
        throw new IllegalStateException();
      }
    }
    return link;
  }

  void checkElimBody(FunctionDefinition definition, ElimBody elimBody, DependentLink parameters, Expression type, Integer level, Expression errorExpr, boolean isSFunc, PatternTypechecking.Mode mode) {
    List<ExtElimClause> exprClauses = new ArrayList<>();
    for (ElimClause<Pattern> clause : elimBody.getClauses()) {
      DependentLink firstBinding = Pattern.getFirstBinding(clause.getPatterns());
      checkStitchedPatterns(clause.getPatterns(), firstBinding, errorExpr);

      ExprSubstitution substitution = new ExprSubstitution();
      ExprSubstitution idpSubst = new ExprSubstitution();
      ExprSubstitution patternSubst = new ExprSubstitution();
      List<ExpressionPattern> exprPatterns = new ArrayList<>();
      exprClauses.add(new ExtElimClause(exprPatterns, clause.getExpression(), idpSubst));
      boolean noEmpty = checkElimPatterns(parameters, clause.getPatterns(), substitution, new ArrayList<>(), idpSubst, patternSubst, new ExprSubstitution(), errorExpr, exprPatterns);
      for (Map.Entry<Binding, Expression> entry : patternSubst.getEntries()) {
        Expression actualType = entry.getKey().getTypeExpr();
        Expression expectedType = entry.getValue().getType().subst(idpSubst);
        if (!new CompareVisitor(myEquations, CMP.LE, mySourceNode).normalizedCompare(actualType.normalize(NormalizationMode.WHNF), expectedType, Type.OMEGA, false)) {
          throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(expectedType, actualType, mySourceNode), errorExpr));
        }
      }
      if (noEmpty) {
        if (clause.getExpression() != null) {
          substitution.addSubst(idpSubst);
          if (clause.getExpression() instanceof CaseExpression) {
            checkCase((CaseExpression) clause.getExpression(), type.subst(substitution), level);
          } else {
            clause.getExpression().accept(this, type.subst(substitution));
          }
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
    ElimBody newBody = new ElimTypechecking(errorReporter, myEquations, type, mode, level, sort != null ? sort.getHLevel() : Level.INFINITY, isSFunc, null, 0, mySourceNode).typecheckElim(exprClauses, parameters);
    if (newBody == null) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Cannot check the body", mySourceNode), errorExpr));
    }
    if (!new CompareVisitor(myEquations, CMP.LE, mySourceNode).compare(newBody.getElimTree(), elimBody.getElimTree())) {
      throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("The elim tree of the body is incorrect", mySourceNode), errorExpr));
    }

    if (!(isSFunc && sort != null && sort.isProp())) {
      new ConditionsChecking(myEquations, errorReporter, mySourceNode).check(elimBody, exprClauses, null, definition);
    }
  }

  private static class MyErrorReporter implements ErrorReporter {
    final Expression errorExpr;

    private MyErrorReporter(Expression errorExpr) {
      this.errorExpr = errorExpr;
    }

    @Override
    public void report(GeneralError error) {
      if (error.level == GeneralError.Level.ERROR || error.level == GeneralError.Level.GOAL) {
        throw new CoreException(CoreErrorWrapper.make(error, errorExpr));
      }
    }
  }

  Expression checkCase(CaseExpression expr, Expression expectedType, Integer level) {
    ExprSubstitution substitution = new ExprSubstitution();
    checkList(expr.getArguments(), expr.getParameters(), substitution, LevelSubstitution.EMPTY);
    checkDependentLink(expr.getParameters(), Type.OMEGA, expr);
    expr.getResultType().accept(this, Type.OMEGA);

    Integer level2 = expr.getResultTypeLevel() == null ? null : checkLevelProof(expr.getResultTypeLevel(), expr.getResultType());

    freeDependentLink(expr.getParameters());
    checkElimBody(null, expr.getElimBody(), expr.getParameters(), expr.getResultType(), level == null ? level2 : level2 == null ? level : Integer.valueOf(Math.min(level, level2)), expr, expr.isSCase(), PatternTypechecking.Mode.CASE);
    return check(expectedType, expr.getResultType().subst(substitution), expr);
  }

  @Override
  public Expression visitCase(CaseExpression expr, Expression expectedType) {
    return checkCase(expr, expectedType, null);
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Expression expectedType) {
    expr.getTypeOf().accept(this, Type.OMEGA);
    return check(expectedType, expr.getExpression().accept(this, expr.getTypeOf()), expr);
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Expression expectedType) {
    return check(expectedType, ExpressionFactory.Fin(expr.suc()), expr);
  }

  @Override
  public Expression visitString(StringExpression expr, Expression expectedType) {
    return check(expectedType, ExpressionFactory.String(), expr);
  }

  @Override
  public Expression visitTypeConstructor(TypeConstructorExpression expr, Expression expectedType) {
    if (expr.getDefinition().getReallyActualBody() instanceof ElimBody) {
      List<? extends ElimClause<Pattern>> clauses = ((ElimBody) expr.getDefinition().getReallyActualBody()).getClauses();
      if (expr.getClauseIndex() >= clauses.size()) {
        throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Index " + expr.getClauseIndex() + " is too big. The number of clauses is " + clauses.size(), mySourceNode), expr));
      }
    }
    LevelSubstitution levelSubst = expr.getType().getLevelSubstitution();
    ExprSubstitution substitution = new ExprSubstitution();
    checkList(expr.getClauseArguments(), expr.getParameters(), substitution, levelSubst);
    expr.getArgument().accept(this, expr.getArgumentType());
    return check(expectedType, expr.getType(), expr);
  }

  @Override
  public Expression visitTypeDestructor(TypeDestructorExpression expr, Expression expectedType) {
    Expression argType = expr.getArgument().getType().normalize(NormalizationMode.WHNF);
    if (!(argType instanceof FunCallExpression && ((FunCallExpression) argType).getDefinition() == expr.getDefinition())) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.refDoc(expr.getDefinition().getRef()), argType, mySourceNode), expr));
    }
    return check(expectedType, expr.getType(), expr);
  }

  @Override
  public Expression visitArray(ArrayExpression expr, Expression expectedType) {
    checkLevels(expr.getLevels(), Prelude.DEP_ARRAY, expr);
    Expression length;
    Expression tailLength = null;
    if (expr.getTail() == null) {
      length = new SmallIntegerExpression(expr.getElements().size());
    } else {
      if (expr.getElements().isEmpty()) {
        throw new CoreException(CoreErrorWrapper.make(new TypecheckingError("Empty array with a tail", mySourceNode), expr));
      }
      tailLength = FieldCallExpression.make(Prelude.ARRAY_LENGTH, expr.getTail());
      int s = expr.getElements().size() - 1;
      for (int i = 0; i < s; i++) {
        tailLength = Suc(tailLength);
      }
      length = Suc(tailLength);
    }
    Sort sort = Sort.STD.subst(expr.getLevels());
    expr.getElementsType().accept(this, new PiExpression(sort.succ(), new TypedSingleDependentLink(true, null, DataCallExpression.make(Prelude.FIN, Levels.EMPTY, Collections.singletonList(length))), new UniverseExpression(sort)));
    List<Expression> elements = expr.getElements();
    for (int i = 0; i < elements.size(); i++) {
      elements.get(i).accept(this, AppExpression.make(expr.getElementsType(), new SmallIntegerExpression(i), true));
    }
    if (expr.getTail() != null) {
      TypedSingleDependentLink lamParam = new TypedSingleDependentLink(true, "j", DataCallExpression.make(Prelude.FIN, Levels.EMPTY, Collections.singletonList(tailLength)));
      expr.getTail().accept(this, new ClassCallExpression(Prelude.DEP_ARRAY, expr.getLevels(), Collections.singletonMap(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(sort.succ(), lamParam, AppExpression.make(expr.getElementsType(), Suc(new ReferenceExpression(lamParam)), true))), Sort.STD, UniverseKind.NO_UNIVERSES));
    }
    return check(expectedType, expr.getType(), expr);
  }

  @Override
  public Expression visitPath(PathExpression expr, Expression expectedType) {
    Sort sort = new Sort(expr.getLevels().get(LevelVariable.PVAR).add(1), Level.INFINITY);
    expr.getArgumentType().accept(this, new PiExpression(sort, UnusedIntervalDependentLink.INSTANCE, new UniverseExpression(expr.getLevels().toSort())));
    TypedSingleDependentLink param = new TypedSingleDependentLink(true, "i", ExpressionFactory.Interval());
    expr.getArgument().accept(this, new PiExpression(sort, param, AppExpression.make(expr.getArgumentType(), new ReferenceExpression(param), true)));
    return check(expectedType, expr.getType(), expr);
  }

  @Override
  public Expression visitAt(AtExpression expr, Expression expectedType) {
    Expression type = expr.getPathArgument().accept(this, null).normalize(NormalizationMode.WHNF);
    if (!(type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.PATH)) {
      throw new CoreException(CoreErrorWrapper.make(new TypeMismatchError(DocFactory.refDoc(Prelude.PATH.getRef()), type, mySourceNode), expr.getPathArgument()));
    }
    expr.getIntervalArgument().accept(this, Interval());
    return check(expectedType, AppExpression.make(((DataCallExpression) type).getDefCallArguments().get(0), expr.getIntervalArgument(), true), expr);
  }
}
