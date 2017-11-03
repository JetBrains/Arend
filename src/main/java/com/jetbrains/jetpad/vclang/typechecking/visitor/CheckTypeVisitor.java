package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.ExpressionInferenceVariable;
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
import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.IncorrectExpressionException;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.WrongReferable;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteLevelExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.Prelude;
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
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.InstancePool;

import java.math.BigInteger;
import java.util.*;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.expression;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements ConcreteExpressionVisitor<ExpectedType, CheckTypeVisitor.Result>, ConcreteLevelExpressionVisitor<LevelVariable, Level> {
  private final TypecheckerState myState;
  private Map<Referable, Binding> myContext;
  private final Set<Binding> myFreeBindings;
  private boolean myHasErrors = false;
  private LocalErrorReporter myErrorReporter;
  private final TypeCheckingDefCall myTypeCheckingDefCall;
  private final ImplicitArgsInference myArgsInference;
  private final Equations myEquations;
  private InstancePool myInstancePool;

  public interface TResult {
    Result toResult(Equations equations);
    DependentLink getParameter();
    TResult applyExpression(Expression expression, LocalErrorReporter errorReporter, Concrete.SourceNode sourceNode);
    List<? extends DependentLink> getImplicitParameters();
  }

  public static class DefCallResult implements TResult {
    private final Concrete.ReferenceExpression myDefCall;
    private final Definition myDefinition;
    private final Sort mySortArgument;
    private final List<Expression> myArguments;
    private List<DependentLink> myParameters;
    private Expression myResultType;
    private final Expression myThisExpr;

    private DefCallResult(Concrete.ReferenceExpression defCall, Definition definition, Sort sortArgument, List<Expression> arguments, List<DependentLink> parameters, Expression resultType, Expression thisExpr) {
      myDefCall = defCall;
      myDefinition = definition;
      mySortArgument = sortArgument;
      myArguments = arguments;
      myParameters = parameters;
      myResultType = resultType;
      myThisExpr = thisExpr;
    }

    public static TResult makeTResult(Concrete.ReferenceExpression defCall, Definition definition, Sort sortArgument, Expression thisExpr) {
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
    public TResult applyExpression(Expression expression, LocalErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
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

    public Concrete.ReferenceExpression getDefCall() {
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
    public Result applyExpression(Expression expr, LocalErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
      expression = new AppExpression(expression, expr);
      Expression newType = type.applyExpression(expr);
      if (newType == null) {
        errorReporter.report(new TypecheckingError("Expected an expression of a pi type", sourceNode));
      } else {
        type = newType;
      }
      return this;
    }

    @Override
    public List<SingleDependentLink> getImplicitParameters() {
      List<SingleDependentLink> params = new ArrayList<>();
      type.getPiParameters(params, true);
      return params;
    }
  }

  private class MyErrorReporter implements LocalErrorReporter {
    private final LocalErrorReporter myErrorReporter;

    private MyErrorReporter(LocalErrorReporter errorReporter) {
      myErrorReporter = errorReporter;
    }

    @Override
    public void report(LocalError localError) {
      myHasErrors = true;
      myErrorReporter.report(localError);
    }

    @Override
    public void report(GeneralError error) {
      myHasErrors = true;
      myErrorReporter.report(error);
    }
  }

  public CheckTypeVisitor(TypecheckerState state, Map<Referable, Binding> localContext, LocalErrorReporter errorReporter, InstancePool pool) {
    myState = state;
    myContext = localContext;
    myFreeBindings = new HashSet<>();
    myErrorReporter = new MyErrorReporter(errorReporter);
    myTypeCheckingDefCall = new TypeCheckingDefCall(this);
    myArgsInference = new StdImplicitArgsInference(this);
    myEquations = new TwoStageEquations(this);
    myInstancePool = pool;
  }

  public void setThis(ClassDefinition thisClass, Binding thisBinding) {
    myTypeCheckingDefCall.setThis(thisClass, thisBinding);
  }

  public TypecheckerState getTypecheckingState() {
    return myState;
  }

  public TypeCheckingDefCall getTypeCheckingDefCall() {
    return myTypeCheckingDefCall;
  }

  public InstancePool getInstancePool() {
    return myInstancePool;
  }

  public void setInstancePool(InstancePool pool) {
    myInstancePool = pool;
  }

  public Map<Referable, Binding> getContext() {
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

  public void setErrorReporter(LocalErrorReporter errorReporter) {
    myErrorReporter = new MyErrorReporter(errorReporter);
  }

  public Equations getEquations() {
    return myEquations;
  }

  public boolean hasErrors() {
    return myHasErrors;
  }

  private static Sort getSortOf(Expression expr) {
    Sort sort = expr == null ? null : expr.toSort();
    if (sort == null) {
      assert expr != null && expr.isInstance(ErrorExpression.class);
      return Sort.PROP;
    } else {
      return sort;
    }
  }

  private Result checkResult(ExpectedType expectedType, Result result, Concrete.Expression expression) {
    if (result == null || expectedType == null || !(expectedType instanceof Expression)) {
      return result;
    } else {
      return compare(result, (Expression) expectedType, expression) ? result : null;
    }
  }

  public boolean compare(Result result, Expression expectedType, Concrete.Expression expr) {
    result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (result.type.isLessOrEquals(expectedType, myEquations, expr)) {
      result.expression = OfTypeExpression.make(result.expression, result.type, expectedType);
      return true;
    }

    if (!result.type.isInstance(ErrorExpression.class)) {
      myErrorReporter.report(new TypeMismatchError(termDoc(expectedType), termDoc(result.type), expr));
    }
    return false;
  }

  private Result tResultToResult(ExpectedType expectedType, TResult result, Concrete.Expression expr) {
    if (result != null && expectedType != null) {
      result = myArgsInference.inferTail(result, expectedType, expr);
    }
    return result == null ? null : checkResult(expectedType, result.toResult(myEquations), expr);
  }

  public Result checkExpr(Concrete.Expression expr, ExpectedType expectedType) {
    if (expr == null) {
      myErrorReporter.report(new TypecheckingError("Incomplete expression", null));
      return null;
    }

    try {
      return expr.accept(this, expectedType);
    } catch (IncorrectExpressionException e) {
      myErrorReporter.report(new TypecheckingError(e.getMessage(), expr));
      return null;
    }
  }

  public Result finalCheckExpr(Concrete.Expression expr, ExpectedType expectedType, boolean returnExpectedType) {
    if (!(expectedType instanceof Expression)) {
      returnExpectedType = false;
    }

    Result result = checkExpr(expr, expectedType);
    if (result == null && !returnExpectedType) return null;
    LevelSubstitution substitution = myEquations.solve(expr);
    if (returnExpectedType) {
      if (result == null) {
        result = new Result(null, null);
      }
      result.type = (Expression) expectedType;
    }
    if (!substitution.isEmpty()) {
      if (result.expression != null) {
        result.expression = result.expression.subst(substitution);
      }
      result.type = result.type.subst(new ExprSubstitution(), substitution);
    }

    LocalErrorReporterCounter counter = new LocalErrorReporterCounter(Error.Level.ERROR, myErrorReporter);
    if (result.expression != null) {
      result.expression = result.expression.strip(counter);
    }
    result.type = result.type.strip(counter.getErrorsNumber() == 0 ? myErrorReporter : DummyErrorReporter.INSTANCE);
    return result;
  }

  public Type checkType(Concrete.Expression expr, ExpectedType expectedType) {
    if (expr == null) {
      myErrorReporter.report(new TypecheckingError("Incomplete expression", null));
      return null;
    }

    Result result;
    try {
      result = expr.accept(this, expectedType);
    } catch (IncorrectExpressionException e) {
      myErrorReporter.report(new TypecheckingError(e.getMessage(), expr));
      return null;
    }
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
        if (!result.type.isInstance(ErrorExpression.class)) {
          myErrorReporter.report(new TypeMismatchError(text("a universe"), termDoc(result.type), expr));
        }
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

  public Type finalCheckType(Concrete.Expression expr, ExpectedType expectedType) {
    Type result = checkType(expr, expectedType);
    if (result == null) return null;
    return result.subst(new ExprSubstitution(), myEquations.solve(expr)).strip(myErrorReporter);
  }

  private boolean compareExpressions(boolean isLeft, Expression expected, Expression actual, Concrete.Expression expr) {
    if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, actual, expected, expr)) {
      myErrorReporter.report(new PathEndpointMismatchError(isLeft, expected, actual, expr));
      return false;
    }
    return true;
  }

  private boolean checkPath(TResult result, Concrete.Expression expr) {
    if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.PATH_CON) {
      myErrorReporter.report(new TypecheckingError("Expected an argument for 'path'", expr));
      return false;
    }
    if (result instanceof Result) {
      ConCallExpression conCall = ((Result) result).expression.checkedCast(ConCallExpression.class);
      if (conCall != null && conCall.getDefinition() == Prelude.PATH_CON) {
        if (!compareExpressions(true, conCall.getDataTypeArguments().get(1), new AppExpression(conCall.getDefCallArguments().get(0), ExpressionFactory.Left()), expr) ||
          !compareExpressions(false, conCall.getDataTypeArguments().get(2), new AppExpression(conCall.getDefCallArguments().get(0), ExpressionFactory.Right()), expr)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public Result visitApp(Concrete.AppExpression expr, ExpectedType expectedType) {
    TResult result = myArgsInference.infer(expr, expectedType);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  public CheckTypeVisitor.TResult getLocalVar(Concrete.ReferenceExpression expr) {
    if (expr.getReferent() instanceof UnresolvedReference || expr.getReferent() instanceof RedirectingReferable) {
      throw new IllegalStateException();
    }
    if (expr.getReferent() instanceof ErrorReference) {
      return null;
    }

    Binding def = myContext.get(expr.getReferent());
    if (def == null) {
      myErrorReporter.report(new IncorrectReferenceError(expr.getReferent(), expr));
      return null;
    }
    Expression type = def.getTypeExpr();
    if (type == null) {
      myErrorReporter.report(new ReferenceTypeError(expr.getReferent()));
      return null;
    } else {
      return new Result(new ReferenceExpression(def), type);
    }
  }

  @Override
  public Result visitReference(Concrete.ReferenceExpression expr, ExpectedType expectedType) {
    if (!(expr.getReferent() instanceof GlobalReferable) && (expr.getPLevel() != null || expr.getHLevel() != null)) {
      myErrorReporter.report(new TypecheckingError("Level specifications are allowed only after definitions", expr.getPLevel() != null ? expr.getPLevel() : expr.getHLevel()));
    }
    TResult result = expr.getReferent() instanceof GlobalReferable ? myTypeCheckingDefCall.typeCheckDefCall((GlobalReferable) expr.getReferent(), expr) : getLocalVar(expr);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  @Override
  public Result visitInferenceReference(Concrete.InferenceReferenceExpression expr, ExpectedType params) {
    return new Result(new InferenceReferenceExpression(expr.getVariable(), myEquations), expr.getVariable().getType());
  }

  private TypedSingleDependentLink visitNameParameter(Concrete.NameParameter param, int argIndex, Concrete.SourceNode sourceNode) {
    Referable referable = param.getReferable();
    String name = referable == null ? null : referable.textRepresentation();
    Sort sort = Sort.generateInferVars(myEquations, sourceNode);
    InferenceVariable inferenceVariable = new LambdaInferenceVariable(name == null ? "_" : "type-of-" + name, new UniverseExpression(sort), argIndex, sourceNode, false, getAllBindings());
    Expression argType = new InferenceReferenceExpression(inferenceVariable, myEquations);

    TypedSingleDependentLink link = new TypedSingleDependentLink(param.getExplicit(), name, new TypeExpression(argType, sort));
    if (referable != null) {
      myContext.put(referable, link);
    }
    return link;
  }

  private SingleDependentLink visitTypeParameter(Concrete.TypeParameter param) {
    Concrete.Expression paramType = param.getType();
    Type argResult = checkType(paramType, ExpectedType.OMEGA);
    if (argResult == null) return null;

    if (param instanceof Concrete.TelescopeParameter) {
      List<? extends Referable> referableList = ((Concrete.TelescopeParameter) param).getReferableList();
      List<String> names = new ArrayList<>(referableList.size());
      for (Referable referable : referableList) {
        names.add(referable == null ? null : referable.textRepresentation());
      }
      SingleDependentLink link = ExpressionFactory.singleParams(param.getExplicit(), names, argResult);
      int i = 0;
      for (SingleDependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
        if (referableList.get(i) != null) {
          myContext.put(referableList.get(i), link1);
        }
      }
      return link;
    } else {
      return ExpressionFactory.singleParams(param.getExplicit(), Collections.singletonList(null), argResult);
    }
  }

  private Result visitLam(List<? extends Concrete.Parameter> parameters, Concrete.LamExpression expr, Expression expectedType, int argIndex) {
    if (parameters.isEmpty()) {
      return checkExpr(expr.getBody(), expectedType);
    }

    Concrete.Parameter param = parameters.get(0);
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

    if (param instanceof Concrete.NameParameter) {
      if (expectedType == null || !expectedType.isInstance(PiExpression.class)) {
        TypedSingleDependentLink link = visitNameParameter((Concrete.NameParameter) param, argIndex, expr);
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
        Referable referable = ((Concrete.NameParameter) param).getReferable();
        SingleDependentLink piParams = piExpectedType.getParameters();
        if (piParams.isExplicit() && !param.getExplicit()) {
          myErrorReporter.report(new TypecheckingError(ordinal(argIndex) + " argument of the lambda is implicit, but the first parameter of the expected type is not", expr));
        }
        SingleDependentLink link = new TypedSingleDependentLink(piParams.isExplicit(), referable == null ? null : referable.textRepresentation(), piParams.getType());
        if (referable != null) {
          myContext.put(referable, link);
        } else {
          myFreeBindings.add(link); // TODO[references]
        }
        Expression codomain = piExpectedType.getCodomain().subst(piParams, new ReferenceExpression(link));
        Result bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, piParams.getNext().hasNext() ? new PiExpression(piExpectedType.getResultSort(), piParams.getNext(), codomain) : codomain, argIndex + 1);
        if (bodyResult == null) return null;
        Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType()), myEquations, expr);
        return new Result(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
      }
    } else if (param instanceof Concrete.TypeParameter) {
      SingleDependentLink link = visitTypeParameter((Concrete.TypeParameter) param);
      if (link == null) {
        return null;
      }

      SingleDependentLink actualLink = null;
      Expression expectedBodyType = null;
      int namesCount = param instanceof Concrete.TelescopeParameter ? ((Concrete.TelescopeParameter) param).getReferableList().size() : 1;
      if (expectedType != null) {
        Concrete.Expression paramType = ((Concrete.TypeParameter) param).getType();
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
            myErrorReporter.report(new TypecheckingError(ordinal(argIndex) + " argument of the lambda is implicit, but the first parameter of the expected type is not", expr));
          }
          if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, argExpr, argExpectedType, paramType)) {
            if (!argType.isInstance(ErrorExpression.class)) {
              myErrorReporter.report(new TypeMismatchError(termDoc(argExpectedType), termDoc(argType), paramType));
            }
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
  public Result visitLam(Concrete.LamExpression expr, ExpectedType expectedType) {
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      Result result = visitLam(expr.getParameters(), expr, expectedType instanceof Expression ? (Expression) expectedType : null, 1);
      if (result != null && expectedType != null && !(expectedType instanceof Expression)) {
        if (!result.type.isInstance(ErrorExpression.class)) {
          myErrorReporter.report(new TypeMismatchError(typeDoc(expectedType), termDoc(result.type), expr));
        }
        return null;
      }
      return result;
    }
  }

  @Override
  public Result visitPi(Concrete.PiExpression expr, ExpectedType expectedType) {
    List<SingleDependentLink> list = new ArrayList<>();
    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      for (Concrete.TypeParameter arg : expr.getParameters()) {
        Type result = checkType(arg.getType(), ExpectedType.OMEGA);
        if (result == null) return null;

        if (arg instanceof Concrete.TelescopeParameter) {
          List<? extends Referable> referableList = ((Concrete.TelescopeParameter) arg).getReferableList();
          List<String> names = new ArrayList<>(referableList.size());
          for (Referable referable : referableList) {
            names.add(referable == null ? null : referable.textRepresentation());
          }
          SingleDependentLink link = ExpressionFactory.singleParams(arg.getExplicit(), names, result);
          list.add(link);
          int i = 0;
          for (SingleDependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
            if (referableList.get(i) != null) {
              myContext.put(referableList.get(i), link1);
            }
          }
        } else {
          list.add(new TypedSingleDependentLink(arg.getExplicit(), null, result));
        }

        sorts.add(result.getSortOfType());
      }

      Type result = checkType(expr.getCodomain(), ExpectedType.OMEGA);
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
  public Result visitUniverse(Concrete.UniverseExpression expr, ExpectedType expectedType) {
    Level pLevel = expr.getPLevel() != null ? expr.getPLevel().accept(this, LevelVariable.PVAR) : new Level(LevelVariable.PVAR);
    Level hLevel = expr.getHLevel() != null ? expr.getHLevel().accept(this, LevelVariable.HVAR) : new Level(LevelVariable.HVAR);

    if (pLevel.isInfinity()) {
      myErrorReporter.report(new TypecheckingError("\\inf is not a correct p-level", expr));
      pLevel = new Level(LevelVariable.PVAR);
    }

    UniverseExpression universe = new UniverseExpression(new Sort(pLevel, hLevel));
    return checkResult(expectedType, new Result(universe, new UniverseExpression(universe.getSort().succ())), expr);
  }

  @Override
  public Level visitInf(Concrete.InfLevelExpression expr, LevelVariable param) {
    return Level.INFINITY;
  }

  @Override
  public Level visitLP(Concrete.PLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.PVAR) {
      myErrorReporter.report(new TypecheckingError("Expected " + base, expr));
    }
    return new Level(base);
  }

  @Override
  public Level visitLH(Concrete.HLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.HVAR) {
      myErrorReporter.report(new TypecheckingError("Expected " + base, expr));
    }
    return new Level(base);
  }

  @Override
  public Level visitNumber(Concrete.NumberLevelExpression expr, LevelVariable base) {
    return new Level(expr.getNumber());
  }

  @Override
  public Level visitSuc(Concrete.SucLevelExpression expr, LevelVariable base) {
    return expr.getExpression().accept(this, base).add(1);
  }

  @Override
  public Level visitMax(Concrete.MaxLevelExpression expr, LevelVariable base) {
    return expr.getLeft().accept(this, base).max(expr.getRight().accept(this, base));
  }

  @Override
  public Level visitVar(Concrete.InferVarLevelExpression expr, LevelVariable base) {
    myErrorReporter.report(new TypecheckingError("Cannot typecheck an inference variable", expr));
    return new Level(base);
  }

  @Override
  public Result visitGoal(Concrete.GoalExpression expr, ExpectedType expectedType) {
    List<Error> errors = Collections.emptyList();
    Result exprResult = null;
    if (expr.getExpression() != null) {
      LocalErrorReporter errorReporter = myErrorReporter;
      boolean hasErrors = myHasErrors;
      errors = new ArrayList<>();
      myErrorReporter = new ListLocalErrorReporter(errors);
      exprResult = checkExpr(expr.getExpression(), expectedType);
      myErrorReporter = errorReporter;
      myHasErrors = hasErrors;
    }

    TypecheckingError error = new GoalError(expr.getName(), myContext, expectedType, exprResult == null ? null : exprResult.type, errors, expr);
    myErrorReporter.report(error);
    Expression result = new ErrorExpression(null, error);
    return new Result(result, result);
  }

  @Override
  public Result visitInferHole(Concrete.InferHoleExpression expr, ExpectedType expectedType) {
    if (expectedType instanceof Expression) {
      return new Result(new InferenceReferenceExpression(new ExpressionInferenceVariable((Expression) expectedType, expr, getAllBindings()), myEquations), (Expression) expectedType);
    } else {
      myErrorReporter.report(new ArgInferenceError(expression(), expr, new Expression[0]));
      return null;
    }
  }

  @Override
  public Result visitTuple(Concrete.TupleExpression expr, ExpectedType expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType instanceof Expression) {
      expectedTypeNorm = ((Expression) expectedType).normalize(NormalizeVisitor.Mode.WHNF);
      if (expectedTypeNorm.isInstance(SigmaExpression.class)) {
        SigmaExpression expectedTypeSigma = expectedTypeNorm.cast(SigmaExpression.class);
        DependentLink sigmaParams = expectedTypeSigma.getParameters();
        int sigmaParamsSize = DependentLink.Helper.size(sigmaParams);

        if (expr.getFields().size() != sigmaParamsSize) {
          myErrorReporter.report(new TypecheckingError("Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), expr));
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Result tupleResult = new Result(new TupleExpression(fields, expectedTypeSigma), (Expression) expectedType);
        ExprSubstitution substitution = new ExprSubstitution();
        for (Concrete.Expression field : expr.getFields()) {
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

  private DependentLink visitParameters(List<? extends Concrete.TypeParameter> parameters, List<Sort> resultSorts) {
    LinkList list = new LinkList();

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      for (Concrete.TypeParameter arg : parameters) {
        Type result = checkType(arg.getType(), ExpectedType.OMEGA);
        if (result == null) return null;

        if (arg instanceof Concrete.TelescopeParameter) {
          List<? extends Referable> referableList = ((Concrete.TelescopeParameter) arg).getReferableList();
          List<String> names = new ArrayList<>(referableList.size());
          for (Referable referable : referableList) {
            names.add(referable == null ? null : referable.textRepresentation());
          }
          DependentLink link = ExpressionFactory.parameter(arg.getExplicit(), names, result);
          list.append(link);
          int i = 0;
          for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
            if (referableList.get(i) != null) {
              myContext.put(referableList.get(i), link1);
            }
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
  public Result visitSigma(Concrete.SigmaExpression expr, ExpectedType expectedType) {
    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());
    DependentLink args = visitParameters(expr.getParameters(), sorts);
    if (args == null || !args.hasNext()) return null;
    Sort sort = generateUpperBound(sorts, expr);
    return checkResult(expectedType, new Result(new SigmaExpression(sort, args), new UniverseExpression(sort)), expr);
  }

  @Override
  public Result visitBinOp(Concrete.BinOpExpression expr, ExpectedType expectedType) {
    return tResultToResult(expectedType, myArgsInference.infer(expr, expectedType), expr);
  }

  @Override
  public Result visitBinOpSequence(Concrete.BinOpSequenceExpression expr, ExpectedType expectedType) {
    assert expr.getSequence().isEmpty();
    return checkExpr(expr.getLeft(), expectedType);
  }

  @Override
  public Result visitCase(Concrete.CaseExpression expr, ExpectedType expectedType) {
    if (expectedType == null) {
      myErrorReporter.report(new TypecheckingError("Cannot infer the result type", expr));
      return null;
    }
    if (!(expectedType instanceof Expression)) {
      expectedType = new UniverseExpression(Sort.generateInferVars(myEquations, expr));
    }

    List<? extends Concrete.Expression> abstractExprs = expr.getExpressions();
    LinkList list = new LinkList();
    List<Expression> expressions = new ArrayList<>(abstractExprs.size());
    for (Concrete.Expression expression : abstractExprs) {
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
    return new Result(new CaseExpression(list.getFirst(), (Expression) expectedType, elimTree, expressions), (Expression) expectedType);
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

  private Sort generateUpperBound(List<Sort> sorts, Concrete.SourceNode sourceNode) {
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
  public Result visitProj(Concrete.ProjExpression expr, ExpectedType expectedType) {
    Concrete.Expression expr1 = expr.getExpression();
    Result exprResult = checkExpr(expr1, null);
    if (exprResult == null) return null;
    exprResult.type = exprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (!exprResult.type.isInstance(SigmaExpression.class)) {
      if (!exprResult.type.isInstance(ErrorExpression.class)) {
        myErrorReporter.report(new TypeMismatchError(text("A sigma type"), termDoc(exprResult.type), expr1));
      }
      return null;
    }

    DependentLink sigmaParams = exprResult.type.cast(SigmaExpression.class).getParameters();
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      myErrorReporter.report(new TypecheckingError("Index " + (expr.getField() + 1) + " is out of range", expr));
      return null;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    for (int i = 0; sigmaParams != fieldLink; sigmaParams = sigmaParams.getNext(), i++) {
      substitution.add(sigmaParams, ProjExpression.make(exprResult.expression, i));
    }

    exprResult.expression = ProjExpression.make(exprResult.expression, expr.getField());
    exprResult.type = fieldLink.getTypeExpr().subst(substitution);
    return checkResult(expectedType, exprResult, expr);
  }

  public ClassField referableToClassField(Referable referable, Concrete.SourceNode sourceNode) {
    Definition definition = referable instanceof GlobalReferable ? myState.getTypechecked((GlobalReferable) referable) : null;
    if (definition instanceof ClassField) {
      return (ClassField) definition;
    }

    myErrorReporter.report(definition == null ? new NotInScopeError(sourceNode.getData(), null, referable.textRepresentation()) : new WrongReferable("Expected a class field", referable, sourceNode));
    return null;
  }

  @Override
  public Result visitClassExt(Concrete.ClassExtExpression expr, ExpectedType expectedType) {
    // Typecheck the base class

    Concrete.Expression baseClassExpr = expr.getBaseClassExpression();
    Result typeCheckedBaseClass = checkExpr(baseClassExpr, null);
    if (typeCheckedBaseClass == null) {
      return null;
    }
    Expression normalizedBaseClassExpr = typeCheckedBaseClass.expression.normalize(NormalizeVisitor.Mode.WHNF);
    if (!normalizedBaseClassExpr.isInstance(ClassCallExpression.class)) {
      myErrorReporter.report(new TypecheckingError("Expected a class", baseClassExpr));
      return null;
    }
    ClassCallExpression classCallExpr = normalizedBaseClassExpr.cast(ClassCallExpression.class);
    ClassDefinition baseClass = classCallExpr.getDefinition();

    // Check for already implemented fields

    Map<ClassField, Concrete.ClassFieldImpl> classFieldMap = new HashMap<>();
    List<GlobalReferable> alreadyImplementedFields = new ArrayList<>();
    Concrete.SourceNode alreadyImplementedSourceNode = null;
    for (Concrete.ClassFieldImpl statement : expr.getStatements()) {
      ClassField field = referableToClassField(statement.getImplementedField(), statement);
      if (field == null) {
        continue;
      }
      if (baseClass.isImplemented(field) || classFieldMap.containsKey(field)) {
        alreadyImplementedFields.add(field.getReferable());
        alreadyImplementedSourceNode = statement;
      } else {
        classFieldMap.put(field, statement);
      }
    }

    if (!alreadyImplementedFields.isEmpty()) {
      myErrorReporter.report(new FieldsImplementationError(true, alreadyImplementedFields, alreadyImplementedFields.size() > 1 ? expr : alreadyImplementedSourceNode));
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

        Concrete.ClassFieldImpl impl = classFieldMap.get(field);
        if (impl != null) {
          boolean ok = true;
          if (!notImplementedFields.isEmpty()) {
            ClassField found = (ClassField) FindDefCallVisitor.findDefinition(field.getBaseType(resultClassCall.getSortArgument()), notImplementedFields);
            if (found != null) {
              ok = false;
              myErrorReporter.report(new FieldsImplementationError(false, Collections.singletonList(found.getReferable()), impl));
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

  public Expression typecheckImplementation(ClassField field, Concrete.Expression implBody, ClassCallExpression fieldSetClass) {
    CheckTypeVisitor.Result result = checkExpr(implBody, field.getBaseType(fieldSetClass.getSortArgument()).subst(field.getThisParameter(), new NewExpression(fieldSetClass)));
    return result != null ? result.expression : new ErrorExpression(null, null);
  }

  @Override
  public Result visitNew(Concrete.NewExpression expr, ExpectedType expectedType) {
    Result exprResult = checkExpr(expr.getExpression(), null);
    if (exprResult == null) return null;
    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.checkedCast(ClassCallExpression.class);
    if (classCallExpr == null) {
      classCallExpr = normExpr.isInstance(ErrorExpression.class) ? normExpr.cast(ErrorExpression.class).getExpression().normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class) : null;
      if (classCallExpr == null) {
        myErrorReporter.report(new TypecheckingError("Expected a class", expr.getExpression()));
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

  private boolean checkAllImplemented(ClassCallExpression classCall, Concrete.Expression expr) {
    int notImplemented = classCall.getDefinition().getFields().size() - classCall.getDefinition().getImplemented().size() - classCall.getImplementedHere().size();
    if (notImplemented == 0) {
      return true;
    } else {
      List<GlobalReferable> fields = new ArrayList<>(notImplemented);
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (!classCall.isImplemented(field)) {
          fields.add(field.getReferable());
        }
      }
      myErrorReporter.report(new FieldsImplementationError(false, fields, expr));
      return false;
    }
  }

  private Result typecheckLetClause(List<? extends Concrete.Parameter> parameters, Concrete.LetClause letClause, int argIndex) {
    if (parameters.isEmpty()) {
      Concrete.Expression letResult = letClause.getResultType();
      if (letResult != null) {
        Type type = checkType(letResult, ExpectedType.OMEGA);
        if (type == null) return null;
        return checkExpr(letClause.getTerm(), type.getExpr());
      } else {
        return checkExpr(letClause.getTerm(), null);
      }
    }

    Concrete.Parameter param = parameters.get(0);
    if (param instanceof Concrete.NameParameter) {
      TypedSingleDependentLink link = visitNameParameter((Concrete.NameParameter) param, argIndex, letClause);
      Result bodyResult = typecheckLetClause(parameters.subList(1, parameters.size()), letClause, argIndex + 1);
      if (bodyResult == null) return null;
      Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType()), myEquations, letClause);
      return new Result(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
    } else if (param instanceof Concrete.TypeParameter) {
      int namesCount = param instanceof Concrete.TelescopeParameter ? ((Concrete.TelescopeParameter) param).getReferableList().size() : 1;
      SingleDependentLink link = visitTypeParameter((Concrete.TypeParameter) param);
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

  private LetClause typecheckLetClause(Concrete.LetClause clause) {
    LetClause letResult;
    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myContext)) {
      Result result = typecheckLetClause(clause.getParameters(), clause, 1);
      if (result == null) {
        return null;
      }
      letResult = new LetClause(clause.getData().textRepresentation(), result.expression);
    }
    return letResult;
  }

  @Override
  public Result visitLet(Concrete.LetExpression expr, ExpectedType expectedType) {
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      List<? extends Concrete.LetClause> abstractClauses = expr.getClauses();
      List<LetClause> clauses = new ArrayList<>(abstractClauses.size());
      for (Concrete.LetClause clause : abstractClauses) {
        LetClause letClause = typecheckLetClause(clause);
        if (letClause == null) {
          return null;
        }
        myContext.put(clause.getData(), letClause);
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
  public Result visitNumericLiteral(Concrete.NumericLiteral expr, ExpectedType expectedType) {
    BigInteger number = expr.getNumber();
    Expression expression = ExpressionFactory.Zero(); // TODO: Store as a constant
    for (BigInteger i = BigInteger.ZERO; i.compareTo(number) < 0; i = i.add(BigInteger.ONE)) {
      expression = ExpressionFactory.Suc(expression);
    }
    return checkResult(expectedType, new Result(expression, ExpressionFactory.Nat()), expr);
  }
}
