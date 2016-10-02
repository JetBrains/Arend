package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.StringPrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.ExpressionInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LambdaInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.LevelMax;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingDefCall;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.error.DummyLocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporterCounter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.ImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.StdImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.TwoStageEquations;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.ClassViewInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.EmptyInstancePool;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.size;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.expression;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final TypecheckerState myState;
  private ClassDefinition myThisClass;
  private Expression myThisExpr;
  private final List<Binding> myContext;
  private final LocalErrorReporter myErrorReporter;
  private final TypeCheckingDefCall myTypeCheckingDefCall;
  private final TypeCheckingElim myTypeCheckingElim;
  private final ImplicitArgsInference myArgsInference;
  private final Equations myEquations;
  private final ClassViewInstancePool myClassViewInstancePool;

  public static class Result {
    public Expression expression;
    public Type type;

    public Result(Expression expression, Type type) {
      this.expression = expression;
      this.type = type;
    }
  }

  private CheckTypeVisitor(TypecheckerState state, ClassDefinition thisClass, Expression thisExpr, List<Binding> localContext, LocalErrorReporter errorReporter, ClassViewInstancePool pool) {
    myState = state;
    myContext = localContext;
    myErrorReporter = errorReporter;
    myTypeCheckingDefCall = new TypeCheckingDefCall(this);
    myTypeCheckingElim = new TypeCheckingElim(this);
    myArgsInference = new StdImplicitArgsInference(this);
    setThisClass(thisClass, thisExpr);
    myEquations = new TwoStageEquations(this);
    myClassViewInstancePool = pool;
  }

  public void setThisClass(ClassDefinition thisClass, Expression thisExpr) {
    myThisClass = thisClass;
    myThisExpr = thisExpr;
    myTypeCheckingDefCall.setThisClass(thisClass, thisExpr);
  }

  public static class Builder {
    private final TypecheckerState myTypecheckerState;
    private final List<Binding> myLocalContext;
    private final LocalErrorReporter myErrorReporter;
    private ClassViewInstancePool myPool;
    private ClassDefinition myThisClass;
    private Expression myThisExpr;

    public Builder(TypecheckerState typecheckerState, List<Binding> localContext, LocalErrorReporter errorReporter) {
      this.myTypecheckerState = typecheckerState;
      myLocalContext = localContext;
      myErrorReporter = errorReporter;
      myPool = EmptyInstancePool.INSTANCE;
    }

    public Builder thisClass(ClassDefinition thisClass, Expression thisExpr) {
      myThisClass = thisClass;
      myThisExpr = thisExpr;
      return this;
    }

    public Builder instancePool(ClassViewInstancePool pool) {
      myPool = pool;
      return this;
    }

    public CheckTypeVisitor build() {
      return new CheckTypeVisitor(myTypecheckerState, myThisClass, myThisExpr, myLocalContext, myErrorReporter, myPool);
    }
  }

  public TypecheckerState getTypecheckingState() {
    return myState;
  }

  public TypeCheckingDefCall getTypeCheckingDefCall() {
    return myTypeCheckingDefCall;
  }

  public ClassViewInstancePool getClassViewInstancePool() {
    return myClassViewInstancePool;
  }

  public ImplicitArgsInference getImplicitArgsInference() {
    return myArgsInference;
  }

  public TypeCheckingElim getTypeCheckingElim() {
    return myTypeCheckingElim;
  }

  public List<Binding> getContext() {
    return myContext;
  }

  public LocalErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  public Equations getEquations() {
    return myEquations;
  }

  public Result checkResult(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    }

    if (compare(result, expectedType, expression)) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    } else {
      return null;
    }
  }

  public boolean compare(Result result, Expression expectedType, Abstract.Expression expr) {
    Expression expectedType1 = expectedType.normalize(NormalizeVisitor.Mode.NF);
    if (expectedType1.isAnyUniverse()) {
      if (result.type.toSorts() != null) {
        return true;
      }
    } else {
      if (result.type.isLessOrEquals(expectedType1, myEquations, expr)) {
        result.expression = new OfTypeExpression(result.expression, expectedType1);
        // TODO [sorts]: what is this???
        // if (expectedType1.toUniverse() != null && result.type.toSorts() == null) {
          result.type = expectedType1;
        // }
        return true;
      }
    }

    LocalTypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
    expr.setWellTyped(myContext, Error(result.expression, error));
    myErrorReporter.report(error);
    return false;
  }

  public Result checkResultImplicit(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    }
    return myArgsInference.inferTail(result, expectedType, expression);
  }

  public Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Incomplete expression", null);
      myErrorReporter.report(error);
      return null;
    }
    return expr.accept(this, expectedType);
  }

  public Result checkType(Abstract.Expression expr, Expression expectedType) {
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;
    LevelSubstitution substitution = myEquations.solve(expr);
    if (!substitution.getDomain().isEmpty()) {
      result.expression = result.expression.subst(substitution);
      result.type = result.type.subst(new ExprSubstitution(), substitution);
    }

    LocalErrorReporterCounter counter = new LocalErrorReporterCounter(myErrorReporter);
    result.expression = result.expression.strip(new HashSet<>(myContext), counter);
    result.type = result.type.strip(new HashSet<>(myContext), counter.getErrorsNumber() == 0 ? myErrorReporter : new DummyLocalErrorReporter());
    return result;
  }

  private boolean compareExpressions(Result result, Expression expected, Expression actual, Abstract.Expression expr) {
    if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, expected.normalize(NormalizeVisitor.Mode.NF), actual.normalize(NormalizeVisitor.Mode.NF), expr)) {
      LocalTypeCheckingError error = new SolveEquationError<>(expected.normalize(NormalizeVisitor.Mode.HUMAN_NF), actual.normalize(NormalizeVisitor.Mode.HUMAN_NF), null, expr);
      expr.setWellTyped(myContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return false;
    }
    return true;
  }

  private boolean checkPath(Result result, Abstract.Expression expr) {
    if (result != null) {
      ConCallExpression conExpr = result.expression.toConCall();
      if (conExpr != null && conExpr.getDefinition() == Prelude.PATH_CON) {
        if (conExpr.getDefCallArguments().isEmpty()) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Expected an argument for 'path'", expr);
          expr.setWellTyped(myContext, Error(result.expression, error));
          myErrorReporter.report(error);
          return false;
        }

        List<? extends Expression> args = conExpr.getDataTypeArguments();
        if (!compareExpressions(result, args.get(1), Apps(conExpr.getDefCallArguments().get(0), Left()), expr) ||
            !compareExpressions(result, args.get(2), Apps(conExpr.getDefCallArguments().get(0), Right()), expr)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    Result result = myArgsInference.infer(expr, expectedType);
    if (!checkPath(result, expr)) {
      return null;
    }
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitAppLevel(Abstract.ApplyLevelExpression app_expr, Expression expectedType) {
    List<Abstract.Expression> levelExprs = new ArrayList<>();
    Abstract.Expression expr = app_expr;

    while (expr instanceof Abstract.ApplyLevelExpression) {
      levelExprs.add(((Abstract.ApplyLevelExpression)expr).getLevel());
      expr = ((Abstract.ApplyLevelExpression)expr).getFunction();
    }

    Result result = myTypeCheckingDefCall.typeCheckDefCall((Abstract.DefCallExpression)expr);
    if (result == null) {
      return null;
    }

    DefCallExpression defCall = result.expression.getFunction().toDefCall();
    if (defCall == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Level can only be assigned to a definition", expr);
      expr.setWellTyped(myContext, Error(result.expression, error));
      myErrorReporter.report(error);
      return null;
    }

    Collections.reverse(levelExprs);

    List<Binding> polyParams = defCall.getDefinition().getPolyParams();

    for (int i = 0; i < levelExprs.size(); ++i) {
      Binding param = polyParams.get(i);
      Level level = typeCheckLevel(levelExprs.get(i), null, param.getType().toDefCall().getDefinition() == Prelude.CNAT ? -1 : 0);
      if (level == null) {
        return null;
      }
      Level value = defCall.getPolyParamsSubst().get(param);
      if (value == null) {
        assert false;
        return null;
      }
      //if (value.isBinding() && value.getUnitBinding() instanceof LevelInferenceBinding) {
      myEquations.add(value, level, Equations.CMP.EQ, expr);
      //}
    }

    return result;
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    Result result = myTypeCheckingDefCall.typeCheckDefCall(expr);
    if (!checkPath(result, expr)) {
      return null;
    }
    return checkResultImplicit(expectedType, result, expr);
  }

  @Override
  public Result visitModuleCall(Abstract.ModuleCallExpression expr, Expression params) {
    if (expr.getModule() == null) {
      LocalTypeCheckingError error = new UnresolvedReferenceError(expr, new ModulePath(expr.getPath()).toString());
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    Definition typeChecked = myState.getTypechecked(expr.getModule());
    if (typeChecked == null) {
      assert false;
      LocalTypeCheckingError error = new LocalTypeCheckingError("Internal error: module '" + new ModulePath(expr.getPath()) + "' is not available yet", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    return new Result(ClassCall((ClassDefinition) typeChecked), new PiUniverseType(EmptyDependentLink.getInstance(), ((ClassDefinition) typeChecked).getSorts()));
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    List<DependentLink> piParams = new ArrayList<>();
    Expression expectedCodomain = expectedType == null ? null : expectedType.getPiParameters(piParams, true, false);
    LinkList list = new LinkList();
    DependentLink actualPiLink = null;
    Result result = new Result(null, null);
    ExprSubstitution piLamSubst = new ExprSubstitution();
    int piParamsIndex = 0;
    int argIndex = 1;

    Result bodyResult;
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        List<String> names;
        Result argResult = null;
        Abstract.Expression argType = null;
        boolean isExplicit = argument.getExplicit();

        if (argument instanceof Abstract.NameArgument) {
          names = Collections.singletonList(((Abstract.NameArgument) argument).getName());
        } else if (argument instanceof Abstract.TypeArgument) {
          names = argument instanceof Abstract.TelescopeArgument ? ((Abstract.TelescopeArgument) argument).getNames() : Collections.<String>singletonList(null);
          argType = ((Abstract.TypeArgument) argument).getType();
          argResult = typeCheck(argType, Universe());
          if (argResult == null) return null;
        } else {
          throw new IllegalStateException();
        }

        DependentLink link = param(isExplicit, names, argResult == null ? null : argResult.expression);
        list.append(link);

        for (String name : names) {
          if (piParamsIndex < piParams.size()) {
            DependentLink piLink = piParams.get(piParamsIndex++);
            if (piLink.isExplicit() != isExplicit) {
              myErrorReporter.report(new LocalTypeCheckingError(ordinal(argIndex) + " argument of the lambda should be " + (piLink.isExplicit() ? "explicit" : "implicit"), expr));
              link.setExplicit(piLink.isExplicit());
            }

            Expression piLinkType = piLink.getType().subst(piLamSubst);
            if (argResult != null) {
              if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, piLinkType.normalize(NormalizeVisitor.Mode.NF), argResult.expression.normalize(NormalizeVisitor.Mode.NF), argType)) {
                LocalTypeCheckingError error = new TypeMismatchError(piLinkType.normalize(NormalizeVisitor.Mode.HUMAN_NF), argResult.expression.normalize(NormalizeVisitor.Mode.HUMAN_NF), argType);
                myErrorReporter.report(error);
                return null;
              }
            } else {
              link.setType(piLinkType);
            }

            piLamSubst.add(piLink, Reference(link));
          } else {
            if (argResult == null) {
              LevelInferenceVariable pLvl = new LevelInferenceVariable("plvl-of-" + name, Lvl(), expr);
              LevelInferenceVariable hLvl = new LevelInferenceVariable("hlvl-of-" + name, CNat(), expr);
              myEquations.addVariable(pLvl);
              myEquations.addVariable(hLvl);
              InferenceVariable inferenceVariable = new LambdaInferenceVariable("type-of-" + name, Universe(new Level(pLvl), new Level(hLvl)), argIndex, expr, false);
              link.setType(new InferenceReferenceExpression(inferenceVariable));
            }
            if (actualPiLink == null) {
              actualPiLink = link;
            }
          }

          argIndex++;
          myContext.add(link);
          link = link.getNext();
        }
      }

      Expression expectedBodyType = null;
      if (actualPiLink == null && expectedCodomain != null) {
        expectedBodyType = expectedCodomain.fromPiParameters(piParams.subList(piParamsIndex, piParams.size())).subst(piLamSubst);
      }

      Abstract.Expression body = expr.getBody();
      bodyResult = typeCheck(body, expectedBodyType);
      if (bodyResult == null) return null;
      if (actualPiLink != null && expectedCodomain != null) {
        result.expression = bodyResult.expression;
        result.type = bodyResult.type.addParameters(actualPiLink, true);
        if (!compare(result, expectedCodomain, body)) {
          return null;
        }
      }
    }

    result.expression = Lam(list.getFirst(), bodyResult.expression);
    result.type = bodyResult.type.addParameters(list.getFirst(), true);
    return result;
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    return checkResult(expectedType, visitArguments(expr.getArguments(), expr.getCodomain(), expr), expr);
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    int hlevel = expr.getUniverse().myHLevel == Abstract.UniverseExpression.Universe.NOT_TRUNCATED ? Sort.NOT_TRUNCATED : expr.getUniverse().myHLevel;
    UniverseExpression universe = Universe(expr.getUniverse().myPLevel, hlevel);
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getSort().succ())), expr);
  }

  private Level typeCheckLevel(Abstract.Expression expr, Expression expectedType, int minValue) {
    int num_sucs = 0;
    LocalTypeCheckingError error = null;

    if (expr instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression)expr).getName().equals("inf")) {
      return Level.INFINITY;
    }

    while (expr instanceof Abstract.AppExpression) {
      Abstract.AppExpression app = (Abstract.AppExpression) expr;
      Abstract.Expression suc = app.getFunction();
      if (!(suc instanceof Abstract.DefCallExpression) || !((Abstract.DefCallExpression) suc).getName().equals("suc")) {
        error = new LocalTypeCheckingError("Expression " + suc + " is invalid, 'suc' expected", expr);
        break;
      }
      expr = app.getArgument().getExpression();
      ++num_sucs;
    }

    if (error == null) {
      if (expr instanceof Abstract.NumericLiteral) {
        int val = ((Abstract.NumericLiteral) expr).getNumber();
        return new Level(val + num_sucs - minValue);
      }
      Result refResult = typeCheck(expr, expectedType);
      if (refResult != null) {
        ReferenceExpression ref = refResult.expression.toReference();
        if (ref != null) {
          return new Level(ref.getBinding(), num_sucs);
        }
      }
      error = new LocalTypeCheckingError("Invalid level expression", expr);
    }

    myErrorReporter.report(error);
    return null;
  }

  private LevelMax typeCheckLevelMax(Abstract.Expression expr, Expression expectedType, int minValue) {
    LevelMax result = new LevelMax();
    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    Abstract.Expression max = Abstract.getFunction(expr, args);

    if (max instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) max).getName().equals("max")) {
      for (Abstract.ArgumentExpression arg : args) {
        result = result.max(typeCheckLevel(arg.getExpression(), expectedType, minValue));
      }
      return result;
    }
    return new LevelMax(typeCheckLevel(expr, expectedType, minValue));
  }


  public SortMax visitDataUniverse(Abstract.PolyUniverseExpression expr) {
    LevelMax levelP = typeCheckLevelMax(expr.getPLevel(), Lvl(), 0);
    LevelMax levelH = typeCheckLevelMax(expr.getHLevel(), CNat(), -1);
    if (levelP == null || levelH == null) return null;
    return new SortMax(levelP, levelH);
  }

  @Override
  public Result visitPolyUniverse(Abstract.PolyUniverseExpression expr, Expression expectedType) {
    Level levelP = typeCheckLevel(expr.getPLevel(), Lvl(), 0);
    Level levelH = typeCheckLevel(expr.getHLevel(), CNat(), -1);
    if (levelP == null || levelH == null) return null;
    UniverseExpression universe = Universe(new Sort(levelP, levelH));
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getSort().succ())), expr);
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    LocalTypeCheckingError error = new GoalError(myContext, expectedType == null ? null : expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
    expr.setWellTyped(myContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    if (expectedType != null) {
      return new Result(new InferenceReferenceExpression(new ExpressionInferenceVariable(expectedType, expr)), expectedType);
    } else {
      LocalTypeCheckingError error = new ArgInferenceError(expression(), expr, new Expression[0]);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType != null) {
      expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      SigmaExpression expectedTypeSigma = expectedTypeNorm.toSigma();
      if (expectedTypeSigma != null) {
        DependentLink sigmaParams = expectedTypeSigma.getParameters();
        int sigmaParamsSize = size(sigmaParams);

        if (expr.getFields().size() != sigmaParamsSize) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), expr);
          expr.setWellTyped(myContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Result tupleResult = new Result(Tuple(fields, expectedTypeSigma), expectedType);
        ExprSubstitution substitution = new ExprSubstitution();
        for (Abstract.Expression field : expr.getFields()) {
          Expression expType = sigmaParams.getType().subst(substitution);
          Result result = typeCheck(field, expType);
          if (result == null) return null;
          fields.add(result.expression);
          substitution.add(sigmaParams, result.expression);

          sigmaParams = sigmaParams.getNext();
        }
        return tupleResult;
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    LinkList list = new LinkList();
    Result tupleResult = new Result(null, null);
    for (int i = 0; i < expr.getFields().size(); i++) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (result == null) return null;
      Expression type = result.type.toExpression();
      if (type == null) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer type of " + (i + 1) + "th field", expr.getFields().get(i));
        expr.setWellTyped(myContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }

      fields.add(result.expression);
      list.append(param(type));
    }

    SigmaExpression type = Sigma(list.getFirst());
    tupleResult.expression = Tuple(fields, type);
    tupleResult.type = type;
    tupleResult = checkResult(expectedTypeNorm, tupleResult, expr);
    return tupleResult;
  }

  public Result visitArguments(List<? extends Abstract.TypeArgument> arguments, Abstract.Expression codomain, Abstract.Expression expr) {
    Result argsResult = new Result(null, null);
    LinkList list = new LinkList();
    Result codomainResult = null;
    SortMax maxDomainUni = new SortMax();

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (Abstract.TypeArgument arg : arguments) {
        Result result = typeCheck(arg.getType(), Universe());
        if (result == null) return null;
        maxDomainUni.add(result.type.toSorts());

        if (arg instanceof Abstract.TelescopeArgument) {
          DependentLink link = param(arg.getExplicit(), ((Abstract.TelescopeArgument) arg).getNames(), result.expression);
          list.append(link);
          myContext.addAll(DependentLink.Helper.toContext(link));
        } else {
          DependentLink link = param(arg.getExplicit(), (String) null, result.expression);
          list.append(link);
          myContext.add(link);
        }
      }

      if (codomain != null) {
        codomainResult = typeCheck(codomain, Universe());
        if (codomainResult == null) return null;
      }
    }

    if (codomainResult != null) {
      SortMax codomainUniverse = codomainResult.type.toSorts();
      maxDomainUni.getPLevel().add(codomainUniverse.getPLevel());
      maxDomainUni = new SortMax(maxDomainUni.getPLevel(), codomainUniverse.getHLevel());
    }

    argsResult.expression = codomainResult == null ? Sigma(list.getFirst()) : Pi(list.getFirst(), codomainResult.expression);
    argsResult.type = new PiUniverseType(EmptyDependentLink.getInstance(), maxDomainUni);
    return argsResult;
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression expectedType) {
    return checkResult(expectedType, visitArguments(expr.getArguments(), null, expr), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression expectedType) {
    return checkResultImplicit(expectedType, myArgsInference.infer(expr, expectedType), expr);
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Expression expectedType) {
    assert expr.getSequence().isEmpty();
    return typeCheck(expr.getLeft(), expectedType);
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression expectedType) {
    LocalTypeCheckingError error = new LocalTypeCheckingError("\\elim is allowed only at the root of a definition", expr);
    myErrorReporter.report(error);
    expr.setWellTyped(myContext, Error(null, error));
    return null;
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Expression expectedType) {
    if (expectedType == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer type of the result", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Result caseResult = new Result(null, expectedType);
    LetClause letBinding = let(Abstract.CaseExpression.FUNCTION_NAME, EmptyDependentLink.getInstance(), expectedType, (ElimTreeNode) null);
    List<? extends Abstract.Expression> expressions = expr.getExpressions();

    LinkList list = new LinkList();
    List<Expression> letArguments = new ArrayList<>(expressions.size());
    for (int i = 0; i < expressions.size(); i++) {
      Result exprResult = typeCheck(expressions.get(i), null);
      if (exprResult == null) return null;
      Expression type = exprResult.type.toExpression();
      if (type == null) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer type of " + (i + 1) + "th expression", expressions.get(i));
        expr.setWellTyped(myContext, Error(null, error));
        myErrorReporter.report(error);
        return null;
      }

      list.append(param(true, vars(Abstract.CaseExpression.ARGUMENT_NAME + i), type));
      letArguments.add(exprResult.expression);
    }
    letBinding.setParameters(list.getFirst());

    ElimTreeNode elimTree = myTypeCheckingElim.typeCheckElim(expr, list.getFirst(), expectedType, true);
    if (elimTree == null) return null;
    letBinding.setElimTree(elimTree);

    caseResult.expression = Let(lets(letBinding), Apps(Reference(letBinding), letArguments));
    expr.setWellTyped(myContext, caseResult.expression);
    return caseResult;
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression expectedType) {
    Abstract.Expression expr1 = expr.getExpression();
    Result exprResult = typeCheck(expr1, null);
    if (exprResult == null) return null;
    SigmaExpression sigmaType = null;
    exprResult.type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (exprResult.type instanceof Expression) {
      sigmaType = ((Expression) exprResult.type).toSigma();
    }
    if (sigmaType == null) {
      LocalTypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A sigma type"), exprResult.type, expr1);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    DependentLink sigmaParams = sigmaType.getParameters();
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Index " + (expr.getField() + 1) + " out of range", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    for (int i = 0; sigmaParams != fieldLink; sigmaParams = sigmaParams.getNext(), i++) {
      substitution.add(sigmaParams, Proj(exprResult.expression, i));
    }

    exprResult.expression = Proj(exprResult.expression, expr.getField());
    exprResult.type = fieldLink.getType().subst(substitution);
    return checkResult(expectedType, exprResult, expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression expectedType) {
    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result typeCheckedBaseClass = typeCheck(baseClassExpr, null);
    if (typeCheckedBaseClass == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = typeCheckedBaseClass.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normalizedBaseClassExpr.toClassCall();
    if (classCallExpr == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a class", baseClassExpr);
      expr.setWellTyped(myContext, Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassDefinition baseClass = classCallExpr.getDefinition();
    if (baseClass.hasErrors()) {
      LocalTypeCheckingError error = new HasErrors(baseClass.getAbstractDefinition(), expr);
      expr.setWellTyped(myContext, Error(classCallExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    FieldSet fieldSet = new FieldSet();
    Result classExtResult = new Result(null, null);
    ClassCallExpression resultExpr = classCallExpr instanceof ClassViewCallExpression ? new ClassViewCallExpression(baseClass, fieldSet, ((ClassViewCallExpression) classCallExpr).getClassView()) : ClassCall(baseClass, fieldSet);

    fieldSet.addFieldsFrom(classCallExpr.getFieldSet(), resultExpr);
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : classCallExpr.getFieldSet().getImplemented()) {
      boolean ok = fieldSet.implementField(entry.getKey(), entry.getValue(), resultExpr);
      assert ok;
    }

    // Some tricks to keep going as long as possible in case of error
    boolean ok = true;
    Collection<? extends Abstract.ImplementStatement> statements = expr.getStatements();
    for (Abstract.ImplementStatement statement : statements) {
      Definition implementedDef = myState.getTypechecked(statement.getImplementedField());
      if (!(implementedDef instanceof ClassField)) {
        myErrorReporter.report(new LocalTypeCheckingError("'" + implementedDef.getName() + "' is not a field", statement));
        continue;
      }
      ClassField field = (ClassField) implementedDef;
      if (fieldSet.isImplemented(field)) {
        myErrorReporter.report(new LocalTypeCheckingError("Field '" + field.getName() + "' is already implemented", statement));
        continue;
      }

      Result result = fieldSet.implementField(field, statement.getExpression(), this, resultExpr);
      if (result == null || result.expression.toError() != null) {
        ok = false;
      }
    }
    if (!ok) return null;

    classExtResult.expression = resultExpr;
    classExtResult.type = new PiUniverseType(EmptyDependentLink.getInstance(), resultExpr.getSorts());
    return checkResult(expectedType, classExtResult, expr);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.toClassCall();
    if (classCallExpr == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a class", expr.getExpression());
      expr.setWellTyped(myContext, Error(normExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    int remaining = classCallExpr.getFieldSet().getFields().size() - classCallExpr.getFieldSet().getImplemented().size();

    if (remaining == 0) {
      exprResult.expression = New(classCallExpr);
      exprResult.type = normExpr;
      return checkResult(expectedType, exprResult, expr);
    } else {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Class '" + classCallExpr.getDefinition().getName() + "' has " + remaining + " not implemented fields", expr);
      expr.setWellTyped(myContext, Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private LetClause typeCheckLetClause(Abstract.LetClause clause) {
    LinkList links = new LinkList();
    Expression resultType;
    ElimTreeNode elimTree;
    LetClause letResult;

    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument arg : clause.getArguments()) {
        if (arg instanceof Abstract.TelescopeArgument) {
          Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) arg;
          Result result = typeCheck(teleArg.getType(), Universe());
          if (result == null) return null;
          links.append(param(teleArg.getExplicit(), teleArg.getNames(), result.expression));
          for (DependentLink link = links.getLast(); link != EmptyDependentLink.getInstance(); link = link.getNext()) {
            myContext.add(link);
          }
        } else {
          throw new IllegalStateException();
        }
      }

      Expression expectedType = null;
      if (clause.getResultType() != null) {
        Result result = typeCheck(clause.getResultType(), null);
        if (result == null) return null;
        expectedType = result.expression;
      }

      if (clause.getTerm() instanceof Abstract.ElimExpression)  {
        myContext.subList(myContext.size() - size(links.getFirst()), myContext.size()).clear();
        elimTree = myTypeCheckingElim.typeCheckElim((Abstract.ElimExpression) clause.getTerm(), clause.getArrow() == Abstract.Definition.Arrow.LEFT ? links.getFirst() : null, expectedType, false);
        if (elimTree == null) return null;
        resultType = expectedType;
      } else {
        Result termResult = typeCheck(clause.getTerm(), expectedType);
        if (termResult == null) return null;
        Expression type = expectedType != null ? expectedType : termResult.type.toExpression();
        if (type == null) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer type of expression", clause.getTerm());
          clause.getTerm().setWellTyped(myContext, Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        elimTree = top(links.getFirst(), leaf(clause.getArrow(), termResult.expression));
        resultType = type;
      }

      LocalTypeCheckingError error = TypeCheckingElim.checkCoverage(clause, links.getFirst(), elimTree, expectedType);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
      error = TypeCheckingElim.checkConditions(clause, links.getFirst(), elimTree);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
    }

    letResult = new LetClause(clause.getName(), links.getFirst(), resultType, elimTree);
    myContext.add(letResult);
    return letResult;
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Expression expectedType) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      List<LetClause> clauses = new ArrayList<>();
      Result letResult = new Result(null, null);
      for (int i = 0; i < expr.getClauses().size(); i++) {
        LetClause clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (clauseResult == null) return null;
        clauses.add(clauseResult);
      }
      Result result = typeCheck(expr.getExpression(), expectedType);
      if (result == null) return null;

      LetExpression letExpr = Let(clauses, result.expression);
      letResult.expression = letExpr;
      letResult.type = letExpr.getType(result.type);
      return letResult;
    }
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, Expression expectedType) {
    int number = expr.getNumber();
    Expression expression = Zero();
    for (int i = 0; i < number; ++i) {
      expression = Suc(expression);
    }
    return checkResult(expectedType, new Result(expression, Nat()), expr);
  }
}
