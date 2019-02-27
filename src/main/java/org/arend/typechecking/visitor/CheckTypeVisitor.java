package org.arend.typechecking.visitor;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.*;
import org.arend.core.context.param.*;
import org.arend.core.definition.*;
import org.arend.core.elimtree.Clause;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.let.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.ReplaceBindingVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.DummyErrorReporter;
import org.arend.error.Error;
import org.arend.error.GeneralError;
import org.arend.error.IncorrectExpressionException;
import org.arend.error.doc.DocFactory;
import org.arend.naming.error.WrongReferable;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.term.concrete.ConcreteLevelExpressionVisitor;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.ListLocalErrorReporter;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.LocalErrorReporterCounter;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.implicitargs.ImplicitArgsInference;
import org.arend.typechecking.implicitargs.StdImplicitArgsInference;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.implicitargs.equations.TwoStageEquations;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.patternmatching.ConditionsChecking;
import org.arend.typechecking.patternmatching.ElimTypechecking;
import org.arend.typechecking.patternmatching.PatternTypechecking;
import org.arend.util.Pair;

import java.math.BigInteger;
import java.util.*;

import static org.arend.typechecking.error.local.ArgInferenceError.expression;
import static org.arend.typechecking.error.local.ArgInferenceError.ordinal;

public class CheckTypeVisitor implements ConcreteExpressionVisitor<ExpectedType, CheckTypeVisitor.Result>, ConcreteLevelExpressionVisitor<LevelVariable, Level> {
  private final TypecheckerState myState;
  private Map<Referable, Binding> myContext;
  private Set<Binding> myFreeBindings;
  private Definition.TypeCheckingStatus myStatus = Definition.TypeCheckingStatus.NO_ERRORS;
  private LocalErrorReporter myErrorReporter;
  private final ImplicitArgsInference myArgsInference;
  private final Equations myEquations;
  private GlobalInstancePool myInstancePool;

  public interface TResult {
    Result toResult(CheckTypeVisitor checkTypeVisitor);
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

    private DefCallResult(Concrete.ReferenceExpression defCall, Definition definition, Sort sortArgument, List<Expression> arguments, List<DependentLink> parameters, Expression resultType) {
      myDefCall = defCall;
      myDefinition = definition;
      mySortArgument = sortArgument;
      myArguments = arguments;
      myParameters = parameters;
      myResultType = resultType;
    }

    public static TResult makeTResult(Concrete.ReferenceExpression defCall, Definition definition, Sort sortArgument) {
      List<DependentLink> parameters = new ArrayList<>();
      Expression resultType = definition.getTypeWithParams(parameters, sortArgument);

      if (parameters.isEmpty()) {
        return new Result(definition.getDefCall(sortArgument, Collections.emptyList()), resultType);
      } else {
        return new DefCallResult(defCall, definition, sortArgument, new ArrayList<>(), parameters, resultType);
      }
    }

    @Override
    public Result toResult(CheckTypeVisitor visitor) {
      if (myParameters.isEmpty()) {
        return new Result(myDefinition.getDefCall(mySortArgument, myArguments), myResultType);
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
          SingleDependentLink parameter = ExpressionFactory.singleParams(link.isExplicit(), names, link.getType().subst(new SubstVisitor(substitution, LevelSubstitution.EMPTY)));
          parameters.add(parameter);
          names.clear();

          for (; parameter.hasNext(); parameter = parameter.getNext(), link0 = link0.getNext()) {
            substitution.add(link0, new ReferenceExpression(parameter));
            myArguments.add(new ReferenceExpression(parameter));
          }

          link0 = null;
        }
      }

      Expression expression = myDefinition.getDefCall(mySortArgument, myArguments);
      Expression type = myResultType.subst(substitution, LevelSubstitution.EMPTY);
      Sort codSort = visitor.getSortOf(type.getType(), myDefCall);
      for (int i = parameters.size() - 1; i >= 0; i--) {
        codSort = PiExpression.generateUpperBound(parameters.get(i).getType().getSortOfType(), codSort, visitor.getEquations(), myDefCall);
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
      return size > 1 ? this : new Result(myDefinition.getDefCall(mySortArgument, myArguments), myResultType);
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
      return expressions.size() < size ? this : new Result(myDefinition.getDefCall(mySortArgument, myArguments), myResultType);
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
    public Result toResult(CheckTypeVisitor visitor) {
      return this;
    }

    @Override
    public DependentLink getParameter() {
      type = type.normalize(NormalizeVisitor.Mode.WHNF);
      return type.isInstance(PiExpression.class) ? type.cast(PiExpression.class).getParameters() : EmptyDependentLink.getInstance();
    }

    @Override
    public Result applyExpression(Expression expr, LocalErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
      expression = AppExpression.make(expression, expr);
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

    private void setStatus(Error error) {
      myStatus = myStatus.max(error.level == Error.Level.ERROR ? Definition.TypeCheckingStatus.HAS_ERRORS : Definition.TypeCheckingStatus.HAS_WARNINGS);
    }

    @Override
    public void report(LocalError localError) {
      setStatus(localError);
      myErrorReporter.report(localError);
    }

    @Override
    public void report(GeneralError error) {
      setStatus(error);
      myErrorReporter.report(error);
    }
  }

  public CheckTypeVisitor(TypecheckerState state, Map<Referable, Binding> localContext, LocalErrorReporter errorReporter, GlobalInstancePool pool) {
    myState = state;
    myContext = localContext;
    myFreeBindings = new HashSet<>();
    myErrorReporter = new MyErrorReporter(errorReporter);
    myArgsInference = new StdImplicitArgsInference(this);
    myEquations = new TwoStageEquations(this);
    myInstancePool = pool;
  }

  public void setHasErrors() {
    myStatus = Definition.TypeCheckingStatus.HAS_ERRORS;
  }

  public TypecheckerState getTypecheckingState() {
    return myState;
  }

  public GlobalInstancePool getInstancePool() {
    return myInstancePool;
  }

  public void setInstancePool(GlobalInstancePool pool) {
    myInstancePool = pool;
  }

  public Map<Referable, Binding> getContext() {
    return myContext;
  }

  public void setContext(Map<Referable, Binding> context) {
    myContext = context;
  }

  public Set<Binding> getFreeBindings() {
    return myFreeBindings;
  }

  public void setFreeBindings(Set<Binding> freeBindings) {
    myFreeBindings = freeBindings;
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

  public Definition.TypeCheckingStatus getStatus() {
    return myStatus;
  }

  private Sort getSortOf(Expression expr, Concrete.SourceNode sourceNode) {
    Sort sort = expr == null ? null : expr.toSort();
    if (sort == null) {
      assert expr != null;
      if (expr.isInstance(ErrorExpression.class)) {
        return Sort.STD;
      }
      Sort result = Sort.generateInferVars(myEquations, false, sourceNode);
      if (!CompareVisitor.compare(myEquations, Equations.CMP.LE, expr, new UniverseExpression(result), sourceNode)) {
        myErrorReporter.report(new TypeMismatchError(DocFactory.text("a type"), expr, sourceNode));
      }
      return result;
    } else {
      return sort;
    }
  }

  private Result checkResult(ExpectedType expectedType, Result result, Concrete.Expression expr) {
    if (result == null || expectedType == null || expectedType == ExpectedType.OMEGA && result.type instanceof UniverseExpression) {
      return result;
    }

    CompareVisitor cmpVisitor = new CompareVisitor(DummyEquations.getInstance(), Equations.CMP.LE, expr);
    if (expectedType instanceof Expression && cmpVisitor.nonNormalizingCompare(result.type, (Expression) expectedType)) {
      return result;
    }

    result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
    expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
    Result coercedResult = CoerceData.coerce(result, expectedType, expr, this);
    if (coercedResult != null) {
      return coercedResult;
    }

    return expectedType instanceof Expression ? checkResultExpr((Expression) expectedType, result, expr) : result;
  }

  private Result checkResultExpr(Expression expectedType, Result result, Concrete.Expression expr) {
    if (new CompareVisitor(myEquations, Equations.CMP.LE, expr).normalizedCompare(result.type, expectedType)) {
      result.expression = OfTypeExpression.make(result.expression, result.type, expectedType);
      return result;
    }

    if (!result.type.isError()) {
      myErrorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
    }
    return null;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean checkNormalizedResult(ExpectedType expectedType, Result result, Concrete.Expression expr, boolean strict) {
    if (expectedType instanceof Expression && new CompareVisitor(strict ? DummyEquations.getInstance() : myEquations, Equations.CMP.LE, expr).normalizedCompare(result.type, (Expression) expectedType) || expectedType == ExpectedType.OMEGA && result.type.isInstance(UniverseExpression.class)) {
      if (!strict && expectedType instanceof Expression) {
        result.expression = OfTypeExpression.make(result.expression, result.type, (Expression) expectedType);
      }
      return true;
    }

    if (!strict && !result.type.isError()) {
      myErrorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
    }

    return false;
  }

  private Result tResultToResult(ExpectedType expectedType, TResult result, Concrete.Expression expr) {
    if (result != null) {
      result = myArgsInference.inferTail(result, expectedType, expr);
    }
    return result == null ? null : checkResult(expectedType, result.toResult(this), expr);
  }

  public Result checkExpr(Concrete.Expression expr, ExpectedType expectedType) {
    if (expr == null) {
      assert false;
      myErrorReporter.report(new LocalError(Error.Level.ERROR, "Incomplete expression"));
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
        result = new Result(null, (Expression) expectedType);
      } else if (!result.type.isInstance(ClassCallExpression.class)) { // Use the inferred type if it is a class call
        result.type = (Expression) expectedType;
      }
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
      assert false;
      myErrorReporter.report(new LocalError(Error.Level.ERROR, "Incomplete expression"));
      return null;
    }

    Result result;
    try {
      ExpectedType expectedType1 = expectedType;
      if (expectedType instanceof Expression) {
        expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
        if (((Expression) expectedType).getStuckInferenceVariable() != null) {
          expectedType1 = ExpectedType.OMEGA;
        }
      }

      result = expr.accept(this, expectedType1);
      if (expectedType1 != expectedType) {
        result.type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
        result = checkResultExpr((Expression) expectedType, result, expr);
      }
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
      Expression stuck = type.getCanonicalStuckExpression();
      if (stuck == null || !stuck.isInstance(InferenceReferenceExpression.class) && !stuck.isError()) {
        if (!result.type.isError()) {
          myErrorReporter.report(new TypeMismatchError(DocFactory.text("a universe"), result.type, expr));
        }
        return null;
      }

      universe = new UniverseExpression(Sort.generateInferVars(myEquations, false, expr));
      InferenceVariable infVar = stuck.getInferenceVariable();
      if (infVar != null) {
        myEquations.addEquation(type, universe, Equations.CMP.LE, expr, infVar, null);
      }
    }

    return new TypeExpression(result.expression, universe.getSort());
  }

  public Type finalCheckType(Concrete.Expression expr, ExpectedType expectedType) {
    Type result = checkType(expr, expectedType);
    if (result == null) return null;
    return result.subst(new SubstVisitor(new ExprSubstitution(), myEquations.solve(expr))).strip(myErrorReporter);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean compareExpressions(boolean isLeft, Expression expected, Expression actual, Concrete.Expression expr) {
    if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, actual, expected, expr)) {
      myErrorReporter.report(new PathEndpointMismatchError(isLeft, expected, actual, expr));
      return false;
    }
    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkPath(TResult result, Concrete.Expression expr) {
    if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.PATH_CON) {
      myErrorReporter.report(new TypecheckingError("Expected an argument for 'path'", expr));
      return false;
    }
    if (result instanceof Result) {
      ConCallExpression conCall = ((Result) result).expression.checkedCast(ConCallExpression.class);
      if (conCall != null && conCall.getDefinition() == Prelude.PATH_CON) {
        //noinspection RedundantIfStatement
        if (!compareExpressions(true, conCall.getDataTypeArguments().get(1), AppExpression.make(conCall.getDefCallArguments().get(0), ExpressionFactory.Left()), expr) ||
          !compareExpressions(false, conCall.getDataTypeArguments().get(2), AppExpression.make(conCall.getDefCallArguments().get(0), ExpressionFactory.Right()), expr)) {
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

  private CheckTypeVisitor.TResult getLocalVar(Referable ref, Concrete.SourceNode sourceNode) {
    if (ref instanceof UnresolvedReference || ref instanceof RedirectingReferable) {
      throw new IllegalStateException();
    }
    if (ref instanceof ErrorReference) {
      return null;
    }

    Binding def = myContext.get(ref);
    if (def == null) {
      myErrorReporter.report(new IncorrectReferenceError(ref, sourceNode));
      return null;
    }
    Expression type = def.getTypeExpr();
    if (type == null) {
      myErrorReporter.report(new ReferenceTypeError(ref));
      return null;
    } else {
      return new Result(def instanceof EvaluatingBinding ? ((EvaluatingBinding) def).getExpression() : new ReferenceExpression(def), type);
    }
  }

  private Definition getTypeCheckedDefinition(TCReferable definition, Concrete.Expression expr) {
    Definition typeCheckedDefinition = myState.getTypechecked(definition);
    if (typeCheckedDefinition == null) {
      myErrorReporter.report(new IncorrectReferenceError(definition, expr));
      return null;
    }
    if (!typeCheckedDefinition.status().headerIsOK()) {
      myErrorReporter.report(new HasErrors(Error.Level.ERROR, definition, expr));
      return null;
    } else {
      if (typeCheckedDefinition.status() == Definition.TypeCheckingStatus.BODY_HAS_ERRORS) {
        myErrorReporter.report(new HasErrors(Error.Level.WARNING, definition, expr));
      }
      return typeCheckedDefinition;
    }
  }

  private CheckTypeVisitor.TResult typeCheckDefCall(TCReferable resolvedDefinition, Concrete.ReferenceExpression expr) {
    Definition definition = getTypeCheckedDefinition(resolvedDefinition, expr);
    if (definition == null) {
      return null;
    }

    Sort sortArgument;
    boolean isMin = definition instanceof DataDefinition && !definition.getParameters().hasNext();
    if (expr.getPLevel() == null && expr.getHLevel() == null) {
      sortArgument = isMin ? Sort.PROP : Sort.generateInferVars(myEquations, definition.hasUniverses(), expr);
      Level hLevel = null;
      if (!isMin && definition == Prelude.ISO) {
        hLevel = new Level(sortArgument.getHLevel().getVar(), -1);
        sortArgument = new Sort(sortArgument.getPLevel(), hLevel);
      }
      if (definition instanceof DataDefinition && !sortArgument.isProp()) {
        hLevel = ((DataDefinition) definition).getSort().getHLevel();
      } else if (definition instanceof FunctionDefinition && !sortArgument.isProp()) {
        UniverseExpression universe = ((FunctionDefinition) definition).getResultType().getPiParameters(null, false).checkedCast(UniverseExpression.class);
        if (universe != null) {
          hLevel = universe.getSort().getHLevel();
        }
      }
      if (hLevel != null && hLevel.getConstant() == -1 && hLevel.getVar() == LevelVariable.HVAR && hLevel.getMaxConstant() == 0) {
        myEquations.bindVariables((InferenceLevelVariable) sortArgument.getPLevel().getVar(), (InferenceLevelVariable) sortArgument.getHLevel().getVar());
      }
    } else {
      Level pLevel = null;
      if (expr.getPLevel() != null) {
        pLevel = expr.getPLevel().accept(this, LevelVariable.PVAR);
      }
      if (pLevel == null) {
        if (isMin) {
          pLevel = new Level(0);
        } else {
          InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, definition.hasUniverses(), expr.getPLevel());
          myEquations.addVariable(pl);
          pLevel = new Level(pl);
        }
      }

      Level hLevel = null;
      if (expr.getHLevel() != null) {
        hLevel = expr.getHLevel().accept(this, LevelVariable.HVAR);
      }
      if (hLevel == null) {
        if (isMin) {
          hLevel = new Level(-1);
        } else {
          InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, definition.hasUniverses(), expr.getHLevel());
          myEquations.addVariable(hl);
          hLevel = new Level(hl);
        }
      }

      sortArgument = new Sort(pLevel, hLevel);
    }

    return CheckTypeVisitor.DefCallResult.makeTResult(expr, definition, sortArgument);
  }

  public Referable getUnderlyingTypecheckable(Referable ref, Concrete.SourceNode sourceNode) {
    if (!(ref instanceof LocatedReferable)) {
      return ref;
    }
    LocatedReferable underlyingRef = ((LocatedReferable) ref).getUnderlyingTypecheckable();
    if (underlyingRef == null && sourceNode != null) {
      myErrorReporter.report(new TypecheckingError("Reference to incorrect synonym '" + ref.textRepresentation() + "'", sourceNode));
    }
    return underlyingRef;
  }

  public TResult visitReference(Concrete.ReferenceExpression expr) {
    Referable ref = expr.getReferent();
    if (!(ref instanceof GlobalReferable) && (expr.getPLevel() != null || expr.getHLevel() != null)) {
      myErrorReporter.report(new TypecheckingError("Level specifications are allowed only after definitions", expr.getPLevel() != null ? expr.getPLevel() : expr.getHLevel()));
    }
    ref = getUnderlyingTypecheckable(ref, expr);
    return ref == null ? null : ref instanceof TCReferable ? typeCheckDefCall((TCReferable) ref, expr) : getLocalVar(expr.getReferent(), expr);
  }

  @Override
  public Result visitReference(Concrete.ReferenceExpression expr, ExpectedType expectedType) {
    TResult result = visitReference(expr);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  public Result checkArgument(Concrete.Expression expr, ExpectedType expectedType, TResult result) {
    return expr instanceof Concrete.ThisExpression && result instanceof DefCallResult && ((DefCallResult) result).getDefinition().isGoodParameter(((DefCallResult) result).getArguments().size())
      ? tResultToResult(expectedType, getLocalVar(((Concrete.ThisExpression) expr).getReferent(), expr), expr)
      : checkExpr(expr, expectedType);
  }

  @Override
  public Result visitThis(Concrete.ThisExpression expr, ExpectedType expectedType) {
    myErrorReporter.report(new TypecheckingError("\\this expressions are allowed only in appropriate arguments of definitions and class extensions", expr));
    return null;
  }

  @Override
  public Result visitInferenceReference(Concrete.InferenceReferenceExpression expr, ExpectedType params) {
    return new Result(new InferenceReferenceExpression(expr.getVariable(), myEquations), expr.getVariable().getType());
  }

  private TypedSingleDependentLink visitNameParameter(Concrete.NameParameter param, int argIndex, Concrete.SourceNode sourceNode) {
    Referable referable = param.getReferable();
    String name = referable == null ? null : referable.textRepresentation();
    Sort sort = Sort.generateInferVars(myEquations, false, sourceNode);
    InferenceVariable inferenceVariable = new LambdaInferenceVariable(name == null ? "_" : "type-of-" + name, new UniverseExpression(sort), argIndex, sourceNode, false, getAllBindings());
    Expression argType = new InferenceReferenceExpression(inferenceVariable, myEquations);

    TypedSingleDependentLink link = new TypedSingleDependentLink(param.getExplicit(), name, new TypeExpression(argType, sort));
    if (referable != null) {
      myContext.put(referable, link);
    } else {
      myFreeBindings.add(link);
    }
    return link;
  }

  private SingleDependentLink visitTypeParameter(Concrete.TypeParameter param, List<Sort> sorts) {
    Type argResult = checkType(param.getType(), ExpectedType.OMEGA);
    if (argResult == null) return null;
    if (sorts != null) {
      sorts.add(argResult.getSortOfType());
    }

    if (param instanceof Concrete.TelescopeParameter) {
      List<? extends Referable> referableList = param.getReferableList();
      SingleDependentLink link = ExpressionFactory.singleParams(param.getExplicit(), param.getNames(), argResult);
      int i = 0;
      for (SingleDependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
        if (referableList.get(i) != null) {
          myContext.put(referableList.get(i), link1);
        } else {
          myFreeBindings.add(link1);
        }
      }
      return link;
    } else {
      return new TypedSingleDependentLink(param.getExplicit(), null, argResult);
    }
  }

  private Result bodyToLam(SingleDependentLink params, Result bodyResult, Concrete.SourceNode sourceNode) {
    if (bodyResult == null) {
      return null;
    }
    Sort sort = PiExpression.generateUpperBound(params.getType().getSortOfType(), getSortOf(bodyResult.type.getType(), sourceNode), myEquations, sourceNode);
    return new Result(new LamExpression(sort, params, bodyResult.expression), new PiExpression(sort, params, bodyResult.type));
  }

  private Result visitLam(List<? extends Concrete.Parameter> parameters, Concrete.LamExpression expr, Expression expectedType, int argIndex) {
    if (parameters.isEmpty()) {
      return checkExpr(expr.getBody(), expectedType);
    }

    Concrete.Parameter param = parameters.get(0);
    if (expectedType != null) {
      expectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      if (param.getExplicit() && expectedType.isInstance(PiExpression.class) && !expectedType.cast(PiExpression.class).getParameters().isExplicit()) {
        PiExpression piExpectedType = expectedType.cast(PiExpression.class);
        SingleDependentLink piParams = piExpectedType.getParameters();
        for (SingleDependentLink link = piParams; link.hasNext(); link = link.getNext()) {
          myFreeBindings.add(link);
        }
        return bodyToLam(piParams, visitLam(parameters, expr, piExpectedType.getCodomain(), argIndex + DependentLink.Helper.size(piParams)), expr);
      }
    }

    if (param instanceof Concrete.NameParameter) {
      if (expectedType == null || !expectedType.isInstance(PiExpression.class)) {
        TypedSingleDependentLink link = visitNameParameter((Concrete.NameParameter) param, argIndex, expr);
        Result bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, null, argIndex + 1);
        if (bodyResult == null) return null;
        Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType(), expr), myEquations, expr);
        Result result = new Result(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
        if (expectedType != null && checkResult(expectedType, result, expr) == null) {
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

        Type paramType = piParams.getType();
        DefCallExpression defCallParamType = paramType.getExpr().checkedCast(DefCallExpression.class);
        if (defCallParamType != null && !defCallParamType.getDefinition().hasUniverses()) { // fixes test pLevelTest
          if (defCallParamType.getDefinition() instanceof DataDefinition) {
            paramType = new DataCallExpression((DataDefinition) defCallParamType.getDefinition(), Sort.generateInferVars(myEquations, defCallParamType.getDefinition().hasUniverses(), param), new ArrayList<>(defCallParamType.getDefCallArguments()));
          } else if (defCallParamType.getDefinition() instanceof FunctionDefinition) {
            paramType = new TypeExpression(new FunCallExpression((FunctionDefinition) defCallParamType.getDefinition(), Sort.generateInferVars(myEquations, defCallParamType.getDefinition().hasUniverses(), param), new ArrayList<>(defCallParamType.getDefCallArguments())), paramType.getSortOfType());
          }
        }

        SingleDependentLink link = new TypedSingleDependentLink(piParams.isExplicit(), referable == null ? null : referable.textRepresentation(), paramType);
        if (referable != null) {
          myContext.put(referable, link);
        } else {
          myFreeBindings.add(link);
        }
        Expression codomain = piExpectedType.getCodomain().subst(piParams, new ReferenceExpression(link));
        return bodyToLam(link, visitLam(parameters.subList(1, parameters.size()), expr, piParams.getNext().hasNext() ? new PiExpression(piExpectedType.getResultSort(), piParams.getNext(), codomain) : codomain, argIndex + 1), expr);
      }
    } else if (param instanceof Concrete.TypeParameter) {
      SingleDependentLink link = visitTypeParameter((Concrete.TypeParameter) param, null);
      if (link == null) {
        return null;
      }

      SingleDependentLink actualLink = null;
      Expression expectedBodyType = null;
      int namesCount = param.getNumberOfParameters();
      if (expectedType != null) {
        Concrete.Expression paramType = param.getType();
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
            if (!argType.isError()) {
              myErrorReporter.report(new TypeMismatchError("Type mismatch in an argument of the lambda", argExpectedType, argType, paramType));
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
      Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOf(bodyResult.type.getType(), expr), myEquations, expr);
      if (actualLink != null) {
        if (checkResult(expectedType, new Result(null, new PiExpression(sort, actualLink, bodyResult.type)), expr) == null) {
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
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        Result result = visitLam(expr.getParameters(), expr, expectedType instanceof Expression ? (Expression) expectedType : null, 1);
        if (result != null && expectedType != null && !(expectedType instanceof Expression)) {
          if (!result.type.isError()) {
            myErrorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
          }
          return null;
        }
        return result;
      }
    }
  }

  @Override
  public Result visitPi(Concrete.PiExpression expr, ExpectedType expectedType) {
    List<SingleDependentLink> list = new ArrayList<>();
    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        for (Concrete.TypeParameter arg : expr.getParameters()) {
          SingleDependentLink link = visitTypeParameter(arg, sorts);
          if (link == null) {
            return null;
          }
          list.add(link);
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
  }

  @Override
  public Result visitUniverse(Concrete.UniverseExpression expr, ExpectedType expectedType) {
    Level pLevel = expr.getPLevel() != null ? expr.getPLevel().accept(this, LevelVariable.PVAR) : null;
    Level hLevel = expr.getHLevel() != null ? expr.getHLevel().accept(this, LevelVariable.HVAR) : null;

    if (pLevel != null && pLevel.isInfinity()) {
      myErrorReporter.report(new TypecheckingError("\\inf is not a correct p-level", expr));
      pLevel = null;
    }

    if (pLevel == null) {
      InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, true, expr);
      myEquations.addVariable(pl);
      pLevel = new Level(pl);
    }

    if (hLevel == null) {
      InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, true, expr);
      myEquations.addVariable(hl);
      hLevel = new Level(hl);
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
      Definition.TypeCheckingStatus status = myStatus;
      errors = new ArrayList<>();
      myErrorReporter = new ListLocalErrorReporter(errors);
      exprResult = checkExpr(expr.getExpression(), expectedType);
      myErrorReporter = errorReporter;
      myStatus = status;
    }

    TypecheckingError error = new GoalError(expr.getName(), myContext, expectedType, exprResult == null ? null : exprResult.type, errors, expr);
    myErrorReporter.report(error);
    Expression result = new ErrorExpression(exprResult == null ? null : exprResult.expression, error);
    return new Result(result, expectedType instanceof Expression ? (Expression) expectedType : result);
  }

  @Override
  public Result visitHole(Concrete.HoleExpression expr, ExpectedType expectedType) {
    if (expr.getError() != null) {
      return null;
    }

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
      list.append(ExpressionFactory.parameter(null, result.type instanceof Type ? (Type) result.type : new TypeExpression(result.type, getSortOf(result.type.getType(), expr))));
    }

    Sort sortArgument = Sort.generateInferVars(myEquations, false, expr);
    SigmaExpression type = new SigmaExpression(sortArgument, list.getFirst());
    return checkResult(expectedTypeNorm, new Result(new TupleExpression(fields, type), type), expr);
  }

  private DependentLink visitParameters(List<? extends Concrete.TypeParameter> parameters, ExpectedType expectedType, List<Sort> resultSorts) {
    LinkList list = new LinkList();

    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        for (Concrete.TypeParameter arg : parameters) {
          Type result = checkType(arg.getType(), expectedType == null ? ExpectedType.OMEGA : expectedType);
          if (result == null) return null;

          if (arg instanceof Concrete.TelescopeParameter) {
            List<? extends Referable> referableList = arg.getReferableList();
            DependentLink link = ExpressionFactory.parameter(arg.getExplicit(), arg.getNames(), result);
            list.append(link);
            int i = 0;
            for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
              if (referableList.get(i) != null) {
                myContext.put(referableList.get(i), link1);
              } else {
                myFreeBindings.add(link1);
              }
            }
          } else {
            DependentLink link = ExpressionFactory.parameter(arg.getExplicit(), (String) null, result);
            list.append(link);
          }

          Sort resultSort = null;
          if (expectedType instanceof Expression) {
            UniverseExpression universe = ((Expression) expectedType).checkedCast(UniverseExpression.class);
            if (universe != null && universe.getSort().isProp()) {
              resultSort = Sort.PROP;
            }
          }
          resultSorts.add(resultSort == null ? result.getSortOfType() : resultSort);
        }
      }
    }

    return list.getFirst();
  }

  @Override
  public Result visitSigma(Concrete.SigmaExpression expr, ExpectedType expectedType) {
    if (expr.getParameters().isEmpty()) {
      return checkResult(expectedType, new Result(new SigmaExpression(Sort.PROP, EmptyDependentLink.getInstance()), new UniverseExpression(Sort.PROP)), expr);
    }

    for (Concrete.TypeParameter parameter : expr.getParameters()) {
      if (!parameter.getExplicit()) {
        myErrorReporter.report(new TypecheckingError("Parameters in sigma types must be explicit", parameter));
        parameter.setExplicit(true);
      }
    }

    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());
    DependentLink args = visitParameters(expr.getParameters(), expectedType, sorts);
    if (args == null || !args.hasNext()) return null;
    Sort sort = generateUpperBound(sorts, expr);
    return checkResult(expectedType, new Result(new SigmaExpression(sort, args), new UniverseExpression(sort)), expr);
  }

  @Override
  public Result visitBinOpSequence(Concrete.BinOpSequenceExpression expr, ExpectedType expectedType) {
    throw new IllegalStateException();
  }

  public Integer getExpressionLevel(DependentLink link, Expression type, Expression expr, Equations equations, Concrete.SourceNode sourceNode) {
    boolean ok = expr != null;

    int level = -2;
    if (ok) {
      List<DependentLink> parameters = new ArrayList<>();
      for (; link.hasNext(); link = link.getNext()) {
        parameters.add(link);
      }

      Expression resultType = type == null ? null : type.getPiParameters(parameters, false);
      for (int i = 0; i < parameters.size(); i++) {
        link = parameters.get(i);
        if (link instanceof TypedDependentLink) {
          if (!CompareVisitor.compare(equations, Equations.CMP.EQ, link.getTypeExpr(), expr, sourceNode)) {
            ok = false;
            break;
          }
        }

        List<Expression> pathArgs = new ArrayList<>();
        pathArgs.add(expr);
        pathArgs.add(new ReferenceExpression(link));
        i++;
        if (i >= parameters.size()) {
          ok = false;
          break;
        }
        link = parameters.get(i);
        if (!CompareVisitor.compare(equations, Equations.CMP.EQ, link.getTypeExpr(), expr, sourceNode)) {
          ok = false;
          break;
        }

        pathArgs.add(new ReferenceExpression(link));
        expr = new FunCallExpression(Prelude.PATH_INFIX, Sort.STD, pathArgs);
        level++;
      }

      if (ok && resultType != null && !CompareVisitor.compare(equations, Equations.CMP.EQ, resultType, expr, sourceNode)) {
        ok = false;
      }
    }

    if (!ok) {
      myErrorReporter.report(new TypecheckingError("\\level has wrong format", sourceNode));
      return null;
    } else {
      return level;
    }
  }

  @Override
  public Result visitCase(Concrete.CaseExpression expr, ExpectedType expectedType) {
    if (expectedType == null && expr.getResultType() == null) {
      myErrorReporter.report(new TypecheckingError("Cannot infer the result type", expr));
      return null;
    }

    List<? extends Concrete.CaseArgument> caseArgs = expr.getArguments();
    LinkList list = new LinkList();
    List<Expression> expressions = new ArrayList<>(caseArgs.size());

    ExprSubstitution substitution = new ExprSubstitution();
    Type resultType = null;
    Expression resultExpr;
    Integer level = null;
    Expression resultTypeLevel = null;
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      try (Utils.SetContextSaver ignored1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        for (Concrete.CaseArgument caseArg : caseArgs) {
          Type argType = null;
          if (caseArg.type != null) {
            argType = checkType(caseArg.type, ExpectedType.OMEGA);
          }

          Result exprResult = checkExpr(caseArg.expression, argType == null ? null : argType.getExpr().subst(substitution));
          if (exprResult == null) return null;
          DependentLink link = ExpressionFactory.parameter(caseArg.referable == null ? null : caseArg.referable.textRepresentation(), argType != null ? argType : exprResult.type instanceof Type ? (Type) exprResult.type : new TypeExpression(exprResult.type, getSortOf(exprResult.type.getType(), expr)));
          list.append(link);
          if (caseArg.referable != null) {
            myContext.put(caseArg.referable, link);
          }
          myFreeBindings.add(link);
          expressions.add(exprResult.expression);
          substitution.add(link, exprResult.expression);
        }

        if (expr.getResultType() != null) {
          resultType = checkType(expr.getResultType(), ExpectedType.OMEGA);
        }
        if (resultType == null && expectedType == null) {
          return null;
        }
        resultExpr = resultType != null ? resultType.getExpr() : expectedType instanceof Expression ? (Expression) expectedType : new UniverseExpression(Sort.generateInferVars(myEquations, false, expr));

        if (expr.getResultTypeLevel() != null) {
          CheckTypeVisitor.Result levelResult = checkExpr(expr.getResultTypeLevel(), null);
          if (levelResult != null) {
            resultTypeLevel = levelResult.expression;
            level = getExpressionLevel(EmptyDependentLink.getInstance(), levelResult.type, resultExpr, myEquations, expr.getResultTypeLevel());
          }
        }
      }
    }

    // Check if the level of the result type is specified explicitly
    List<Clause> resultClauses = new ArrayList<>();
    if (expr.getResultTypeLevel() == null && expr.getResultType() instanceof Concrete.TypedExpression) {
      Concrete.Expression typeType = ((Concrete.TypedExpression) expr.getResultType()).type;
      if (typeType instanceof Concrete.UniverseExpression) {
        Concrete.UniverseExpression universeType = (Concrete.UniverseExpression) typeType;
        if (universeType.getHLevel() instanceof Concrete.NumberLevelExpression) {
          level = ((Concrete.NumberLevelExpression) universeType.getHLevel()).getNumber();
        }
      }
    }

    // Try to infer level either directly or from a path type.
    if (level == null && expr.getResultTypeLevel() == null) {
      Sort sort = resultType == null ? null : resultType.getSortOfType();
      if (sort == null) {
        Expression type = resultExpr.getType();
        if (type != null) {
          sort = type.toSort();
        }
      }
      if (sort != null && sort.getHLevel().isClosed()) {
        if (sort.getHLevel() != Level.INFINITY) {
          level = sort.getHLevel().getConstant();
        }
      } else if (sort == null || sort.getHLevel().getVar() instanceof InferenceLevelVariable) {
        resultExpr = resultExpr.normalize(NormalizeVisitor.Mode.WHNF);
        DataCallExpression dataCall = resultExpr.checkedCast(DataCallExpression.class);
        if (dataCall != null && dataCall.getDefinition() == Prelude.PATH) {
          LamExpression lamExpr = dataCall.getDefCallArguments().get(0).normalize(NormalizeVisitor.Mode.WHNF).checkedCast(LamExpression.class);
          Expression bodyType = lamExpr == null ? null : lamExpr.getBody().getType();
          UniverseExpression universeBodyType = bodyType == null ? null : bodyType.checkedCast(UniverseExpression.class);
          if (universeBodyType != null && universeBodyType.getSort().getHLevel().isClosed() && universeBodyType.getSort().getHLevel() != Level.INFINITY) {
            level = universeBodyType.getSort().getHLevel().getConstant() - 1;
            if (level < -1) {
              level = -1;
            }
          }
        }
      }
    }

    ElimTree elimTree = new ElimTypechecking(this, resultExpr, EnumSet.of(PatternTypechecking.Flag.ALLOW_CONDITIONS, PatternTypechecking.Flag.CHECK_COVERAGE), level).typecheckElim(expr.getClauses(), expr, list.getFirst(), resultClauses);
    if (elimTree == null) {
      return null;
    }

    ConditionsChecking.check(resultClauses, elimTree, myErrorReporter);
    Result result = new Result(new CaseExpression(list.getFirst(), resultExpr, resultTypeLevel, elimTree, expressions), resultType != null ? resultExpr.subst(substitution) : resultExpr);
    return resultType == null ? result : checkResult(expectedType, result, expr);
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

    Sort sortResult = Sort.generateInferVars(myEquations, false, sourceNode);
    for (Sort sort : sorts) {
      myEquations.addEquation(sort.getPLevel(), sortResult.getPLevel(), Equations.CMP.LE, sourceNode);
      myEquations.addEquation(sort.getHLevel(), sortResult.getHLevel(), Equations.CMP.LE, sourceNode);
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
      if (!exprResult.type.isError()) {
        myErrorReporter.report(new TypeMismatchError(DocFactory.text("A sigma type"), exprResult.type, expr1));
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

  private Definition referableToDefinition(Referable referable, Concrete.SourceNode sourceNode) {
    if (referable instanceof ErrorReference) {
      return null;
    }
    referable = getUnderlyingTypecheckable(referable, sourceNode);
    if (referable == null) {
      return null;
    }

    Definition definition = referable instanceof TCReferable ? myState.getTypechecked((TCReferable) referable) : null;
    if (definition == null && sourceNode != null) {
      myErrorReporter.report(new TypecheckingError("Internal error: definition '" + referable.textRepresentation() + "' was not typechecked", sourceNode));
    }
    return definition;
  }

  public <T extends Definition> T referableToDefinition(Referable referable, Class<T> clazz, String errorMsg, Concrete.SourceNode sourceNode) {
    Definition definition = referableToDefinition(referable, sourceNode);
    if (definition == null) {
      return null;
    }
    if (clazz.isInstance(definition)) {
      return clazz.cast(definition);
    }

    if (sourceNode != null) {
      myErrorReporter.report(new WrongReferable(errorMsg, referable, sourceNode));
    }
    return null;
  }

  public ClassField referableToClassField(Referable referable, Concrete.SourceNode sourceNode) {
    return referableToDefinition(referable, ClassField.class, "Expected a class field", sourceNode);
  }

  @Override
  public Result visitClassExt(Concrete.ClassExtExpression expr, ExpectedType expectedType) {
    // Typecheck the base class

    Concrete.Expression baseClassExpr = expr.getBaseClassExpression();
    Result typeCheckedBaseClass = checkExpr(baseClassExpr, null);
    if (typeCheckedBaseClass == null) {
      return null;
    }
    ClassCallExpression classCall = typeCheckedBaseClass.expression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(ClassCallExpression.class);
    if (classCall == null) {
      myErrorReporter.report(new TypecheckingError("Expected a class", baseClassExpr));
      return null;
    }

    // Typecheck field implementations
    return visitClassExt(expr.getStatements(), expectedType, classCall, null, expr);
  }

  public Result visitClassExt(List<? extends Concrete.ClassFieldImpl> classFieldImpls, ExpectedType expectedType, ClassCallExpression classCallExpr, Set<ClassField> pseudoImplemented, Concrete.Expression expr) {
    ClassDefinition baseClass = classCallExpr.getDefinition();
    Map<ClassField, Expression> fieldSet = new HashMap<>(classCallExpr.getImplementedHere());
    ClassCallExpression resultClassCall = new ClassCallExpression(baseClass, classCallExpr.getSortArgument(), fieldSet, Sort.PROP, baseClass.hasUniverses());

    for (Concrete.ClassFieldImpl statement : classFieldImpls) {
      Definition definition = referableToDefinition(statement.getImplementedField(), statement);
      if (definition == null) {
        continue;
      }

      if (definition instanceof ClassField) {
        ClassField field = (ClassField) definition;
        Expression impl = typecheckImplementation(field, statement.implementation, resultClassCall);
        if (impl != null) {
          Expression oldImpl = null;
          if (!field.isProperty()) {
            oldImpl = resultClassCall.getImplementationHere(field);
            if (oldImpl == null) {
              LamExpression lamImpl = resultClassCall.getDefinition().getImplementation(field);
              oldImpl = lamImpl == null ? null : lamImpl.getBody();
            }
          }
          if (oldImpl != null) {
            if (!classCallExpr.isImplemented(field) || !CompareVisitor.compare(myEquations, Equations.CMP.EQ, impl, oldImpl, statement.implementation)) {
              myErrorReporter.report(new FieldsImplementationError(true, Collections.singletonList(field.getReferable()), statement));
            }
          } else if (!resultClassCall.isImplemented(field)) {
            fieldSet.put(field, impl);
          }
        } else if (pseudoImplemented != null) {
          pseudoImplemented.add(field);
        } else if (!resultClassCall.isImplemented(field)) {
          fieldSet.put(field, new ErrorExpression(null, null));
        }
      } else if (definition instanceof ClassDefinition) {
        Result result = checkExpr(statement.implementation, null);
        if (result != null) {
          Expression type = result.type.normalize(NormalizeVisitor.Mode.WHNF);
          ClassCallExpression classCall = type.checkedCast(ClassCallExpression.class);
          if (classCall == null) {
            if (!type.isInstance(ErrorExpression.class)) {
              InferenceVariable var = type instanceof InferenceReferenceExpression ? ((InferenceReferenceExpression) type).getVariable() : null;
              myErrorReporter.report(var == null ? new TypeMismatchError(DocFactory.text("a class"), type, statement.implementation) : var.getErrorInfer());
            }
          } else {
            if (!classCall.getDefinition().isSubClassOf((ClassDefinition) definition)) {
              myErrorReporter.report(new TypeMismatchError(new ClassCallExpression((ClassDefinition) definition, Sort.PROP), type, statement.implementation));
            } else {
              for (ClassField field : ((ClassDefinition) definition).getFields()) {
                Expression impl = FieldCallExpression.make(field, classCall.getSortArgument(), result.expression);
                Expression oldImpl = field.isProperty() ? null : resultClassCall.getImplementation(field, result.expression);
                if (oldImpl != null) {
                  if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, impl, oldImpl, statement.implementation)) {
                    myErrorReporter.report(new FieldsImplementationError(true, Collections.singletonList(field.getReferable()), statement.implementation));
                  }
                } else if (!resultClassCall.isImplemented(field)) {
                  fieldSet.put(field, impl);
                }
              }
            }
          }
        }
      } else {
        myErrorReporter.report(new WrongReferable("Expected either a field or a class", statement.getImplementedField(), statement));
      }
    }

    resultClassCall = fixClassExtSort(resultClassCall, expr);
    resultClassCall.updateHasUniverses();
    return checkResult(expectedType, new Result(resultClassCall, new UniverseExpression(resultClassCall.getSort())), expr);
  }

  public ClassCallExpression fixClassExtSort(ClassCallExpression classCall, Concrete.SourceNode sourceNode) {
    Expression thisExpr = new ReferenceExpression(ExpressionFactory.parameter("this", classCall));
    Level hLevel = classCall.getDefinition().getLevels().get(classCall.getImplementedHere().keySet());
    List<Sort> sorts = hLevel != null && hLevel.isProp() ? null : new ArrayList<>();
    for (ClassField field : classCall.getDefinition().getFields()) {
      if (classCall.isImplemented(field)) continue;
      PiExpression fieldType = field.getType(classCall.getSortArgument());
      if (fieldType.getCodomain().isInstance(ErrorExpression.class)) continue;
      if (sorts != null) {
        sorts.add(getSortOf(fieldType.applyExpression(thisExpr).normalize(NormalizeVisitor.Mode.WHNF).getType(), sourceNode));
      }
    }

    if (hLevel != null && sorts != null) {
      for (int i = 0; i < sorts.size(); i++) {
        sorts.set(i, new Sort(sorts.get(i).getPLevel(), hLevel));
      }
    }

    return new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), classCall.getImplementedHere(), sorts == null ? Sort.PROP : generateUpperBound(sorts, sourceNode).subst(classCall.getSortArgument().toLevelSubstitution()), classCall.hasUniverses());
  }

  private Expression typecheckImplementation(ClassField field, Concrete.Expression implBody, ClassCallExpression fieldSetClass) {
    PiExpression piType = field.getType(fieldSetClass.getSortArgument());
    ReplaceBindingVisitor visitor = new ReplaceBindingVisitor(piType.getParameters(), fieldSetClass);
    Expression type = piType.getCodomain().accept(visitor, null);
    if (!visitor.isOK()) {
      myErrorReporter.report(new TypecheckingError("The type of '" + field.getName() + "' depends non-trivially on \\this parameter", implBody));
      return null;
    }

    if (implBody instanceof Concrete.HoleExpression && field.getReferable().isParameterField() && !field.getReferable().isExplicitField() && field.isTypeClass() && type instanceof ClassCallExpression && !((ClassCallExpression) type).getDefinition().isRecord()) {
      return new InferenceReferenceExpression(new TypeClassInferenceVariable(field.getName(), type, ((ClassCallExpression) type).getDefinition().getReferable(), null, implBody, getAllBindings()), myEquations);
    }

    CheckTypeVisitor.Result result = implBody instanceof Concrete.ThisExpression && fieldSetClass.getDefinition().isGoodField(field)
      ? tResultToResult(type, getLocalVar(((Concrete.ThisExpression) implBody).getReferent(), implBody), implBody)
      : checkExpr(implBody, type);
    return result == null ? null : result.expression;
  }

  @Override
  public Result visitNew(Concrete.NewExpression expr, ExpectedType expectedType) {
    Result exprResult = null;
    Set<ClassField> pseudoImplemented = Collections.emptySet();
    if (expr.getExpression() instanceof Concrete.ClassExtExpression || expr.getExpression() instanceof Concrete.ReferenceExpression) {
      Concrete.Expression baseExpr = expr.getExpression() instanceof Concrete.ClassExtExpression ? ((Concrete.ClassExtExpression) expr.getExpression()).getBaseClassExpression() : expr.getExpression();
      if (baseExpr instanceof Concrete.HoleExpression || baseExpr instanceof Concrete.ReferenceExpression && expectedType instanceof ClassCallExpression) {
        ClassDefinition actualClassDef = null;
        if (baseExpr instanceof Concrete.HoleExpression && !(expectedType instanceof ClassCallExpression)) {
          myErrorReporter.report(new TypecheckingError("Cannot infer an expression", baseExpr));
          return null;
        }
        if (baseExpr instanceof Concrete.ReferenceExpression) {
          Referable ref = getUnderlyingTypecheckable(((Concrete.ReferenceExpression) baseExpr).getReferent(), baseExpr);
          boolean ok = ref instanceof TCReferable;
          if (ok) {
            Definition actualDef = myState.getTypechecked((TCReferable) ref);
            if (actualDef instanceof ClassDefinition) {
              ok = ((ClassDefinition) actualDef).isSubClassOf(((ClassCallExpression) expectedType).getDefinition());
              if (actualDef != ((ClassCallExpression) expectedType).getDefinition()) {
                actualClassDef = (ClassDefinition) actualDef;
              }
            } else {
              ok = false;
            }
          }
          if (!ok) {
            myErrorReporter.report(new TypeMismatchError(expectedType, baseExpr, baseExpr));
            return null;
          }
        }
        ClassCallExpression expectedClassCall = (ClassCallExpression) expectedType;

        if (!(expr.getExpression() instanceof Concrete.ClassExtExpression)) {
          return checkAllImplemented(expectedClassCall, Collections.emptySet(), expr) ? new Result(new NewExpression(expectedClassCall), expectedClassCall) : null;
        }

        boolean ok = true;
        if (actualClassDef != null) {
          for (ClassField implField : expectedClassCall.getImplementedHere().keySet()) {
            if (actualClassDef.isImplemented(implField)) {
              ok = false;
              break;
            }
          }
        }
        if (actualClassDef != null) {
          expectedClassCall = new ClassCallExpression(actualClassDef, expectedClassCall.getSortArgument(), ok ? expectedClassCall.getImplementedHere() : Collections.emptyMap(), expectedClassCall.getSort(), actualClassDef.hasUniverses());
          expectedClassCall.updateHasUniverses();
        }
        pseudoImplemented = new HashSet<>();
        exprResult = visitClassExt(((Concrete.ClassExtExpression) expr.getExpression()).getStatements(), null, expectedClassCall, pseudoImplemented, expr.getExpression());
        if (exprResult == null) {
          return null;
        }
      }
    }

    if (exprResult == null) {
      exprResult = checkExpr(expr.getExpression(), null);
      if (exprResult == null) {
        return null;
      }
    }

    Expression normExpr = exprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
    ClassCallExpression classCallExpr = normExpr.checkedCast(ClassCallExpression.class);
    if (classCallExpr == null) {
      TypecheckingError error = new TypecheckingError("Expected a class", expr.getExpression());
      myErrorReporter.report(error);
      return new Result(new ErrorExpression(null, error), normExpr);
    }

    if (checkAllImplemented(classCallExpr, pseudoImplemented, expr)) {
      return checkResult(expectedType, new Result(new NewExpression(classCallExpr), normExpr), expr);
    } else {
      return null;
    }
  }

  public boolean checkAllImplemented(ClassCallExpression classCall, Set<ClassField> pseudoImplemented, Concrete.SourceNode sourceNode) {
    int notImplemented = classCall.getDefinition().getNumberOfNotImplementedFields() - classCall.getImplementedHere().size();
    if (notImplemented == 0) {
      return true;
    } else {
      List<GlobalReferable> fields = new ArrayList<>(notImplemented);
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (!classCall.isImplemented(field) && !pseudoImplemented.contains(field)) {
          fields.add(field.getReferable());
        }
      }
      if (!fields.isEmpty()) {
        myErrorReporter.report(new FieldsImplementationError(false, fields, sourceNode));
      }
      return false;
    }
  }

  private Result typecheckLetClause(List<? extends Concrete.Parameter> parameters, Concrete.LetClause letClause, int argIndex) {
    if (parameters.isEmpty()) {
      Concrete.Expression letResult = letClause.getResultType();
      if (letResult != null) {
        Type type = checkType(letResult, ExpectedType.OMEGA);
        if (type == null) {
          return null;
        }

        Result result = checkExpr(letClause.getTerm(), type.getExpr());
        if (result == null) {
          return new Result(new ErrorExpression(type.getExpr(), null), type.getExpr());
        }
        if (result.expression.isInstance(ErrorExpression.class)) {
          result.expression = new ErrorExpression(type.getExpr(), result.expression.cast(ErrorExpression.class).getError());
        }
        return new Result(result.expression, type.getExpr());
      } else {
        return checkExpr(letClause.getTerm(), null);
      }
    }

    Concrete.Parameter param = parameters.get(0);
    if (param instanceof Concrete.NameParameter) {
      return bodyToLam(visitNameParameter((Concrete.NameParameter) param, argIndex, letClause), typecheckLetClause(parameters.subList(1, parameters.size()), letClause, argIndex + 1), letClause);
    } else if (param instanceof Concrete.TypeParameter) {
      SingleDependentLink link = visitTypeParameter((Concrete.TypeParameter) param, null);
      return link == null ? null : bodyToLam(link, typecheckLetClause(parameters.subList(1, parameters.size()), letClause, argIndex + param.getNumberOfParameters()), letClause);
    } else {
      throw new IllegalStateException();
    }
  }

  private void getLetClauseName(Concrete.LetClausePattern pattern, StringBuilder builder) {
    if (pattern.getReferable() != null) {
      builder.append(pattern.getReferable().textRepresentation());
    } else {
      boolean first = true;
      for (Concrete.LetClausePattern subPattern : pattern.getPatterns()) {
        if (first) {
          first = false;
        } else {
          builder.append('_');
        }
        getLetClauseName(subPattern, builder);
      }
    }
  }

  private Pair<LetClause,Expression> typecheckLetClause(Concrete.LetClause clause) {
    try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myContext)) {
      try (Utils.SetContextSaver ignore1 = new Utils.SetContextSaver<>(myFreeBindings)) {
        Result result = typecheckLetClause(clause.getParameters(), clause, 1);
        if (result == null) {
          return null;
        }
        StringBuilder builder = new StringBuilder();
        getLetClauseName(clause.getPattern(), builder);
        return new Pair<>(new LetClause(builder.toString(), null, result.expression), result.type);
      }
    }
  }

  /*
  private LetClausePattern typecheckLetClausePattern(Concrete.LetClausePattern pattern) {
    if (pattern.getReferable() != null) {
      return new NameLetClausePattern(pattern.getReferable().textRepresentation());
    }

    List<Pair<ClassField,LetClausePattern>> patterns = new ArrayList<>(pattern.getPatterns().size());
    for (Concrete.LetClausePattern subPattern : pattern.getPatterns()) {
      patterns.add(new Pair<>(typecheckLetClausePattern(subPattern)));
    }
    return new LetClausePattern(patterns);
  }
  */

  private LetClausePattern typecheckLetClausePattern(Concrete.LetClausePattern pattern, Expression expression, Expression type) {
    if (pattern.getReferable() != null) {
      String name = pattern.getReferable().textRepresentation();
      myContext.put(pattern.getReferable(), new EvaluatingBinding(name, expression, type));
      return new NameLetClausePattern(name);
    }

    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    SigmaExpression sigma = type.checkedCast(SigmaExpression.class);
    ClassCallExpression classCall = type.checkedCast(ClassCallExpression.class);
    List<ClassField> notImplementedFields = classCall == null ? null : classCall.getNotImplementedFields();
    int numberOfPatterns = pattern.getPatterns().size();
    if (sigma == null && classCall == null || sigma != null && DependentLink.Helper.size(sigma.getParameters()) != numberOfPatterns || notImplementedFields != null && notImplementedFields.size() != numberOfPatterns) {
      myErrorReporter.report(new TypeMismatchError("Cannot match an expression with the pattern", DocFactory.text(sigma == null && classCall == null ? "A sigma type or a record" : sigma != null ? "A sigma type with " + numberOfPatterns + " fields" : "A records with " + numberOfPatterns + " not implemented fields"), type, pattern));
      return null;
    }

    List<LetClausePattern> patterns = new ArrayList<>();
    DependentLink link = sigma == null ? null : sigma.getParameters();
    for (int i = 0; i < numberOfPatterns; i++) {
      assert link != null || notImplementedFields != null;
      Concrete.LetClausePattern subPattern = pattern.getPatterns().get(i);
      LetClausePattern letClausePattern = typecheckLetClausePattern(subPattern, link != null ? ProjExpression.make(expression, i) : FieldCallExpression.make(notImplementedFields.get(i), classCall.getSortArgument(), expression), link != null ? link.getTypeExpr() : notImplementedFields.get(i).getType(classCall.getSortArgument()).applyExpression(expression));
      if (letClausePattern == null) {
        return null;
      }
      patterns.add(letClausePattern);
      if (link != null) {
        link = link.getNext();
      }
    }

    return sigma == null ? new RecordLetClausePattern(notImplementedFields, patterns) : new TupleLetClausePattern(patterns);
  }

  @Override
  public Result visitLet(Concrete.LetExpression expr, ExpectedType expectedType) {
    try (Utils.SetContextSaver ignored = new Utils.SetContextSaver<>(myContext)) {
      List<? extends Concrete.LetClause> abstractClauses = expr.getClauses();
      List<LetClause> clauses = new ArrayList<>(abstractClauses.size());
      for (Concrete.LetClause clause : abstractClauses) {
        Pair<LetClause, Expression> pair = typecheckLetClause(clause);
        if (pair == null) {
          return null;
        }
        if (clause.getPattern().getReferable() != null) {
          pair.proj1.setPattern(new NameLetClausePattern(clause.getPattern().getReferable().textRepresentation()));
          myContext.put(clause.getPattern().getReferable(), pair.proj1);
        } else {
          LetClausePattern pattern = typecheckLetClausePattern(clause.getPattern(), new ReferenceExpression(pair.proj1), pair.proj2);
          if (pattern == null) {
            return null;
          }
          pair.proj1.setPattern(pattern);
        }
        clauses.add(pair.proj1);
      }

      Result result = checkExpr(expr.getExpression(), expectedType);
      if (result == null) {
        return null;
      }

      ExprSubstitution substitution = new ExprSubstitution();
      for (LetClause clause : clauses) {
        substitution.add(clause, clause.getExpression().subst(substitution));
      }
      return new Result(new LetExpression(clauses, result.expression), result.type.subst(substitution));
    }
  }

  @Override
  public Result visitNumericLiteral(Concrete.NumericLiteral expr, ExpectedType expectedType) {
    Expression resultExpr;
    BigInteger number = expr.getNumber();
    boolean isNegative = number.signum() < 0;
    try {
      int value = number.intValueExact();
      resultExpr = new SmallIntegerExpression(isNegative ? -value : value);
    } catch (ArithmeticException e) {
      resultExpr = new BigIntegerExpression(isNegative ? number.negate() : number);
    }

    Result result;
    if (isNegative) {
      result = new Result(ExpressionFactory.Neg(resultExpr), ExpressionFactory.Int());
    } else {
      result = new Result(resultExpr, ExpressionFactory.Nat());
    }
    return checkResult(expectedType, result, expr);
  }

  @Override
  public Result visitTyped(Concrete.TypedExpression expr, ExpectedType expectedType) {
    Type type = checkType(expr.type, ExpectedType.OMEGA);
    if (type == null) {
      return checkExpr(expr.expression, expectedType);
    } else {
      return checkResult(expectedType, checkExpr(expr.expression, type.getExpr()), expr);
    }
  }
}
