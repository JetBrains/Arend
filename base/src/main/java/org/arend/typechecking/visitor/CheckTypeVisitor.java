package org.arend.typechecking.visitor;

import org.arend.core.context.LinkList;
import org.arend.core.context.Utils;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.TypedEvaluatingBinding;
import org.arend.core.context.binding.inference.*;
import org.arend.core.context.param.*;
import org.arend.core.definition.*;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.*;
import org.arend.core.expr.let.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.*;
import org.arend.ext.ArendExtension;
import org.arend.ext.FreeBindingsModifier;
import org.arend.ext.concrete.ConcreteNumberPattern;
import org.arend.ext.concrete.ConcreteParameter;
import org.arend.ext.concrete.ConcretePattern;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteLamExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreInferenceVariable;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.expr.AbstractedExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.CoreInferenceReferenceExpression;
import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.core.level.CoreSort;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.core.ops.SubstitutionPair;
import org.arend.ext.error.*;
import org.arend.ext.instance.InstanceSearchParameters;
import org.arend.ext.instance.SubclassSearchParameters;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.typechecking.*;
import org.arend.ext.variable.Variable;
import org.arend.extImpl.AbstractedDependentLinkType;
import org.arend.extImpl.AbstractedExpressionImpl;
import org.arend.extImpl.ContextDataImpl;
import org.arend.extImpl.UncheckedExpressionImpl;
import org.arend.extImpl.userData.UserDataHolderImpl;
import org.arend.naming.reference.*;
import org.arend.naming.renamer.Renamer;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.term.concrete.ConcreteLevelExpressionVisitor;
import org.arend.typechecking.FieldDFS;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.TypecheckingContext;
import org.arend.typechecking.computation.ComputationRunner;
import org.arend.typechecking.doubleChecker.CoreException;
import org.arend.typechecking.doubleChecker.CoreExpressionChecker;
import org.arend.typechecking.error.CycleError;
import org.arend.typechecking.error.ErrorReporterCounter;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.arend.typechecking.implicitargs.ImplicitArgsInference;
import org.arend.typechecking.implicitargs.StdImplicitArgsInference;
import org.arend.typechecking.implicitargs.equations.*;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;
import org.arend.typechecking.order.MapDFS;
import org.arend.typechecking.patternmatching.*;
import org.arend.typechecking.result.DefCallResult;
import org.arend.typechecking.result.TResult;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.util.Pair;
import org.arend.util.SingletonList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

import static org.arend.typechecking.error.local.inference.ArgInferenceError.expression;

public class CheckTypeVisitor extends UserDataHolderImpl implements ConcreteExpressionVisitor<Expression, TypecheckingResult>, ConcreteLevelExpressionVisitor<LevelVariable, Level>, ExpressionTypechecker {
  private final Equations myEquations;
  private GlobalInstancePool myInstancePool;
  private final ImplicitArgsInference myArgsInference;
  protected Map<Referable, Binding> context;
  private MyErrorReporter errorReporter;
  private final List<ClassCallExpression.ClassCallBinding> myClassCallBindings = new ArrayList<>();
  private final List<DeferredMeta> myDeferredMetasBeforeSolver = new ArrayList<>();
  private final List<DeferredMeta> myDeferredMetasBeforeLevels = new ArrayList<>();
  private final List<DeferredMeta> myDeferredMetasAfterLevels = new ArrayList<>();
  private final ArendExtension myArendExtension;
  private TypecheckerState mySavedState;
  private Stage myCurrentStage;

  private static class DeferredMeta {
    final MetaDefinition meta;
    final Map<Referable, Binding> context;
    final ContextDataImpl contextData;
    final InferenceVariable inferenceVar;
    final MyErrorReporter errorReporter;

    private DeferredMeta(MetaDefinition meta, Map<Referable, Binding> context, ContextDataImpl contextData, InferenceVariable inferenceVar, MyErrorReporter errorReporter) {
      this.meta = meta;
      this.context = context;
      this.contextData = contextData;
      this.inferenceVar = inferenceVar;
      this.errorReporter = errorReporter;
    }
  }

  public static class MyErrorReporter implements ErrorReporter {
    private final CountingErrorReporter myErrorReporter;
    private Definition.TypeCheckingStatus myStatus = Definition.TypeCheckingStatus.NO_ERRORS;

    private MyErrorReporter(ErrorReporter errorReporter) {
      myErrorReporter = new CountingErrorReporter(GeneralError.Level.ERROR, errorReporter);
    }

    private void setStatus(GeneralError error) {
      myStatus = myStatus.max(error.level == GeneralError.Level.ERROR ? Definition.TypeCheckingStatus.HAS_ERRORS : Definition.TypeCheckingStatus.HAS_WARNINGS);
    }

    @Override
    public void report(GeneralError error) {
      setStatus(error);
      myErrorReporter.report(error);
    }
  }

  public void setStatus(Definition.TypeCheckingStatus status) {
    errorReporter.myStatus = errorReporter.myStatus.max(status);
  }

  private CheckTypeVisitor(Map<Referable, Binding> localContext, ErrorReporter errorReporter, GlobalInstancePool pool, ArendExtension arendExtension, UserDataHolderImpl holder, Stage currentStage) {
    this.errorReporter = new MyErrorReporter(errorReporter);
    myEquations = new TwoStageEquations(this);
    myInstancePool = pool;
    myArgsInference = new StdImplicitArgsInference(this);
    context = localContext;
    myArendExtension = arendExtension;
    if (holder != null) {
      setUserData(holder);
    }
    myCurrentStage = currentStage;
  }

  public CheckTypeVisitor(ErrorReporter errorReporter, GlobalInstancePool pool, ArendExtension arendExtension) {
    this(new LinkedHashMap<>(), errorReporter, pool, arendExtension, null, null);
  }

  public ArendExtension getExtension() {
    return myArendExtension;
  }

  public TypecheckingContext saveTypecheckingContext() {
    return new TypecheckingContext(new LinkedHashMap<>(context), myInstancePool.getInstanceProvider(), myInstancePool.getInstancePool(), myArendExtension, copyUserData(), myCurrentStage);
  }

  public static CheckTypeVisitor loadTypecheckingContext(TypecheckingContext typecheckingContext, ErrorReporter errorReporter) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(typecheckingContext.localContext, errorReporter, null, typecheckingContext.arendExtension, typecheckingContext.userDataHolder, typecheckingContext.currentStage);
    visitor.setInstancePool(new GlobalInstancePool(typecheckingContext.instanceProvider, visitor, typecheckingContext.localInstancePool));
    return visitor;
  }

  public Referable addBinding(@Nullable Referable referable, Binding binding) {
    Referable ref = referable != null ? referable : new FakeLocalReferable(binding.getName());
    context.put(ref, binding);
    return ref;
  }

  public void addBindings(Map<Referable, Binding> bindings) {
    context.putAll(bindings);
  }

  public GlobalInstancePool getInstancePool() {
    return myInstancePool;
  }

  public void setInstancePool(GlobalInstancePool pool) {
    myInstancePool = pool;
  }

  @NotNull
  public Map<Referable, Binding> getContext() {
    return context;
  }

  public void copyContextFrom(Map<? extends Referable, ? extends Binding> context) {
    this.context = new LinkedHashMap<>(context);
  }

  public Set<Binding> getAllBindings() {
    return new HashSet<>(context.values());
  }

  private static class VeryFakeLocalReferable extends FakeLocalReferable {
    public VeryFakeLocalReferable(String name) {
      super(name);
    }
  }

  @Override
  public @NotNull List<CoreBinding> getFreeBindingsList() {
    List<CoreBinding> result = new ArrayList<>();
    for (Map.Entry<Referable, Binding> entry : context.entrySet()) {
      if (!(entry.getKey() instanceof VeryFakeLocalReferable)) {
        result.add(entry.getValue());
      }
    }
    return result;
  }

  @Override
  public @Nullable CoreBinding getFreeBinding(@NotNull ArendRef ref) {
    if (!(ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return context.get(ref);
  }

  @NotNull
  @Override
  public ErrorReporter getErrorReporter() {
    return errorReporter;
  }

  public Equations getEquations() {
    return myEquations;
  }

  public int getNumberOfErrors() {
    return errorReporter.myErrorReporter.getErrorsNumber();
  }

  public Definition.TypeCheckingStatus getStatus() {
    return errorReporter.myStatus;
  }

  @Override
  public boolean compare(@NotNull UncheckedExpression expr1, @NotNull UncheckedExpression expr2, @NotNull CMP cmp, @Nullable ConcreteSourceNode marker, boolean allowEquations, boolean normalize) {
    CompareVisitor visitor = new CompareVisitor(myEquations, cmp, marker instanceof Concrete.SourceNode ? (Concrete.SourceNode) marker : null);
    if (!allowEquations) {
      visitor.doNotAllowEquations();
    }
    if (!normalize) {
      visitor.doNotNormalize();
    }
    return visitor.compare(UncheckedExpressionImpl.extract(expr1), UncheckedExpressionImpl.extract(expr2), null);
  }

  public TypecheckingResult checkResult(Expression expectedType, TypecheckingResult result, Concrete.Expression expr) {
    boolean isOmega = expectedType instanceof Type && ((Type) expectedType).isOmega();
    if (result == null || expectedType == null || isOmega && result.type instanceof UniverseExpression) {
      return result;
    }

    CompareVisitor cmpVisitor = new CompareVisitor(DummyEquations.getInstance(), CMP.LE, expr);
    if (!isOmega && cmpVisitor.nonNormalizingCompare(result.type, expectedType, Type.OMEGA)) {
      return result;
    }

    result.type = result.type.normalize(NormalizationMode.WHNF);
    expectedType = expectedType.normalize(NormalizationMode.WHNF);
    if (!isOmega) {
      ClassCallExpression actualClassCall = result.type.cast(ClassCallExpression.class);
      ClassCallExpression expectedClassCall = expectedType.cast(ClassCallExpression.class);
      if (actualClassCall != null && expectedClassCall != null && actualClassCall.getDefinition().isSubClassOf(expectedClassCall.getDefinition())) {
        boolean replace = false;
        for (ClassField field : expectedClassCall.getImplementedHere().keySet()) {
          if (!actualClassCall.isImplemented(field)) {
            replace = true;
            break;
          }
        }

        if (replace) {
          if (!actualClassCall.getImplementedHere().isEmpty()) {
            actualClassCall = new ClassCallExpression(actualClassCall.getDefinition(), actualClassCall.getSortArgument(), Collections.emptyMap(), actualClassCall.getSort(), actualClassCall.getUniverseKind());
          }
          result.expression = new NewExpression(result.expression, actualClassCall);
          result.type = result.expression.getType();
          return checkResultExpr(expectedClassCall, result, expr);
        }
      }
    }

    TypecheckingResult coercedResult = CoerceData.coerce(result, expectedType, expr, this);
    if (coercedResult != null) {
      return coercedResult;
    }

    return isOmega ? result : checkResultExpr(expectedType, result, expr);
  }

  private TypecheckingResult checkResultExpr(Expression expectedType, TypecheckingResult result, Concrete.Expression expr) {
    if (new CompareVisitor(myEquations, CMP.LE, expr).normalizedCompare(result.type, expectedType, Type.OMEGA)) {
      result.expression = OfTypeExpression.make(result.expression, result.type, expectedType);
      return result;
    }

    if (!result.type.isError()) {
      errorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
    }
    return null;
  }

  public boolean checkNormalizedResult(Expression expectedType, TypecheckingResult result, Concrete.Expression expr, boolean strict) {
    boolean isOmega = expectedType instanceof Type && ((Type) expectedType).isOmega();
    if (isOmega && result.type.isInstance(UniverseExpression.class) || expectedType != null && !isOmega && new CompareVisitor(strict ? new LevelEquationsWrapper(myEquations) : myEquations, CMP.LE, expr).normalizedCompare(result.type, expectedType, Type.OMEGA)) {
      if (!strict && !isOmega) {
        result.expression = OfTypeExpression.make(result.expression, result.type, expectedType);
      }
      return true;
    }

    if (!strict && !result.type.isError()) {
      errorReporter.report(new TypeMismatchError(expectedType, result.type, expr));
    }

    return false;
  }

  private TypecheckingResult tResultToResult(Expression expectedType, TResult result, Concrete.Expression expr) {
    if (result != null) {
      result = myArgsInference.inferTail(result, expectedType, expr);
    }
    return result == null ? null : checkResult(expectedType, result.toResult(this), expr);
  }

  @Nullable
  @Override
  public TypecheckingResult typecheck(@NotNull ConcreteExpression expression, @Nullable CoreExpression expectedType) {
    if (!(expression instanceof Concrete.Expression && (expectedType == null || expectedType instanceof Expression))) {
      throw new IllegalArgumentException();
    }
    Concrete.Expression expr = DesugarVisitor.desugar(((Concrete.Expression) expression).accept(new ReplaceVarConcreteVisitor(context.keySet()), null), errorReporter);
    Expression type = expectedType == null ? null : (Expression) expectedType;
    TypecheckingResult result = checkExpr(expr, type);
    if (result == null || result.expression.isError()) {
      if (result == null) {
        result = new TypecheckingResult(null, type == null ? new ErrorExpression() : type);
      }
      result.expression = new ErrorWithConcreteExpression(expr);
    }
    return result;
  }

  @Nullable
  @Override
  public TypecheckingResult check(@NotNull UncheckedExpression expression, @NotNull ConcreteSourceNode sourceNode) {
    if (!(sourceNode instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }

    Expression expr = UncheckedExpressionImpl.extract(expression);
    try {
      return new TypecheckingResult(expr, expr.accept(new CoreExpressionChecker(getAllBindings(), myEquations, (Concrete.SourceNode) sourceNode), null));
    } catch (CoreException e) {
      errorReporter.report(e.error);
      return null;
    }
  }

  @Override
  public @Nullable TypecheckingResult replaceType(@NotNull TypedExpression typedExpression, @NotNull CoreExpression type, @Nullable ConcreteSourceNode marker) {
    if (!(type instanceof Expression && marker instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }
    TypecheckingResult result = TypecheckingResult.fromChecked(typedExpression);
    return result.type.isError() ? result : CompareVisitor.compare(myEquations, CMP.LE, result.type, (Expression) type, Type.OMEGA, (Concrete.SourceNode) marker) ? new TypecheckingResult(result.expression, (Expression) type) : null;
  }

  @Override
  public @Nullable DependentLink typecheckParameters(@NotNull Collection<? extends ConcreteParameter> parameters) {
    return visitParameters(parameters, null, null);
  }

  @Override
  public @NotNull Concrete.Pattern desugarNumberPattern(@NotNull ConcreteNumberPattern pattern) {
    if (!(pattern instanceof Concrete.NumberPattern)) {
      throw new IllegalArgumentException();
    }
    return DesugarVisitor.desugarNumberPattern((Concrete.NumberPattern) pattern, errorReporter);
  }

  @Override
  public @Nullable List<CorePattern> typecheckPatterns(@NotNull Collection<? extends ConcretePattern> patterns, @NotNull CoreParameter parameters, @NotNull ConcreteSourceNode marker) {
    if (!(parameters instanceof DependentLink)) {
      throw new IllegalArgumentException();
    }
    List<Concrete.Pattern> patterns1 = new ArrayList<>(patterns.size());
    for (ConcretePattern pattern : patterns) {
      if (!(pattern instanceof Concrete.Pattern)) {
        throw new IllegalArgumentException();
      }
      patterns1.add((Concrete.Pattern) pattern);
    }
    try (var ignored = new Utils.SetContextSaver<>(context)) {
      PatternTypechecking.Result result = new PatternTypechecking(errorReporter, PatternTypechecking.Mode.CASE, this, false, null, Collections.emptyList()).typecheckPatterns(patterns1, null, (DependentLink) parameters, new ExprSubstitution(), new ExprSubstitution(), marker);
      //noinspection unchecked
      return result == null ? null : (List<CorePattern>) (List<?>) result.getPatterns();
    }
  }

  private static class ListParametersProvider implements ParametersProvider {
    private DependentLink parameter;
    private SingleDependentLink single = EmptyDependentLink.getInstance();
    private final ExprSubstitution substitution = new ExprSubstitution();

    ListParametersProvider(DependentLink parameter) {
      this.parameter = parameter;
    }

    @Override
    public @Nullable SingleDependentLink nextParameter() {
      if (!single.hasNext()) {
        if (!parameter.hasNext()) {
          return null;
        }

        List<String> names = new ArrayList<>();
        parameter = parameter.getNextTyped(names);
        single = ExpressionFactory.singleParams(parameter.isExplicit(), names, parameter.getType().subst(new SubstVisitor(substitution, LevelSubstitution.EMPTY)));
        parameter = parameter.getNext();
      }

      SingleDependentLink result = single;
      single = single.getNext();
      return result;
    }

    @Override
    public @Nullable Expression getType() {
      return null;
    }

    @Override
    public void subst(DependentLink param, Expression expr) {
      substitution.add(param, expr);
    }
  }

  @Override
  public @Nullable TypedExpression typecheckLambda(@NotNull ConcreteLamExpression expr, @NotNull CoreParameter parameters) {
    if (!(parameters instanceof DependentLink && expr instanceof Concrete.LamExpression)) {
      throw new IllegalArgumentException();
    }

    Concrete.LamExpression lamExpr = (Concrete.LamExpression) expr;
    try (var ignored = new Utils.SetContextSaver<>(context)) {
      return visitLam(lamExpr.getParameters(), lamExpr, new ListParametersProvider((DependentLink) parameters));
    }
  }

  @Override
  public @NotNull DependentLink makeParameters(@NotNull List<? extends CoreExpression> types, @NotNull ConcreteSourceNode marker) {
    if (!(marker instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }

    DependentLink result = EmptyDependentLink.getInstance();
    for (int i = types.size() - 1; i >= 0; i--) {
      if (!(types.get(i) instanceof Expression)) {
        throw new IllegalArgumentException();
      }
      Expression typeExpr = (Expression) types.get(i);
      result = new TypedDependentLink(true, null, typeExpr instanceof Type ? (Type) typeExpr : new TypeExpression(typeExpr, getSortOfType(typeExpr, (Concrete.SourceNode) marker)), result);
    }
    return result;
  }

  @Override
  public @NotNull CoreParameter mergeParameters(@NotNull List<? extends CoreParameter> parameters) {
    if (parameters.isEmpty()) {
      return EmptyDependentLink.getInstance();
    }
    CoreParameter param = parameters.get(parameters.size() - 1);
    if (!(param instanceof DependentLink)) {
      throw new IllegalArgumentException();
    }

    DependentLink link = (DependentLink) param;
    for (int i = parameters.size() - 2; i >= 0; i--) {
      if (!(parameters.get(i) instanceof DependentLink)) {
        throw new IllegalArgumentException();
      }
      if (parameters.get(i).hasNext()) {
        DependentLink link1 = DependentLink.Helper.copy((DependentLink) parameters.get(i));
        DependentLink.Helper.getLast(link1).setNext(link);
        link = link1;
      }
    }
    return link;
  }

  private void checkSubstExpr(Expression expr, Collection<? extends CoreBinding> bindings) {
    Set<CoreBinding> bindingsSet = new HashSet<>(bindings);

    Set<Binding> freeBindings = FreeVariablesCollector.getFreeVariables(expr);
    //noinspection SuspiciousMethodCalls
    freeBindings.removeAll(bindings);
    for (Binding binding : freeBindings) {
      if (binding.getTypeExpr().findFreeBindings(bindingsSet) != null) {
        throw new IllegalArgumentException("Invalid substitution");
      }
    }

    for (CoreBinding binding : bindings) {
      bindingsSet.remove(binding);
      if (binding.getTypeExpr().findFreeBindings(bindingsSet) != null) {
        throw new IllegalArgumentException("Invalid substitution");
      }
    }
  }

  @Override
  public @Nullable Expression substitute(@NotNull CoreExpression expression, @Nullable CoreSort sort, @NotNull List<SubstitutionPair> substPairs) {
    if (!(expression instanceof Expression && (sort == null || sort instanceof Sort))) throw new IllegalArgumentException();
    if (substPairs.isEmpty()) {
      return sort == null ? (Expression) expression : ((Expression) expression).subst(((Sort) sort).toLevelSubstitution());
    }

    List<CoreBinding> substVars = new ArrayList<>(substPairs.size());
    for (SubstitutionPair pair : substPairs) {
      substVars.add(pair.binding);
    }
    checkSubstExpr((Expression) expression, substVars);

    ExprSubstitution substitution = new ExprSubstitution();
    SubstVisitor substVisitor = new SubstVisitor(substitution, sort == null ? LevelSubstitution.EMPTY : ((Sort) sort).toLevelSubstitution());
    for (SubstitutionPair pair : substPairs) {
      if (!(pair.binding instanceof Binding)) {
        throw new IllegalArgumentException();
      }
      Expression type = ((Binding) pair.binding).getTypeExpr();
      TypecheckingResult result = typecheck(pair.expression, substVisitor.isEmpty() ? type : type.accept(substVisitor, null));
      if (result == null) return null;
      substitution.add((Binding) pair.binding, result.expression);
    }

    return ((Expression) expression).accept(substVisitor, null);
  }

  @Override
  public @Nullable AbstractedExpression substituteAbstractedExpression(@NotNull AbstractedExpression expression, @Nullable CoreSort sort, @NotNull List<? extends ConcreteExpression> arguments) {
    if (arguments.isEmpty()) {
      return expression;
    }
    if (!(sort == null || sort instanceof Sort)) {
      throw new IllegalArgumentException();
    }

    int i = 0;
    ExprSubstitution substitution = new ExprSubstitution();
    SubstVisitor substVisitor = new SubstVisitor(substitution, sort == null ? LevelSubstitution.EMPTY : ((Sort) sort).toLevelSubstitution());
    while (i < arguments.size()) {
      DependentLink link;
      int drop = 0;
      if (expression instanceof AbstractedExpressionImpl) {
        link = ((AbstractedExpressionImpl) expression).getParameters();
      } else if (expression instanceof AbstractedDependentLinkType) {
        link = ((AbstractedDependentLinkType) expression).getParameters();
        drop = arguments.size() - i;
        if (drop > ((AbstractedDependentLinkType) expression).getSize()) {
          throw new IllegalArgumentException();
        }
      } else {
        throw new IllegalArgumentException();
      }

      for (; i < arguments.size(); i++, link = link.getNext()) {
        TypecheckingResult arg = typecheck(arguments.get(i), link.getTypeExpr().accept(substVisitor, null));
        if (arg == null || arg.expression.isError()) {
          return null;
        }
        substitution.add(link, arg.expression);
      }

      if (expression instanceof AbstractedDependentLinkType) {
        AbstractedDependentLinkType abs = (AbstractedDependentLinkType) expression;
        return AbstractedExpressionImpl.subst(AbstractedDependentLinkType.make(DependentLink.Helper.get(abs.getParameters(), drop), abs.getSize() - drop), substVisitor);
      }

      AbstractedExpressionImpl abs = (AbstractedExpressionImpl) expression;
      if (link.hasNext()) {
        return AbstractedExpressionImpl.subst(AbstractedExpressionImpl.make(link, abs.getExpression()), substVisitor);
      }
      expression = abs.getExpression();
    }
    return AbstractedExpressionImpl.subst(expression, substVisitor);
  }

  @Override
  public @NotNull CoreExpression makeLambda(@NotNull List<? extends CoreParameter> parameters, @NotNull CoreExpression body, @NotNull ConcreteSourceNode marker) {
    if (parameters.size() == 0) return body;
    if (!(body instanceof Expression && marker instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }
    for (CoreParameter parameter : parameters) {
      if (!(parameter instanceof DependentLink)) throw new IllegalArgumentException();
    }
    //noinspection unchecked
    checkSubstExpr((Expression) body, (Collection<? extends CoreBinding>) parameters);

    Sort sort = getSortOfType(((Expression) body).computeType(), (Concrete.SourceNode) marker);
    if (parameters.size() == 1 && parameters.get(0) instanceof SingleDependentLink) {
      SingleDependentLink param = (SingleDependentLink) parameters.get(0);
      return new LamExpression(PiExpression.generateUpperBound(param.getType().getSortOfType(), sort, myEquations, (Concrete.SourceNode) marker), param, (Expression) body);
    }

    List<Pair<SingleDependentLink,Sort>> params = new ArrayList<>();
    ExprSubstitution substitution = new ExprSubstitution();
    for (CoreParameter parameter : parameters) {
      DependentLink param = (DependentLink) parameter;
      sort = PiExpression.generateUpperBound(param.getType().getSortOfType(), sort, myEquations, (Concrete.SourceNode) marker);
      params.add(new Pair<>(new TypedSingleDependentLink(param.isExplicit(), param.getName(), param.getType()), sort));
    }

    Expression result = ((Expression) body).subst(substitution);
    for (int i = params.size() - 1; i >= 0; i--) {
      result = new LamExpression(params.get(i).proj2, params.get(i).proj1, result);
    }
    return result;
  }

  @Override
  public @Nullable CoreParameter substituteParameters(@NotNull CoreParameter parameters, @Nullable CoreSort sort, @NotNull List<? extends ConcreteExpression> arguments) {

    boolean allNull = true;
    for (ConcreteExpression argument : arguments) {
      if (argument != null) {
        allNull = false;
        break;
      }
    }
    if (allNull) {
      return parameters;
    }

    if (!(parameters instanceof DependentLink && (sort == null || sort instanceof Sort))) {
      throw new IllegalArgumentException();
    }

    List<DependentLink> links = DependentLink.Helper.toList((DependentLink) parameters);
    if (arguments.size() > links.size()) {
      throw new IllegalArgumentException();
    }

    ExprSubstitution substitution = new ExprSubstitution();
    SubstVisitor substVisitor = new SubstVisitor(substitution, sort == null ? LevelSubstitution.EMPTY : ((Sort) sort).toLevelSubstitution());
    LinkList list = new LinkList();
    for (int i = 0; i < links.size();) {
      int typedIndex = i;
      while (links.get(typedIndex) instanceof UntypedDependentLink) {
        typedIndex++;
      }
      int last = typedIndex;
      for (; last >= i; last--) {
        if (last >= arguments.size() || arguments.get(last) == null) {
          break;
        }
      }

      Type type = links.get(typedIndex).getType().subst(substVisitor);
      for (; i <= typedIndex; i++) {
        if (i >= arguments.size() || arguments.get(i) == null) {
          DependentLink link = i < last ? new UntypedDependentLink(links.get(i).getName()) : new TypedDependentLink(links.get(typedIndex).isExplicit(), links.get(i).getName(), type, links.get(typedIndex).isHidden(), EmptyDependentLink.getInstance());
          substitution.add(links.get(i), new ReferenceExpression(link));
          list.append(link);
        } else {
          TypecheckingResult result = typecheck(arguments.get(i), type.getExpr());
          if (result == null) {
            return null;
          }
          substitution.add(links.get(i), result.expression);
        }
      }
    }
    return list.getFirst();
  }

  public TypecheckingResult checkExpr(Concrete.Expression expr, Expression expectedType) {
    try {
      return expr.accept(this, expectedType);
    } catch (IncorrectExpressionException e) {
      errorReporter.report(new TypecheckingError(e.getMessage(), expr));
      return null;
    }
  }

  public TypecheckingResult finalCheckExpr(Concrete.Expression expr, Expression expectedType) {
    return finalize(checkExpr(expr, expectedType), expr, false);
  }

  private void invokeDeferredMetas(InPlaceLevelSubstVisitor substVisitor, StripVisitor stripVisitor, Stage stage) {
    myCurrentStage = stage;
    List<DeferredMeta> deferredMetas = stage == Stage.BEFORE_SOLVER ? myDeferredMetasBeforeSolver : stage == Stage.BEFORE_LEVELS ? myDeferredMetasBeforeLevels : myDeferredMetasAfterLevels;
    // Indexed loop is required since deferredMetas can be modified during the loop
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < deferredMetas.size(); i++) {
      DeferredMeta deferredMeta = deferredMetas.get(i);
      Expression type = deferredMeta.contextData.getExpectedType();
      if (substVisitor != null && !substVisitor.isEmpty()) {
        type.accept(substVisitor, null);
      }
      if (stripVisitor != null) {
        type = type.accept(stripVisitor, null);
        deferredMeta.contextData.setExpectedType(type.accept(new StripVisitor(), null));

        TypedDependentLink lastTyped = null;
        for (Binding binding : deferredMeta.context.values()) {
          if (binding instanceof UntypedDependentLink) {
            TypedDependentLink typed = ((UntypedDependentLink) binding).getNextTyped(null);
            if (typed != lastTyped) {
              lastTyped = typed;
              typed.subst(substVisitor);
              typed.strip(stripVisitor);
            }
          } else {
            if (binding != lastTyped) {
              binding.subst(substVisitor);
              binding.strip(stripVisitor);
            }
            lastTyped = null;
          }
        }
      }

      CheckTypeVisitor checkTypeVisitor;
      MyErrorReporter originalErrorReporter = errorReporter;
      Map<Referable, Binding> originalContext = context;
      if (stage != Stage.AFTER_LEVELS) {
        checkTypeVisitor = this;
        errorReporter = deferredMeta.errorReporter;
        context = deferredMeta.context;
      } else {
        checkTypeVisitor = new CheckTypeVisitor(deferredMeta.context, deferredMeta.errorReporter, null, myArendExtension, this, myCurrentStage);
        checkTypeVisitor.setInstancePool(new GlobalInstancePool(myInstancePool.getInstanceProvider(), checkTypeVisitor, myInstancePool.getInstancePool()));
      }

      int numberOfErrors = checkTypeVisitor.getNumberOfErrors();
      Concrete.ReferenceExpression refExpr = deferredMeta.contextData.getReferenceExpression();
      TypecheckingResult result = checkTypeVisitor.invokeMeta(deferredMeta.meta, deferredMeta.contextData);
      fixCheckedExpression(result, refExpr.getReferent(), refExpr);
      if (result != null) {
        result = checkTypeVisitor.checkResult(type, result, refExpr);
        if (stage == Stage.AFTER_LEVELS) {
          result = checkTypeVisitor.finalize(result, refExpr, false);
        }
      }
      errorReporter = originalErrorReporter;
      context = originalContext;
      if (result == null && checkTypeVisitor.getNumberOfErrors() == numberOfErrors) {
        deferredMeta.errorReporter.report(new TypecheckingError("Meta '" + refExpr.getReferent().getRefName() + "' failed", refExpr));
      }
      deferredMeta.inferenceVar.solve(this, result == null ? new ErrorExpression() : result.expression);
    }
    deferredMetas.clear();
    myCurrentStage = null;
  }

  public TypecheckingResult finalize(TypecheckingResult result, Concrete.SourceNode sourceNode, boolean propIfPossible) {
    if (result == null) {
      return null;
    }

    invokeDeferredMetas(null, null, Stage.BEFORE_SOLVER);
    myEquations.solveEquations();
    invokeDeferredMetas(null, null, Stage.BEFORE_LEVELS);
    LevelEquationsSolver levelSolver = myEquations.makeLevelEquationsSolver();
    if (propIfPossible) {
      Sort sort = result.type.getSortOfType();
      if (sort != null) {
        levelSolver.addPropEquationIfPossible(sort.getHLevel());
      }
    }
    LevelSubstitution levelSubstitution = levelSolver.solveLevels();
    myEquations.finalizeEquations(levelSubstitution, sourceNode);
    InPlaceLevelSubstVisitor substVisitor = new InPlaceLevelSubstVisitor(levelSubstitution);
    if (!substVisitor.isEmpty()) {
      if (result.expression != null) {
        result.expression.accept(substVisitor, null);
      }
      result.type.accept(substVisitor, null);
    }

    ErrorReporterCounter counter = new ErrorReporterCounter(GeneralError.Level.ERROR, errorReporter);
    StripVisitor stripVisitor = new StripVisitor(counter);
    invokeDeferredMetas(substVisitor, stripVisitor, Stage.AFTER_LEVELS);
    if (result.expression != null) {
      result.expression = result.expression.accept(stripVisitor, null);
    }
    stripVisitor.setErrorReporter(counter.getErrorsNumber() == 0 ? errorReporter : DummyErrorReporter.INSTANCE);
    result.type = result.type.accept(stripVisitor, null);
    return result;
  }

  public Type checkType(Concrete.Expression expr, Expression expectedType) {
    if (expr == null) {
      assert false;
      errorReporter.report(new LocalError(GeneralError.Level.ERROR, "Incomplete expression"));
      return null;
    }

    TypecheckingResult result;
    try {
      Expression expectedType1 = expectedType;
      boolean isOmega = expectedType instanceof Type && ((Type) expectedType).isOmega();
      if (!isOmega) {
        expectedType = expectedType.normalize(NormalizationMode.WHNF);
        if (expectedType.getStuckInferenceVariable() != null) {
          expectedType1 = Type.OMEGA;
        }
      }

      result = expr.accept(this, expectedType1);
      if (result != null && expectedType1 != expectedType) {
        result.type = result.type.normalize(NormalizationMode.WHNF);
        result = checkResultExpr(expectedType, result, expr);
      }
    } catch (IncorrectExpressionException e) {
      errorReporter.report(new TypecheckingError(e.getMessage(), expr));
      return null;
    }
    if (result == null) {
      return null;
    }
    if (result.expression instanceof Type) {
      return (Type) result.expression;
    }

    Expression type = result.type.normalize(NormalizationMode.WHNF);
    UniverseExpression universe = type.cast(UniverseExpression.class);
    if (universe == null) {
      Expression stuck = type.getStuckExpression();
      if (stuck == null || !stuck.isInstance(InferenceReferenceExpression.class) && !stuck.isError()) {
        if (stuck == null || !stuck.isError()) {
          errorReporter.report(new TypeMismatchError(DocFactory.text("a universe"), type, expr));
        }
        return null;
      }

      universe = new UniverseExpression(Sort.generateInferVars(myEquations, false, expr));
      InferenceVariable infVar = stuck.getInferenceVariable();
      if (infVar != null) {
        myEquations.addEquation(type, universe, Type.OMEGA, CMP.LE, expr, infVar, null);
      }
    }

    return new TypeExpression(result.expression, universe.getSort());
  }

  public Type finalCheckType(Concrete.Expression expr, Expression expectedType, boolean propIfPossible) {
    Type result = checkType(expr, expectedType);
    if (result == null) return null;
    invokeDeferredMetas(null, null, Stage.BEFORE_SOLVER);
    myEquations.solveEquations();
    invokeDeferredMetas(null, null, Stage.BEFORE_LEVELS);
    LevelEquationsSolver levelSolver = myEquations.makeLevelEquationsSolver();
    if (propIfPossible) {
      Sort sort = result.getSortOfType();
      if (sort != null) {
        levelSolver.addPropEquationIfPossible(sort.getHLevel());
      }
    }
    LevelSubstitution levelSubstitution = levelSolver.solveLevels();
    myEquations.finalizeEquations(levelSubstitution, expr);
    InPlaceLevelSubstVisitor substVisitor = new InPlaceLevelSubstVisitor(levelSubstitution);
    if (!substVisitor.isEmpty()) {
      result.subst(substVisitor);
    }
    StripVisitor stripVisitor = new StripVisitor(errorReporter);
    invokeDeferredMetas(substVisitor, stripVisitor, Stage.AFTER_LEVELS);
    return result.strip(stripVisitor);
  }

  public TypecheckingResult checkArgument(Concrete.Expression expr, Expression expectedType, TResult result) {
    Concrete.ThisExpression thisExpr = null;
    Binding binding = null;
    if (expr instanceof Concrete.ThisExpression) {
      thisExpr = (Concrete.ThisExpression) expr;
    } else if (expr instanceof Concrete.TypedExpression) {
      Concrete.TypedExpression typedExpr = (Concrete.TypedExpression) expr;
      if (!myClassCallBindings.isEmpty() && typedExpr.expression instanceof Concrete.ThisExpression && ((Concrete.ThisExpression) typedExpr.expression).getReferent() == null && typedExpr.type instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) typedExpr.type).getReferent() instanceof TCDefReferable) {
        Definition def = ((TCDefReferable) ((Concrete.ReferenceExpression) typedExpr.type).getReferent()).getTypechecked();
        if (def instanceof ClassDefinition) {
          for (int i = myClassCallBindings.size() - 1; i >= 0; i--) {
            if (myClassCallBindings.get(i).getTypeExpr().getDefinition() == def) {
              thisExpr = (Concrete.ThisExpression) typedExpr.expression;
              binding = myClassCallBindings.get(i);
              break;
            }
          }
        }
      }
    }

    if (thisExpr != null && (result == null || result instanceof DefCallResult && ((DefCallResult) result).getDefinition().isGoodParameter(((DefCallResult) result).getArguments().size()))) {
      boolean ok = true;
      TResult tResult;
      if (thisExpr.getReferent() != null) {
         tResult = getLocalVar(thisExpr.getReferent(), expr);
      } else {
        if (myClassCallBindings.isEmpty()) {
          ok = false;
          tResult = null;
        } else {
          if (binding == null) {
            binding = myClassCallBindings.get(myClassCallBindings.size() - 1);
          }
          tResult = new TypecheckingResult(new ReferenceExpression(binding), binding.getTypeExpr());
        }
      }
      if (ok) {
        return tResultToResult(expectedType, tResult, expr);
      }
    }

    return checkExpr(expr, expectedType);
  }

  // Classes

  @Override
  public TypecheckingResult visitClassExt(Concrete.ClassExtExpression expr, Expression expectedType) {
    Concrete.Expression baseClassExpr = expr.getBaseClassExpression();
    if (baseClassExpr instanceof Concrete.AppExpression) {
      baseClassExpr = ((Concrete.AppExpression) baseClassExpr).getFunction();
    }
    if (baseClassExpr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) baseClassExpr).getReferent() instanceof MetaReferable) {
      return checkMeta((Concrete.ReferenceExpression) baseClassExpr, expr.getBaseClassExpression() instanceof Concrete.AppExpression ? ((Concrete.AppExpression) expr.getBaseClassExpression()).getArguments() : Collections.emptyList(), expr.getCoclauses(), expectedType);
    }

    baseClassExpr = desugarClassApp(expr.getBaseClassExpression(), false);
    if (baseClassExpr instanceof Concrete.ClassExtExpression) {
      Concrete.ClassExtExpression classExt = (Concrete.ClassExtExpression) baseClassExpr;
      classExt.getStatements().addAll(expr.getStatements());
      expr = classExt;
      baseClassExpr = classExt.getBaseClassExpression();
    }
    TypecheckingResult typeCheckedBaseClass = baseClassExpr instanceof Concrete.ReferenceExpression ? tResultToResult(null, visitReference((Concrete.ReferenceExpression) baseClassExpr), baseClassExpr) : checkExpr(baseClassExpr, null);
    if (typeCheckedBaseClass == null) {
      return null;
    }

    ClassCallExpression classCall = typeCheckedBaseClass.expression.normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
    if (classCall == null) {
      errorReporter.report(new TypecheckingError("Expected a class", expr.getBaseClassExpression()));
      return null;
    }

    return expr.getStatements().isEmpty() ? typeCheckedBaseClass : typecheckClassExt(expr.getStatements(), expectedType, classCall, null, expr, false);
  }

  public TypecheckingResult typecheckClassExt(List<? extends Concrete.ClassFieldImpl> classFieldImpls, Expression expectedType, ClassCallExpression classCallExpr, Set<ClassField> pseudoImplemented, Concrete.Expression expr, boolean useDefaults) {
    return typecheckClassExt(classFieldImpls, expectedType, null, classCallExpr, pseudoImplemented, expr, useDefaults);
  }

  private void checkImplementationCycle(FieldDFS dfs, ClassField field, Expression implementation, ClassCallExpression classCall, Concrete.SourceNode sourceNode) {
    List<ClassField> cycle = dfs.checkDependencies(field, FieldsCollector.getFields(implementation, classCall.getThisBinding(), classCall.getDefinition().getFields()));
    if (cycle != null) {
      errorReporter.report(CycleError.fromTypechecked(cycle, sourceNode));
    }
    classCall.getImplementedHere().put(field, cycle == null ? implementation : new ErrorExpression());
  }

  private TypecheckingResult typecheckClassExt(List<? extends Concrete.ClassFieldImpl> classFieldImpls, Expression expectedType, Expression renewExpr, ClassCallExpression classCallExpr, Set<ClassField> pseudoImplemented, Concrete.Expression expr, boolean useDefaults) {
    ClassDefinition baseClass = classCallExpr.getDefinition();
    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassCallExpression resultClassCall = new ClassCallExpression(baseClass, classCallExpr.getSortArgument(), fieldSet, Sort.PROP, baseClass.getUniverseKind());
    resultClassCall.copyImplementationsFrom(classCallExpr);
    resultClassCall.updateHasUniverses();

    Set<ClassField> defined = new HashSet<>();
    List<Pair<Definition,Concrete.ClassFieldImpl>> implementations = new ArrayList<>(classFieldImpls.size());
    for (Concrete.ClassFieldImpl classFieldImpl : classFieldImpls) {
      Definition definition = referableToDefinition(classFieldImpl.getImplementedField(), classFieldImpl);
      if (definition != null) {
        implementations.add(new Pair<>(definition,classFieldImpl));
        if (definition instanceof ClassField) {
          defined.add((ClassField) definition);
        } else if (definition instanceof ClassDefinition) {
          defined.addAll(((ClassDefinition) definition).getFields());
        }
      }
    }

    if (renewExpr != null) {
      for (ClassField field : baseClass.getFields()) {
        if (!defined.contains(field) && !resultClassCall.isImplemented(field)) {
          Set<ClassField> found = FindDefCallVisitor.findDefinitions(field.getType(Sort.STD).getCodomain(), defined);
          if (!found.isEmpty()) {
            Concrete.SourceNode sourceNode = null;
            for (Pair<Definition, Concrete.ClassFieldImpl> implementation : implementations) {
              if (implementation.proj1 instanceof ClassField && found.contains(implementation.proj1)) {
                sourceNode = implementation.proj2;
                break;
              }
            }
            if (sourceNode == null) {
              sourceNode = expr;
            }
            errorReporter.report(new FieldDependencyError(field, found, sourceNode));
            return null;
          }
          fieldSet.put(field, FieldCallExpression.make(field, classCallExpr.getSortArgument(), renewExpr));
        }
      }
    } else if (useDefaults && !baseClass.getDefaults().isEmpty()) {
      MapDFS<ClassField> dfs = new MapDFS<>(baseClass.getDefaultDependencies());
      dfs.visit(defined);
      dfs.visit(resultClassCall.getImplementedHere().keySet());
      Set<ClassField> notDefault = dfs.getVisited();
      for (ClassField field : baseClass.getFields()) {
        if (!defined.contains(field) && !resultClassCall.isImplemented(field)) {
          AbsExpression defaultImpl = baseClass.getDefault(field);
          if (defaultImpl != null && !notDefault.contains(field)) {
            resultClassCall.getImplementedHere().put(field, defaultImpl.apply(new ReferenceExpression(resultClassCall.getThisBinding()), resultClassCall.getSortArgument()));
          }
        }
      }
    }

    if (!implementations.isEmpty()) {
      FieldDFS dfs = new FieldDFS(resultClassCall.getDefinition());

      Referable thisRef = addBinding(null, resultClassCall.getThisBinding());
      myClassCallBindings.add(resultClassCall.getThisBinding());
      try {
        for (Pair<Definition, Concrete.ClassFieldImpl> pair : implementations) {
          if (pair.proj1 instanceof ClassField) {
            ClassField field = (ClassField) pair.proj1;
            TypecheckingResult implResult = typecheckImplementation(field, pair.proj2.implementation, resultClassCall);
            if (implResult != null) {
              Expression oldImpl = null;
              if (!field.isProperty()) {
                oldImpl = resultClassCall.getAbsImplementationHere(field);
                if (oldImpl == null) {
                  AbsExpression absImpl = resultClassCall.getDefinition().getImplementation(field);
                  oldImpl = absImpl == null ? null : absImpl.getExpression().subst(resultClassCall.getSortArgument().toLevelSubstitution());
                }
              }
              if (oldImpl != null) {
                if (!classCallExpr.isImplemented(field) || !CompareVisitor.compare(myEquations, CMP.EQ, implResult.expression, oldImpl, implResult.type, pair.proj2.implementation)) {
                  errorReporter.report(new FieldsImplementationError(true, baseClass.getReferable(), Collections.singletonList(field.getReferable()), pair.proj2));
                }
              } else if (!resultClassCall.isImplemented(field)) {
                checkImplementationCycle(dfs, field, implResult.expression, resultClassCall, pair.proj2.implementation);
              }
            } else if (pseudoImplemented != null) {
              pseudoImplemented.add(field);
            } else if (!resultClassCall.isImplemented(field)) {
              fieldSet.put(field, new ErrorExpression());
            }
          } else if (pair.proj1 instanceof ClassDefinition) {
            TypecheckingResult result = checkExpr(pair.proj2.implementation, null);
            if (result != null) {
              Expression type = result.type.normalize(NormalizationMode.WHNF);
              ClassCallExpression classCall = type.cast(ClassCallExpression.class);
              if (classCall == null) {
                if (!type.isInstance(ErrorExpression.class)) {
                  InferenceVariable var = type instanceof InferenceReferenceExpression ? ((InferenceReferenceExpression) type).getVariable() : null;
                  errorReporter.report(var != null && !(var instanceof MetaInferenceVariable) ? var.getErrorInfer() : new TypeMismatchError(DocFactory.text("a class"), type, pair.proj2.implementation));
                }
              } else {
                if (!classCall.getDefinition().isSubClassOf((ClassDefinition) pair.proj1)) {
                  errorReporter.report(new TypeMismatchError(new ClassCallExpression((ClassDefinition) pair.proj1, Sort.PROP), type, pair.proj2.implementation));
                } else {
                  if (!new CompareVisitor(myEquations, CMP.LE, pair.proj2.implementation).compareClassCallSortArguments(classCall, resultClassCall)) {
                    errorReporter.report(new TypeMismatchError(new ClassCallExpression(classCall.getDefinition(), resultClassCall.getSortArgument()), classCall, pair.proj2.implementation));
                    return null;
                  }
                  for (ClassField field : ((ClassDefinition) pair.proj1).getFields()) {
                    Expression impl = FieldCallExpression.make(field, classCall.getSortArgument(), result.expression);
                    Expression oldImpl = field.isProperty() ? null : resultClassCall.getImplementation(field, result.expression);
                    if (oldImpl != null) {
                      if (!CompareVisitor.compare(myEquations, CMP.EQ, impl, oldImpl, classCall.getDefinition().getFieldType(field, classCall.getSortArgument(), result.expression), pair.proj2.implementation)) {
                        errorReporter.report(new FieldsImplementationError(true, baseClass.getReferable(), Collections.singletonList(field.getReferable()), pair.proj2));
                      }
                    } else if (!resultClassCall.isImplemented(field)) {
                      checkImplementationCycle(dfs, field, impl, resultClassCall, pair.proj2.implementation);
                    }
                  }
                  resultClassCall.updateHasUniverses();
                }
              }
            }
          } else {
            errorReporter.report(new WrongReferable("Expected either a field or a class", pair.proj2.getImplementedField(), pair.proj2));
          }
        }
      } finally {
        myClassCallBindings.remove(myClassCallBindings.size() - 1);
      }
      context.remove(thisRef);
    }

    fixClassExtSort(resultClassCall, expr);
    resultClassCall.updateHasUniverses();
    return checkResult(expectedType, new TypecheckingResult(resultClassCall, new UniverseExpression(resultClassCall.getSort())), expr);
  }

  static void setCaseLevel(Concrete.Expression expr) {
    while (expr instanceof Concrete.LamExpression) {
      expr = ((Concrete.LamExpression) expr).getBody();
    }
    if (expr instanceof Concrete.CaseExpression) {
      Concrete.CaseExpression caseExpr = (Concrete.CaseExpression) expr;
      caseExpr.level = -1;
      caseExpr.setSCase(true);
    }
  }

  private TypecheckingResult typecheckImplementation(ClassField field, Concrete.Expression implBody, ClassCallExpression fieldSetClass) {
    Expression type = fieldSetClass.getDefinition().getFieldType(field, fieldSetClass.getSortArgument(), new ReferenceExpression(fieldSetClass.getThisBinding()));

    // Expression type = FieldCallExpression.make(field, fieldSetClass.getSortArgument(), new ReferenceExpression(fieldSetClass.getThisBinding())).getType();
    if (implBody instanceof Concrete.HoleExpression && field.getReferable().isParameterField() && !field.getReferable().isExplicitField() && field.isTypeClass() && type instanceof ClassCallExpression && !((ClassCallExpression) type).getDefinition().isRecord()) {
      TypecheckingResult result;
      ClassDefinition classDef = ((ClassCallExpression) type).getDefinition();
      RecursiveInstanceHoleExpression holeExpr = implBody instanceof RecursiveInstanceHoleExpression ? (RecursiveInstanceHoleExpression) implBody : null;
      if (classDef.getClassifyingField() == null) {
        TypecheckingResult instance = myInstancePool.getInstance(null, type, new SubclassSearchParameters(classDef), implBody, holeExpr);
        if (instance == null) {
          ArgInferenceError error = new InstanceInferenceError(classDef.getReferable(), implBody, holeExpr, new Expression[0]);
          errorReporter.report(error);
          result = new TypecheckingResult(new ErrorExpression(error), type);
        } else {
          result = instance;
          if (result.type == null) {
            result.type = type;
          }
        }
      } else {
        result = new TypecheckingResult(new InferenceReferenceExpression(new TypeClassInferenceVariable(field.getName(), type, classDef, false, implBody, holeExpr, getAllBindings()), myEquations), type);
      }
      return result;
    }

    if (field.isProperty()) {
      setCaseLevel(implBody);
    }
    TypecheckingResult result = fieldSetClass.getDefinition().isGoodField(field) ? checkArgument(implBody, type, null) : checkExpr(implBody, type);
    if (result == null) {
      return null;
    }
    result.type = type;
    return result;
  }

  @Override
  public TypecheckingResult visitNew(Concrete.NewExpression expr, Expression expectedType) {
    if (expr.getExpression() instanceof Concrete.ClassExtExpression) {
      Concrete.ClassExtExpression classExt = (Concrete.ClassExtExpression) expr.getExpression();
      Concrete.Expression baseClassExpr = classExt.getBaseClassExpression();
      if (baseClassExpr instanceof Concrete.AppExpression) {
        baseClassExpr = ((Concrete.AppExpression) baseClassExpr).getFunction();
      }
      if (baseClassExpr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) baseClassExpr).getReferent() instanceof MetaReferable) {
        return checkMeta((Concrete.ReferenceExpression) baseClassExpr, classExt.getBaseClassExpression() instanceof Concrete.AppExpression ? ((Concrete.AppExpression) classExt.getBaseClassExpression()).getArguments() : Collections.emptyList(), classExt.getCoclauses(), expectedType);
      }
    }

    TypecheckingResult exprResult = null;
    Set<ClassField> pseudoImplemented = Collections.emptySet();
    Concrete.Expression classExpr = desugarClassApp(expr.getExpression() instanceof Concrete.ClassExtExpression ? ((Concrete.ClassExtExpression) expr.getExpression()).getBaseClassExpression() : expr.getExpression(), !(expr.getExpression() instanceof Concrete.ClassExtExpression));
    if (classExpr instanceof Concrete.ClassExtExpression || classExpr instanceof Concrete.ReferenceExpression) {
      if (expr.getExpression() instanceof Concrete.ClassExtExpression) {
        if (classExpr instanceof Concrete.ClassExtExpression) {
          ((Concrete.ClassExtExpression) classExpr).getStatements().addAll(((Concrete.ClassExtExpression) expr.getExpression()).getStatements());
        } else {
          classExpr = expr.getExpression();
        }
      }
      if (expectedType != null) {
        expectedType = expectedType.normalize(NormalizationMode.WHNF);
      }
      Concrete.Expression baseExpr = classExpr instanceof Concrete.ClassExtExpression ? ((Concrete.ClassExtExpression) classExpr).getBaseClassExpression() : classExpr;
      Definition actualDef = expectedType instanceof ClassCallExpression && baseExpr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) baseExpr).getReferent() instanceof TCDefReferable ? ((TCDefReferable) ((Concrete.ReferenceExpression) baseExpr).getReferent()).getTypechecked() : null;
      if (baseExpr instanceof Concrete.HoleExpression || actualDef instanceof ClassDefinition) {
        ClassCallExpression actualClassCall = null;
        if (baseExpr instanceof Concrete.HoleExpression && !(expectedType instanceof ClassCallExpression)) {
          errorReporter.report(new TypecheckingError("Cannot infer an expression", baseExpr));
          return null;
        }

        ClassCallExpression expectedClassCall = (ClassCallExpression) expectedType;
        if (baseExpr instanceof Concrete.ReferenceExpression) {
          Concrete.ReferenceExpression baseRefExpr = (Concrete.ReferenceExpression) baseExpr;
          boolean ok = ((ClassDefinition) actualDef).isSubClassOf(expectedClassCall.getDefinition());
          if (ok && (actualDef != expectedClassCall.getDefinition() || baseRefExpr.getPLevel() != null || baseRefExpr.getHLevel() != null)) {
            boolean fieldsOK = true;
            for (ClassField implField : expectedClassCall.getImplementedHere().keySet()) {
              if (((ClassDefinition) actualDef).isImplemented(implField)) {
                fieldsOK = false;
                break;
              }
            }
            Level pLevel = baseRefExpr.getPLevel() == null ? null : baseRefExpr.getPLevel().accept(this, LevelVariable.PVAR);
            Level hLevel = baseRefExpr.getHLevel() == null ? null : baseRefExpr.getHLevel().accept(this, LevelVariable.HVAR);
            Sort expectedSort = expectedClassCall.getSortArgument();
            actualClassCall = new ClassCallExpression((ClassDefinition) actualDef, pLevel == null && hLevel == null ? expectedSort : new Sort(pLevel == null ? expectedSort.getPLevel() : pLevel, hLevel == null ? expectedSort.getHLevel() : hLevel), new HashMap<>(), expectedClassCall.getSort(), actualDef.getUniverseKind());
            if (fieldsOK) {
              actualClassCall.copyImplementationsFrom(expectedClassCall);
            }
          }
          if (!ok) {
            errorReporter.report(new TypeMismatchError(expectedType, baseExpr, baseExpr));
            return null;
          }
        }

        if (actualClassCall != null) {
          expectedClassCall = actualClassCall;
          expectedClassCall.updateHasUniverses();
        }
        pseudoImplemented = new HashSet<>();
        exprResult = typecheckClassExt(classExpr instanceof Concrete.ClassExtExpression ? ((Concrete.ClassExtExpression) classExpr).getStatements() : Collections.emptyList(), null, expectedClassCall, pseudoImplemented, classExpr, true);
        if (exprResult == null) {
          return null;
        }
      }
    }

    Expression renewExpr = null;
    if (exprResult == null) {
      Concrete.Expression baseClassExpr;
      List<Concrete.ClassFieldImpl> classFieldImpls;
      if (expr.getExpression() instanceof Concrete.ClassExtExpression) {
        baseClassExpr = ((Concrete.ClassExtExpression) expr.getExpression()).getBaseClassExpression();
        classFieldImpls = ((Concrete.ClassExtExpression) expr.getExpression()).getStatements();
      } else {
        baseClassExpr = expr.getExpression();
        classFieldImpls = Collections.emptyList();
      }

      TypecheckingResult typeCheckedBaseClass = checkExpr(baseClassExpr, null);
      if (typeCheckedBaseClass == null) {
        return null;
      }

      typeCheckedBaseClass.expression = typeCheckedBaseClass.expression.normalize(NormalizationMode.WHNF);
      ClassCallExpression classCall = typeCheckedBaseClass.expression.cast(ClassCallExpression.class);
      if (classCall == null) {
        classCall = typeCheckedBaseClass.type.normalize(NormalizationMode.WHNF).cast(ClassCallExpression.class);
        if (classCall == null) {
          errorReporter.report(new TypecheckingError("Expected a class or a class instance", baseClassExpr));
          return null;
        }
        renewExpr = typeCheckedBaseClass.expression;
      }

      exprResult = typecheckClassExt(classFieldImpls, null, renewExpr, classCall, null, baseClassExpr, true);
      if (exprResult == null) {
        return null;
      }
    }

    Expression normExpr = exprResult.expression.normalize(NormalizationMode.WHNF);
    ClassCallExpression classCallExpr = normExpr.cast(ClassCallExpression.class);
    if (classCallExpr == null) {
      TypecheckingError error = new TypecheckingError("Expected a class", expr.getExpression());
      errorReporter.report(error);
      return new TypecheckingResult(new ErrorExpression(error), normExpr);
    }

    if (checkAllImplemented(classCallExpr, pseudoImplemented, expr)) {
      return checkResult(expectedType, new TypecheckingResult(new NewExpression(null, classCallExpr), classCallExpr), expr);
    } else {
      return null;
    }
  }

  public boolean checkAllImplemented(ClassCallExpression classCall, Set<ClassField> pseudoImplemented, Concrete.SourceNode sourceNode) {
    var expectedFields = classCall.getDefinition().getNumberOfNotImplementedFields();
    int notImplemented = expectedFields - classCall.getImplementedHere().size();
    if (notImplemented == 0) {
      return true;
    } else if (notImplemented < 0) {
      throw new IllegalArgumentException("Too many implemented fields (expected " + expectedFields + "): " + classCall.getImplementedHere());
    } else {
      List<FieldReferable> fields = new ArrayList<>(notImplemented);
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (!classCall.isImplemented(field) && !pseudoImplemented.contains(field)) {
          fields.add(field.getReferable());
        }
      }
      if (!fields.isEmpty()) {
        errorReporter.report(new FieldsImplementationError(false, classCall.getDefinition().getReferable(), fields, sourceNode));
      }
      return false;
    }
  }

  // Variables

  private Definition referableToDefinition(Referable referable, Concrete.SourceNode sourceNode) {
    if (referable == null || referable instanceof ErrorReference) {
      return null;
    }

    if (!(referable instanceof GlobalReferable)) {
      if (sourceNode != null) {
        errorReporter.report(new WrongReferable("Expected a definition", referable, sourceNode));
      }
      return null;
    }

    Definition definition = referable instanceof TCDefReferable ? ((TCDefReferable) referable).getTypechecked() : null;
    if (definition == null && sourceNode != null) {
      errorReporter.report(new TypecheckingError("Internal error: definition '" + referable.textRepresentation() + "' was not typechecked", sourceNode));
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
      errorReporter.report(new WrongReferable(errorMsg, referable, sourceNode));
    }
    return null;
  }

  public ClassField referableToClassField(Referable referable, Concrete.SourceNode sourceNode) {
    return referableToDefinition(referable, ClassField.class, "Expected a class field", sourceNode);
  }

  private Definition getTypeCheckedDefinition(TCDefReferable definition, Concrete.Expression expr) {
    Definition typeCheckedDefinition = definition.getTypechecked();
    if (typeCheckedDefinition == null) {
      errorReporter.report(new IncorrectReferenceError(definition, expr));
      return null;
    }
    if (!typeCheckedDefinition.status().headerIsOK()) {
      errorReporter.report(new HasErrors(GeneralError.Level.ERROR, definition, expr));
      return null;
    } else {
      if (typeCheckedDefinition.status().hasDepProblems()) {
        setStatus(Definition.TypeCheckingStatus.DEP_PROBLEMS);
      }
      return typeCheckedDefinition;
    }
  }

  private TResult typeCheckDefCall(TCDefReferable resolvedDefinition, Concrete.ReferenceExpression expr) {
    Definition definition = getTypeCheckedDefinition(resolvedDefinition, expr);
    if (definition == null) {
      return null;
    }

    Sort sortArgument;
    boolean isMin = definition instanceof DataDefinition && !definition.getParameters().hasNext() && definition.getUniverseKind() == UniverseKind.NO_UNIVERSES;
    if (expr.getPLevel() == null && expr.getHLevel() == null) {
      sortArgument = isMin ? Sort.PROP : Sort.generateInferVars(getEquations(), definition.getUniverseKind(), expr);
      if (definition == Prelude.PATH || definition == Prelude.PATH_INFIX) {
        InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, definition.getUniverseKind() != UniverseKind.NO_UNIVERSES, expr);
        myEquations.addVariable(pl);
        myEquations.addEquation(new Level(sortArgument.getPLevel().getVar()), new Level(pl), CMP.LE, expr);
        getEquations().bindVariables(pl, (InferenceLevelVariable) sortArgument.getHLevel().getVar());
        return DefCallResult.makePathType(expr, definition == Prelude.PATH_INFIX, sortArgument, new Sort(new Level(pl), new Level(sortArgument.getHLevel().getVar(), -1, -1)));
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
          InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, definition.getUniverseKind() != UniverseKind.NO_UNIVERSES, expr);
          getEquations().addVariable(pl);
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
          InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, definition.getUniverseKind() != UniverseKind.NO_UNIVERSES, expr);
          getEquations().addVariable(hl);
          hLevel = new Level(hl);
        }
      }

      sortArgument = new Sort(pLevel, hLevel);
    }

    return DefCallResult.makeTResult(expr, definition, sortArgument);
  }

  private TResult getLocalVar(Referable ref, Concrete.SourceNode sourceNode) {
    if (ref instanceof UnresolvedReference || ref instanceof RedirectingReferable) {
      errorReporter.report(new TypecheckingError("Unresolved reference `" + ref.textRepresentation() + "`. This may be caused by a bug in a meta resolver.", sourceNode));
      return null;
    }
    if (ref instanceof ErrorReference) {
      return null;
    }

    Binding def = context.get(ref);
    if (def == null) {
      errorReporter.report(new IncorrectReferenceError(ref, sourceNode));
      return null;
    }
    Expression type = def.getTypeExpr();
    if (type == null) {
      errorReporter.report(new ReferenceTypeError(ref));
      return null;
    } else {
      return new TypecheckingResult(def instanceof TypedEvaluatingBinding ? ((TypedEvaluatingBinding) def).getExpression() : new ReferenceExpression(def), type);
    }
  }

  public TResult visitReference(Concrete.ReferenceExpression expr) {
    Referable ref = expr.getReferent();
    if (ref instanceof CoreReferable) {
      TypecheckingResult result = ((CoreReferable) ref).result;
      fixCheckedExpression(result, ref, expr);
      return new TypecheckingResult(result.expression, result.type);
    }

    if (!(ref instanceof GlobalReferable) && (expr.getPLevel() != null || expr.getHLevel() != null)) {
      errorReporter.report(new IgnoredLevelsError(expr.getPLevel(), expr.getHLevel()));
    }
    return ref instanceof TCDefReferable ? typeCheckDefCall((TCDefReferable) ref, expr) : getLocalVar(expr.getReferent(), expr);
  }

  @Override
  public TypecheckingResult visitReference(Concrete.ReferenceExpression expr, Expression expectedType) {
    if (expr.getReferent() instanceof MetaReferable) {
      return checkMeta(expr, Collections.emptyList(), null, expectedType);
    }

    if (expr.getReferent() instanceof TCDefReferable && ((TCDefReferable) expr.getReferent()).getTypechecked() instanceof ClassDefinition) {
      List<SingleDependentLink> parameters = expectedType == null ? null : new ArrayList<>();
      if (expectedType != null) {
        expectedType = expectedType.normalizePi(parameters);
      }
      Concrete.Expression dExpr = desugarClassApp(expr, Collections.emptyList(), expr, parameters, true);
      if (dExpr != expr) {
        return checkExpr(dExpr, expectedType);
      }
    }

    if (Prelude.ZERO != null && expr.getReferent() == Prelude.ZERO.getRef() && expectedType != null) {
      expectedType = expectedType.normalize(NormalizationMode.WHNF);
      if (expectedType instanceof DataCallExpression && ((DataCallExpression) expectedType).getDefinition() == Prelude.FIN) {
        return checkResult(expectedType, new TypecheckingResult(new SmallIntegerExpression(0), new DataCallExpression(Prelude.FIN, Sort.PROP, new SingletonList<>(new SmallIntegerExpression(1)))), expr);
      }
    }

    TResult result = visitReference(expr);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  @Override
  public TypecheckingResult visitThis(Concrete.ThisExpression expr, Expression expectedType) {
    errorReporter.report(new TypecheckingError("\\this expressions are allowed only in appropriate arguments of definitions and class extensions", expr));
    return null;
  }

  @Override
  public TypecheckingResult visitHole(Concrete.HoleExpression expr, Expression expectedType) {
    boolean isOmega = expectedType instanceof Type && ((Type) expectedType).isOmega();
    if (expr.isErrorHole()) {
      return expectedType != null && !isOmega ? new TypecheckingResult(new ErrorExpression(expr.getError()), expectedType) : null;
    }

    if (expectedType != null && !isOmega) {
      return new TypecheckingResult(new InferenceReferenceExpression(myArgsInference.newInferenceVariable(expectedType, expr), getEquations()), expectedType);
    } else {
      errorReporter.report(new ArgInferenceError(expression(), expr, new Expression[0]));
      return null;
    }
  }

  // Level expressions

  @Override
  public Level visitInf(Concrete.InfLevelExpression expr, LevelVariable base) {
    if (base == LevelVariable.PVAR) {
      errorReporter.report(new TypecheckingError("\\inf is not a correct p-level", expr));
      return new Level(base);
    }
    return Level.INFINITY;
  }

  @Override
  public Level visitLP(Concrete.PLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.PVAR) {
      errorReporter.report(new TypecheckingError("Expected " + base, expr));
    }
    return new Level(base);
  }

  @Override
  public Level visitLH(Concrete.HLevelExpression expr, LevelVariable base) {
    if (base != LevelVariable.HVAR) {
      errorReporter.report(new TypecheckingError("Expected " + base, expr));
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
    errorReporter.report(new TypecheckingError("Cannot typecheck an inference variable", expr));
    return new Level(base);
  }

  // Sorts

  public Sort getSortOfType(Expression expr, Concrete.SourceNode sourceNode) {
    Expression type = expr.getType(true);
    Sort sort = type == null ? null : type.toSort();
    if (sort == null) {
      assert type != null;
      if (type.isInstance(ErrorExpression.class)) {
        return Sort.STD;
      }
      Sort result = Sort.generateInferVars(getEquations(), false, sourceNode);
      if (!CompareVisitor.compare(getEquations(), CMP.LE, type, new UniverseExpression(result), Type.OMEGA, sourceNode)) {
        errorReporter.report(new TypeMismatchError(DocFactory.text("a type"), type, sourceNode));
      }
      return result;
    } else {
      return sort;
    }
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

    Sort sortResult = Sort.generateInferVars(getEquations(), false, sourceNode);
    for (Sort sort : sorts) {
      getEquations().addEquation(sort.getPLevel(), sortResult.getPLevel(), CMP.LE, sourceNode);
      getEquations().addEquation(sort.getHLevel(), sortResult.getHLevel(), CMP.LE, sourceNode);
    }
    return sortResult;
  }

  public void fixClassExtSort(ClassCallExpression classCall, Concrete.SourceNode sourceNode) {
    Expression thisExpr = new ReferenceExpression(ExpressionFactory.parameter("this", classCall));
    Integer hLevel = classCall.getDefinition().getUseLevel(classCall.getImplementedHere(), classCall.getThisBinding(), true);
    List<Sort> sorts = hLevel != null && hLevel == -1 ? null : new ArrayList<>();
    for (ClassField field : classCall.getDefinition().getFields()) {
      if (classCall.isImplemented(field)) continue;
      Expression fieldType = classCall.getDefinition().getFieldType(field, classCall.getSortArgument(), thisExpr).normalize(NormalizationMode.WHNF);
      if (sorts != null && !fieldType.isInstance(ErrorExpression.class)) {
        sorts.add(getSortOfType(fieldType, sourceNode));
      }
    }

    if (hLevel != null && sorts != null) {
      for (int i = 0; i < sorts.size(); i++) {
        sorts.set(i, new Sort(sorts.get(i).getPLevel(), new Level(hLevel)));
      }
    }

    classCall.setSort(sorts == null ? Sort.PROP : generateUpperBound(sorts, sourceNode).subst(classCall.getSortArgument().toLevelSubstitution()));
  }

  // Parameters

  private TypedSingleDependentLink visitNameParameter(Concrete.NameParameter param, Concrete.SourceNode sourceNode) {
    Referable referable = param.getReferable();
    String name = referable == null ? null : referable.textRepresentation();
    Sort sort = Sort.generateInferVars(myEquations, false, sourceNode);
    InferenceVariable inferenceVariable = new LambdaInferenceVariable(name == null ? "_" : "type-of-" + name, new UniverseExpression(sort), param.getReferable(), false, sourceNode, getAllBindings());
    Expression argType = new InferenceReferenceExpression(inferenceVariable, myEquations);

    TypedSingleDependentLink link = new TypedSingleDependentLink(param.isExplicit(), name, new TypeExpression(argType, sort));
    addBinding(referable, link);
    return link;
  }

  private SingleDependentLink visitTypeParameter(Concrete.TypeParameter param, List<Sort> sorts, Type expectedType) {
    Type argResult = checkType(param.getType(), Type.OMEGA);
    if (argResult == null) return null;
    if (expectedType != null) {
      Expression expected = expectedType.getExpr().normalize(NormalizationMode.WHNF).getUnderlyingExpression();
      if ((expected instanceof ClassCallExpression || expected instanceof PiExpression || expected instanceof SigmaExpression || expected instanceof UniverseExpression)
          && expected.isLessOrEquals(argResult.getExpr(), myEquations, param)) {
        argResult = expectedType;
      }
    }
    if (sorts != null) {
      sorts.add(argResult.getSortOfType());
    }

    if (param instanceof Concrete.TelescopeParameter) {
      List<? extends Referable> referableList = param.getReferableList();
      SingleDependentLink link = ExpressionFactory.singleParams(param.isExplicit(), param.getNames(), argResult);
      int i = 0;
      for (SingleDependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
        addBinding(referableList.get(i) , link1);
      }
      return link;
    } else {
      return new TypedSingleDependentLink(param.isExplicit(), null, argResult);
    }
  }

  private DependentLink visitParameters(Collection<? extends ConcreteParameter> parameters, Expression expectedType, List<Sort> resultSorts) {
    LinkList list = new LinkList();

    try (var ignored = new Utils.SetContextSaver<>(context)) {
      for (ConcreteParameter parameter : parameters) {
        if (!(parameter instanceof Concrete.TypeParameter)) {
          throw new IllegalArgumentException();
        }
        Concrete.TypeParameter arg = (Concrete.TypeParameter) parameter;
        Type result = checkType(arg.getType(), expectedType == null ? Type.OMEGA : expectedType);
        if (result == null) return null;

        if (arg instanceof Concrete.TelescopeParameter) {
          List<? extends Referable> referableList = arg.getReferableList();
          DependentLink link = ExpressionFactory.parameter(arg.isExplicit(), arg.getNames(), result);
          list.append(link);
          int i = 0;
          for (DependentLink link1 = link; link1.hasNext(); link1 = link1.getNext(), i++) {
            addBinding(referableList.get(i), link1);
          }
        } else {
          list.append(ExpressionFactory.parameter(arg.isExplicit(), (String) null, result));
        }

        if (resultSorts != null) {
          resultSorts.add(result.getSortOfType());
        }
      }
    }

    return list.getFirst();
  }

  // Pi

  private TypecheckingResult bodyToLam(SingleDependentLink params, TypecheckingResult bodyResult, Concrete.SourceNode sourceNode) {
    if (bodyResult == null) {
      return null;
    }
    Sort sort = PiExpression.generateUpperBound(params.getType().getSortOfType(), getSortOfType(bodyResult.type, sourceNode), myEquations, sourceNode);
    return new TypecheckingResult(new LamExpression(sort, params, bodyResult.expression), new PiExpression(sort, params, bodyResult.type));
  }

  private interface ParametersProvider {
    @Nullable SingleDependentLink nextParameter();
    @Nullable Expression getType();
    void subst(DependentLink param, Expression expr);
  }

  private static final ParametersProvider NULL_PARAMETERS_PROVIDER = new ParametersProvider() {
    @Override
    public @Nullable SingleDependentLink nextParameter() {
      return null;
    }

    @Override
    public @Nullable Expression getType() {
      return null;
    }

    @Override
    public void subst(DependentLink param, Expression expr) {

    }
  };

  private static class ExpressionParametersProvider implements ParametersProvider {
    private Sort sort = Sort.STD;
    private SingleDependentLink parameter = EmptyDependentLink.getInstance();
    private Expression expression;
    private final ExprSubstitution substitution = new ExprSubstitution();

    public ExpressionParametersProvider(Expression expression) {
      this.expression = expression;
    }

    @Override
    public @Nullable SingleDependentLink nextParameter() {
      if (!parameter.hasNext()) {
        expression = expression.subst(substitution).normalize(NormalizationMode.WHNF);
        substitution.clear();
        if (!(expression instanceof PiExpression)) {
          return null;
        }

        PiExpression piExpr = (PiExpression) expression;
        sort = piExpr.getResultSort();
        parameter = piExpr.getParameters();
        expression = piExpr.getCodomain();
      }

      SingleDependentLink result = parameter;
      parameter = parameter.getNext();
      return result;
    }

    @Override
    public @Nullable Expression getType() {
      return (parameter.hasNext() ? new PiExpression(sort, parameter, expression) : expression).subst(substitution);
    }

    @Override
    public void subst(DependentLink param, Expression expr) {
      substitution.add(param, expr);
    }
  }

  private TypecheckingResult visitLam(List<? extends Concrete.Parameter> parameters, Concrete.LamExpression expr, ParametersProvider provider) {
    if (parameters.isEmpty()) {
      return checkExpr(expr.getBody(), provider.getType());
    }

    Concrete.Parameter param = parameters.get(0);
    SingleDependentLink piParam = provider.nextParameter();
    if (piParam != null && !piParam.isExplicit() && param.isExplicit()) {
      for (SingleDependentLink link = piParam; link.hasNext(); link = link.getNext()) {
        addBinding(null, link);
        if (link instanceof UntypedSingleDependentLink) {
          provider.nextParameter();
        }
      }
      return bodyToLam(piParam, visitLam(parameters, expr, provider), expr);
    }

    if (param instanceof Concrete.NameParameter) {
      if (piParam == null) {
        TypedSingleDependentLink link = visitNameParameter((Concrete.NameParameter) param, expr);
        TypecheckingResult bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, NULL_PARAMETERS_PROVIDER);
        if (bodyResult == null) return null;
        Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOfType(bodyResult.type, expr), myEquations, expr);
        TypecheckingResult result = new TypecheckingResult(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
        Expression expectedType = provider.getType();
        if (expectedType != null && checkResult(expectedType, result, expr) == null) {
          return null;
        }
        return result;
      } else {
        Referable referable = ((Concrete.NameParameter) param).getReferable();
        if (piParam.isExplicit() && !param.isExplicit()) {
          errorReporter.report(new ImplicitLambdaError(referable, -1, param));
        }

        Type paramType = piParam.getType();
        DefCallExpression defCallParamType = paramType.getExpr().cast(DefCallExpression.class);
        if (defCallParamType != null && defCallParamType.getDefinition().getUniverseKind() == UniverseKind.NO_UNIVERSES) { // fixes test pLevelTest
          Definition definition = defCallParamType.getDefinition();
          Sort sortArg = definition instanceof DataDefinition || definition instanceof FunctionDefinition || definition instanceof ClassDefinition ? Sort.generateInferVars(myEquations, false, param) : null;
          if (definition instanceof ClassDefinition) {
            ClassCallExpression classCall = (ClassCallExpression) defCallParamType;
            for (Map.Entry<ClassField, Expression> entry : classCall.getImplementedHere().entrySet()) {
              Expression type = entry.getValue().getType();
              if (type == null || !CompareVisitor.compare(myEquations, CMP.LE, type, classCall.getDefinition().getFieldType(entry.getKey(), sortArg, new ReferenceExpression(classCall.getThisBinding())), Type.OMEGA, param)) {
                sortArg = null;
                break;
              }
            }
          } else if (sortArg != null) {
            ExprSubstitution substitution = new ExprSubstitution();
            LevelSubstitution levelSubst = sortArg.toLevelSubstitution();
            DependentLink link = definition.getParameters();
            for (Expression arg : defCallParamType.getDefCallArguments()) {
              Expression type = arg.getType();
              if (type == null || !CompareVisitor.compare(myEquations, CMP.LE, type, link.getTypeExpr().subst(substitution, levelSubst), Type.OMEGA, param)) {
                sortArg = null;
                break;
              }
              substitution.add(link, arg);
              link = link.getNext();
            }
          }

          if (sortArg != null) {
            if (definition instanceof DataDefinition) {
              paramType = new DataCallExpression((DataDefinition) definition, sortArg, new ArrayList<>(defCallParamType.getDefCallArguments()));
            } else if (definition instanceof FunctionDefinition) {
              paramType = new TypeExpression(FunCallExpression.make((FunctionDefinition) definition, sortArg, new ArrayList<>(defCallParamType.getDefCallArguments())), paramType.getSortOfType());
            } else {
              ClassCallExpression classCall = (ClassCallExpression) defCallParamType;
              paramType = new ClassCallExpression((ClassDefinition) definition, sortArg, classCall.getImplementedHere(), classCall.getDefinition().computeSort(sortArg, classCall.getImplementedHere(), classCall.getThisBinding()), classCall.getUniverseKind());
            }
          }
        }

        SingleDependentLink link = new TypedSingleDependentLink(piParam.isExplicit(), referable == null ? null : referable.textRepresentation(), paramType);
        addBinding(referable, link);
        provider.subst(piParam, new ReferenceExpression(link));
        return bodyToLam(link, visitLam(parameters.subList(1, parameters.size()), expr, provider), expr);
      }
    } else if (param instanceof Concrete.TypeParameter) {
      SingleDependentLink link = visitTypeParameter((Concrete.TypeParameter) param, null, piParam == null || piParam.isExplicit() != param.isExplicit() ? null : piParam.getType());
      if (link == null) {
        return null;
      }

      int namesCount = param.getNumberOfParameters();
      SingleDependentLink actualLink = link;
      if (piParam != null) {
        Expression argType = link.getTypeExpr();
        for (int i = 0; i < namesCount; i++, actualLink = actualLink.getNext()) {
          while (piParam instanceof UntypedDependentLink && i < namesCount - 1) {
            provider.subst(piParam, new ReferenceExpression(actualLink));
            piParam = provider.nextParameter();
            actualLink = actualLink.getNext();
            i++;
          }
          if (piParam == null) {
            break;
          }
          if (piParam.isExplicit() && !param.isExplicit() && i < namesCount) {
            errorReporter.report(new ImplicitLambdaError(param.getReferableList().get(i), namesCount > 1 ? i : -1, param));
          }
          if (!CompareVisitor.compare(myEquations, CMP.EQ, argType, piParam.getTypeExpr(), Type.OMEGA, param.getType())) {
            if (!argType.isError()) {
              errorReporter.report(new TypeMismatchError("Type mismatch in an argument of the lambda", piParam.getTypeExpr(), argType, param.getType()));
              return null;
            }
          }

          provider.subst(piParam, new ReferenceExpression(actualLink));
          if (i < namesCount - 1) {
            piParam = provider.nextParameter();
          }
        }
      }

      TypecheckingResult bodyResult = visitLam(parameters.subList(1, parameters.size()), expr, actualLink.hasNext() ? NULL_PARAMETERS_PROVIDER : provider);
      if (bodyResult == null) return null;
      Sort sort = PiExpression.generateUpperBound(link.getType().getSortOfType(), getSortOfType(bodyResult.type, expr), myEquations, expr);
      if (actualLink.hasNext()) {
        Expression expectedType = provider.getType();
        if (expectedType != null && checkResult(expectedType, new TypecheckingResult(null, new PiExpression(sort, actualLink, bodyResult.type)), expr) == null) {
          return null;
        }
      }

      return new TypecheckingResult(new LamExpression(sort, link, bodyResult.expression), new PiExpression(sort, link, bodyResult.type));
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public TypecheckingResult visitLam(Concrete.LamExpression expr, Expression expectedType) {
    try (var ignored = new Utils.SetContextSaver<>(context)) {
      return visitLam(expr.getParameters(), expr, expectedType == null ? NULL_PARAMETERS_PROVIDER : new ExpressionParametersProvider(expectedType));
    }
  }

  @Override
  public TypecheckingResult visitPi(Concrete.PiExpression expr, Expression expectedType) {
    List<SingleDependentLink> list = new ArrayList<>();
    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());

    try (var ignored = new Utils.SetContextSaver<>(context)) {
      for (Concrete.TypeParameter arg : expr.getParameters()) {
        SingleDependentLink link = visitTypeParameter(arg, sorts, null);
        if (link == null) {
          return null;
        }
        list.add(link);
      }

      Type result = checkType(expr.getCodomain(), Type.OMEGA);
      if (result == null) return null;
      Sort codSort = result.getSortOfType();

      Expression piExpr = result.getExpr();
      for (int i = list.size() - 1; i >= 0; i--) {
        codSort = PiExpression.generateUpperBound(sorts.get(i), codSort, myEquations, expr);
        piExpr = new PiExpression(codSort, list.get(i), piExpr);
      }

      return checkResult(expectedType, new TypecheckingResult(piExpr, new UniverseExpression(codSort)), expr);
    }
  }

  // Sigma

  @Override
  public TypecheckingResult visitSigma(Concrete.SigmaExpression expr, Expression expectedType) {
    if (expr.getParameters().isEmpty()) {
      return checkResult(expectedType, new TypecheckingResult(new SigmaExpression(Sort.PROP, EmptyDependentLink.getInstance()), new UniverseExpression(Sort.PROP)), expr);
    }

    List<Sort> sorts = new ArrayList<>(expr.getParameters().size());
    DependentLink args = visitParameters(expr.getParameters(), expectedType, sorts);
    if (args == null || !args.hasNext()) return null;
    Sort sort = generateUpperBound(sorts, expr);
    return checkResult(expectedType, new TypecheckingResult(new SigmaExpression(sort, args), new UniverseExpression(sort)), expr);
  }

  @Override
  public TypecheckingResult visitTuple(Concrete.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm = expectedType == null ? null : expectedType.normalize(NormalizationMode.WHNF);
    SigmaExpression expectedTypeSigma = expectedTypeNorm == null ? null : expectedTypeNorm.cast(SigmaExpression.class);
    if (expectedTypeSigma != null) {
      DependentLink sigmaParams = expectedTypeSigma.getParameters();
      int sigmaParamsSize = DependentLink.Helper.size(sigmaParams);

      if (expr.getFields().size() != sigmaParamsSize) {
        errorReporter.report(new TypecheckingError("Expected a tuple with " + sigmaParamsSize + " fields, but given " + expr.getFields().size(), expr));
        return null;
      }

      List<Expression> fields = new ArrayList<>(expr.getFields().size());
      TypecheckingResult tupleResult = new TypecheckingResult(new TupleExpression(fields, expectedTypeSigma), expectedType);
      ExprSubstitution substitution = new ExprSubstitution();
      for (Concrete.Expression field : expr.getFields()) {
        Expression expType = sigmaParams.getTypeExpr().subst(substitution);
        TypecheckingResult result = checkExpr(field, expType);
        if (result == null) return null;
        fields.add(result.expression);
        substitution.add(sigmaParams, result.expression);

        sigmaParams = sigmaParams.getNext();
      }
      return tupleResult;
    }

    List<Sort> sorts = new ArrayList<>(expr.getFields().size());
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    LinkList list = new LinkList();
    for (int i = 0; i < expr.getFields().size(); i++) {
      TypecheckingResult result = checkExpr(expr.getFields().get(i), null);
      if (result == null) return null;
      fields.add(result.expression);
      Sort sort = getSortOfType(result.type, expr);
      sorts.add(sort);
      list.append(ExpressionFactory.parameter(null, result.type instanceof Type ? (Type) result.type : new TypeExpression(result.type, sort)));
    }

    SigmaExpression type = new SigmaExpression(generateUpperBound(sorts, expr), list.getFirst());
    return checkResult(expectedTypeNorm, new TypecheckingResult(new TupleExpression(fields, type), type), expr);
  }

  @Override
  public TypecheckingResult visitProj(Concrete.ProjExpression expr, Expression expectedType) {
    Concrete.Expression expr1 = expr.getExpression();
    TypecheckingResult exprResult = checkExpr(expr1, null);
    if (exprResult == null) return null;

    exprResult.type = exprResult.type.normalize(NormalizationMode.WHNF);
    if (!(exprResult.type instanceof SigmaExpression)) {
      TypecheckingResult coercedResult = CoerceData.coerceToKey(exprResult, new CoerceData.SigmaKey(), expr1, this);
      if (coercedResult != null) {
        exprResult = coercedResult;
      }
    }
    SigmaExpression sigmaExpr = exprResult.type.cast(SigmaExpression.class);
    if (sigmaExpr == null) {
      Expression stuck = exprResult.type.getStuckExpression();
      if (stuck == null || !stuck.isError()) {
        errorReporter.report(new TypeMismatchError(DocFactory.text("A sigma type"), exprResult.type, expr1));
      }
      return null;
    }

    DependentLink sigmaParams = sigmaExpr.getParameters();
    if (expr.getField() < 0) {
      errorReporter.report(new TypecheckingError("Index " + (expr.getField() +1) + " is too small; the lower bound of projection index is 1", expr));
      return null;
    }
    DependentLink fieldLink = DependentLink.Helper.get(sigmaParams, expr.getField());
    if (!fieldLink.hasNext()) {
      errorReporter.report(new TypecheckingError("Index " + (expr.getField() + 1) + " is out of range; the number of parameters is " + DependentLink.Helper.size(sigmaParams), expr));
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

  // Let

  private TypecheckingResult typecheckLetClause(List<? extends Concrete.Parameter> parameters, Concrete.LetClause letClause, boolean useSpecifiedType) {
    if (parameters.isEmpty()) {
      Concrete.Expression letResult = letClause.getResultType();
      if (letResult != null) {
        Type type = checkType(letResult, Type.OMEGA);
        if (type == null) {
          return null;
        }

        TypecheckingResult result = checkExpr(letClause.getTerm(), type.getExpr());
        if (result == null) {
          return new TypecheckingResult(new ErrorExpression(type.getExpr()), type.getExpr());
        }
        ErrorExpression errorExpr = result.expression.cast(ErrorExpression.class);
        if (errorExpr != null) {
          result.expression = errorExpr.replaceExpression(type.getExpr());
        }
        return useSpecifiedType ? new TypecheckingResult(result.expression, type.getExpr()) : result;
      } else {
        return checkExpr(letClause.getTerm(), null);
      }
    }

    Concrete.Parameter param = parameters.get(0);
    if (param instanceof Concrete.NameParameter) {
      return bodyToLam(visitNameParameter((Concrete.NameParameter) param, letClause), typecheckLetClause(parameters.subList(1, parameters.size()), letClause, false), letClause);
    } else if (param instanceof Concrete.TypeParameter) {
      SingleDependentLink link = visitTypeParameter((Concrete.TypeParameter) param, null, null);
      return link == null ? null : bodyToLam(link, typecheckLetClause(parameters.subList(1, parameters.size()), letClause, false), letClause);
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

  private Pair<HaveClause,Expression> typecheckLetClause(Concrete.LetClause clause, boolean isHave) {
    try (var ignore = new Utils.SetContextSaver<>(context)) {
      TypecheckingResult result = typecheckLetClause(clause.getParameters(), clause, true);
      if (result == null) {
        return null;
      }

      String name;
      if (clause.getPattern().isIgnored()) {
        name = null;
      } else {
        StringBuilder builder = new StringBuilder();
        getLetClauseName(clause.getPattern(), builder);
        name = Renamer.getValidName(builder.toString(), Renamer.UNNAMED);
      }
      if (result.expression.isInstance(ErrorExpression.class)) {
        result.expression = new OfTypeExpression(result.expression, result.type);
      }
      return new Pair<>(LetClause.make(!isHave, name, null, result.expression), result.type);
    }
  }

  private LetClausePattern typecheckLetClausePattern(Concrete.LetClausePattern pattern, Expression expression, Expression type, Set<Binding> bindings) {
    Referable referable = pattern.getReferable();
    if (referable != null || pattern.isIgnored()) {
      if (pattern.type != null) {
        Type typeResult = checkType(pattern.type, Type.OMEGA);
        if (typeResult != null && !type.isLessOrEquals(typeResult.getExpr(), myEquations, pattern.type)) {
          errorReporter.report(new TypeMismatchError(typeResult.getExpr(), type, pattern.type));
        }
      }

      String name = referable == null ? null : referable.textRepresentation();
      if (referable != null) {
        Binding binding = new TypedEvaluatingBinding(name, expression, type);
        bindings.add(binding);
        addBinding(referable, binding);
      }
      return new NameLetClausePattern(name);
    }

    type = type.normalize(NormalizationMode.WHNF);
    SigmaExpression sigma = type.cast(SigmaExpression.class);
    ClassCallExpression classCall = type.cast(ClassCallExpression.class);
    List<ClassField> notImplementedFields = classCall == null ? null : classCall.getNotImplementedFields();
    int numberOfPatterns = pattern.getPatterns().size();
    if (sigma == null && classCall == null || sigma != null && DependentLink.Helper.size(sigma.getParameters()) != numberOfPatterns || notImplementedFields != null && notImplementedFields.size() != numberOfPatterns) {
      errorReporter.report(new TypeMismatchError("Cannot match an expression with the pattern", DocFactory.text(sigma == null && classCall == null ? "A sigma type or a record" : sigma != null ? "A sigma type with " + numberOfPatterns + " fields" : "A records with " + numberOfPatterns + " not implemented fields"), type, pattern));
      return null;
    }

    List<LetClausePattern> patterns = new ArrayList<>();
    DependentLink link = sigma == null ? null : sigma.getParameters();
    for (int i = 0; i < numberOfPatterns; i++) {
      assert link != null || notImplementedFields != null;
      Concrete.LetClausePattern subPattern = pattern.getPatterns().get(i);
      Expression newType;
      if (link != null) {
        ExprSubstitution substitution = new ExprSubstitution();
        int j = 0;
        for (DependentLink link1 = sigma.getParameters(); link1 != link; link1 = link1.getNext(), j++) {
          substitution.add(link1, ProjExpression.make(expression, j));
        }
        newType = link.getTypeExpr().subst(substitution);
      } else {
        newType = classCall.getDefinition().getFieldType(notImplementedFields.get(i), classCall.getSortArgument(), expression);
      }
      LetClausePattern letClausePattern = typecheckLetClausePattern(subPattern, link != null ? ProjExpression.make(expression, i) : FieldCallExpression.make(notImplementedFields.get(i), classCall.getSortArgument(), expression), newType, bindings);
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
  public TypecheckingResult visitLet(Concrete.LetExpression expr, Expression expectedType) {
    try (var ignored = new Utils.SetContextSaver<>(context)) {
      try (var ignored1 = new Utils.ContextSaver(myInstancePool == null ? Collections.emptyList() : myInstancePool.getLocalInstances())) {
        List<? extends Concrete.LetClause> abstractClauses = expr.getClauses();
        List<HaveClause> clauses = new ArrayList<>(abstractClauses.size());
        Set<Binding> definedBindings = new HashSet<>();
        for (Concrete.LetClause clause : abstractClauses) {
          Pair<HaveClause, Expression> pair = typecheckLetClause(clause, expr.isHave());
          if (pair == null) {
            return null;
          }
          Referable referable = clause.getPattern().getReferable();
          if (referable != null || clause.getPattern().isIgnored()) {
            pair.proj1.setPattern(new NameLetClausePattern(referable == null ? null : referable.textRepresentation()));
            if (referable != null) {
              addBinding(referable, pair.proj1);
            }
          } else {
            addBinding(null, pair.proj1);
            LetClausePattern pattern = typecheckLetClausePattern(clause.getPattern(), new ReferenceExpression(pair.proj1), pair.proj2, definedBindings);
            if (pattern == null) {
              return null;
            }
            pair.proj1.setPattern(pattern);
          }
          clauses.add(pair.proj1);

          if (myInstancePool != null && pair.proj2 instanceof ClassCallExpression && !((ClassCallExpression) pair.proj2).getDefinition().isRecord()) {
            ClassDefinition classDef = ((ClassCallExpression) pair.proj2).getDefinition();
            Expression instance = new ReferenceExpression(pair.proj1);
            myInstancePool.addLocalInstance(classDef.getClassifyingField() == null ? null : FieldCallExpression.make(classDef.getClassifyingField(), ((ClassCallExpression) pair.proj2).getSortArgument(), instance), classDef, instance);
          }
        }

        TypecheckingResult result = checkExpr(expr.getExpression(), expectedType);
        if (result == null) {
          return null;
        }

        Expression resultType;
        if (expr.isHave()) {
          if (expectedType == null) {
            definedBindings.addAll(clauses);
            CoreBinding binding = result.type.findFreeBindings(definedBindings);
            if (binding != null) {
              errorReporter.report(new TypecheckingError("\\have bindings occur freely in the result type", expr));
              return null;
            }
            resultType = result.type;
          } else {
            resultType = expectedType;
          }
        } else {
          ExprSubstitution substitution = new ExprSubstitution();
          for (HaveClause clause : clauses) {
            substitution.add(clause, clause.getExpression().subst(substitution));
          }
          resultType = result.type.subst(substitution);
        }
        return new TypecheckingResult(new LetExpression(expr.isStrict(), clauses, result.expression), resultType);
      }
    }
  }

  // Other

  private boolean compareExpressions(boolean isLeft, Expression expected, Expression actual, Expression type, Concrete.Expression expr) {
    if (!CompareVisitor.compare(getEquations(), CMP.EQ, actual, expected, type, expr)) {
      errorReporter.report(new PathEndpointMismatchError(isLeft, expected, actual, expr));
      return false;
    }
    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkPath(TResult result, Concrete.Expression expr) {
    if (result instanceof DefCallResult && ((DefCallResult) result).getDefinition() == Prelude.PATH_CON) {
      errorReporter.report(new TypecheckingError("Expected an argument for 'path'", expr));
      return false;
    }
    if (result instanceof TypecheckingResult) {
      ConCallExpression conCall = ((TypecheckingResult) result).expression.cast(ConCallExpression.class);
      if (conCall != null && conCall.getDefinition() == Prelude.PATH_CON) {
        Expression arg = conCall.getDefCallArguments().get(0);
        if (arg instanceof LamExpression && ((LamExpression) arg).getBody() instanceof GoalErrorExpression && !((LamExpression) arg).getParameters().getNext().hasNext()) {
          DependentLink param = ((LamExpression) arg).getParameters();
          GoalErrorExpression goalExpr = (GoalErrorExpression) ((LamExpression) arg).getBody();
          ExprSubstitution leftSubst = new ExprSubstitution(param, ExpressionFactory.Left());
          ExprSubstitution rightSubst = new ExprSubstitution(param, ExpressionFactory.Right());
          goalExpr.goalError.addCondition(new Condition(null, leftSubst, conCall.getDataTypeArguments().get(1)));
          goalExpr.goalError.addCondition(new Condition(null, rightSubst, conCall.getDataTypeArguments().get(2)));
          if (goalExpr.useExpression()) {
            return withErrorReporter(new ListErrorReporter(goalExpr.goalError.errors), tc ->
                   compareExpressions(true,  conCall.getDataTypeArguments().get(1), goalExpr.getExpression().subst(leftSubst), AppExpression.make(conCall.getDataTypeArguments().get(0), ExpressionFactory.Left(), true),  expr)
                && compareExpressions(false, conCall.getDataTypeArguments().get(2), goalExpr.getExpression().subst(rightSubst), AppExpression.make(conCall.getDataTypeArguments().get(0), ExpressionFactory.Right(), true), expr));
          }
        } else {
          return compareExpressions(true,  conCall.getDataTypeArguments().get(1), AppExpression.make(arg, ExpressionFactory.Left(), true),  AppExpression.make(conCall.getDataTypeArguments().get(0), ExpressionFactory.Left(), true),  expr)
              && compareExpressions(false, conCall.getDataTypeArguments().get(2), AppExpression.make(arg, ExpressionFactory.Right(), true), AppExpression.make(conCall.getDataTypeArguments().get(0), ExpressionFactory.Right(), true), expr);
        }
      }
    }
    return true;
  }

  @Nullable
  @Override
  public TypedExpression defer(@NotNull MetaDefinition meta, @NotNull ContextData contextData, @NotNull CoreExpression type, @NotNull Stage stage) {
    if (myCurrentStage != null && myCurrentStage.ordinal() >= stage.ordinal()) {
      if (stage.ordinal() > Stage.BEFORE_SOLVER.ordinal()) {
        myEquations.solveEquations();
      }
      return meta.checkAndInvokeMeta(this, contextData);
    }

    if (!meta.checkContextData(contextData, errorReporter)) {
      return null;
    }
    ConcreteReferenceExpression refExpr = contextData.getReferenceExpression();
    if (!(refExpr instanceof Concrete.ReferenceExpression && type instanceof Expression)) {
      throw new IllegalArgumentException();
    }

    Expression expectedType = (Expression) type;
    ContextDataImpl contextDataImpl = new ContextDataImpl((Concrete.ReferenceExpression) refExpr, contextData.getArguments(), contextData.getCoclauses(), contextData.getClauses(), expectedType, contextData.getUserData());
    InferenceVariable inferenceVar = new MetaInferenceVariable(expectedType, (Concrete.ReferenceExpression) refExpr, getAllBindings());
    (stage == Stage.BEFORE_SOLVER ? myDeferredMetasBeforeSolver : stage == Stage.BEFORE_LEVELS ? myDeferredMetasBeforeLevels : myDeferredMetasAfterLevels)
      .add(new DeferredMeta(meta, new LinkedHashMap<>(context), contextDataImpl, inferenceVar, errorReporter));
    return new TypecheckingResult(new InferenceReferenceExpression(inferenceVar), expectedType);
  }

  private void fixCheckedExpression(TypecheckingResult result, Referable referable, Concrete.SourceNode sourceNode) {
    if (result == null || result.type != null) {
      return;
    }

    result.type = result.expression.getType();
    if (result.type == null) {
      TypecheckingError error = new TypeComputationError(referable, result.expression, sourceNode);
      errorReporter.report(error);
      result.type = new ErrorExpression(error);
    }
  }

  private TypecheckingResult invokeMeta(MetaDefinition meta, ContextData contextData) {
    try {
      return TypecheckingResult.fromChecked(meta.invokeMeta(this, contextData));
    } catch (MetaException e) {
      if (e.error.cause == null) {
        e.error.cause = contextData.getMarker();
      }
      errorReporter.report(e.error);
      ErrorExpression expr = new ErrorExpression(e.error);
      return new TypecheckingResult(expr, expr);
    }
  }

  private TypecheckingResult checkMeta(Concrete.ReferenceExpression refExpr, List<Concrete.Argument> arguments, Concrete.Coclauses coclauses, Expression expectedType) {
    MetaDefinition meta = ((MetaReferable) refExpr.getReferent()).getDefinition();
    if (meta == null) {
      errorReporter.report(new TypecheckingError("Meta '" + refExpr.getReferent().getRefName() + "' is empty", refExpr));
      return null;
    }
    ContextData contextData = new ContextDataImpl(refExpr, arguments, coclauses, null, expectedType, null);
    if (!meta.checkContextData(contextData, errorReporter)) {
      return null;
    }

    int numberOfErrors = getNumberOfErrors();
    TypecheckingResult result = invokeMeta(meta, contextData);
    fixCheckedExpression(result, refExpr.getReferent(), refExpr);
    if (result != null) {
      return result.getType() == expectedType ? result : checkResult(expectedType, result, refExpr);
    }
    if (getNumberOfErrors() == numberOfErrors) {
      errorReporter.report(new TypecheckingError("Meta '" + refExpr.getReferent().getRefName() + "' failed", refExpr));
    }
    return null;
  }

  private void getNotImplementedFields(ClassDefinition origClassDef, ClassDefinition classDef, List<ClassField> result, Set<ClassDefinition> visited) {
    if (!visited.add(classDef)) {
      return;
    }

    for (ClassDefinition superClass : classDef.getSuperClasses()) {
      getNotImplementedFields(origClassDef, superClass, result, visited);
    }
    for (ClassField field : classDef.getPersonalFields()) {
      if (!origClassDef.isImplemented(field)) {
        result.add(field);
      }
    }
  }

  Concrete.Expression desugarClassApp(Concrete.Expression expr, boolean inferTailImplicits) {
    if (expr instanceof Concrete.AppExpression && ((Concrete.AppExpression) expr).getFunction() instanceof Concrete.ReferenceExpression) {
      return desugarClassApp((Concrete.ReferenceExpression) ((Concrete.AppExpression) expr).getFunction(), ((Concrete.AppExpression) expr).getArguments(), expr, null, inferTailImplicits);
    } else if (inferTailImplicits && expr instanceof Concrete.ReferenceExpression) {
      return desugarClassApp((Concrete.ReferenceExpression) expr, Collections.emptyList(), expr, null, true);
    } else {
      return expr;
    }
  }

  private Concrete.Expression desugarClassApp(Concrete.ReferenceExpression fun, List<Concrete.Argument> arguments, Concrete.Expression expr, List<SingleDependentLink> expectedParams, boolean inferTailImplicits) {
    Referable ref = fun.getReferent();
    if (!(ref instanceof TCDefReferable)) {
      return expr;
    }
    Definition def = ((TCDefReferable) ref).getTypechecked();
    if (!(def instanceof ClassDefinition)) {
      return expr;
    }
    ClassDefinition classDef = (ClassDefinition) def;

    // Convert class call with arguments to class extension.
    List<Concrete.ClassFieldImpl> classFieldImpls = new ArrayList<>();
    List<ClassField> notImplementedFields = new ArrayList<>();
    getNotImplementedFields(classDef, classDef, notImplementedFields, new HashSet<>());
    int j = 0;
    for (int i = 0; i < arguments.size(); i++, j++) {
      if (j >= notImplementedFields.size()) {
        errorReporter.report(new TypecheckingError("Too many arguments. Class '" + ref.textRepresentation() + "' " + (notImplementedFields.isEmpty() ? "does not have fields" : "has only " + ArgInferenceError.number(notImplementedFields.size(), "field")), arguments.get(i).expression));
        break;
      }

      ClassField field = notImplementedFields.get(j);
      boolean fieldExplicit = field.getReferable().isExplicitField();
      if (fieldExplicit && !arguments.get(i).isExplicit()) {
        errorReporter.report(new ArgumentExplicitnessError(true, arguments.get(i).expression));
        while (i < arguments.size() && !arguments.get(i).isExplicit()) {
          i++;
        }
        if (i == arguments.size()) {
          break;
        }
      }

      Concrete.Expression argument = arguments.get(i).expression;
      if (fieldExplicit == arguments.get(i).isExplicit()) {
        classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), field.getReferable(), argument, null));
      } else {
        classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), field.getReferable(), new Concrete.HoleExpression(argument.getData()), null));
        i--;
      }
    }

    Object data = arguments.isEmpty() ? fun.getData() : arguments.get(arguments.size() - 1).getExpression().getData();
    if (inferTailImplicits) {
      int maxIndex;
      if (expectedParams != null) {
        int numberOfImplicitParams = 0;
        for (; numberOfImplicitParams < expectedParams.size(); numberOfImplicitParams++) {
          if (expectedParams.get(numberOfImplicitParams).isExplicit()) {
            break;
          }
        }
        int maxImplicitField = j;
        for (; maxImplicitField < notImplementedFields.size(); maxImplicitField++) {
          if (notImplementedFields.get(maxImplicitField).getReferable().isExplicitField() || !notImplementedFields.get(maxImplicitField).getReferable().isParameterField()) {
            break;
          }
        }
        maxIndex = maxImplicitField - numberOfImplicitParams;
      } else {
        maxIndex = notImplementedFields.size();
      }
      for (; j < maxIndex; j++) {
        ClassField field = notImplementedFields.get(j);
        if (field.getReferable().isExplicitField() || !field.getReferable().isParameterField()) {
          break;
        }
        if (!(field.getResultType() instanceof ClassCallExpression) || ((ClassCallExpression) field.getResultType()).getDefinition().isRecord()) {
          break;
        }

        classFieldImpls.add(new Concrete.ClassFieldImpl(data, field.getReferable(), new Concrete.HoleExpression(data), null));
      }
    }

    if (expectedParams != null && !expectedParams.isEmpty()) {
      List<Concrete.Parameter> lamParams = new ArrayList<>(expectedParams.size());
      for (SingleDependentLink param : expectedParams) {
        ClassField field = notImplementedFields.get(j);
        if (param.isExplicit() != field.getReferable().isExplicitField()) {
          lamParams = null;
          break;
        }
        Referable argRef = new LocalReferable(field.getName());
        lamParams.add(new Concrete.NameParameter(fun.getData(), param.isExplicit(), argRef));
        classFieldImpls.add(new Concrete.ClassFieldImpl(data, field.getReferable(), new Concrete.ReferenceExpression(data, argRef), null));
        j++;
      }
      if (lamParams != null) {
        return new Concrete.LamExpression(expr.getData(), lamParams, Concrete.ClassExtExpression.make(expr.getData(), fun, new Concrete.Coclauses(expr.getData(), classFieldImpls)));
      }
    }

    return classFieldImpls.isEmpty() ? fun : Concrete.ClassExtExpression.make(expr.getData(), fun, new Concrete.Coclauses(expr.getData(), classFieldImpls));
  }

  @Override
  public TypecheckingResult visitApp(Concrete.AppExpression expr, Expression expectedType) {
    if (expr.getFunction() instanceof Concrete.ReferenceExpression) {
      Concrete.ReferenceExpression refExpr = (Concrete.ReferenceExpression) expr.getFunction();
      if (refExpr.getReferent() instanceof TCDefReferable && ((TCDefReferable) refExpr.getReferent()).getTypechecked() instanceof ClassDefinition) {
        List<SingleDependentLink> params = expectedType == null ? null : new ArrayList<>();
        if (expectedType != null) {
          expectedType = expectedType.normalizePi(params);
        }
        Concrete.Expression dExpr = desugarClassApp(refExpr, expr.getArguments(), expr, params, true);
        if (dExpr != expr) {
          return checkExpr(dExpr, expectedType);
        }
      }
    }

    if (expr.getFunction() instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) expr.getFunction()).getReferent() instanceof MetaReferable) {
      return checkMeta((Concrete.ReferenceExpression) expr.getFunction(), expr.getArguments(), null, expectedType);
    }

    if (Prelude.DIV_MOD != null && Prelude.MOD != null) {
      Referable referable = expr.getFunction() instanceof Concrete.ReferenceExpression ? ((Concrete.ReferenceExpression) expr.getFunction()).getReferent() : null;
      if ((referable == Prelude.MOD.getReferable() || referable == Prelude.DIV_MOD.getReferable()) && expr.getArguments().size() == 2 && expr.getArguments().get(0).isExplicit() && expr.getArguments().get(1).isExplicit()) {
        TypecheckingResult arg1 = checkExpr(expr.getArguments().get(0).getExpression(), ExpressionFactory.Nat());
        TypecheckingResult arg2 = checkExpr(expr.getArguments().get(1).getExpression(), ExpressionFactory.Nat());
        if (arg1 == null || arg2 == null) {
          return null;
        }

        Type type;
        IntegerExpression intExpr = arg2.expression.cast(IntegerExpression.class);
        ConCallExpression conCall = arg2.expression.cast(ConCallExpression.class);
        if (intExpr != null && !intExpr.isZero() || conCall != null && conCall.getDefinition() == Prelude.SUC) {
          type = ExpressionFactory.Fin(arg2.expression);
        } else {
          type = ExpressionFactory.Nat();
        }

        boolean isMod = referable == Prelude.MOD.getReferable();
        if (!isMod) {
          type = ExpressionFactory.divModType(type);
        }
        return checkResult(expectedType, new TypecheckingResult(FunCallExpression.make(isMod ? Prelude.MOD : Prelude.DIV_MOD, Sort.PROP, Arrays.asList(arg1.expression, arg2.expression)), type.getExpr()), expr);
      }
    }

    TResult result = myArgsInference.infer(expr, expectedType);
    if (result == null || !checkPath(result, expr)) {
      return null;
    }

    return tResultToResult(expectedType, result, expr);
  }

  @Override
  public TypecheckingResult visitUniverse(Concrete.UniverseExpression expr, Expression expectedType) {
    Level pLevel = expr.getPLevel() != null ? expr.getPLevel().accept(this, LevelVariable.PVAR) : null;
    Level hLevel = expr.getHLevel() != null ? expr.getHLevel().accept(this, LevelVariable.HVAR) : null;

    if (pLevel == null) {
      InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, true, expr);
      getEquations().addVariable(pl);
      pLevel = new Level(pl);
    }

    if (hLevel == null) {
      InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, true, expr);
      getEquations().addVariable(hl);
      hLevel = new Level(hl);
    }

    UniverseExpression universe = new UniverseExpression(new Sort(pLevel, hLevel));
    return checkResult(expectedType, new TypecheckingResult(universe, new UniverseExpression(universe.getSort().succ())), expr);
  }

  @Override
  public TypecheckingResult visitTyped(Concrete.TypedExpression expr, Expression expectedType) {
    Type type = checkType(expr.type, Type.OMEGA);
    if (type == null) {
      return checkExpr(expr.expression, expectedType);
    } else {
      return checkResult(expectedType, checkExpr(expr.expression, type.getExpr()), expr);
    }
  }

  @Override
  public @Nullable TypecheckingResult checkNumber(@NotNull BigInteger number, @Nullable CoreExpression expectedType, @NotNull ConcreteExpression marker) {
    if (!((expectedType == null || expectedType instanceof Expression) && marker instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }

    boolean isNegative = number.signum() < 0;
    IntegerExpression resultExpr;
    try {
      int value = number.intValueExact();
      resultExpr = new SmallIntegerExpression(isNegative ? -value : value);
    } catch (ArithmeticException e) {
      resultExpr = new BigIntegerExpression(isNegative ? number.negate() : number);
    }

    TypecheckingResult result;
    if (isNegative) {
      result = new TypecheckingResult(ExpressionFactory.Neg(resultExpr), ExpressionFactory.Int());
    } else {
      Expression ty;
      if (expectedType != null) {
        expectedType = expectedType.normalize(NormalizationMode.WHNF);
      }
      if (expectedType instanceof DataCallExpression && ((DataCallExpression) expectedType).getDefinition() == Prelude.FIN) {
        ty = ExpressionFactory.Fin(resultExpr.suc());
      } else {
        ty = ExpressionFactory.Nat();
      }
      result = new TypecheckingResult(resultExpr, ty);
    }
    return checkResult((Expression) expectedType, result, (Concrete.Expression) marker);
  }

  @Override
  public TypecheckingResult visitNumericLiteral(Concrete.NumericLiteral expr, Expression expectedType) {
    BigInteger number = expr.getNumber();
    if (myArendExtension != null) {
      var checker = myArendExtension.getLiteralTypechecker();
      if (checker != null) {
        int numberOfErrors = getNumberOfErrors();
        TypecheckingResult result = TypecheckingResult.fromChecked(checker.typecheckNumber(number, this, new ContextDataImpl(expr, Collections.emptyList(), null, null, expectedType, null)));
        if (result == null && getNumberOfErrors() == numberOfErrors) {
          errorReporter.report(new TypecheckingError("Cannot check number", expr));
        }
        return result;
      }
    }

    return checkNumber(number, expectedType, expr);
  }

  @Override
  public TypecheckingResult visitStringLiteral(Concrete.StringLiteral expr, Expression expectedType) {
    var string = expr.getUnescapedString();
    if (myArendExtension != null) {
      var checker = myArendExtension.getLiteralTypechecker();
      if (checker != null) {
        int numberOfErrors = getNumberOfErrors();
        TypecheckingResult result = TypecheckingResult.fromChecked(checker.typecheckString(string, this, new ContextDataImpl(expr, Collections.emptyList(), null, null, expectedType, null)));
        if (result == null && getNumberOfErrors() == numberOfErrors) {
          errorReporter.report(new TypecheckingError("Cannot check string", expr));
        }
        return result;
      }
    }

    errorReporter.report(new GeneralError(GeneralError.Level.ERROR, "`StringTypechecker` not found!"));
    return null;
  }

  @Override
  public <T> T withErrorReporter(@NotNull ErrorReporter errorReporter, @NotNull Function<ExpressionTypechecker, T> action) {
    MyErrorReporter originalErrorReport = this.errorReporter;
    this.errorReporter = new MyErrorReporter(errorReporter);
    try {
      return action.apply(this);
    } finally {
      this.errorReporter = originalErrorReport;
    }
  }

  @Override
  public <T> T withFreeBindings(@NotNull FreeBindingsModifier modifier, @NotNull Function<ExpressionTypechecker, T> action) {
    if (modifier.commands.isEmpty()) {
      return action.apply(this);
    }

    try (var ignored = new Utils.CompleteMapContextSaver<>(context)) {
      for (FreeBindingsModifier.Command command : modifier.commands) {
        switch (command.kind) {
          case ADD:
            for (Object binding : (Collection<?>) command.bindings) {
              if (!(binding instanceof Binding)) {
                throw new IllegalArgumentException();
              }
              addBinding(null, (Binding) binding);
            }
            break;
          case CLEAR:
            context.clear();
            break;
          case REMOVE: {
            Set<?> bindings = (Set<?>) command.bindings;
            context.entrySet().removeIf(entry -> bindings.contains(entry.getValue()));
            break;
          }
          case RETAIN: {
            Set<?> bindings = (Set<?>) command.bindings;
            context.entrySet().removeIf(entry -> !bindings.contains(entry.getValue()));
            break;
          }
          case REPLACE:
          case REPLACE_REMOVE: {
            Map<?, ?> replacement = (Map<?, ?>) command.bindings;
            for (Iterator<Map.Entry<Referable, Binding>> iterator = context.entrySet().iterator(); iterator.hasNext(); ) {
              Map.Entry<Referable, Binding> entry = iterator.next();
              Object newBinding = replacement.get(entry.getValue());
              if (newBinding != null) {
                if (!(newBinding instanceof Binding)) {
                  throw new IllegalArgumentException();
                }
                entry.setValue((Binding) newBinding);
              } else if (command.kind == FreeBindingsModifier.Command.Kind.REPLACE_REMOVE) {
                iterator.remove();
              }
            }
            break;
          }
        }
      }

      return action.apply(this);
    }
  }

  public void variableSolved(InferenceVariable variable) {
    if (mySavedState != null) {
      mySavedState.solvedVariables.add(variable);
    }
  }

  private void saveState() {
    TypecheckerState state = new TypecheckerState(errorReporter, myDeferredMetasBeforeSolver.size(), myDeferredMetasBeforeLevels.size(), myDeferredMetasAfterLevels.size(), copyUserData(), mySavedState);
    errorReporter = new MyErrorReporter(new ListErrorReporter());
    myEquations.saveState(state);
    mySavedState = state;
  }

  private void restoreState() {
    if (!(errorReporter.myErrorReporter.getErrorReporter() instanceof ListErrorReporter)) {
      throw new IllegalStateException();
    }
    ((ListErrorReporter) errorReporter.myErrorReporter.getErrorReporter()).reportTo(mySavedState.errorReporter);
    errorReporter = mySavedState.errorReporter;
    mySavedState = mySavedState.previousState;
  }

  @Override
  public <T> T withCurrentState(@NotNull Function<ExpressionTypechecker, T> action) {
    saveState();
    try {
      return action.apply(this);
    } finally {
      restoreState();
    }
  }

  @Override
  public void updateSavedState() {
    if (mySavedState == null || !(errorReporter.myErrorReporter.getErrorReporter() instanceof ListErrorReporter)) {
      throw new IllegalStateException();
    }

    ((ListErrorReporter) errorReporter.myErrorReporter.getErrorReporter()).reportTo(mySavedState.errorReporter);
    TypecheckerState state = new TypecheckerState(mySavedState.errorReporter, myDeferredMetasBeforeSolver.size(), myDeferredMetasBeforeLevels.size(), myDeferredMetasAfterLevels.size(), copyUserData(), mySavedState.previousState);
    myEquations.saveState(state);
    mySavedState = state;
  }

  private void loadState(TypecheckerState state) {
    if (!(errorReporter.myErrorReporter.getErrorReporter() instanceof ListErrorReporter)) {
      throw new IllegalStateException();
    }
    ((ListErrorReporter) errorReporter.myErrorReporter.getErrorReporter()).getErrorList().clear();
    if (state.numberOfDeferredMetasBeforeSolver < myDeferredMetasBeforeSolver.size()) {
      myDeferredMetasBeforeSolver.subList(state.numberOfDeferredMetasBeforeSolver, myDeferredMetasBeforeSolver.size()).clear();
    }
    if (state.numberOfDeferredMetasBeforeLevels < myDeferredMetasBeforeLevels.size()) {
      myDeferredMetasBeforeLevels.subList(state.numberOfDeferredMetasBeforeLevels, myDeferredMetasBeforeLevels.size()).clear();
    }
    if (state.numberOfDeferredMetasAfterLevels < myDeferredMetasAfterLevels.size()) {
      myDeferredMetasAfterLevels.subList(state.numberOfDeferredMetasAfterLevels, myDeferredMetasAfterLevels.size()).clear();
    }
    setUserData(state.userDataHolder);

    for (InferenceVariable var : state.solvedVariables) {
      var.unsolve();
    }
    state.solvedVariables.clear();

    myEquations.loadState(state);
  }

  @Override
  public void loadSavedState() {
    if (mySavedState == null) {
      throw new IllegalStateException();
    }
    loadState(mySavedState);
  }

  @Override
  public boolean solveInferenceVariable(@NotNull CoreInferenceVariable variable, @NotNull CoreExpression expression) {
    if (!(variable instanceof InferenceVariable && expression instanceof Expression) || variable instanceof MetaInferenceVariable) {
      throw new IllegalArgumentException();
    }
    return myEquations.solve((InferenceVariable) variable, (Expression) expression);
  }

  @Override
  public @NotNull CoreInferenceReferenceExpression generateNewInferenceVariable(@NotNull String name, @NotNull CoreExpression type, @NotNull ConcreteSourceNode marker, boolean isSolvableFromEquations) {
    if (!(type instanceof Expression && marker instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }
    return new InferenceReferenceExpression(new UserInferenceVariable(name, (Expression) type, (Concrete.SourceNode) marker, getAllBindings(), isSolvableFromEquations));
  }

  @Override
  public @NotNull CoreSort generateSort(@NotNull ConcreteSourceNode marker) {
    if (!(marker instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }
    return Sort.generateInferVars(myEquations, true, (Concrete.SourceNode) marker);
  }

  @Override
  public @Nullable ConcreteExpression findInstance(@NotNull InstanceSearchParameters parameters, @Nullable UncheckedExpression classifyingExpression, @NotNull ConcreteSourceNode sourceNode) {
    if (!(sourceNode instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }
    return myInstancePool.getInstance(UncheckedExpressionImpl.extract(classifyingExpression), parameters, (Concrete.SourceNode) sourceNode, null);
  }

  @Override
  public @Nullable TypedExpression findInstance(@NotNull InstanceSearchParameters parameters, @Nullable UncheckedExpression classifyingExpression, @Nullable CoreExpression expectedType, @NotNull ConcreteSourceNode sourceNode) {
    if (!((expectedType == null || expectedType instanceof Expression) && sourceNode instanceof Concrete.SourceNode)) {
      throw new IllegalArgumentException();
    }
    return myInstancePool.getInstance(UncheckedExpressionImpl.extract(classifyingExpression), expectedType == null ? null : (Expression) expectedType, parameters, (Concrete.SourceNode) sourceNode, null);
  }

  @Override
  public void checkCancelled() {
    ComputationRunner.checkCanceled();
  }

  @Override
  public TypecheckingResult visitGoal(Concrete.GoalExpression expr, Expression expectedType) {
    List<GeneralError> errors = expr.errors;
    GoalSolver.CheckGoalResult goalResult = null;
    GoalSolver solver = expr.useGoalSolver ? expr.goalSolver : myArendExtension != null ? myArendExtension.getGoalSolver() : null;
    if (expr.expression != null || solver != null) {
      errors = new ArrayList<>(expr.errors);
      goalResult = withErrorReporter(new ListErrorReporter(errors), tc -> {
        if (solver == null) {
          return new GoalSolver.CheckGoalResult(expr.originalExpression, checkExpr(expr.expression, expectedType));
        } else {
          return solver.checkGoal(tc, expr, expectedType);
        }
      });
    }

    if (goalResult != null && (!(goalResult.concreteExpression == null || goalResult.concreteExpression instanceof Concrete.Expression) || !(goalResult.typedExpression == null || goalResult.typedExpression.getExpression() instanceof Expression))) {
      throw new IllegalArgumentException();
    }

    GoalError error = new GoalError(saveTypecheckingContext(), expectedType, goalResult == null ? null : (Concrete.Expression) goalResult.concreteExpression, errors, solver, expr);
    errorReporter.report(error);
    Expression result = new GoalErrorExpression(goalResult == null || goalResult.typedExpression == null ? null : (Expression) goalResult.typedExpression.getExpression(), error);
    return new TypecheckingResult(result, expectedType != null && !(expectedType instanceof Type && ((Type) expectedType).isOmega()) ? expectedType : result);
  }

  @Override
  public TypecheckingResult visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Expression expectedType) {
    throw new IllegalStateException();
  }

  @Override
  public TypecheckingResult visitApplyHole(Concrete.ApplyHoleExpression expr, Expression params) {
    errorReporter.report(new TypecheckingError("`__` not allowed here", expr));
    return null;
  }

  public static Expression getLevelExpression(Type type, int level) {
    if (level < -1) {
      return type.getExpr();
    }

    SingleDependentLink params = ExpressionFactory.singleParams(true, Arrays.asList("x" + (level + 2), "y" + (level + 2)), type);
    Sort sort = type.getSortOfType();
    return new PiExpression(sort, params, getLevelExpression(new TypeExpression(FunCallExpression.make(Prelude.PATH_INFIX, sort, Arrays.asList(type.getExpr(), new ReferenceExpression(params), new ReferenceExpression(params.getNext()))), sort), level - 1));
  }

  public Integer getExpressionLevel(DependentLink link, Expression type, Expression expr, Equations equations, Concrete.SourceNode sourceNode) {
    return getExpressionLevel(link, type, expr, equations, sourceNode, errorReporter);
  }

  public static Integer getExpressionLevel(DependentLink link, Expression type, Expression expr, Equations equations, Concrete.SourceNode sourceNode, ErrorReporter errorReporter) {
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
          if (!CompareVisitor.compare(equations, CMP.EQ, link.getTypeExpr(), expr, Type.OMEGA, sourceNode)) {
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
        if (!CompareVisitor.compare(equations, CMP.EQ, link.getTypeExpr(), expr, Type.OMEGA, sourceNode)) {
          ok = false;
          break;
        }

        pathArgs.add(new ReferenceExpression(link));
        expr = FunCallExpression.make(Prelude.PATH_INFIX, Sort.STD, pathArgs);
        level++;
      }

      if (ok && resultType != null && !CompareVisitor.compare(equations, CMP.EQ, resultType, expr, Type.OMEGA, sourceNode)) {
        ok = false;
      }
    }

    if (!ok || level < -1) {
      errorReporter.report(new TypecheckingError("\\level has wrong format", sourceNode));
      return null;
    } else {
      return level;
    }
  }

  private Expression checkedSubst(Expression expr, ExprSubstitution substitution, Concrete.SourceNode sourceNode) {
    if (substitution.isEmpty()) {
      return expr;
    }

    Set<Binding> allowedBindings = new HashSet<>();
    for (Variable variable : substitution.getKeys()) {
      if (variable instanceof Binding) {
        allowedBindings.add((Binding) variable);
      }
    }

    Set<Object> foundVars = new LinkedHashSet<>();
    expr.accept(new FindMissingBindingVisitor(allowedBindings) {
      @Override
      public Boolean visitReference(ReferenceExpression expr, Void params) {
        Binding binding = expr.getBinding();
        if (!allowedBindings.contains(binding) && binding.getTypeExpr().findBinding(substitution.getKeys()) != null) {
          foundVars.add(binding);
        }
        return false;
      }
    }, null);

    if (!foundVars.isEmpty()) {
      for (Map.Entry<Referable, Binding> entry : context.entrySet()) {
        if (foundVars.remove(entry.getValue())) {
          foundVars.add(entry.getKey());
        }
      }
      errorReporter.report(new ElimSubstError(foundVars, sourceNode));
      return expr;
    }

    return expr.subst(substitution);
  }

  Integer minInteger(Integer int1, Integer int2) {
    return int1 == null ? int2 : int2 == null ? int1 : Integer.valueOf(Math.min(int1, int2));
  }

  @Override
  public TypecheckingResult visitCase(Concrete.CaseExpression expr, Expression expectedType) {
    if (expectedType == null && expr.getResultType() == null) {
      errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.CASE_RESULT_TYPE, expr));
      return null;
    }

    List<? extends Concrete.CaseArgument> caseArgs = expr.getArguments();
    LinkList list = new LinkList();
    List<Expression> expressions = new ArrayList<>(caseArgs.size());

    ExprSubstitution substitution = new ExprSubstitution();
    Type resultType = null;
    Expression resultExpr;
    Integer level = expr.level >= -1 ? expr.level : null;
    Expression resultTypeLevel = null;
    Map<Referable, Binding> origElimBindings = new HashMap<>();
    ExprSubstitution elimSubst = new ExprSubstitution();
    try (var ignored = new Utils.SetContextSaver<>(context)) {
      for (Concrete.CaseArgument caseArg : caseArgs) {
        Type argType = null;
        if (caseArg.type != null) {
          argType = checkType(caseArg.type, Type.OMEGA);
        }

        Expression argTypeExpr = argType == null ? null : argType.getExpr().subst(substitution);
        TypecheckingResult exprResult = checkExpr(caseArg.expression, argTypeExpr);
        if (exprResult == null) return null;
        if (caseArg.isElim && !(exprResult.expression instanceof ReferenceExpression)) {
          errorReporter.report(new TypecheckingError("Expected a variable", caseArg.expression));
          return null;
        }
        if (argType == null || caseArg.isElim) {
          exprResult.type = checkedSubst(exprResult.type, elimSubst, caseArg.expression);
        }
        Referable asRef = caseArg.isElim ? ((Concrete.ReferenceExpression) caseArg.expression).getReferent() : caseArg.referable;
        DependentLink link = ExpressionFactory.parameter(asRef == null ? null : asRef.textRepresentation(), argType != null ? argType : exprResult.type instanceof Type ? (Type) exprResult.type : new TypeExpression(exprResult.type, getSortOfType(exprResult.type, expr)));
        list.append(link);
        if (caseArg.isElim) {
          if (argTypeExpr != null) {
            Expression actualArgType = exprResult.type.subst(substitution);
            if (!CompareVisitor.compare(myEquations, CMP.EQ, actualArgType, argTypeExpr, Type.OMEGA, caseArg.type)) {
              errorReporter.report(new TypeMismatchError(actualArgType, argTypeExpr, caseArg.expression));
              return null;
            }
          }
          Binding origBinding = ((ReferenceExpression) exprResult.expression).getBinding();
          origElimBindings.put(asRef, origBinding);
          elimSubst.add(origBinding, new ReferenceExpression(link));
        }
        addBinding(asRef, link);
        expressions.add(exprResult.expression.subst(substitution));
        substitution.add(link, exprResult.expression);
      }

      if (expr.getResultType() != null) {
        resultType = checkType(expr.getResultType(), Type.OMEGA);
      }
      if (resultType == null && expectedType == null) {
        return null;
      }
      resultExpr = resultType != null ? resultType.getExpr() : !(expectedType instanceof Type && ((Type) expectedType).isOmega()) ? checkedSubst(expectedType, elimSubst, expr.getResultType() != null ? expr.getResultType() : expr) : new UniverseExpression(Sort.generateInferVars(myEquations, false, expr));

      if (expr.getResultTypeLevel() != null) {
        TypecheckingResult levelResult = checkExpr(expr.getResultTypeLevel(), null);
        if (levelResult != null) {
          resultTypeLevel = levelResult.expression;
          level = minInteger(level, getExpressionLevel(EmptyDependentLink.getInstance(), levelResult.type, resultExpr, myEquations, expr.getResultTypeLevel()));
        }
      }
    }

    List<Referable> addedRefs = new ArrayList<>();
    for (Map.Entry<Referable, Binding> entry : origElimBindings.entrySet()) {
      context.remove(entry.getKey());
      VeryFakeLocalReferable ref = new VeryFakeLocalReferable(entry.getValue().getName());
      context.put(ref, entry.getValue());
      addedRefs.add(ref);
    }

    // Check if the level of the result type is specified explicitly
    if (expr.getResultTypeLevel() == null && expr.getResultType() instanceof Concrete.TypedExpression) {
      Concrete.Expression typeType = ((Concrete.TypedExpression) expr.getResultType()).type;
      if (typeType instanceof Concrete.UniverseExpression) {
        Concrete.UniverseExpression universeType = (Concrete.UniverseExpression) typeType;
        if (universeType.getHLevel() instanceof Concrete.NumberLevelExpression) {
          level = minInteger(level, ((Concrete.NumberLevelExpression) universeType.getHLevel()).getNumber());
        }
      }
    }

    // Try to infer level from \\use annotations of the definition in the result type.
    if (expr.getResultTypeLevel() == null) {
      DefCallExpression defCall = resultExpr.cast(DefCallExpression.class);
      Integer level2 = defCall == null ? null : defCall.getUseLevel();
      if (level2 == null) {
        defCall = resultExpr.getPiParameters(null, false).cast(DefCallExpression.class);
        if (defCall != null) {
          level2 = defCall.getUseLevel();
        }
      }
      level = minInteger(level, level2);
    }

    Level actualLevel;
    {
      Sort sort = resultType == null ? null : resultType.getSortOfType();
      actualLevel = sort != null ? sort.getHLevel() : Level.INFINITY;
    }

    List<ExtElimClause> clauses;
    try {
      PatternTypechecking patternTypechecking = new PatternTypechecking(errorReporter, PatternTypechecking.Mode.CASE, this, false, expressions, Collections.emptyList());
      clauses = patternTypechecking.typecheckClauses(expr.getClauses(), list.getFirst(), resultExpr);
    } finally {
      for (Referable ref : addedRefs) {
        context.remove(ref);
      }
      context.putAll(origElimBindings);
    }
    if (clauses == null) {
      return null;
    }
    ElimBody elimBody = new ElimTypechecking(errorReporter, myEquations, resultExpr, PatternTypechecking.Mode.CASE, level, actualLevel, expr.isSCase(), expr.getClauses(), expr).typecheckElim(clauses, list.getFirst());
    if (elimBody == null) {
      return null;
    }

    if (!(expr.isSCase() && actualLevel.isProp())) {
      new ConditionsChecking(myEquations, errorReporter, expr).check(clauses, expr.getClauses(), elimBody);
    }
    TypecheckingResult result = new TypecheckingResult(new CaseExpression(expr.isSCase(), list.getFirst(), resultExpr, resultTypeLevel, elimBody, expressions), resultType != null ? resultExpr.subst(substitution) : expectedType);
    return resultType == null ? result : checkResult(expectedType, result, expr);
  }

  @Override
  public TypecheckingResult visitEval(Concrete.EvalExpression expr, Expression expectedType) {
    TypecheckingResult result = checkExpr(expr.getExpression(), expr.isPEval() ? null : expectedType);
    if (result == null) {
      return null;
    }

    FunCallExpression funCall = result.expression instanceof FunCallExpression ? (FunCallExpression) result.expression : null;
    CaseExpression caseExpr = funCall != null ? null : result.expression instanceof CaseExpression ? (CaseExpression) result.expression : null;
    if ((caseExpr == null || !caseExpr.isSCase()) && (funCall == null || funCall.getDefinition().getKind() != CoreFunctionDefinition.Kind.SFUNC)) {
      errorReporter.report(new TypecheckingError(
        funCall != null ? "Expected a function (defined as \\sfunc) applied to arguments" :
        caseExpr != null ? "Expected an \\scase expression" :
          "Expected a function or an \\scase expression", expr.getExpression()));
      return null;
    }
    if (funCall != null && !(funCall.getDefinition().getActualBody() instanceof ElimBody)) {
      errorReporter.report(new FunctionWithoutBodyError(funCall.getDefinition(), expr.getExpression()));
      return null;
    }

    PEvalExpression pEvalResult = new PEvalExpression(result.expression);
    Expression normExpr = pEvalResult.eval();
    if (normExpr == null) {
      errorReporter.report(new TypecheckingError("Expression does not evaluate", expr.getExpression()));
      return null;
    }

    if (!expr.isPEval()) {
      return new TypecheckingResult(normExpr, result.type);
    }

    Expression typeType = result.type.getType();
    if (typeType == null) {
      errorReporter.report(new TypecheckingError("Cannot infer the universe of the type of the expression", expr.getExpression()));
      return null;
    }

    Sort sortArg;
    typeType = typeType.normalize(NormalizationMode.WHNF);
    UniverseExpression universe = typeType.cast(UniverseExpression.class);
    if (universe != null) {
      sortArg = universe.getSort();
    } else {
      sortArg = Sort.generateInferVars(myEquations, false, expr.getExpression());
      myEquations.addEquation(typeType, new UniverseExpression(sortArg), Type.OMEGA, CMP.LE, expr.getExpression(), typeType.getStuckInferenceVariable(), null);
    }

    List<Expression> args = new ArrayList<>(3);
    args.add(result.type);
    args.add(result.expression);
    args.add(normExpr);
    return checkResult(expectedType, new TypecheckingResult(pEvalResult, FunCallExpression.make(Prelude.PATH_INFIX, sortArg, args)), expr);
  }
}
