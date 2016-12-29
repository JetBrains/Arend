package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.ExpressionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.LambdaInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.PiTypeOmega;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.*;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
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
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.TwoStageEquations;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.ClassViewInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.EmptyInstancePool;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.expression;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Type, CheckTypeVisitor.Result> {
  private final TypecheckerState myState;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private ClassDefinition myThisClass;
  private Expression myThisExpr;
  private final List<Binding> myContext;
  private final List<LevelBinding> myLvlContext;
  private final LocalErrorReporter myErrorReporter;
  private final TypeCheckingDefCall myTypeCheckingDefCall;
  private final TypeCheckingElim myTypeCheckingElim;
  private final ImplicitArgsInference myArgsInference;
  private final Equations myEquations;
  private ClassViewInstancePool myClassViewInstancePool;

  public static class PreResult {
    private Expression expression;
    private TypeMax type;
    private List<DependentLink> parameters = new ArrayList<>();
    private List<Level> levels = new ArrayList<>();

    public PreResult(Expression expression, TypeMax type) {
      this.expression = expression;
      if (type != null) {
        List<DependentLink> params = new ArrayList<>();
        this.type = type.getPiParameters(params, true, false);
        this.parameters.addAll(params);
      }
    }

    public PreResult(Expression expression, TypeMax type, List<DependentLink> parameters) {
      this(expression, type);
      List<DependentLink> params = new ArrayList<>(parameters);
      params.addAll(this.parameters);
      this.parameters = params;
    }

    public PreResult(Expression expression, TypeMax type, DependentLink parameters) {
      this(expression, type, DependentLink.Helper.toList(parameters));
    }

    public Expression getExpression() {
      return expression;
    }

    public TypeMax getAtomicType() {
      return type;
    }

    public List<DependentLink> getParameters() {
      return parameters;
    }

    public List<Level> getLevels() { return levels; }

    public void reset(Expression expression) {
      this.expression = expression;
    }

    public void reset(Expression expression, TypeMax type) {
      PreResult result = new PreResult(expression, type);
      this.expression = result.expression;
      this.type = result.type;
      this.parameters = result.parameters;
    }

    public int getNumberOfImplicitParameters() {
      int number = 0;
      for (DependentLink param : parameters) {
        if (param.isExplicit()) {
          break;
        }
        number++;
      }
      return number;
    }

    public void applyThis(Expression thisExpr) {
      assert expression.toDefCall() != null && !parameters.isEmpty();
      expression = expression.toDefCall().applyThis(thisExpr);
      ExprSubstitution subst = DependentLink.Helper.toSubstitution(parameters.get(0), Collections.singletonList(thisExpr));
      parameters = DependentLink.Helper.subst(parameters.subList(1, parameters.size()), subst, new LevelSubstitution());
      type = type.subst(subst, new LevelSubstitution());
    }

    public void applyLevels(List<Level> levels) {
      this.levels.addAll(levels);
    }

    public void applyExpressions(List<? extends Expression> expressions) {
      expression = expression.addArguments(expressions);
      ExprSubstitution subst = new ExprSubstitution();
      int size = Math.min(expressions.size(), parameters.size());
      int i = 0;
      for (; i < size; ++i) {
        subst.add(parameters.get(i), expressions.get(i));
      }
      parameters = DependentLink.Helper.subst(parameters.subList(i, parameters.size()), subst, new LevelSubstitution());
      type = type.subst(subst, new LevelSubstitution());
      List<DependentLink> newParams = new ArrayList<>();
      type = type.getPiParameters(newParams, true, false);
      parameters.addAll(newParams);
    }

    public PreResult subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
      List<DependentLink> params = DependentLink.Helper.subst(parameters, exprSubst, levelSubst);
      return new PreResult(expression.subst(exprSubst, levelSubst), type.subst(exprSubst, levelSubst), params);
    }
  }

  public static class Result extends PreResult {
    public Result(Expression expression, TypeMax type) {
      super(expression, type);
    }

    public TypeMax getType() {
      return getAtomicType().fromPiParameters(getParameters());
    }
  }

  private CheckTypeVisitor(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ClassDefinition thisClass, Expression thisExpr, List<Binding> localContext, List<LevelBinding> lvlContext, LocalErrorReporter errorReporter, ClassViewInstancePool pool) {
    myState = state;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myContext = localContext;
    myLvlContext = lvlContext;
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
    private final List<LevelBinding> myLvlContext;
    private final LocalErrorReporter myErrorReporter;
    private ClassViewInstancePool myPool;
    private ClassDefinition myThisClass;
    private Expression myThisExpr;
    private StaticNamespaceProvider myStaticNsProvider;
    private DynamicNamespaceProvider myDynamicNsProvider;

    public Builder(TypecheckerState typecheckerState, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, List<Binding> localContext, List<LevelBinding> lvlContext, LocalErrorReporter errorReporter) {
      this.myTypecheckerState = typecheckerState;
      myStaticNsProvider = staticNsProvider;
      myDynamicNsProvider = dynamicNsProvider;
      myLocalContext = localContext;
      myLvlContext = lvlContext;
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
      return new CheckTypeVisitor(myTypecheckerState, myStaticNsProvider, myDynamicNsProvider, myThisClass, myThisExpr, myLocalContext, myLvlContext, myErrorReporter, myPool);
    }
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

  public List<LevelBinding> getLevelContext() {
    return myLvlContext;
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
      expression.setWellTyped(myContext, result.getExpression());
      return result;
    }

    if (compare(result, expectedType, expression)) {
      expression.setWellTyped(myContext, result.getExpression());
      return result;
    } else {
      return null;
    }
  }

  public boolean compare(Result result, Type expectedType, Abstract.Expression expr) {
    if (result.getType().isLessOrEquals(expectedType.normalize(NormalizeVisitor.Mode.NF), myEquations, expr)) {
        if (expectedType instanceof Expression) {
      //    Expression exprType = (Expression)expectedType;
          result.reset(new OfTypeExpression(result.getExpression(), expectedType), expectedType);
        }
      return true;
    }

    LocalTypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), result.getType().normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
    expr.setWellTyped(myContext, ExpressionFactory.Error(result.getExpression(), error));
    myErrorReporter.report(error);
    return false;
  }

  public Result checkResultImplicit(Type expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(myContext, result.getExpression());
      return result;
    }
    return myArgsInference.inferTail(result, expectedType, expression);
  }

  public TypeMax checkFunOrDataType(Abstract.Expression typeExpr) {
    TypeMax type = typeMax(typeExpr, false);
    if (type == null) return null;
    LevelSubstitution levelSubst = myEquations.solve(typeExpr);
    LocalErrorReporterCounter counter = new LocalErrorReporterCounter(myErrorReporter);
    return type.subst(new ExprSubstitution(), levelSubst).strip(new HashSet<>(myContext), counter);
  }

  public Type checkParamType(Abstract.Expression typeExpr) {
    Type type = (Type)typeMax(typeExpr, true);
    if (type == null) return null;
    LevelSubstitution levelSubst = myEquations.solve(typeExpr);
    LocalErrorReporterCounter counter = new LocalErrorReporterCounter(myErrorReporter);
    return type.subst(new ExprSubstitution(), levelSubst).strip(new HashSet<>(myContext), counter);
  }

  private TypeMax typeMax(Abstract.Expression type, boolean onlyOmegaAllowed) {
    if (type instanceof Abstract.PiExpression) {
      Abstract.PiExpression piType = (Abstract.PiExpression) type;
      DependentLink args = visitArguments(piType.getArguments(), type);
      if (args == null) return null;
      try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
        myContext.addAll(DependentLink.Helper.toContext(args));

        List<DependentLink> allArgs = DependentLink.Helper.toList(args);
        TypeMax codomain = typeMax(piType.getCodomain(), onlyOmegaAllowed);
        if (codomain == null) return null;
        codomain = codomain.getPiParameters(allArgs, true, false);
        ExprSubstitution exprSubst = new ExprSubstitution();
        allArgs = DependentLink.Helper.toList(DependentLink.Helper.mergeList(allArgs, exprSubst));

        return codomain.subst(exprSubst, new LevelSubstitution()).fromPiParameters(allArgs);
      }
    } else if (type instanceof Abstract.PolyUniverseExpression) {
      SortMax sort = sortMax((Abstract.PolyUniverseExpression)type);
      if (sort == null) return null;
      if (onlyOmegaAllowed && sort.toSort() == null) return null;
      return sort.toType();
    }
    Result result = typeCheck(type, new PiTypeOmega());
    if (result == null) return null;
    return result.getExpression();
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
      result.reset(result.getExpression().subst(substitution), result.getType().subst(new ExprSubstitution(), substitution));
    }

    LocalErrorReporterCounter counter = new LocalErrorReporterCounter(myErrorReporter);
    Expression term = result.getExpression().strip(new HashSet<>(myContext), counter);
    TypeMax type = result.getType().strip(new HashSet<>(myContext), counter.getErrorsNumber() == 0 ? myErrorReporter : new DummyLocalErrorReporter());
    result.reset(term, type);
    return result;
  }

  private boolean compareExpressions(PreResult result, Expression expected, Expression actual, Abstract.Expression expr) {
    if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, expected.normalize(NormalizeVisitor.Mode.NF), actual.normalize(NormalizeVisitor.Mode.NF), expr)) {
      LocalTypeCheckingError error = new ExpressionMismatchError(expected.normalize(NormalizeVisitor.Mode.HUMAN_NF), actual.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(result.getExpression(), error));
      myErrorReporter.report(error);
      return false;
    }
    return true;
  }

  private boolean checkPath(PreResult result, Abstract.Expression expr) {
    ConCallExpression conExpr = result.getExpression().toConCall();
    if (conExpr != null && conExpr.getDefinition() == Prelude.PATH_CON) {
      if (conExpr.getDefCallArguments().isEmpty()) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Expected an argument for 'path'", expr);
        expr.setWellTyped(myContext, ExpressionFactory.Error(result.getExpression(), error));
        myErrorReporter.report(error);
        return false;
      }

      List<? extends Expression> args = conExpr.getDataTypeArguments();
      if (!compareExpressions(result, args.get(1), conExpr.getDefCallArguments().get(0).addArgument(ExpressionFactory.Left()), expr) ||
          !compareExpressions(result, args.get(2), conExpr.getDefCallArguments().get(0).addArgument(ExpressionFactory.Right()), expr)) {
        return false;
      }
    }
    return true;
  }

  private Result checkDefCall(PreResult result, Abstract.Expression expr) {
    if (result == null) {
      return null;
    }

    if (result.getExpression().toDefCall() == null || result.getParameters().isEmpty()) {
      return new Result(result.getExpression(), result.getAtomicType().fromPiParameters(result.getParameters()));
    }

    Expression expression = result.getExpression();
    ExprSubstitution allSubst = new ExprSubstitution();
    ExprSubstitution absSubst = new ExprSubstitution();
    List<DependentLink> defParams = new ArrayList<>();
    result.getExpression().toDefCall().getDefinition().getTypeWithParams(defParams, result.getExpression().toDefCall().getPolyArguments());
    int numDefCallParameters = defParams.size();
    int numDefCallArguments = expression.toDefCall().getDefCallArguments().size() + (expression.toConCall() != null ? expression.toConCall().getDataTypeArguments().size() : 0);
    int numAbsParams = numDefCallParameters - numDefCallArguments;
    assert result.getParameters().size() >= numAbsParams;
    DependentLink absParams = numAbsParams > 0 ? DependentLink.Helper.mergeList(result.getParameters().subList(0, numAbsParams), absSubst) : EmptyDependentLink.getInstance();
    DependentLink allParams = DependentLink.Helper.mergeList(result.getParameters(), allSubst);

    int argIndex = 0;
    for (DependentLink link = absParams; link.hasNext() && argIndex < numAbsParams; link = link.getNext(), ++argIndex) {
      if (link.getType().toExpression() == null) {
        InferenceLevelVariable pLvl = new InferenceLevelVariable("plvl-of-" + link.getName(), LevelVariable.LvlType.PLVL, expr);
        InferenceLevelVariable hLvl = new InferenceLevelVariable("hlvl-of-" + link.getName(), LevelVariable.LvlType.HLVL, expr);
        myEquations.addVariable(pLvl);
        myEquations.addVariable(hLvl);
        Expression type = ExpressionFactory.Universe(new Level(pLvl), new Level(hLvl));
        if (link.getType().getPiParameters().hasNext()) {
          type = Pi(link.getType().getPiParameters(), type);
        }
        link.setType(type);
      }
      expression = expression.addArgument(ExpressionFactory.Reference(link));
    }
    if (absParams.hasNext()) {
      expression = ExpressionFactory.Lam(absParams, expression.subst(absSubst));
    }

    return new Result(expression, result.getAtomicType().subst(allSubst, new LevelSubstitution()).fromPiParameters(DependentLink.Helper.toList(allParams)));
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Type expectedType) {
    PreResult result = myArgsInference.infer(expr, expectedType);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return checkResultImplicit(expectedType, checkDefCall(result, expr), expr);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Type expectedType) {
    PreResult result = myTypeCheckingDefCall.typeCheckDefCall(expr);

    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return checkResultImplicit(expectedType, checkDefCall(result, expr), expr);
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

    return new Result(ExpressionFactory.ClassCall((ClassDefinition) typeChecked, new LevelArguments()), new PiUniverseType(EmptyDependentLink.getInstance(), ((ClassDefinition) typeChecked).getSorts()));
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Type expectedType) {
    List<DependentLink> piParams = new ArrayList<>();
    Type expectedCodomain = expectedType == null ? null : expectedType.getPiParameters(piParams, true, false);
    LinkList list = new LinkList();
    DependentLink actualPiLink = null;
    Result result;
    ExprSubstitution piLamSubst = new ExprSubstitution();
    int piParamsIndex = 0;
    int argIndex = 1;

    Result bodyResult;
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        List<String> names;
        Expression argType = null;
        Abstract.Expression argAbsType = null;
        boolean isExplicit = argument.getExplicit();

        if (argument instanceof Abstract.NameArgument) {
          names = Collections.singletonList(((Abstract.NameArgument) argument).getName());
        } else if (argument instanceof Abstract.TypeArgument) {
          names = argument instanceof Abstract.TelescopeArgument ? ((Abstract.TelescopeArgument) argument).getNames() : Collections.<String>singletonList(null);
          argAbsType = ((Abstract.TypeArgument) argument).getType();
          Result argResult = typeCheck(argAbsType, new PiTypeOmega());
          if (argResult == null) return null;
          argType = argResult.getExpression();
        } else {
          throw new IllegalStateException();
        }

        DependentLink link = param(isExplicit, names, argType);
        list.append(link);

        for (String name : names) {
          if (piParamsIndex < piParams.size()) {
            DependentLink piLink = piParams.get(piParamsIndex++);
            if (piLink.isExplicit() != isExplicit) {
              myErrorReporter.report(new LocalTypeCheckingError(ordinal(argIndex) + " argument of the lambda should be " + (piLink.isExplicit() ? "explicit" : "implicit"), expr));
              link.setExplicit(piLink.isExplicit());
            }

            Expression piLinkType = piLink.getType().toExpression().subst(piLamSubst);
            if (argType != null) {
              if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, piLinkType.normalize(NormalizeVisitor.Mode.NF), argType.normalize(NormalizeVisitor.Mode.NF), argAbsType)) {
                LocalTypeCheckingError error = new TypeMismatchError(piLinkType.normalize(NormalizeVisitor.Mode.HUMAN_NF), argType.normalize(NormalizeVisitor.Mode.HUMAN_NF), argAbsType);
                myErrorReporter.report(error);
                return null;
              }
            } else {
              link.setType(piLinkType);
            }

            piLamSubst.add(piLink, ExpressionFactory.Reference(link));
          } else {
            if (argType == null) {
              InferenceLevelVariable pLvl = new InferenceLevelVariable("plvl-of-" + name, LevelVariable.LvlType.PLVL, expr);
              InferenceLevelVariable hLvl = new InferenceLevelVariable("hlvl-of-" + name, LevelVariable.LvlType.HLVL, expr);
              myEquations.addVariable(pLvl);
              myEquations.addVariable(hLvl);
              InferenceVariable inferenceVariable = new LambdaInferenceVariable("type-of-" + name, ExpressionFactory.Universe(new Level(pLvl), new Level(hLvl)), argIndex, expr, false);
              link.setType(new InferenceReferenceExpression(inferenceVariable, myEquations));
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

      Type expectedBodyType = null;
      if (actualPiLink == null && expectedCodomain != null) {
        expectedBodyType = expectedCodomain.fromPiParameters(piParams.subList(piParamsIndex, piParams.size())).subst(piLamSubst, new LevelSubstitution());
      }

      Abstract.Expression body = expr.getBody();
      bodyResult = typeCheck(body, expectedBodyType);
      if (bodyResult == null) return null;
      if (actualPiLink != null && expectedCodomain != null) {
        result = new Result(bodyResult.getExpression(), bodyResult.getType().addParameters(actualPiLink, true));
        if (!compare(result, expectedCodomain, body)) {
          return null;
        }
      }
    }

    return new Result(ExpressionFactory.Lam(list.getFirst(), bodyResult.getExpression()),
            bodyResult.getType().addParameters(list.getFirst(), true));
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Type expectedType) {
    DependentLink args = visitArguments(expr.getArguments(), expr);
    if (args == null || !args.hasNext()) return null;
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      myContext.addAll(DependentLink.Helper.toContext(args));
      Result result = typeCheck(expr.getCodomain(), new PiTypeOmega());
      if (result == null) return null;
      Expression piExpr = ExpressionFactory.Pi(args, result.getExpression());
      return checkResult(expectedType, new Result(piExpr, piExpr.getType()), expr);
    }
  }

  @Override
  public Result visitLvl(Abstract.LvlExpression expr, Type params) {
    throw new IllegalStateException();
  }

  public Level typeCheckLevel(Abstract.Expression expr, int minValue) {
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
      } else if (expr instanceof Abstract.DefCallExpression) {
        String name = ((Abstract.DefCallExpression)expr).getName();
        if (name.equals("inf")) {
          return Level.INFINITY;
        }
        if (!name.equals("suc")) {
          Variable var = new TypeCheckingDefCall(this).getLocalVar((Abstract.DefCallExpression) expr, myLvlContext);
          if (var == null) {
            return null;
          }
          return new Level((LevelBinding) var, num_sucs);
        }
      }
      error = new LocalTypeCheckingError("Invalid level expression", expr);
    }

    myErrorReporter.report(error);
    return null;
  }

  public SortMax sortMax(Abstract.PolyUniverseExpression expr) {
    LevelMax levelP = expr.getPLevel() == null ? LevelMax.INFINITY : new LevelMax();
    if (expr.getPLevel() != null) {
      for (Abstract.Expression maxArgExpr : expr.getPLevel()) {
        Level maxArg = typeCheckLevel(maxArgExpr, 0);
        if (maxArg == null) return null;
        levelP = levelP.max(maxArg);
      }
    }
    LevelMax levelH = expr.getHLevel() != Abstract.PolyUniverseExpression.NOT_TRUNCATED ? new LevelMax(new Level(expr.getHLevel() + 1)) : LevelMax.INFINITY;
    if (levelP == null) return null;
    return new SortMax(levelP, levelH);
  }

  @Override
  public Result visitPolyUniverse(Abstract.PolyUniverseExpression expr, Type expectedType) {
    if (expr.getPLevel() != null && expr.getPLevel().size() != 1) {
      myErrorReporter.report(new LocalTypeCheckingError("\'max\' can only be used in a result type", expr));
      return null;
    }
    Level pLevel = expr.getPLevel() == null ? Level.INFINITY : typeCheckLevel(expr.getPLevel().get(0), 0);
    Level hLevel = expr.getHLevel() != Abstract.PolyUniverseExpression.NOT_TRUNCATED ? new Level(expr.getHLevel() + 1) : Level.INFINITY;
    if (pLevel == null) return null;
    if (pLevel.isInfinity()) {
      myErrorReporter.report(new LocalTypeCheckingError("\\Type can only used as Pi codomain in definition parameters or result type", expr));
      return null;
    }
    UniverseExpression universe = ExpressionFactory.Universe(new Sort(pLevel, hLevel));
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getSort().succ())), expr);
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
    if (expectedType != null) {
      return new Result(new InferenceReferenceExpression(new ExpressionInferenceVariable(expectedType, expr), myEquations), expectedType);
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
    if (expectedType != null && expectedType.toExpression() != null) {
      expectedTypeNorm = expectedType.toExpression().normalize(NormalizeVisitor.Mode.WHNF);
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
        Result tupleResult = new Result(ExpressionFactory.Tuple(fields, expectedTypeSigma), expectedType);
        ExprSubstitution substitution = new ExprSubstitution();
        for (Abstract.Expression field : expr.getFields()) {
          Expression expType = sigmaParams.getType().toExpression().subst(substitution);
          Result result = typeCheck(field, expType);
          if (result == null) return null;
          fields.add(result.getExpression());
          substitution.add(sigmaParams, result.getExpression());

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
      Expression type = result.getType().toExpression();
      if (type == null) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer type of " + (i + 1) + "th field", expr.getFields().get(i));
        expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
        myErrorReporter.report(error);
        return null;
      }

      fields.add(result.getExpression());
      list.append(ExpressionFactory.param(type));
    }

    SigmaExpression type = ExpressionFactory.Sigma(list.getFirst());
    tupleResult = checkResult(expectedTypeNorm, new Result(ExpressionFactory.Tuple(fields, type), type), expr);
    return tupleResult;
  }

  public DependentLink visitArguments(List<? extends Abstract.TypeArgument> arguments, Abstract.Expression expr) {
    LinkList list = new LinkList();

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (Abstract.TypeArgument arg : arguments) {
        Result result = typeCheck(arg.getType(), new PiTypeOmega());
        if (result == null) return null;

        Expression paramType = result.getExpression();
        result.reset(paramType, paramType.getType());

        if (arg instanceof Abstract.TelescopeArgument) {
          DependentLink link = param(arg.getExplicit(), ((Abstract.TelescopeArgument) arg).getNames(), result.getExpression());
          list.append(link);
          myContext.addAll(DependentLink.Helper.toContext(link));
        } else {
          DependentLink link = param(arg.getExplicit(), (String) null, result.getExpression());
          list.append(link);
          myContext.add(link);
        }
      }
    }

    return list.getFirst();
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Type expectedType) {
    DependentLink args = visitArguments(expr.getArguments(), expr);
    if (args == null || !args.hasNext()) return null;
    Expression sigmaExpr = ExpressionFactory.Sigma(args);
    return checkResult(expectedType, new Result(sigmaExpr, sigmaExpr.getType()), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Type expectedType) {
    PreResult result = myArgsInference.infer(expr, expectedType);
    if (result == null) {
      return null;
    }
    return checkResultImplicit(expectedType, checkDefCall(result, expr), expr);
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

    Result caseResult = new Result(null, expectedType);
    LetClause letBinding = let(Abstract.CaseExpression.FUNCTION_NAME, EmptyDependentLink.getInstance(), (Expression) expectedType, (ElimTreeNode) null);
    List<? extends Abstract.Expression> expressions = expr.getExpressions();

    LinkList list = new LinkList();
    List<Expression> letArguments = new ArrayList<>(expressions.size());
    for (int i = 0; i < expressions.size(); i++) {
      Result exprResult = typeCheck(expressions.get(i), null);
      if (exprResult == null) return null;
      Expression type = exprResult.getType().toExpression();
      if (type == null) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer type of " + (i + 1) + "th expression", expressions.get(i));
        expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
        myErrorReporter.report(error);
        return null;
      }

      list.append(param(true, ExpressionFactory.vars(Abstract.CaseExpression.ARGUMENT_NAME + i), type));
      letArguments.add(exprResult.getExpression());
    }
    letBinding.setParameters(list.getFirst());

    ElimTreeNode elimTree = myTypeCheckingElim.typeCheckElim(expr, list.getFirst(), expectedType, true, false);
    if (elimTree == null) return null;
    letBinding.setElimTree(elimTree);

    caseResult.reset(ExpressionFactory.Let(ExpressionFactory.lets(letBinding), ExpressionFactory.Apps(ExpressionFactory.Reference(letBinding), letArguments)));
    expr.setWellTyped(myContext, caseResult.getExpression());
    return caseResult;
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Type expectedType) {
    Abstract.Expression expr1 = expr.getExpression();
    Result exprResult = typeCheck(expr1, null);
    if (exprResult == null) return null;
    SigmaExpression sigmaType = null;
    exprResult.reset(exprResult.getExpression(), exprResult.getType().normalize(NormalizeVisitor.Mode.WHNF));
    if (exprResult.getType() instanceof Expression) {
      sigmaType = ((Expression) exprResult.getType()).toSigma();
    }
    if (sigmaType == null) {
      LocalTypeCheckingError error = new TypeMismatchError(new StringPrettyPrintable("A sigma type"), exprResult.getType(), expr1);
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
      substitution.add(sigmaParams, ExpressionFactory.Proj(exprResult.getExpression(), i));
    }

    exprResult.reset(ExpressionFactory.Proj(exprResult.getExpression(), expr.getField()), fieldLink.getType().subst(substitution, new LevelSubstitution()));
    return checkResult(expectedType, exprResult, expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Type expectedType) {
    Abstract.Expression baseClassExpr = expr.getBaseClassExpression();
    Result typeCheckedBaseClass = typeCheck(baseClassExpr, null);
    if (typeCheckedBaseClass == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = typeCheckedBaseClass.getExpression().normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normalizedBaseClassExpr.toClassCall();
    if (classCallExpr == null) {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a class", baseClassExpr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassDefinition baseClass = classCallExpr.getDefinition();
    if (baseClass.hasErrors() == Definition.TypeCheckingStatus.HAS_ERRORS) {
      LocalTypeCheckingError error = new HasErrors(baseClass.getAbstractDefinition(), expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(classCallExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    FieldSet fieldSet = new FieldSet();
    Expression resultExpr = classCallExpr instanceof ClassViewCallExpression ? new ClassViewCallExpression(baseClass, classCallExpr.getPolyArguments(), fieldSet, ((ClassViewCallExpression) classCallExpr).getClassView()) : ExpressionFactory.ClassCall(baseClass, classCallExpr.getPolyArguments(), fieldSet);

    fieldSet.addFieldsFrom(classCallExpr.getFieldSet());
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : classCallExpr.getFieldSet().getImplemented()) {
      boolean ok = fieldSet.implementField(entry.getKey(), entry.getValue());
      assert ok;
    }

    Collection<? extends Abstract.ClassFieldImpl> statements = expr.getStatements();
    Map<ClassField, Abstract.ClassFieldImpl> classFieldMap = new HashMap<>();

    for (Abstract.ClassFieldImpl statement : statements) {
      Abstract.Definition implField = statement.getImplementedField();
      if (implField instanceof Abstract.ClassViewField) {
        implField = ((Abstract.ClassViewField) implField).getUnderlyingField();
      }
      Definition implementedDef = myState.getTypechecked(implField);
      if (!(implementedDef instanceof ClassField)) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("'" + implementedDef.getName() + "' is not a field", statement);
        if (resultExpr instanceof ClassCallExpression) {
          resultExpr = ExpressionFactory.Error(resultExpr, error);
        }
        myErrorReporter.report(error);
        continue;
      }

      ClassField field = (ClassField) implementedDef;
      if (fieldSet.isImplemented(field) || classFieldMap.containsKey(field)) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Field '" + field.getName() + "' is already implemented", statement);
        if (resultExpr instanceof ClassCallExpression) {
          resultExpr = ExpressionFactory.Error(resultExpr, error);
        }
        myErrorReporter.report(error);
        continue;
      }

      classFieldMap.put(field, statement);
    }

    if (!(resultExpr instanceof ClassCallExpression)) {
      return checkResult(expectedType, new Result(resultExpr, new PiUniverseType(EmptyDependentLink.getInstance(), new SortMax())), expr);
    }

    if (!classFieldMap.isEmpty()) {
      Set<? extends ClassField> fields = baseClass.getFieldSet().getFields();
      for (ClassField field : fields) {
        if (fieldSet.isImplemented(field)) {
          continue;
        }

        Abstract.ClassFieldImpl impl = classFieldMap.get(field);
        if (impl != null) {
          if (resultExpr instanceof ClassCallExpression) {
            implementField(fieldSet, field, impl.getImplementation(), this, (ClassCallExpression) resultExpr);
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

    Result result = new Result(resultExpr, new PiUniverseType(EmptyDependentLink.getInstance(), resultExpr instanceof ClassCallExpression ? ((ClassCallExpression) resultExpr).getSorts() : new SortMax()));
    return checkResult(expectedType, result, expr);
  }

  private CheckTypeVisitor.Result implementField(FieldSet fieldSet, ClassField field, Abstract.Expression implBody, CheckTypeVisitor visitor, ClassCallExpression fieldSetClass) {
    CheckTypeVisitor.Result result = visitor.typeCheck(implBody, field.getBaseType().subst(field.getThisParameter(), ExpressionFactory.New(fieldSetClass)));
    fieldSet.implementField(field, new FieldSet.Implementation(null, result != null ? result.getExpression() : ExpressionFactory.Error(null, null)));
    return result;
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Type expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.getExpression().normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.toClassCall();
    if (classCallExpr == null) {
      classCallExpr = normExpr.toError().getExpr().normalize(NormalizeVisitor.Mode.WHNF).toClassCall();
      if (classCallExpr == null) {
        LocalTypeCheckingError error = new LocalTypeCheckingError("Expected a class", expr.getExpression());
        expr.setWellTyped(myContext, ExpressionFactory.Error(normExpr, error));
        myErrorReporter.report(error);
        return null;
      } else {
        exprResult.reset(ExpressionFactory.Error(ExpressionFactory.New(classCallExpr), normExpr.toError().getError()), normExpr);
        return exprResult;
      }
    }

    int remaining = classCallExpr.getFieldSet().getFields().size() - classCallExpr.getFieldSet().getImplemented().size();

    if (remaining == 0) {
      exprResult.reset(ExpressionFactory.New(classCallExpr), normExpr);
      return checkResult(expectedType, exprResult, expr);
    } else {
      LocalTypeCheckingError error = new LocalTypeCheckingError("Class '" + classCallExpr.getDefinition().getName() + "' has " + remaining + " not implemented fields", expr);
      expr.setWellTyped(myContext, ExpressionFactory.Error(null, error));
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
          Result result = typeCheck(teleArg.getType(), new PiTypeOmega(EmptyDependentLink.getInstance()));
          if (result == null) return null;
          Expression argType = result.getExpression();
          links.append(param(teleArg.getExplicit(), teleArg.getNames(), argType));
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
        expectedType = result.getExpression();
      }

      if (clause.getTerm() instanceof Abstract.ElimExpression)  {
        myContext.subList(myContext.size() - DependentLink.Helper.size(links.getFirst()), myContext.size()).clear();
        elimTree = myTypeCheckingElim.typeCheckElim((Abstract.ElimExpression) clause.getTerm(), clause.getArrow() == Abstract.Definition.Arrow.LEFT ? links.getFirst() : null, expectedType, false, false);
        if (elimTree == null) return null;
        resultType = expectedType;
      } else {
        Result termResult = typeCheck(clause.getTerm(), expectedType);
        if (termResult == null) return null;
        Expression type = expectedType != null ? expectedType : termResult.getType().toExpression();
        if (type == null) {
          LocalTypeCheckingError error = new LocalTypeCheckingError("Cannot infer type of expression", clause.getTerm());
          clause.getTerm().setWellTyped(myContext, ExpressionFactory.Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        elimTree = ExpressionFactory.top(links.getFirst(), ExpressionFactory.leaf(clause.getArrow(), termResult.getExpression()));
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

      LetExpression letExpr = ExpressionFactory.Let(clauses, result.getExpression());
      return new Result(letExpr, letExpr.getType(result.getType()));
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
