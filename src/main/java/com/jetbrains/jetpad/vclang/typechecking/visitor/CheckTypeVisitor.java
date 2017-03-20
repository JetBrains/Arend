package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.ExpressionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.LambdaInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.*;
import com.jetbrains.jetpad.vclang.core.definition.Callable;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeOmega;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractLevelExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.prettyprint.StringPrettyPrintable;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingDefCall;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.error.DummyLocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporterCounter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.ImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.StdImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equation;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.TwoStageEquations;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.ClassViewInstancePool;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.expression;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Type, CheckTypeVisitor.Result>, AbstractLevelExpressionVisitor<LevelVariable, Level> {
  private final TypecheckerState myState;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final List<Binding> myContext;
  private final LocalErrorReporter myErrorReporter;
  private final TypeCheckingDefCall myTypeCheckingDefCall;
  private final TypeCheckingElim myTypeCheckingElim;
  private final ImplicitArgsInference myArgsInference;
  private final Equations myEquations;
  private ClassViewInstancePool myClassViewInstancePool;

  public interface TResult {
    Result toResult(Equations equations);
    DependentLink getParameter();
    TResult applyExpressions(List<? extends Expression> expressions);
    List<? extends DependentLink> getImplicitParameters();
  }

  public static class DefCallResult implements TResult {
    private final Abstract.DefCallExpression myDefCall;
    private final Callable myDefinition;
    private final Sort mySortArgument;
    private final List<Expression> myArguments;
    private List<DependentLink> myParameters;
    private Expression myResultType;
    private Expression myThisExpr;

    private DefCallResult(Abstract.DefCallExpression defCall, Callable definition, Sort sortArgument, List<Expression> arguments, List<DependentLink> parameters, Expression resultType, Expression thisExpr) {
      myDefCall = defCall;
      myDefinition = definition;
      mySortArgument = sortArgument;
      myArguments = arguments;
      myParameters = parameters;
      myResultType = resultType;
      myThisExpr = thisExpr;
    }

    public static TResult makeTResult(Abstract.DefCallExpression defCall, Callable definition, Sort sortArgument, Expression thisExpr) {
      List<DependentLink> parameters = new ArrayList<>();
      Expression resultType = definition.getTypeWithParams(parameters, sortArgument);
      if (thisExpr != null) {
        ExprSubstitution subst = DependentLink.Helper.toSubstitution(parameters.get(0), Collections.singletonList(thisExpr));
        parameters = DependentLink.Helper.subst(parameters.subList(1, parameters.size()), subst, LevelSubstitution.EMPTY);
        resultType = resultType.subst(subst, LevelSubstitution.EMPTY);
      }

      if (parameters.isEmpty()) {
        return new Result(definition.getDefCall(sortArgument, thisExpr, Collections.emptyList()), resultType);
      } else {
        return new DefCallResult(defCall, definition, sortArgument, new ArrayList<>(), parameters, resultType, thisExpr);
      }
    }

    @Override
    public Result toResult(Equations equations) {
      if (myParameters.isEmpty()) {
        return new Result(myDefinition.getDefCall(mySortArgument, myThisExpr, myArguments), myResultType);
      }

      List<SingleDependentLink> parameters = new ArrayList<>();
      ExprSubstitution substitution = new ExprSubstitution();
      List<String> names = new ArrayList<>();
      DependentLink link0 = null;
      for (DependentLink link : myParameters) {
        if (link0 == null) {
          link0 = link;
        }

        names.add(link.getName());
        if (link instanceof TypedDependentLink) {
          SingleDependentLink parameter = singleParam(link.isExplicit(), names, link.getType().subst(substitution, LevelSubstitution.EMPTY));
          parameters.add(parameter);
          names.clear();

          for (; parameter.hasNext(); parameter = parameter.getNext(), link0 = link0.getNext()) {
            substitution.add(link0, Reference(parameter));
            myArguments.add(Reference(parameter));
          }

          link0 = null;
        }
      }

      Expression expression = myDefinition.getDefCall(mySortArgument, myThisExpr, myArguments);
      Expression type = myResultType.subst(substitution, LevelSubstitution.EMPTY);
      Level codPLevel = type.getType().toSort().getPLevel();
      for (int i = parameters.size() - 1; i >= 0; i--) {
        codPLevel = PiExpression.generateUpperBound(parameters.get(i).getType().getType().toSort().getPLevel(), codPLevel, equations, myDefCall);
        expression = new LamExpression(codPLevel, parameters.get(i), expression);
        type = new PiExpression(codPLevel, parameters.get(i), type);
      }
      return new Result(expression, type);
    }

    @Override
    public DependentLink getParameter() {
      return myParameters.get(0);
    }

    @Override
    public TResult applyExpressions(List<? extends Expression> expressions) {
      int size = myParameters.size();
      List<? extends Expression> args = expressions.size() <= size ? expressions : expressions.subList(0, size);
      myArguments.addAll(args);
      ExprSubstitution subst = new ExprSubstitution();
      for (int i = 0; i < args.size(); i++) {
        subst.add(myParameters.get(i), args.get(i));
      }
      myParameters = DependentLink.Helper.subst(myParameters.subList(args.size(), size), subst, LevelSubstitution.EMPTY);
      myResultType = myResultType.subst(subst, LevelSubstitution.EMPTY);

      if (expressions.size() < size) {
        return this;
      }

      Result result = new Result(myDefinition.getDefCall(mySortArgument, myThisExpr, myArguments), myResultType);
      if (size < expressions.size()) {
       result = result.applyExpressions(expressions.subList(size, expressions.size()));
      }
      return result;
    }

    @Override
    public List<DependentLink> getImplicitParameters() {
      List<DependentLink> params = new ArrayList<>(myParameters.size());
      for (DependentLink param : myParameters) {
        if (param.isExplicit()) {
          break;
        }
        params.add(param);
      }
      return params;
    }

    public Abstract.DefCallExpression getDefCall() {
      return myDefCall;
    }

    public Callable getDefinition() {
      return myDefinition;
    }

    public List<? extends Expression> getArguments() {
      return myArguments;
    }

    public Sort getSortArgument() {
      return mySortArgument;
    }
  }

  public static class Result implements TResult {
    public Expression expression;
    public Expression type;

    public Result(Expression expression, Expression type) {
      this.expression = expression;
      this.type = type;
    }

    @Override
    public Result toResult(Equations equations) {
      return this;
    }

    @Override
    public DependentLink getParameter() {
      type = type.normalize(NormalizeVisitor.Mode.WHNF);
      return type.toPi() == null ? EmptyDependentLink.getInstance() : type.toPi().getParameters();
    }

    @Override
    public Result applyExpressions(List<? extends Expression> expressions) {
      expression = expression.addArguments(expressions);
      type = type.applyExpressions(expressions);
      return this;
    }

    @Override
    public List<SingleDependentLink> getImplicitParameters() {
      List<SingleDependentLink> params = new ArrayList<>();
      type.getPiParameters(params, true, true);
      return params;
    }
  }

  public CheckTypeVisitor(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, List<Binding> localContext, LocalErrorReporter errorReporter, ClassViewInstancePool pool) {
    myState = state;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myContext = localContext;
    myErrorReporter = errorReporter;
    myTypeCheckingDefCall = new TypeCheckingDefCall(this);
    myTypeCheckingElim = new TypeCheckingElim(this);
    myArgsInference = new StdImplicitArgsInference(this);
    myEquations = new TwoStageEquations(this);
    myClassViewInstancePool = pool;
  }

  public void setThisClass(ClassDefinition thisClass, Expression thisExpr) {
    myTypeCheckingDefCall.setThisClass(thisClass, thisExpr);
  }

  public TypecheckerState getTypecheckingState() {
    return myState;
  }

  public StaticNamespaceProvider getStaticNamespaceProvider() {
    return myStaticNsProvider;
  }

  public DynamicNamespaceProvider getDynamicNamespaceProvider() {
    return myDynamicNsProvider;
  }

  public TypeCheckingDefCall getTypeCheckingDefCall() {
    return myTypeCheckingDefCall;
  }

  public ClassViewInstancePool getClassViewInstancePool() {
    return myClassViewInstancePool;
  }

  public void setClassViewInstancePool(ClassViewInstancePool pool) {
    myClassViewInstancePool = pool;
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

  public Result checkResult(Type expectedType, Result result, Abstract.Expression expression) {
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

  public boolean compare(Result result, Type expectedType, Abstract.Expression expr) {
    if (result.type.isLessOrEquals(expectedType, myEquations, expr)) {
      if (expectedType instanceof Expression) {
        result.expression = new OfTypeExpression(result.expression, (Expression) expectedType);
        result.type = (Expression) expectedType;
      }
      return true;
    }

    LocalTypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
    expr.setWellTyped(myContext, ExpressionFactory.Error(result.expression, error));
    myErrorReporter.report(error);
    return false;
  }

  private Result tResultToResult(Type expectedType, TResult result, Abstract.Expression expr) {
    if (result != null && expectedType != null) {
      result = myArgsInference.inferTail(result, expectedType, expr);
    }
    return result == null ? null : checkResult(expectedType, result.toResult(myEquations), expr);
  }

  public Result typeCheck(Abstract.Expression expr, Type expectedType) {
    if (expr == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Incomplete expression", null);
      myErrorReporter.report(error);
      return null;
    }
    return expr.accept(this, expectedType);
  }

  public Result checkType(Abstract.Expression expr, Type expectedType) {
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;
    LevelSubstitution substitution = myEquations.solve(expr);
    if (!substitution.isEmpty()) {
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
      LocalTypeCheckingError error = new ExpressionMismatchError(expected.normalize(NormalizeVisitor.Mode.HUMAN_NF), actual.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(result.expression, error));
      myErrorReporter.report(error);
      return false;
    }
    return true;
  }

  private boolean checkPath(TResult result, Abstract.Expression expr) {
    if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.PATH_CON) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected an argument for 'path'", expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(result.toResult(myEquations).expression, error));
      myErrorReporter.report(error);
      return false;
    }
    if (result instanceof Result) {
      ConCallExpression conCall = ((Result) result).expression.toConCall();
      if (conCall != null && conCall.getDefinition() == Prelude.PATH_CON) {
        if (!compareExpressions((Result) result, conCall.getDataTypeArguments().get(1), conCall.getDefCallArguments().get(0).addArgument(ExpressionFactory.Left()), expr) ||
          !compareExpressions((Result) result, conCall.getDataTypeArguments().get(2), conCall.getDefCallArguments().get(0).addArgument(ExpressionFactory.Right()), expr)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Type expectedType) {
    TResult result = myArgsInference.infer(expr, expectedType);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  public CheckTypeVisitor.TResult getLocalVar(Abstract.DefCallExpression expr) {
    String name = expr.getName();
    for (int i = myContext.size() - 1; i >= 0; i--) {
      Binding def = myContext.get(i);
      if (name.equals(def.getName())) {
        if (def instanceof LetClause) {
          return DefCallResult.makeTResult(expr, (LetClause) def, Sort.ZERO, null);
        } else {
          return new Result(Reference(def), def.getType());
        }
      }
    }

    LocalTypeCheckingError error = new NotInScopeError(expr, name);
    expr.setWellTyped(myContext, Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Type expectedType) {
    TResult result = expr.getExpression() == null && expr.getReferent() == null ? getLocalVar(expr) : myTypeCheckingDefCall.typeCheckDefCall(expr);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  @Override
  public Result visitModuleCall(Abstract.ModuleCallExpression expr, Type expectedType) {
    if (expr.getModule() == null) {
      LocalTypeCheckingError error = new UnresolvedReferenceError(expr, expr.getPath().toString());
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    Definition typeChecked = myState.getTypechecked(expr.getModule());
    if (typeChecked == null) {
      assert false;
      LocalTypeCheckingError error = new LocalTypeCheckingError("Internal error: module '" + expr.getPath() + "' is not available yet", expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    return new Result(ExpressionFactory.ClassCall((ClassDefinition) typeChecked, Sort.ZERO), new UniverseExpression(((ClassDefinition) typeChecked).getSort().subst(Sort.ZERO.toLevelSubstitution())));
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Type expectedType) {
    List<SingleDependentLink> piParams = new ArrayList<>();
    Type expectedCodomain = expectedType == null ? null : expectedType.getPiParameters(piParams, true, false);
    List<Pair<SingleDependentLink, Level>> links = new ArrayList<>(expr.getArguments().size());
    SingleDependentLink actualPiLink = null;
    ExprSubstitution piLamSubst = new ExprSubstitution();
    int piParamsIndex = 0;
    int argIndex = 1;

    Result bodyResult;
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        List<String> names;
        Expression argType = null;
        Level pLevel = null;
        Abstract.Expression argAbsType = null;
        boolean isExplicit = argument.getExplicit();

        if (argument instanceof Abstract.NameArgument) {
          names = Collections.singletonList(((Abstract.NameArgument) argument).getName());
        } else if (argument instanceof Abstract.TypeArgument) {
          names = argument instanceof Abstract.TelescopeArgument ? ((Abstract.TelescopeArgument) argument).getNames() : Collections.<String>singletonList(null);
          argAbsType = ((Abstract.TypeArgument) argument).getType();
          Result argResult = typeCheck(argAbsType, TypeOmega.INSTANCE);
          if (argResult == null) return null;
          argType = argResult.expression;
          pLevel = argResult.type.toSort().getPLevel();
        } else {
          throw new IllegalStateException();
        }

        SingleDependentLink origLink = singleParam(isExplicit, names, argType);
        SingleDependentLink link = origLink;

        for (String name : names) {
          if (piParamsIndex < piParams.size()) {
            DependentLink piLink = piParams.get(piParamsIndex++);
            if (piLink.isExplicit() != isExplicit) {
              myErrorReporter.report(new LocalTypeCheckingError(ordinal(argIndex) + " argument of the lambda should be " + (piLink.isExplicit() ? "explicit" : "implicit"), expr));
              link.setExplicit(piLink.isExplicit());
            }

            Expression piLinkType = piLink.getType().subst(piLamSubst);
            if (argType != null) {
              if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, piLinkType.normalize(NormalizeVisitor.Mode.NF), argType.normalize(NormalizeVisitor.Mode.NF), argAbsType)) {
                LocalTypeCheckingError error = new TypeMismatchError(piLinkType.normalize(NormalizeVisitor.Mode.HUMAN_NF), argType.normalize(NormalizeVisitor.Mode.HUMAN_NF), argAbsType);
                myErrorReporter.report(error);
                return null;
              }
            } else {
              argType = piLinkType;
              pLevel = argType.getType().toSort().getPLevel();
              link.setType(argType);
            }

            piLamSubst.add(piLink, ExpressionFactory.Reference(link));
          } else {
            if (argType == null) {
              InferenceLevelVariable pLvl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, expr);
              InferenceLevelVariable hLvl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, expr);
              myEquations.addVariable(pLvl);
              myEquations.addVariable(hLvl);
              pLevel = new Level(pLvl);
              InferenceVariable inferenceVariable = new LambdaInferenceVariable("type-of-" + name, ExpressionFactory.Universe(pLevel, new Level(hLvl)), argIndex, expr, false);
              argType = new InferenceReferenceExpression(inferenceVariable, myEquations);
              link.setType(argType);
            }
            if (actualPiLink == null) {
              actualPiLink = link;
            }
          }

          argIndex++;
          myContext.add(link);
          link = link.getNext();
        }

        links.add(new Pair<>(origLink, pLevel));
      }

      Type expectedBodyType = null;
      if (actualPiLink == null && expectedCodomain != null) {
        expectedBodyType = expectedCodomain instanceof Expression ? ((Expression) expectedCodomain).fromPiParametersSingle(piParams.subList(piParamsIndex, piParams.size())).subst(piLamSubst, LevelSubstitution.EMPTY) : expectedCodomain;
      }

      Abstract.Expression body = expr.getBody();
      bodyResult = typeCheck(body, expectedBodyType);
      if (bodyResult == null) return null;
      if (actualPiLink != null && expectedCodomain != null && !compare(new Result(bodyResult.expression, new PiExpression(new Level(0), actualPiLink, bodyResult.type)), expectedCodomain, body)) {
        return null;
      }
    }

    Expression exprResult = bodyResult.expression;
    Expression typeResult = bodyResult.type;
    Level codPLevel = typeResult.getType().toSort().getPLevel();
    for (int i = links.size() - 1; i >= 0; i--) {
      codPLevel = PiExpression.generateUpperBound(links.get(i).proj2, codPLevel, myEquations, expr);
      exprResult = new LamExpression(codPLevel, links.get(i).proj1, exprResult);
      typeResult = new PiExpression(codPLevel, links.get(i).proj1, typeResult);
    }
    return new Result(exprResult, typeResult);
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Type expectedType) {
    List<SingleDependentLink> list = new ArrayList<>();
    List<Sort> sorts = new ArrayList<>(expr.getArguments().size());

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (Abstract.TypeArgument arg : expr.getArguments()) {
        Result result = typeCheck(arg.getType(), TypeOmega.INSTANCE);
        if (result == null) return null;

        if (arg instanceof Abstract.TelescopeArgument) {
          SingleDependentLink link = singleParam(arg.getExplicit(), ((Abstract.TelescopeArgument) arg).getNames(), result.expression);
          list.add(link);
          myContext.addAll(DependentLink.Helper.toContext(link));
        } else {
          SingleDependentLink link = singleParam(arg.getExplicit(), Collections.singletonList(null), result.expression);
          list.add(link);
          myContext.add(link);
        }

        result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
        sorts.add(result.type.toSort());
      }

      Result result = typeCheck(expr.getCodomain(), TypeOmega.INSTANCE);
      if (result == null) return null;
      Sort codSort = result.type.toSort();

      Level codPLevel = codSort.getPLevel();
      Expression piExpr = result.expression;
      for (int i = list.size() - 1; i >= 0; i--) {
        codPLevel = PiExpression.generateUpperBound(sorts.get(i).getPLevel(), codPLevel, myEquations, expr);
        piExpr = new PiExpression(codPLevel, list.get(i), piExpr);
      }

      return checkResult(expectedType, new Result(piExpr, new UniverseExpression(list.isEmpty() ? codSort : new Sort(((PiExpression) piExpr).getPLevel(), codSort.getHLevel()))), expr);
    }
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Type expectedType) {
    Level pLevel = expr.getPLevel() != null ? expr.getPLevel().accept(this, LevelVariable.PVAR) : new Level(LevelVariable.PVAR);
    Level hLevel = expr.getHLevel() != null ? expr.getHLevel().accept(this, LevelVariable.HVAR) : new Level(LevelVariable.HVAR);

    if (pLevel == null || hLevel == null) {
      myErrorReporter.report(new LocalTypeCheckingError("\\max can only be used in a result type", expr));
      return null;
    }
    if (pLevel.isInfinity()) {
      myErrorReporter.report(new LocalTypeCheckingError("\\Type can only used as Pi codomain in definition parameters or result type", expr));
      return null;
    }

    UniverseExpression universe = ExpressionFactory.Universe(new Sort(pLevel, hLevel));
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getSort().succ())), expr);
  }

  @Override
  public Level visitInf(Abstract.InfLevelExpression expr, LevelVariable param) {
    return Level.INFINITY;
  }

  @Override
  public Level visitLP(Abstract.PLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.PVAR) {
      myErrorReporter.report(new LocalTypeCheckingError("Expected \\lp", expr));
    }
    return new Level(base, 0);
  }

  @Override
  public Level visitLH(Abstract.HLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.HVAR) {
      myErrorReporter.report(new LocalTypeCheckingError("Expected \\lh", expr));
    }
    return new Level(base, 0);
  }

  @Override
  public Level visitNumber(Abstract.NumberLevelExpression expr, LevelVariable base) {
    return new Level(expr.getNumber() - (base == LevelVariable.PVAR ? 0 : -1));
  }

  @Override
  public Level visitSuc(Abstract.SucLevelExpression expr, LevelVariable base) {
    return expr.getExpression().accept(this, base).add(1);
  }

  @Override
  public Level visitMax(Abstract.MaxLevelExpression expr, LevelVariable base) {
    return expr.getLeft().accept(this, base).max(expr.getRight().accept(this, base));
  }

  @Override
  public Level visitVar(Abstract.InferVarLevelExpression expr, LevelVariable base) {
    if (base.getType() != expr.getVariable().getType()) {
      myErrorReporter.report(new LocalTypeCheckingError("Expected " + base, expr));
    }
    return new Level(expr.getVariable());
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Type expectedType) {
    LocalTypeCheckingError error = new GoalError(myContext, expectedType == null ? null : expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
    Expression result = ExpressionFactory.Error(null, error);
    expr.setWellTyped(myContext, result);
    myErrorReporter.report(error);
    return new Result(result, result);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Type expectedType) {
    if (expectedType instanceof Expression) {
      return new Result(new InferenceReferenceExpression(new ExpressionInferenceVariable((Expression) expectedType, expr), myEquations), (Expression) expectedType);
    } else {
      LocalTypeCheckingError error = new ArgInferenceError(expression(), expr, new Expression[0]);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Type expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType instanceof Expression) {
      expectedTypeNorm = ((Expression) expectedType).normalize(NormalizeVisitor.Mode.WHNF);
      SigmaExpression expectedTypeSigma = expectedTypeNorm.toSigma();
      if (expectedTypeSigma != null) {
        DependentLink sigmaParams = expectedTypeSigma.getParameters();
        int sigmaParamsSize = DependentLink.Helper.size(sigmaParams);

        if (expr.getFields().size() != sigmaParamsSize) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), expr);
          expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Result tupleResult = new Result(ExpressionFactory.Tuple(fields, expectedTypeSigma), (Expression) expectedType);
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
    Result tupleResult;
    for (int i = 0; i < expr.getFields().size(); i++) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (result == null) return null;
      fields.add(result.expression);
      list.append(ExpressionFactory.param(result.type));
    }

    Sort sortArgument = Sort.generateInferVars(myEquations, expr);
    SigmaExpression type = new SigmaExpression(sortArgument, list.getFirst());
    tupleResult = checkResult(expectedTypeNorm, new Result(ExpressionFactory.Tuple(fields, type), type), expr);
    return tupleResult;
  }

  private DependentLink visitArguments(List<? extends Abstract.TypeArgument> arguments, List<Sort> resultSorts) {
    LinkList list = new LinkList();

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (Abstract.TypeArgument arg : arguments) {
        Result result = typeCheck(arg.getType(), TypeOmega.INSTANCE);
        if (result == null) return null;

        if (arg instanceof Abstract.TelescopeArgument) {
          DependentLink link = param(arg.getExplicit(), ((Abstract.TelescopeArgument) arg).getNames(), result.expression);
          list.append(link);
          myContext.addAll(DependentLink.Helper.toContext(link));
        } else {
          DependentLink link = param(arg.getExplicit(), (String) null, result.expression);
          list.append(link);
          myContext.add(link);
        }

        result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
        resultSorts.add(result.type.toSort());
      }
    }

    return list.getFirst();
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Type expectedType) {
    List<Sort> sorts = new ArrayList<>(expr.getArguments().size());
    DependentLink args = visitArguments(expr.getArguments(), sorts);
    if (args == null || !args.hasNext()) return null;
    Sort sort = SigmaExpression.getUpperBound(sorts, myEquations, expr);
    return checkResult(expectedType, new Result(new SigmaExpression(sort, args), new UniverseExpression(sort)), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Type expectedType) {
    return tResultToResult(expectedType, myArgsInference.infer(expr, expectedType), expr);
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Type expectedType) {
    assert expr.getSequence().isEmpty();
    return typeCheck(expr.getLeft(), expectedType);
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Type expectedType) {
    LocalTypeCheckingError error = new LocalTypeCheckingError("\\elim is allowed only at the root of a definition", expr);
    myErrorReporter.report(error);
    expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
    return null;
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Type expectedType) {
    if (expectedType == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer type of the result", expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    if (!(expectedType instanceof Expression)) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Type of \\case cannot be \\Type", expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    Result caseResult = new Result(null, (Expression) expectedType);
    List<? extends Abstract.Expression> expressions = expr.getExpressions();

    List<SingleDependentLink> links = new ArrayList<>();
    List<Expression> letArguments = new ArrayList<>(expressions.size());
    List<Level> domPLevels = new ArrayList<>(expressions.size());
    for (int i = 0; i < expressions.size(); i++) {
      Result exprResult = typeCheck(expressions.get(i), null);
      if (exprResult == null) return null;
      links.add(singleParam(true, ExpressionFactory.vars(Abstract.CaseExpression.ARGUMENT_NAME + i), exprResult.type));
      letArguments.add(exprResult.expression);
      domPLevels.add(exprResult.type.getType().toSort().getPLevel());
    }

    if (links.size() > 1) { // TODO: Fix this
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected exactly one argument", expr);
      myErrorReporter.report(error);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      return null;
    }

    ElimTreeNode elimTree = myTypeCheckingElim.typeCheckElim(expr, links.isEmpty() ? EmptyDependentLink.getInstance() : links.get(0), (Expression) expectedType, true, false);
    if (elimTree == null) return null;

    LocalTypeCheckingError error = TypeCheckingElim.checkCoverage("\\case", expr, links, elimTree, (Expression) expectedType);
    if (error != null) {
      myErrorReporter.report(error);
      return null;
    }
    error = TypeCheckingElim.checkConditions("\\case", expr, links, elimTree);
    if (error != null) {
      myErrorReporter.report(error);
      return null;
    }

    Level codPLevel = ((Expression) expectedType).getType().toSort().getPLevel();
    List<Level> pLevels = generateUpperBounds(domPLevels, codPLevel, expr);
    LetClause letBinding = new LetClause(Abstract.CaseExpression.FUNCTION_NAME, pLevels, links, (Expression) expectedType, elimTree);
    caseResult.expression = ExpressionFactory.Let(ExpressionFactory.lets(letBinding), new LetClauseCallExpression(letBinding, letArguments));
    expr.setWellTyped(myContext, caseResult.expression);
    return caseResult;
  }

  private List<Level> generateUpperBounds(List<Level> domPLevels, Level codPLevel, Abstract.SourceNode sourceNode) {
    if (domPLevels.isEmpty()) {
      return Collections.emptyList();
    }

    List<Level> resultPLevels = new ArrayList<>(domPLevels.size());
    for (Level domPLevel : domPLevels) {
      InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, sourceNode);
      myEquations.addVariable(pl);
      Level pLevel = new Level(pl);
      resultPLevels.add(pLevel);
      myEquations.add(domPLevel, pLevel, Equations.CMP.LE, sourceNode);
      if (!resultPLevels.isEmpty()) {
        myEquations.add(pLevel, resultPLevels.get(resultPLevels.size() - 1), Equations.CMP.LE, sourceNode);
      }
    }

    myEquations.add(codPLevel, resultPLevels.get(resultPLevels.size() - 1), Equations.CMP.LE, sourceNode);
    return resultPLevels;
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Type expectedType) {
    Abstract.Expression expr1 = expr.getExpression();
    Result exprResult = typeCheck(expr1, null);
    if (exprResult == null) return null;
    exprResult.type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    SigmaExpression sigmaType = exprResult.type.toSigma();
    if (sigmaType == null) {
      LocalTypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A sigma type"), exprResult.type, expr1);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    DependentLink sigmaParams = sigmaType.getParameters();
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Index " + (expr.getField() + 1) + " out of range", expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    for (int i = 0; sigmaParams != fieldLink; sigmaParams = sigmaParams.getNext(), i++) {
      substitution.add(sigmaParams, ExpressionFactory.Proj(exprResult.expression, i));
    }

    exprResult.expression = ExpressionFactory.Proj(exprResult.expression, expr.getField());
    exprResult.type = fieldLink.getType().subst(substitution, LevelSubstitution.EMPTY);
    return checkResult(expectedType, exprResult, expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Type expectedType) {
    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result typeCheckedBaseClass = typeCheck(baseClassExpr, null);
    if (typeCheckedBaseClass == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = typeCheckedBaseClass.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normalizedBaseClassExpr.toClassCall();
    if (classCallExpr == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a class", baseClassExpr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassDefinition baseClass = classCallExpr.getDefinition();
    if (!baseClass.status().bodyIsOK()) {
      LocalTypeCheckingError error = new HasErrors(baseClass.getAbstractDefinition(), expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(classCallExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    FieldSet fieldSet = new FieldSet();
    ClassCallExpression resultClassCall = ExpressionFactory.ClassCall(baseClass, classCallExpr.getSortArgument(), fieldSet);
    Expression resultExpr = resultClassCall;

    fieldSet.addFieldsFrom(classCallExpr.getFieldSet());
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : classCallExpr.getFieldSet().getImplemented()) {
      boolean ok = fieldSet.implementField(entry.getKey(), entry.getValue());
      assert ok;
    }

    Collection<? extends Abstract.ClassFieldImpl> statements = expr.getStatements();
    Map<ClassField, Abstract.ClassFieldImpl> classFieldMap = new HashMap<>();

    for (Abstract.ClassFieldImpl statement : statements) {
      ClassField field = (ClassField) myState.getTypechecked(statement.getImplementedField());
      if (fieldSet.isImplemented(field) || classFieldMap.containsKey(field)) {
        myErrorReporter.report(new LocalTypeCheckingError("Field '" + field.getName() + "' is already implemented", statement));
      } else {
        classFieldMap.put(field, statement);
      }
    }

    if (!classFieldMap.isEmpty()) {
      for (ClassField field : baseClass.getFieldSet().getFields()) {
        if (fieldSet.isImplemented(field)) {
          continue;
        }

        Abstract.ClassFieldImpl impl = classFieldMap.get(field);
        if (impl != null) {
          if (resultExpr instanceof ClassCallExpression) {
            implementField(fieldSet, field, impl.getImplementation(), (ClassCallExpression) resultExpr);
          }
          classFieldMap.remove(field);
          if (classFieldMap.isEmpty()) {
            break;
          }
        } else {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Field '" + field.getName() + "' is not implemented", expr);
          if (resultExpr instanceof ClassCallExpression) {
            resultExpr = ExpressionFactory.Error(resultExpr, error);
          }
          myErrorReporter.report(error);
        }
      }
    }

    fieldSet.updateSorts(resultClassCall);
    return checkResult(expectedType, new Result(resultExpr, new UniverseExpression(resultExpr instanceof ClassCallExpression ? ((ClassCallExpression) resultExpr).getSort() : Sort.PROP)), expr);
  }

  public CheckTypeVisitor.Result implementField(FieldSet fieldSet, ClassField field, Abstract.Expression implBody, ClassCallExpression fieldSetClass) {
    CheckTypeVisitor.Result result = typeCheck(implBody, field.getBaseType(fieldSetClass.getSortArgument()).subst(field.getThisParameter(), ExpressionFactory.New(fieldSetClass)));
    fieldSet.implementField(field, new FieldSet.Implementation(null, result != null ? result.expression : ExpressionFactory.Error(null, null)));
    return result;
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Type expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.toClassCall();
    if (classCallExpr == null) {
      classCallExpr = normExpr.toError().getExpr().normalize(NormalizeVisitor.Mode.WHNF).toClassCall();
      if (classCallExpr == null) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a class", expr.getExpression());
        expr.setWellTyped(myContext, ExpressionFactory.Error(normExpr, error));
        myErrorReporter.report(error);
        return null;
      } else {
        exprResult.expression = ExpressionFactory.Error(ExpressionFactory.New(classCallExpr), normExpr.toError().getError());
        exprResult.type = normExpr;
        return exprResult;
      }
    }

    if (checkAllImplemented(classCallExpr, expr)) {
      exprResult.expression = ExpressionFactory.New(classCallExpr);
      exprResult.type = normExpr;
      return checkResult(expectedType, exprResult, expr);
    } else {
      return null;
    }
  }

  public boolean checkAllImplemented(ClassCallExpression classCall, Abstract.Expression expr) {
    int remaining = classCall.getFieldSet().getFields().size() - classCall.getFieldSet().getImplemented().size();
    if (remaining == 0) {
      return true;
    } else {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Class '" + classCall.getDefinition().getName() + "' has " + remaining + " not implemented fields", expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
      myErrorReporter.report(error);
      return false;
    }
  }

  private LetClause typeCheckLetClause(Abstract.LetClause clause) {
    List<SingleDependentLink> links = new ArrayList<>(clause.getArguments().size());
    Expression resultType;
    ElimTreeNode elimTree;
    LetClause letResult;
    List<Level> domPLevels = new ArrayList<>(clause.getArguments().size());

    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument arg : clause.getArguments()) {
        if (arg instanceof Abstract.TelescopeArgument) {
          Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) arg;
          Result result = typeCheck(teleArg.getType(), TypeOmega.INSTANCE);
          if (result == null) return null;
          Expression argType = result.expression;
          links.add(singleParam(teleArg.getExplicit(), teleArg.getNames(), argType));
          domPLevels.add(result.type.toSort().getPLevel());
          for (SingleDependentLink link = links.get(links.size() - 1); link.hasNext(); link = link.getNext()) {
            myContext.add(link);
          }
        } else {
          myErrorReporter.report(new LocalTypeCheckingError("Expected a typed parameter", arg));
          return null;
        }
      }

      Expression expectedType = null;
      if (clause.getResultType() != null) {
        Result result = typeCheck(clause.getResultType(), null);
        if (result == null) return null;
        expectedType = result.expression;
      }

      if (clause.getTerm() instanceof Abstract.ElimExpression)  {
        if (links.size() > 1) { // TODO: Fix this
          LocalTypeCheckingError error = new LocalTypeCheckingError("Expected exactly one argument", clause);
          myErrorReporter.report(error);
          return null;
        }

        assert expectedType != null;
        int size = 0;
        for (SingleDependentLink link : links) {
          size += DependentLink.Helper.size(link);
        }
        myContext.subList(myContext.size() - size, myContext.size()).clear();
        elimTree = myTypeCheckingElim.typeCheckElim((Abstract.ElimExpression) clause.getTerm(), clause.getArrow() == Abstract.Definition.Arrow.LEFT ? (links.isEmpty() ? EmptyDependentLink.getInstance() : links.get(0)) : null, expectedType, false, false);
        if (elimTree == null) return null;
        resultType = expectedType;
      } else {
        Result termResult = typeCheck(clause.getTerm(), expectedType);
        if (termResult == null) return null;
        elimTree = ExpressionFactory.top(links, ExpressionFactory.leaf(clause.getArrow(), termResult.expression));
        resultType = expectedType != null ? expectedType : termResult.type;
      }

      LocalTypeCheckingError error = TypeCheckingElim.checkCoverage(clause.getName(), clause, links, elimTree, expectedType);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
      error = TypeCheckingElim.checkConditions(clause, links, elimTree);
      if (error != null) {
        myErrorReporter.report(error);
        return null;
      }
    }

    Level codPLevel = resultType.getType().toSort().getPLevel();
    List<Level> pLevels = generateUpperBounds(domPLevels, codPLevel, clause);
    letResult = new LetClause(clause.getName(), pLevels, links, resultType, elimTree);
    myContext.add(letResult);
    return letResult;
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Type expectedType) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
      List<LetClause> clauses = new ArrayList<>();
      for (int i = 0; i < expr.getClauses().size(); i++) {
        LetClause clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (clauseResult == null) return null;
        clauses.add(clauseResult);
      }
      Result result = typeCheck(expr.getExpression(), expectedType);
      if (result == null) return null;

      LetExpression letExpr = ExpressionFactory.Let(clauses, result.expression);
      return new Result(letExpr, new LetExpression(letExpr.getClauses(), result.type));
    }
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, Type expectedType) {
    int number = expr.getNumber();
    Expression expression = ExpressionFactory.Zero();
    for (int i = 0; i < number; ++i) {
      expression = ExpressionFactory.Suc(expression);
    }
    return checkResult(expectedType, new Result(expression, ExpressionFactory.Nat()), expr);
  }
}
