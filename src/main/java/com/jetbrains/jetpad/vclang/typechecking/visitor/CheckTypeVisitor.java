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
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.elimtree.Clause;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingDefCall;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.error.*;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.ImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.StdImplicitArgsInference;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.TwoStageEquations;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ConditionsChecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ElimTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.PatternTypechecking;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.ClassViewInstancePool;

import java.util.*;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.expression;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements AbstractExpressionVisitor<ExpectedType, CheckTypeVisitor.Result>, AbstractLevelExpressionVisitor<LevelVariable, Level> {
  private final TypecheckerState myState;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private Map<Abstract.ReferableSourceNode, Binding> myContext;
  private final Set<Binding> myFreeBindings;
  private LocalErrorReporter myErrorReporter;
  private final TypeCheckingDefCall myTypeCheckingDefCall;
  private final ImplicitArgsInference myArgsInference;
  private final Equations myEquations;
  private ClassViewInstancePool myClassViewInstancePool;

  public interface TResult {
    Result toResult(Equations equations);
    DependentLink getParameter();
    TResult applyExpression(Expression expression);
    List<? extends DependentLink> getImplicitParameters();
  }

  public static class DefCallResult implements TResult {
    private final Abstract.ReferenceExpression myDefCall;
    private final Definition myDefinition;
    private final Sort mySortArgument;
    private final List<Expression> myArguments;
    private List<DependentLink> myParameters;
    private Expression myResultType;
    private final Expression myThisExpr;

    private DefCallResult(Abstract.ReferenceExpression defCall, Definition definition, Sort sortArgument, List<Expression> arguments, List<DependentLink> parameters, Expression resultType, Expression thisExpr) {
      myDefCall = defCall;
      myDefinition = definition;
      mySortArgument = sortArgument;
      myArguments = arguments;
      myParameters = parameters;
      myResultType = resultType;
      myThisExpr = thisExpr;
    }

    public static TResult makeTResult(Abstract.ReferenceExpression defCall, Definition definition, Sort sortArgument, Expression thisExpr) {
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
          SingleDependentLink parameter = ExpressionFactory.singleParams(link.isExplicit(), names, link.getType().subst(substitution, LevelSubstitution.EMPTY));
          parameters.add(parameter);
          names.clear();

          for (; parameter.hasNext(); parameter = parameter.getNext(), link0 = link0.getNext()) {
            substitution.add(link0, new ReferenceExpression(parameter));
            myArguments.add(new ReferenceExpression(parameter));
          }

          link0 = null;
        }
      }

      Expression expression = myDefinition.getDefCall(mySortArgument, myThisExpr, myArguments);
      Expression type = myResultType.subst(substitution, LevelSubstitution.EMPTY);
      Sort codSort = getSortOf(type.getType());
      for (int i = parameters.size() - 1; i >= 0; i--) {
        codSort = PiExpression.generateUpperBound(parameters.get(i).getType().getSortOfType(), codSort, equations, myDefCall);
        expression = new LamExpression(codSort, parameters.get(i), expression);
        type = new PiExpression(codSort, parameters.get(i), type);
      }
      return new Result(expression, type);
    }

    @Override
    public DependentLink getParameter() {
      return myParameters.get(0);
    }

    @Override
    public TResult applyExpression(Expression expression) {
      int size = myParameters.size();
      myArguments.add(expression);
      ExprSubstitution subst = new ExprSubstitution();
      subst.add(myParameters.get(0), expression);
      myParameters = DependentLink.Helper.subst(myParameters.subList(1, size), subst, LevelSubstitution.EMPTY);
      myResultType = myResultType.subst(subst, LevelSubstitution.EMPTY);
      return size > 1 ? this : new Result(myDefinition.getDefCall(mySortArgument, myThisExpr, myArguments), myResultType);
    }

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

      assert expressions.size() <= size;
      return expressions.size() < size ? this : new Result(myDefinition.getDefCall(mySortArgument, myThisExpr, myArguments), myResultType);
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

    public Abstract.ReferenceExpression getDefCall() {
      return myDefCall;
    }

    public Definition getDefinition() {
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
      return type.isInstance(PiExpression.class) ? type.cast(PiExpression.class).getParameters() : EmptyDependentLink.getInstance();
    }

    @Override
    public Result applyExpression(Expression expr) {
      expression = new AppExpression(expression, expr);
      type = type.applyExpression(expr);
      return this;
    }

    @Override
    public List<SingleDependentLink> getImplicitParameters() {
      List<SingleDependentLink> params = new ArrayList<>();
      type.getPiParameters(params, true);
      return params;
    }
  }

  public CheckTypeVisitor(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, Map<Abstract.ReferableSourceNode, Binding> localContext, LocalErrorReporter errorReporter, ClassViewInstancePool pool) {
    myState = state;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myContext = localContext;
    myFreeBindings = new HashSet<>();
    myErrorReporter = errorReporter;
    myTypeCheckingDefCall = new TypeCheckingDefCall(this);
    myArgsInference = new StdImplicitArgsInference(this);
    myEquations = new TwoStageEquations(this);
    myClassViewInstancePool = pool;
  }

  public void setThis(ClassDefinition thisClass, Binding thisBinding) {
    myTypeCheckingDefCall.setThis(thisClass, thisBinding);
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

  public Map<Abstract.ReferableSourceNode, Binding> getContext() {
    return myContext;
  }

  public Set<Binding> getFreeBindings() {
    return myFreeBindings;
  }

  public Set<Binding> getAllBindings() {
    Set<Binding> allBindings = new HashSet<>(myContext.values());
    allBindings.addAll(myFreeBindings);
    return allBindings;
  }

  public LocalErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  public Equations getEquations() {
    return myEquations;
  }

  private static Sort getSortOf(Expression expr) {
    Sort sort = expr.toSort();
    if (sort == null) {
      assert expr.isInstance(ErrorExpression.class);
      return Sort.PROP;
    } else {
      return sort;
    }
  }

  private Result checkResult(ExpectedType expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null || !(expectedType instanceof Expression)) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    }

    if (compare(result, (Expression) expectedType, expression)) {
      expression.setWellTyped(myContext, result.expression);
      return result;
    } else {
      return null;
    }
  }

  public boolean compare(Result result, Expression expectedType, Abstract.Expression expr) {
    result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (result.type.isLessOrEquals(expectedType, myEquations, expr)) {
      result.expression = OfTypeExpression.make(result.expression, result.type, expectedType);
      return true;
    }

    LocalTypeCheckingError error = new TypeMismatchError(termDoc(expectedType), termDoc(result.type), (Concrete.SourceNode) expr);
    expr.setWellTyped(myContext, new ErrorExpression(result.expression, error));
    myErrorReporter.report(error);
    return false;
  }

  private Result tResultToResult(ExpectedType expectedType, TResult result, Abstract.Expression expr) {
    if (result != null && expectedType != null) {
      result = myArgsInference.inferTail(result, expectedType, expr);
    }
    return result == null ? null : checkResult(expectedType, result.toResult(myEquations), expr);
  }

  public Result checkExpr(Abstract.Expression expr, ExpectedType expectedType) {
    if (expr == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Incomplete expression", null);
      myErrorReporter.report(error);
      return null;
    }
    return expr.accept(this, expectedType);
  }

  public Result finalCheckExpr(Abstract.Expression expr, ExpectedType expectedType) {
    Result result = checkExpr(expr, expectedType);
    if (result == null) return null;
    LevelSubstitution substitution = myEquations.solve(expr);
    if (!substitution.isEmpty()) {
      result.expression = result.expression.subst(substitution);
      result.type = result.type.subst(new ExprSubstitution(), substitution);
    }

    LocalErrorReporterCounter counter = new LocalErrorReporterCounter(myErrorReporter);
    result.expression = result.expression.strip(counter);
    result.type = result.type.strip(counter.getErrorsNumber() == 0 ? myErrorReporter : new DummyLocalErrorReporter());
    return result;
  }

  public Type checkType(Abstract.Expression expr) {
    if (expr == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Incomplete expression", null);
      myErrorReporter.report(error);
      return null;
    }

    Result result = expr.accept(this, ExpectedType.OMEGA);
    if (result == null) {
      return null;
    }
    if (result.expression instanceof Type) {
      return (Type) result.expression;
    }

    Expression type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    UniverseExpression universe = type.checkedCast(UniverseExpression.class);
    if (universe == null) {
      Expression stuck = type.getStuckExpression();
      if (stuck == null || !stuck.isInstance(InferenceReferenceExpression.class) && !stuck.isInstance(ErrorExpression.class)) {
        LocalTypeCheckingError error = new TypeMismatchError(text("a universe"), termDoc(result.type), (Concrete.SourceNode) expr);
        expr.setWellTyped(myContext, new ErrorExpression(result.expression, error));
        myErrorReporter.report(error);
        return null;
      }

      universe = new UniverseExpression(Sort.generateInferVars(myEquations, expr));
      InferenceReferenceExpression infExpr = stuck.checkedCast(InferenceReferenceExpression.class);
      if (infExpr != null && infExpr.getVariable() != null) {
        myEquations.add(type, universe, Equations.CMP.LE, expr, infExpr.getVariable());
      }
    }

    return new TypeExpression(result.expression, universe.getSort());
  }

  public Type finalCheckType(Abstract.Expression expr) {
    Type result = checkType(expr);
    if (result == null) return null;
    return result.subst(new ExprSubstitution(), myEquations.solve(expr)).strip(myErrorReporter);
  }

  private boolean compareExpressions(boolean isLeft, Result result, Expression expected, Expression actual, Abstract.Expression expr) {
    if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, actual, expected, expr)) {
      CompareVisitor.compare(myEquations, Equations.CMP.EQ, actual, expected, expr);
      LocalTypeCheckingError error = new PathEndpointMismatchError(isLeft, expected, actual, (Concrete.SourceNode) expr);
      expr.setWellTyped(myContext, new ErrorExpression(result.expression, error));
      myErrorReporter.report(error);
      return false;
    }
    return true;
  }

  private boolean checkPath(TResult result, Abstract.Expression expr) {
    if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.PATH_CON) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected an argument for 'path'", (Concrete.SourceNode) expr);
      expr.setWellTyped(myContext, new ErrorExpression(result.toResult(myEquations).expression, error));
      myErrorReporter.report(error);
      return false;
    }
    if (result instanceof Result) {
      ConCallExpression conCall = ((Result) result).expression.checkedCast(ConCallExpression.class);
      if (conCall != null && conCall.getDefinition() == Prelude.PATH_CON) {
        if (!compareExpressions(true, (Result) result, conCall.getDataTypeArguments().get(1), new AppExpression(conCall.getDefCallArguments().get(0), ExpressionFactory.Left()), expr) ||
          !compareExpressions(false, (Result) result, conCall.getDataTypeArguments().get(2), new AppExpression(conCall.getDefCallArguments().get(0), ExpressionFactory.Right()), expr)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, ExpectedType expectedType) {
    TResult result = myArgsInference.infer(expr, expectedType);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  public CheckTypeVisitor.TResult getLocalVar(Abstract.ReferenceExpression expr) {
    Binding def = myContext.get(expr.getReferent());
    if (def == null) {
      throw new InconsistentModel();
    }
    return new Result(new ReferenceExpression(def), def.getTypeExpr());
  }

  @Override
  public Result visitReference(Abstract.ReferenceExpression expr, ExpectedType expectedType) {
    TResult result = expr.getExpression() == null && !(expr.getReferent() instanceof Abstract.GlobalReferableSourceNode) ? getLocalVar(expr) : myTypeCheckingDefCall.typeCheckDefCall(expr);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  @Override
  public Result visitInferenceReference(Abstract.InferenceReferenceExpression expr, ExpectedType params) {
    throw new IllegalStateException();
  }

  @Override
  public Result visitModuleCall(Abstract.ModuleCallExpression expr, ExpectedType expectedType) {
    if (expr.getModule() == null) {
      throw new IllegalStateException();
    }
    Definition typeChecked = myState.getTypechecked(expr.getModule());
    if (typeChecked == null) {
      assert false;
      LocalTypeCheckingError error = new LocalTypeCheckingError("Internal error: module '" + expr.getPath() + "' is not available yet", (Concrete.SourceNode) expr);
      expr.setWellTyped(myContext, new ErrorExpression(null, error));
      myErrorReporter.report(error);
      return null;
    }

    return new Result(new ClassCallExpression((ClassDefinition) typeChecked, Sort.PROP), new UniverseExpression(((ClassDefinition) typeChecked).getSort().subst(Sort.PROP.toLevelSubstitution())));
  }

  private TypedSingleDependentLink visitNameParameter(Abstract.NameParameter param, int argIndex, Abstract.SourceNode sourceNode) {
    String name = param.getName();

    InferenceLevelVariable pLvl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, sourceNode);
    InferenceLevelVariable hLvl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, sourceNode);
    myEquations.addVariable(pLvl);
    myEquations.addVariable(hLvl);
    Sort sort = new Sort(new Level(pLvl), new Level(hLvl));
    InferenceVariable inferenceVariable = new LambdaInferenceVariable(name == null ? "_" : "type-of-" + name, new UniverseExpression(sort), argIndex, sourceNode, false, getAllBindings());
    Expression argType = new InferenceReferenceExpression(inferenceVariable, myEquations);

    TypedSingleDependentLink link = new TypedSingleDependentLink(param.getExplicit(), name, new TypeExpression(argType, sort));
    myContext.put(param, link);
    return link;
  }

  private SingleDependentLink visitTypeParameter(Abstract.TypeParameter param) {
    Abstract.Expression paramType = param.getType();
    Type argResult = checkType(paramType);
    if (argResult == null) return null;

    if (param instanceof Abstract.TelescopeParameter) {
      List<? extends Abstract.ReferableSourceNode> referableList = ((Abstract.TelescopeParameter) param).getReferableList();
      List<String> names = new ArrayList<>(referableList.size());
      for (Abstract.ReferableSourceNode referable : referableList) {
        names.add(referable == null ? null : referable.getName());
      }
      SingleDependentLink link = ExpressionFactory.singleParams(param.getExplicit(), names, argResult);
      int i = 0;
      for (SingleDependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
        myContext.put(referableList.get(i), link1);
      }
      return link;
    } else {
      return ExpressionFactory.singleParams(param.getExplicit(), Collections.singletonList(null), argResult);
    }
  }

  private Result visitLam(List<? extends Abstract.Parameter> parameters, Abstract.LamExpression expr, Expression expectedType, int argIndex) {
    if (parameters.isEmpty()) {
      return checkExpr(expr.getBody(), expectedType);
    }

    Abstract.Parameter param = parameters.get(0);
    if (expectedType != null) {
      expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      if (param.getExplicit() && expectedType.isInstance(PiExpression.class) && !expectedType.cast(PiExpression.class).getParameters().isExplicit()) {
        // myContext.put(referable, piParams);
        PiExpression piExpectedType = expectedType.cast(PiExpression.class);
        SingleDependentLink piParams = piExpectedType.getParameters();
        Result bodyResult = visitLam(parameters, expr, piExpectedType.getCodomain(), argIndex + DependentLink.Helper.size(piParams));
        if (bodyResult == null) return null;
        Sort sort = PiExpression.generateUpperBound(piParams.getType().getSortOfType(), getSortOf(bodyResult.type.getType()), myEquations, expr);
        return new Result(new LamExpression(sort, piParams, bodyResult.expression), new PiExpression(sort, piParams, bodyResult.type));
      }
    }

    if (param instanceof Abstract.NameParameter) {
      if (expectedType == null || !expectedType.isInstance(PiExpression.class)) {
        TypedSingleDependentLink link = visitNameParameter((Abstract.NameParameter) param, argIndex, expr);
        Result bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, null, argIndex + 1);
        if (bodyResult == null) return null;
        Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType()), myEquations, expr);
        Result result = new Result(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
        if (expectedType != null && !compare(result, expectedType, expr)) {
          return null;
        }
        return result;
      } else {
        PiExpression piExpectedType = expectedType.cast(PiExpression.class);
        Abstract.ReferableSourceNode referable = (Abstract.NameParameter) param;
        SingleDependentLink piParams = piExpectedType.getParameters();
        if (piParams.isExplicit() && !param.getExplicit()) {
          myErrorReporter.report(new LocalTypeCheckingError(ordinal(argIndex) + " argument of the lambda is implicit, but the first parameter of the expected type is not", (Concrete.SourceNode) expr));
        }
        SingleDependentLink link = new TypedSingleDependentLink(piParams.isExplicit(), referable.getName(), piParams.getType());
        myContext.put(referable, link);
        Expression codomain = piExpectedType.getCodomain().subst(piParams, new ReferenceExpression(link));
        Result bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, piParams.getNext().hasNext() ? new PiExpression(piExpectedType.getResultSort(), piParams.getNext(), codomain) : codomain, argIndex + 1);
        if (bodyResult == null) return null;
        Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType()), myEquations, expr);
        return new Result(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
      }
    } else if (param instanceof Abstract.TypeParameter) {
      SingleDependentLink link = visitTypeParameter((Abstract.TypeParameter) param);
      if (link == null) {
        return null;
      }

      SingleDependentLink actualLink = null;
      Expression expectedBodyType = null;
      int namesCount = param instanceof Abstract.TelescopeParameter ? ((Abstract.TelescopeParameter) param).getReferableList().size() : 1;
      if (expectedType != null) {
        Abstract.Expression paramType = ((Abstract.TypeParameter) param).getType();
        Expression argType = link.getTypeExpr();

        SingleDependentLink lamLink = link;
        ExprSubstitution substitution = new ExprSubstitution();
        Expression argExpr = null;
        int checked = 0;
        while (true) {
          if (!expectedType.isInstance(PiExpression.class)) {
            actualLink = link;
            for (int i = 0; i < checked; i++) {
              actualLink = actualLink.getNext();
            }
            expectedType = expectedType.subst(substitution);
            break;
          }
          if (argExpr == null) {
            argExpr = argType;
          }

          PiExpression piExpectedType = expectedType.cast(PiExpression.class);
          Expression argExpectedType = piExpectedType.getParameters().getTypeExpr().subst(substitution);
          if (piExpectedType.getParameters().isExplicit() && !param.getExplicit()) {
            myErrorReporter.report(new LocalTypeCheckingError(ordinal(argIndex) + " argument of the lambda is implicit, but the first parameter of the expected type is not", (Concrete.SourceNode) expr));
          }
          if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, argExpr, argExpectedType, paramType)) {
            LocalTypeCheckingError error = new TypeMismatchError(termDoc(argExpectedType), termDoc(argType), (Concrete.SourceNode) paramType);
            myErrorReporter.report(error);
            return null;
          }

          int parametersCount = 0;
          for (DependentLink link1 = piExpectedType.getParameters(); link1.hasNext(); link1 = link1.getNext()) {
            parametersCount++;
            if (lamLink.hasNext()) {
              substitution.add(link1, new ReferenceExpression(lamLink));
              lamLink = lamLink.getNext();
            }
          }

          checked += parametersCount;
          if (checked >= namesCount) {
            if (checked == namesCount) {
              expectedBodyType = piExpectedType.getCodomain().subst(substitution);
            } else {
              int skip = parametersCount - (checked - namesCount);
              SingleDependentLink link1 = piExpectedType.getParameters();
              for (int i = 0; i < skip; i++) {
                link1 = link1.getNext();
              }
              expectedBodyType = new PiExpression(piExpectedType.getResultSort(), link1, piExpectedType.getCodomain()).subst(substitution);
            }
            break;
          }
          expectedType = piExpectedType.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }
      }

      Result bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, expectedBodyType, argIndex + namesCount);
      if (bodyResult == null) return null;
      Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType()), myEquations, expr);
      if (actualLink != null) {
        if (!compare(new Result(null, new PiExpression(sort, actualLink, bodyResult.type)), expectedType, expr)) {
          return null;
        }
      }

      return new Result(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, ExpectedType expectedType) {
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      Result result = visitLam(expr.getParameters(), expr, expectedType instanceof Expression ? (Expression) expectedType : null, 1);
      if (result != null) {
        expr.setWellTyped(myContext, result.expression);
        if (expectedType != null && !(expectedType instanceof Expression)) {
          LocalTypeCheckingError error = new TypeMismatchError(typeDoc(expectedType), termDoc(result.type), (Concrete.SourceNode) expr);
          myErrorReporter.report(error);
          return null;
        }
      }
      return result;
    }
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, ExpectedType expectedType) {
    List<SingleDependentLink> list = new ArrayList<>();
    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      for (Abstract.TypeParameter arg : expr.getParameters()) {
        Type result = checkType(arg.getType());
        if (result == null) return null;

        if (arg instanceof Abstract.TelescopeParameter) {
          List<? extends Abstract.ReferableSourceNode> referableList = ((Abstract.TelescopeParameter) arg).getReferableList();
          List<String> names = new ArrayList<>(referableList.size());
          for (Abstract.ReferableSourceNode referable : referableList) {
            names.add(referable == null ? null : referable.getName());
          }
          SingleDependentLink link = ExpressionFactory.singleParams(arg.getExplicit(), names, result);
          list.add(link);
          int i = 0;
          for (SingleDependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
            myContext.put(referableList.get(i), link1);
          }
        } else {
          list.add(new TypedSingleDependentLink(arg.getExplicit(), null, result));
        }

        sorts.add(result.getSortOfType());
      }

      Type result = checkType(expr.getCodomain());
      if (result == null) return null;
      Sort codSort = result.getSortOfType();

      Expression piExpr = result.getExpr();
      for (int i = list.size() - 1; i >= 0; i--) {
        codSort = PiExpression.generateUpperBound(sorts.get(i), codSort, myEquations, expr);
        piExpr = new PiExpression(codSort, list.get(i), piExpr);
      }

      return checkResult(expectedType, new Result(piExpr, new UniverseExpression(codSort)), expr);
    }
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, ExpectedType expectedType) {
    Level pLevel = expr.getPLevel() != null ? expr.getPLevel().accept(this, LevelVariable.PVAR) : new Level(LevelVariable.PVAR);
    Level hLevel = expr.getHLevel() != null ? expr.getHLevel().accept(this, LevelVariable.HVAR) : new Level(LevelVariable.HVAR);

    if (pLevel.isInfinity()) {
      myErrorReporter.report(new LocalTypeCheckingError("\\inf is not a correct p-level", (Concrete.SourceNode) expr));
      pLevel = new Level(LevelVariable.PVAR);
    }

    UniverseExpression universe = new UniverseExpression(new Sort(pLevel, hLevel));
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getSort().succ())), expr);
  }

  @Override
  public Level visitInf(Abstract.InfLevelExpression expr, LevelVariable param) {
    return Level.INFINITY;
  }

  @Override
  public Level visitLP(Abstract.PLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.PVAR) {
      myErrorReporter.report(new LocalTypeCheckingError("Expected " + base, (Concrete.SourceNode) expr));
    }
    return new Level(base);
  }

  @Override
  public Level visitLH(Abstract.HLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.HVAR) {
      myErrorReporter.report(new LocalTypeCheckingError("Expected " + base, (Concrete.SourceNode) expr));
    }
    return new Level(base);
  }

  @Override
  public Level visitNumber(Abstract.NumberLevelExpression expr, LevelVariable base) {
    return new Level(expr.getNumber());
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
    myErrorReporter.report(new LocalTypeCheckingError("Cannot typecheck an inference variable", (Concrete.SourceNode) expr));
    return new Level(base);
  }

  @Override
  public Result visitGoal(Abstract.GoalExpression expr, ExpectedType expectedType) {
    List<Error> errors = Collections.emptyList();
    Result exprResult = null;
    if (expr.getExpression() != null) {
      LocalErrorReporter errorReporter = myErrorReporter;
      errors = new ArrayList<>();
      myErrorReporter = new ListLocalErrorReporter(errors);
      exprResult = checkExpr(expr.getExpression(), expectedType);
      myErrorReporter = errorReporter;
    }

    LocalTypeCheckingError error = new GoalError(expr.getName(), myContext, expectedType, exprResult == null ? null : exprResult.type, errors, (Concrete.Expression) expr);
    Expression result = new ErrorExpression(null, error);
    expr.setWellTyped(myContext, result);
    myErrorReporter.report(error);
    return new Result(result, result);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, ExpectedType expectedType) {
    if (expectedType instanceof Expression) {
      return new Result(new InferenceReferenceExpression(new ExpressionInferenceVariable((Expression) expectedType, expr, getAllBindings()), myEquations), (Expression) expectedType);
    } else {
      LocalTypeCheckingError error = new ArgInferenceError(expression(), (Concrete.SourceNode) expr, new Expression[0]);
      expr.setWellTyped(myContext, new ErrorExpression(null, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, ExpectedType expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType instanceof Expression) {
      expectedTypeNorm = ((Expression) expectedType).normalize(NormalizeVisitor.Mode.WHNF);
      if (expectedTypeNorm.isInstance(SigmaExpression.class)) {
        SigmaExpression expectedTypeSigma = expectedTypeNorm.cast(SigmaExpression.class);
        DependentLink sigmaParams = expectedTypeSigma.getParameters();
        int sigmaParamsSize = DependentLink.Helper.size(sigmaParams);

        if (expr.getFields().size() != sigmaParamsSize) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), (Concrete.SourceNode) expr);
          expr.setWellTyped(myContext, new ErrorExpression(null, error));
          myErrorReporter.report(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Result tupleResult = new Result(new TupleExpression(fields, expectedTypeSigma), (Expression) expectedType);
        ExprSubstitution substitution = new ExprSubstitution();
        for (Abstract.Expression field : expr.getFields()) {
          Expression expType = sigmaParams.getTypeExpr().subst(substitution);
          Result result = checkExpr(field, expType);
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
    for (int i = 0; i < expr.getFields().size(); i++) {
      Result result = checkExpr(expr.getFields().get(i), null);
      if (result == null) return null;
      fields.add(result.expression);
      list.append(ExpressionFactory.parameter(null, new TypeExpression(result.type, getSortOf(result.type.getType()))));
    }

    Sort sortArgument = Sort.generateInferVars(myEquations, expr);
    SigmaExpression type = new SigmaExpression(sortArgument, list.getFirst());
    return checkResult(expectedTypeNorm, new Result(new TupleExpression(fields, type), type), expr);
  }

  private DependentLink visitParameters(List<? extends Abstract.TypeParameter> parameters, List<Sort> resultSorts) {
    LinkList list = new LinkList();

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      for (Abstract.TypeParameter arg : parameters) {
        Type result = checkType(arg.getType());
        if (result == null) return null;

        if (arg instanceof Abstract.TelescopeParameter) {
          List<? extends Abstract.ReferableSourceNode> referableList = ((Abstract.TelescopeParameter) arg).getReferableList();
          List<String> names = new ArrayList<>(referableList.size());
          for (Abstract.ReferableSourceNode referable : referableList) {
            names.add(referable == null ? null : referable.getName());
          }
          DependentLink link = ExpressionFactory.parameter(arg.getExplicit(), names, result);
          list.append(link);
          int i = 0;
          for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
            myContext.put(referableList.get(i), link1);
          }
        } else {
          DependentLink link = ExpressionFactory.parameter(arg.getExplicit(), (String) null, result);
          list.append(link);
        }

        resultSorts.add(result.getSortOfType());
      }
    }

    return list.getFirst();
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, ExpectedType expectedType) {
    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());
    DependentLink args = visitParameters(expr.getParameters(), sorts);
    if (args == null || !args.hasNext()) return null;
    Sort sort = generateUpperBound(sorts, expr);
    return checkResult(expectedType, new Result(new SigmaExpression(sort, args), new UniverseExpression(sort)), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, ExpectedType expectedType) {
    return tResultToResult(expectedType, myArgsInference.infer(expr, expectedType), expr);
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, ExpectedType expectedType) {
    assert expr.getSequence().isEmpty();
    return checkExpr(expr.getLeft(), expectedType);
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, ExpectedType expectedType) {
    if (expectedType == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer the result type", (Concrete.SourceNode) expr);
      expr.setWellTyped(myContext, new ErrorExpression(null, error));
      myErrorReporter.report(error);
      return null;
    }
    if (!(expectedType instanceof Expression)) {
      expectedType = new UniverseExpression(Sort.generateInferVars(myEquations, expr));
    }

    List<? extends Abstract.Expression> abstractExprs = expr.getExpressions();
    LinkList list = new LinkList();
    List<Expression> expressions = new ArrayList<>(abstractExprs.size());
    for (Abstract.Expression expression : abstractExprs) {
      Result exprResult = checkExpr(expression, null);
      if (exprResult == null) return null;
      list.append(ExpressionFactory.parameter(null, new TypeExpression(exprResult.type, getSortOf(exprResult.type.getType()))));
      expressions.add(exprResult.expression);
    }

    List<Clause> resultClauses = new ArrayList<>();
    ElimTree elimTree = new ElimTypechecking(this, (Expression) expectedType, EnumSet.of(PatternTypechecking.Flag.ALLOW_CONDITIONS, PatternTypechecking.Flag.CHECK_COVERAGE)).typecheckElim(expr.getClauses(), expr, list.getFirst(), resultClauses);
    if (elimTree == null) {
      return null;
    }

    ConditionsChecking.check(resultClauses, elimTree, myErrorReporter);

    Expression caseResult = new CaseExpression(list.getFirst(), (Expression) expectedType, elimTree, expressions);
    expr.setWellTyped(myContext, caseResult);
    return new Result(caseResult, (Expression) expectedType);
  }

  private static Sort generateUniqueUpperBound(List<Sort> sorts) {
    LevelVariable pVar = null;
    LevelVariable hVar = null;
    for (Sort sort : sorts) {
      if (sort.getPLevel().getVar() != null) {
        if (pVar != null && pVar != sort.getPLevel().getVar()) {
          return null;
        }
        if (pVar == null) {
          pVar = sort.getPLevel().getVar();
        }
      }
      if (sort.getHLevel().getVar() != null) {
        if (hVar != null && hVar != sort.getHLevel().getVar()) {
          return null;
        }
        if (hVar == null) {
          hVar = sort.getHLevel().getVar();
        }
      }
    }

    if (sorts.isEmpty()) {
      return Sort.PROP;
    } else {
      Sort resultSort = sorts.get(0);
      for (int i = 1; i < sorts.size(); i++) {
        resultSort = resultSort.max(sorts.get(i));
      }
      return resultSort;
    }
  }

  private Sort generateUpperBound(List<Sort> sorts, Abstract.SourceNode sourceNode) {
    Sort resultSort = generateUniqueUpperBound(sorts);
    if (resultSort != null) {
      return resultSort;
    }

    Sort sortResult = Sort.generateInferVars(myEquations, sourceNode);
    for (Sort sort : sorts) {
      myEquations.add(sort.getPLevel(), sortResult.getPLevel(), Equations.CMP.LE, sourceNode);
      myEquations.add(sort.getHLevel(), sortResult.getHLevel(), Equations.CMP.LE, sourceNode);
    }
    return sortResult;
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, ExpectedType expectedType) {
    Abstract.Expression expr1 = expr.getExpression();
    Result exprResult = checkExpr(expr1, null);
    if (exprResult == null) return null;
    exprResult.type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (!exprResult.type.isInstance(SigmaExpression.class)) {
      LocalTypeCheckingError error = new TypeMismatchError(text("A sigma type"), termDoc(exprResult.type), (Concrete.SourceNode) expr1);
      expr.setWellTyped(myContext, new ErrorExpression(null, error));
      myErrorReporter.report(error);
      return null;
    }

    DependentLink sigmaParams = exprResult.type.cast(SigmaExpression.class).getParameters();
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Index " + (expr.getField() + 1) + " is out of range", (Concrete.SourceNode) expr);
      expr.setWellTyped(myContext, new ErrorExpression(null, error));
      myErrorReporter.report(error);
      return null;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    for (int i = 0; sigmaParams != fieldLink; sigmaParams = sigmaParams.getNext(), i++) {
      substitution.add(sigmaParams, new ProjExpression(exprResult.expression, i));
    }

    exprResult.expression = new ProjExpression(exprResult.expression, expr.getField());
    exprResult.type = fieldLink.getTypeExpr().subst(substitution);
    return checkResult(expectedType, exprResult, expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, ExpectedType expectedType) {
    // Typecheck the base class

    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result typeCheckedBaseClass = checkExpr(baseClassExpr, null);
    if (typeCheckedBaseClass == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = typeCheckedBaseClass.expression.normalize(NormalizeVisitor.Mode.WHNF);
    if (!normalizedBaseClassExpr.isInstance(ClassCallExpression.class)) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a class", (Concrete.SourceNode) baseClassExpr);
      expr.setWellTyped(myContext, new ErrorExpression(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }
    ClassCallExpression classCallExpr = normalizedBaseClassExpr.cast(ClassCallExpression.class);
    ClassDefinition baseClass = classCallExpr.getDefinition();

    // Check for already implemented fields

    Map<ClassField, Abstract.ClassFieldImpl> classFieldMap = new HashMap<>();
    List<Abstract.ClassField> alreadyImplementedFields = new ArrayList<>();
    Abstract.SourceNode alreadyImplementedSourceNode = null;
    for (Abstract.ClassFieldImpl statement : expr.getStatements()) {
      ClassField field = (ClassField) myState.getTypechecked(statement.getImplementedField());
      if (baseClass.isImplemented(field) || classFieldMap.containsKey(field)) {
        alreadyImplementedFields.add(field.getAbstractDefinition());
        alreadyImplementedSourceNode = statement;
      } else {
        classFieldMap.put(field, statement);
      }
    }

    if (!alreadyImplementedFields.isEmpty()) {
      myErrorReporter.report(new FieldsImplementationError(true, alreadyImplementedFields, alreadyImplementedFields.size() > 1 ? (Concrete.SourceNode) expr : (Concrete.SourceNode) alreadyImplementedSourceNode));
    }

    // Typecheck statements

    Map<ClassField, Expression> fieldSet = new HashMap<>(classCallExpr.getImplementedHere());
    ClassCallExpression resultClassCall = new ClassCallExpression(baseClass, classCallExpr.getSortArgument(), fieldSet, Sort.PROP);

    if (!classFieldMap.isEmpty()) {
      Set<ClassField> notImplementedFields = new HashSet<>();
      for (ClassField field : baseClass.getFields()) {
        if (resultClassCall.isImplemented(field)) {
          continue;
        }

        Abstract.ClassFieldImpl impl = classFieldMap.get(field);
        if (impl != null) {
          boolean ok = true;
          if (!notImplementedFields.isEmpty()) {
            ClassField found = (ClassField) FindDefCallVisitor.findDefinition(field.getBaseType(resultClassCall.getSortArgument()), notImplementedFields);
            if (found != null) {
              ok = false;
              myErrorReporter.report(new FieldsImplementationError(false, Collections.singletonList(found.getAbstractDefinition()), (Concrete.SourceNode) impl));
            }
          }
          if (ok) {
            fieldSet.put(field, typecheckImplementation(field, impl.getImplementation(), resultClassCall));
            classFieldMap.remove(field);
            if (classFieldMap.isEmpty()) {
              break;
            }
          }
        } else {
          notImplementedFields.add(field);
        }
      }
    }

    // Calculate the sort of the expression

    DependentLink thisParam = ExpressionFactory.parameter("\\this", resultClassCall);
    List<Sort> sorts = new ArrayList<>();
    for (ClassField field : classCallExpr.getDefinition().getFields()) {
      if (resultClassCall.isImplemented(field)) continue;
      Expression baseType = field.getBaseType(classCallExpr.getSortArgument());
      if (baseType.isInstance(ErrorExpression.class)) continue;
      sorts.add(getSortOf(baseType.subst(field.getThisParameter(), new ReferenceExpression(thisParam)).normalize(NormalizeVisitor.Mode.WHNF).getType()));
    }

    resultClassCall = new ClassCallExpression(baseClass, classCallExpr.getSortArgument(), fieldSet, generateUpperBound(sorts, expr));
    return checkResult(expectedType, new Result(resultClassCall, new UniverseExpression(resultClassCall.getSort())), expr);
  }

  public Expression typecheckImplementation(ClassField field, Abstract.Expression implBody, ClassCallExpression fieldSetClass) {
    CheckTypeVisitor.Result result = checkExpr(implBody, field.getBaseType(fieldSetClass.getSortArgument()).subst(field.getThisParameter(), new NewExpression(fieldSetClass)));
    return result != null ? result.expression : new ErrorExpression(null, null);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, ExpectedType expectedType) {
    Result exprResult = checkExpr(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.checkedCast(ClassCallExpression.class);
    if (classCallExpr == null) {
      classCallExpr = normExpr.isInstance(ErrorExpression.class) ? normExpr.cast(ErrorExpression.class).getExpression().normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class) : null;
      if (classCallExpr == null) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a class", (Concrete.SourceNode) expr.getExpression());
        expr.setWellTyped(myContext, new ErrorExpression(normExpr, error));
        myErrorReporter.report(error);
        return null;
      } else {
        exprResult.expression = new ErrorExpression(new NewExpression(classCallExpr), normExpr.cast(ErrorExpression.class).getError());
        exprResult.type = normExpr;
        return exprResult;
      }
    }

    if (checkAllImplemented(classCallExpr, expr)) {
      exprResult.expression = new NewExpression(classCallExpr);
      exprResult.type = normExpr;
      return checkResult(expectedType, exprResult, expr);
    } else {
      return null;
    }
  }

  public boolean checkAllImplemented(ClassCallExpression classCall, Abstract.Expression expr) {
    int notImplemented = classCall.getDefinition().getFields().size() - classCall.getDefinition().getImplemented().size() - classCall.getImplementedHere().size();
    if (notImplemented == 0) {
      return true;
    } else {
      List<Abstract.ClassField> fields = new ArrayList<>(notImplemented);
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (!classCall.isImplemented(field)) {
          fields.add(field.getAbstractDefinition());
        }
      }
      LocalTypeCheckingError error = new FieldsImplementationError(false, fields, (Concrete.SourceNode) expr);
      expr.setWellTyped(myContext, new ErrorExpression(null, error));
      myErrorReporter.report(error);
      return false;
    }
  }

  private Result typecheckLetClause(List<? extends Abstract.Parameter> parameters, Abstract.LetClause letClause, int argIndex) {
    if (parameters.isEmpty()) {
      Abstract.Expression letResult = letClause.getResultType();
      if (letResult != null) {
        Type type = checkType(letResult);
        if (type == null) return null;
        return checkExpr(letClause.getTerm(), type.getExpr());
      } else {
        return checkExpr(letClause.getTerm(), null);
      }
    }

    Abstract.Parameter param = parameters.get(0);
    if (param instanceof Abstract.NameParameter) {
      TypedSingleDependentLink link = visitNameParameter((Abstract.NameParameter) param, argIndex, letClause);
      Result bodyResult = typecheckLetClause(parameters.subList(1, parameters.size()), letClause, argIndex + 1);
      if (bodyResult == null) return null;
      Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType()), myEquations, letClause);
      return new Result(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
    } else if (param instanceof Abstract.TypeParameter) {
      int namesCount = param instanceof Abstract.TelescopeParameter ? ((Abstract.TelescopeParameter) param).getReferableList().size() : 1;
      SingleDependentLink link = visitTypeParameter((Abstract.TypeParameter) param);
      if (link == null) {
        return null;
      }

      Result bodyResult = typecheckLetClause(parameters.subList(1, parameters.size()), letClause, argIndex + namesCount);
      if (bodyResult == null) return null;
      Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType()), myEquations, letClause);
      return new Result(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
    } else {
      throw new IllegalStateException();
    }
  }

  private LetClause typecheckLetClause(Abstract.LetClause clause) {
    LetClause letResult;
    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myContext)) {
      Result result = typecheckLetClause(clause.getParameters(), clause, 1);
      if (result == null) {
        return null;
      }
      letResult = new LetClause(clause.getName(), result.expression);
    }
    myContext.put(clause, letResult);
    return letResult;
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, ExpectedType expectedType) {
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      List<? extends Abstract.LetClause> abstractClauses = expr.getClauses();
      List<LetClause> clauses = new ArrayList<>(abstractClauses.size());
      for (Abstract.LetClause clause : abstractClauses) {
        LetClause letClause = typecheckLetClause(clause);
        if (letClause == null) {
          return null;
        }
        myContext.put(clause, letClause);
        clauses.add(letClause);
      }

      Result result = checkExpr(expr.getExpression(), expectedType);
      if (result == null) {
        return null;
      }

      ExprSubstitution substitution = new ExprSubstitution();
      for (LetClause clause : clauses) {
        substitution.add(clause, clause.getExpression());
      }
      return new Result(new LetExpression(clauses, result.expression), result.type.subst(substitution));
    }
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, ExpectedType expectedType) {
    int number = expr.getNumber();
    Expression expression = ExpressionFactory.Zero();
    for (int i = 0; i < number; ++i) {
      expression = ExpressionFactory.Suc(expression);
    }
    return checkResult(expectedType, new Result(expression, ExpressionFactory.Nat()), expr);
  }
}
