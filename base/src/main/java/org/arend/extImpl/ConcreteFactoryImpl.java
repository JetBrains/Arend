package org.arend.extImpl;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.ReferenceExpression;
import org.arend.ext.concrete.*;
import org.arend.ext.concrete.definition.*;
import org.arend.ext.concrete.expr.*;
import org.arend.ext.concrete.pattern.ConcretePattern;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.AbstractedExpression;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.GoalSolver;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.result.TypecheckingResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;

public class ConcreteFactoryImpl implements ConcreteFactory {
  private final Object myData;

  public ConcreteFactoryImpl(Object data) {
    myData = data;
  }

  @NotNull
  @Override
  public ConcreteReferenceExpression ref(@NotNull ArendRef ref) {
    if (!(ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(myData, (Referable) ref);
  }

  private List<Concrete.LevelExpression> makeLevels(List<? extends ConcreteLevel> levels) {
    if (levels == null) return null;
    List<Concrete.LevelExpression> result = new ArrayList<>(levels.size());
    for (ConcreteLevel level : levels) {
      if (!(level == null || level instanceof Concrete.LevelExpression)) throw new IllegalArgumentException();
      result.add((Concrete.LevelExpression) level);
    }
    return result;
  }

  @NotNull
  @Override
  public ConcreteReferenceExpression ref(@NotNull ArendRef ref, @Nullable List<? extends ConcreteLevel> pLevels, @Nullable List<? extends ConcreteLevel> hLevels) {
    if (!(ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(myData, (Referable) ref, makeLevels(pLevels), makeLevels(hLevels));
  }

  @Override
  public @NotNull ConcreteReferenceExpression ref(@NotNull CoreBinding ref) {
    if (!(ref instanceof Binding)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(myData, new CoreReferable(ref.getName(), new TypecheckingResult(new ReferenceExpression((Binding) ref), ((Binding) ref).getTypeExpr())));
  }

  @NotNull
  @Override
  public ConcreteExpression core(@NotNull TypedExpression expr) {
    return core(null, expr);
  }

  @NotNull
  @Override
  public ConcreteExpression core(@Nullable String name, @NotNull TypedExpression expr) {
    return new Concrete.ReferenceExpression(myData, new CoreReferable(name, TypecheckingResult.fromChecked(Objects.requireNonNull(expr))));
  }

  @Override
  public @NotNull ConcreteExpression abstracted(@NotNull AbstractedExpression expr, @NotNull List<? extends ConcreteExpression> arguments) {
    if (expr.getNumberOfAbstractedBindings() != arguments.size()) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(myData, new AbstractedReferable(expr, arguments));
  }

  @NotNull
  @Override
  public ConcreteExpression meta(@NotNull String name, @NotNull MetaDefinition meta) {
    return new Concrete.ReferenceExpression(myData, new MetaReferable(Precedence.DEFAULT, name, "", meta, null, null));
  }

  @NotNull
  @Override
  public ConcreteExpression thisExpr(@Nullable ArendRef ref) {
    if (!(ref instanceof Referable || ref == null)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ThisExpression(myData, (Referable) ref);
  }

  private List<Concrete.Parameter> parameters(Collection<? extends ConcreteParameter> parameters) {
    List<Concrete.Parameter> result = new ArrayList<>(parameters.size());
    for (ConcreteParameter parameter : parameters) {
      if (!(parameter instanceof Concrete.Parameter)) {
        throw new IllegalArgumentException();
      }
      result.add((Concrete.Parameter) parameter);
    }
    return result;
  }

  @NotNull
  @Override
  public ConcreteExpression lam(@NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression body) {
    if (!(body instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return parameters.isEmpty() ? body : new Concrete.LamExpression(myData, parameters(parameters), (Concrete.Expression) body);
  }

  private List<Concrete.TypeParameter> typeParameters(Collection<? extends ConcreteParameter> parameters) {
    List<Concrete.TypeParameter> result = new ArrayList<>(parameters.size());
    for (ConcreteParameter parameter : parameters) {
      if (!(parameter instanceof Concrete.Parameter)) {
        throw new IllegalArgumentException();
      }
      if (!(parameter instanceof Concrete.TypeParameter)) {
        throw new IllegalArgumentException("Expected a typed parameter");
      }
      result.add((Concrete.TypeParameter) parameter);
    }
    return result;
  }

  @NotNull
  @Override
  public ConcreteExpression pi(@NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression codomain) {
    if (!(codomain instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return parameters.isEmpty() ? codomain : new Concrete.PiExpression(myData, typeParameters(parameters), (Concrete.Expression) codomain);
  }

  @Override
  public @NotNull ConcreteExpression arr(@NotNull ConcreteExpression domain, @NotNull ConcreteExpression codomain) {
    if (!(domain instanceof Concrete.Expression && codomain instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.PiExpression(myData, Collections.singletonList(new Concrete.TypeParameter(myData, true, (Concrete.Expression) domain, false)), (Concrete.Expression) codomain);
  }

  @NotNull
  @Override
  public Concrete.UniverseExpression universe(@Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel) {
    if (!((pLevel == null || pLevel instanceof Concrete.LevelExpression) && (hLevel == null || hLevel instanceof Concrete.LevelExpression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.UniverseExpression(myData, (Concrete.LevelExpression) pLevel, (Concrete.LevelExpression) hLevel);
  }

  @NotNull
  @Override
  public ConcreteExpression hole() {
    return new Concrete.HoleExpression(myData);
  }

  @Override
  public @NotNull ConcreteExpression error(@Nullable GeneralError error) {
    return new Concrete.ErrorHoleExpression(myData, error instanceof LocalError ? (LocalError) error : null);
  }

  @NotNull
  @Override
  public ConcreteExpression goal() {
    return new Concrete.GoalExpression(myData, null, null);
  }

  @NotNull
  @Override
  public Concrete.GoalExpression goal(@Nullable String name, @Nullable ConcreteExpression expression) {
    if (!(expression == null || expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.GoalExpression(myData, name, (Concrete.Expression) expression);
  }

  @Override
  public @NotNull ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression, @Nullable GoalSolver goalSolver) {
    return goal(name, expression, goalSolver, Collections.emptyList());
  }

  @Override
  public @NotNull ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression, @Nullable GoalSolver goalSolver, @NotNull List<GeneralError> errors) {
    if (!(expression == null || expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.GoalExpression(myData, name, (Concrete.Expression) expression, goalSolver, true, errors);
  }

  @NotNull
  @Override
  public ConcreteExpression tuple(@NotNull ConcreteExpression... expressions) {
    if (expressions.length == 1) return expressions[0];
    List<Concrete.Expression> fields = new ArrayList<>(expressions.length);
    for (ConcreteExpression expression : expressions) {
      if (!(expression instanceof Concrete.Expression)) {
        throw new IllegalArgumentException();
      }
      fields.add((Concrete.Expression) expression);
    }
    return new Concrete.TupleExpression(myData, fields);
  }

  @Override
  public @NotNull ConcreteExpression tuple(@NotNull Collection<? extends ConcreteExpression> expressions) {
    if (expressions.size() == 1) return expressions.iterator().next();
    List<Concrete.Expression> fields = new ArrayList<>(expressions.size());
    for (ConcreteExpression expression : expressions) {
      if (!(expression instanceof Concrete.Expression)) {
        throw new IllegalArgumentException();
      }
      fields.add((Concrete.Expression) expression);
    }
    return new Concrete.TupleExpression(myData, fields);
  }

  @NotNull
  @Override
  public ConcreteExpression sigma(@NotNull ConcreteParameter... parameters) {
    return sigma(Arrays.asList(parameters));
  }

  @Override
  public @NotNull ConcreteExpression sigma(@NotNull List<? extends ConcreteParameter> parameters) {
    if (parameters.size() == 1) {
      ConcreteExpression type = parameters.get(0).getType();
      if (type == null) throw new IllegalArgumentException();
      return type;
    }
    return new Concrete.SigmaExpression(myData, typeParameters(new ArrayList<>(parameters)));
  }

  private ConcreteExpression caseExprC(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull List<Concrete.FunctionClause> clauses) {
    if (!((resultType == null || resultType instanceof Concrete.Expression) && (resultTypeLevel == null || resultTypeLevel instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    List<Concrete.CaseArgument> cArgs = new ArrayList<>(arguments.size());
    for (ConcreteCaseArgument argument : arguments) {
      if (!(argument instanceof Concrete.CaseArgument)) {
        throw new IllegalArgumentException();
      }
      cArgs.add((Concrete.CaseArgument) argument);
    }
    return new Concrete.CaseExpression(myData, isSCase, cArgs, (Concrete.Expression) resultType, (Concrete.Expression) resultTypeLevel, clauses);
  }

  @NotNull
  @Override
  public ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull ConcreteClause... clauses) {
    return caseExprC(isSCase, arguments, resultType, resultTypeLevel, functionClauses(Arrays.asList(clauses)));
  }

  private List<Concrete.FunctionClause> functionClauses(Collection<? extends ConcreteClause> clauses) {
    List<Concrete.FunctionClause> cClauses = new ArrayList<>(clauses.size());
    for (ConcreteClause clause : clauses) {
      if (!(clause instanceof Concrete.Clause)) {
        throw new IllegalArgumentException();
      }
      if (!(clause instanceof Concrete.FunctionClause)) {
        throw new IllegalArgumentException("Expected a function clause");
      }
      cClauses.add((Concrete.FunctionClause) clause);
    }
    return cClauses;
  }

  @Override
  public @NotNull ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull Collection<? extends ConcreteClause> clauses) {
    return caseExprC(isSCase, arguments, resultType, resultTypeLevel, functionClauses(clauses));
  }

  @NotNull
  @Override
  public ConcreteExpression eval(@NotNull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.EvalExpression(myData, false, (Concrete.Expression) expression);
  }

  @NotNull
  @Override
  public ConcreteExpression peval(@NotNull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.EvalExpression(myData, true, (Concrete.Expression) expression);
  }

  @NotNull
  @Override
  public ConcreteExpression proj(@NotNull ConcreteExpression expression, int field) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ProjExpression(myData, (Concrete.Expression) expression, field);
  }

  @Override
  public @NotNull ConcreteExpression path(@NotNull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return Concrete.AppExpression.make(myData, new Concrete.ReferenceExpression(myData, Prelude.PATH_CON.getRef()), (Concrete.Expression) expression, true);
  }

  @Override
  public @NotNull ConcreteExpression at(@NotNull ConcreteExpression path, @NotNull ConcreteExpression interval) {
    if (!(path instanceof Concrete.Expression && interval instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return Concrete.AppExpression.make(myData, Concrete.AppExpression.make(myData, new Concrete.ReferenceExpression(myData, Prelude.AT.getRef()), (Concrete.Expression) path, true), (Concrete.Expression) interval, true);
  }

  private List<Concrete.ClassFieldImpl> classFieldImpls(ConcreteClassElement[] elements) {
    List<Concrete.ClassFieldImpl> result = new ArrayList<>(elements.length);
    for (ConcreteClassElement element : elements) {
      if (!(element instanceof Concrete.ClassElement)) {
        throw new IllegalArgumentException();
      }
      if (!(element instanceof Concrete.ClassFieldImpl)) {
        throw new IllegalArgumentException("Expected a field implementation");
      }
      result.add((Concrete.ClassFieldImpl) element);
    }
    return result;
  }

  private List<Concrete.ClassFieldImpl> classFieldImpls(Collection<? extends ConcreteClassElement> elements) {
    List<Concrete.ClassFieldImpl> result = new ArrayList<>(elements.size());
    for (ConcreteClassElement element : elements) {
      if (!(element instanceof Concrete.ClassElement)) {
        throw new IllegalArgumentException();
      }
      if (!(element instanceof Concrete.ClassFieldImpl)) {
        throw new IllegalArgumentException("Expected a field implementation");
      }
      result.add((Concrete.ClassFieldImpl) element);
    }
    return result;
  }

  @NotNull
  @Override
  public ConcreteExpression classExt(@NotNull ConcreteExpression expression, @NotNull ConcreteClassElement... elements) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return Concrete.ClassExtExpression.make(myData, (Concrete.Expression) expression, new Concrete.Coclauses(myData, classFieldImpls(elements)));
  }

  @Override
  public @NotNull ConcreteExpression classExt(@NotNull ConcreteExpression expression, @NotNull Collection<? extends ConcreteClassElement> elements) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return Concrete.ClassExtExpression.make(myData, (Concrete.Expression) expression, new Concrete.Coclauses(myData, classFieldImpls(elements)));
  }

  @NotNull
  @Override
  public ConcreteExpression newExpr(@NotNull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.NewExpression(myData, (Concrete.Expression) expression);
  }

  @NotNull
  @Override
  public ConcreteExpression letExpr(boolean isHave, boolean isStrict, @NotNull Collection<? extends ConcreteLetClause> clauses, @NotNull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    List<Concrete.LetClause> cClauses = new ArrayList<>(clauses.size());
    for (ConcreteLetClause clause : clauses) {
      if (!(clause instanceof Concrete.LetClause)) {
        throw new IllegalArgumentException();
      }
      cClauses.add((Concrete.LetClause) clause);
    }
    return new Concrete.LetExpression(myData, isHave, isStrict, cClauses, (Concrete.Expression) expression);
  }

  @Override
  public @NotNull ConcreteExpression boxExpr(@NotNull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.BoxExpression(myData, (Concrete.Expression) expression);
  }

  @NotNull
  @Override
  public ConcreteExpression number(@NotNull BigInteger number) {
    return new Concrete.NumericLiteral(myData, number);
  }

  @NotNull
  @Override
  public ConcreteExpression number(int number) {
    return new Concrete.NumericLiteral(myData, BigInteger.valueOf(number));
  }

  @Override
  public @NotNull ConcreteExpression string(@NotNull String s) {
    return new Concrete.StringLiteral(myData, s);
  }

  @Override
  public @NotNull ConcreteExpression qName(@NotNull ArendRef ref) {
    if (!(ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.QNameLiteral(myData, new Concrete.ReferenceExpression(myData, (Referable) ref));
  }

  @NotNull
  @Override
  public ConcreteExpression typed(@NotNull ConcreteExpression expression, @NotNull ConcreteExpression type) {
    if (!(expression instanceof Concrete.Expression && type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.TypedExpression(myData, (Concrete.Expression) expression, (Concrete.Expression) type);
  }

  @Override
  public @NotNull Concrete.Expression app(@NotNull ConcreteExpression function, @NotNull Collection<? extends ConcreteArgument> arguments) {
    if (!(function instanceof Concrete.Expression fun)) {
      throw new IllegalArgumentException();
    }
    if (arguments.isEmpty()) {
      return fun;
    }
    List<Concrete.Argument> args = fun instanceof Concrete.AppExpression ? new ArrayList<>(((Concrete.AppExpression) fun).getArguments()) : new ArrayList<>(arguments.size());
    for (ConcreteArgument argument : arguments) {
      if (!(argument instanceof Concrete.Argument)) {
        throw new IllegalArgumentException();
      }
      args.add((Concrete.Argument) argument);
    }
    return Concrete.AppExpression.make(fun.getData(), fun instanceof Concrete.AppExpression ? ((Concrete.AppExpression) fun).getFunction() : fun, args);
  }

  @Override
  public @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, boolean isExplicit, @NotNull Collection<? extends ConcreteExpression> arguments) {
    if (!(function instanceof Concrete.Expression fun)) {
      throw new IllegalArgumentException();
    }
    if (arguments.isEmpty()) {
      return fun;
    }
    List<Concrete.Argument> args = fun instanceof Concrete.AppExpression ? new ArrayList<>(((Concrete.AppExpression) fun).getArguments()) : new ArrayList<>(arguments.size());
    for (ConcreteExpression argument : arguments) {
      if (!(argument instanceof Concrete.Expression)) {
        throw new IllegalArgumentException();
      }
      args.add(new Concrete.Argument((Concrete.Expression) argument, isExplicit));
    }
    return Concrete.AppExpression.make(fun.getData(), fun instanceof Concrete.AppExpression ? ((Concrete.AppExpression) fun).getFunction() : fun, args);
  }

  @Override
  public @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, ConcreteArgument... arguments) {
    return app(function, Arrays.asList(arguments));
  }

  @Override
  public @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, boolean isExplicit, ConcreteExpression... arguments) {
    return app(function, isExplicit, Arrays.asList(arguments));
  }

  @Override
  public @NotNull ConcreteArgument arg(@NotNull ConcreteExpression expression, boolean isExplicit) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.Argument((Concrete.Expression) expression, isExplicit);
  }

  @Override
  public @NotNull ConcreteAppBuilder appBuilder(@NotNull ConcreteExpression function) {
    if (!(function instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new ConcreteAppBuilderImpl(myData, (Concrete.Expression) function);
  }

  @Override
  public @NotNull Concrete.FunctionDefinition function(@NotNull ArendRef ref, @NotNull FunctionKind kind, @NotNull Collection<? extends ConcreteParameter> parameters, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull ConcreteFunctionBody body) {
    if (!(ref instanceof ConcreteLocatedReferable cRef)) {
      throw new IllegalArgumentException("The reference must be a global reference with a parent");
    }
    if (kind.isUse() || kind.isCoclause()) {
      throw new IllegalArgumentException("\\use definitions and coclause functions are not supported yet");
    }
    if (!((resultType == null || resultType instanceof Concrete.Expression) && (resultTypeLevel == null || resultTypeLevel instanceof Concrete.Expression) && body instanceof Concrete.FunctionBody)) {
      throw new IllegalArgumentException();
    }

    cRef.setKind(GlobalReferable.kindFromFunction(kind));
    Concrete.FunctionDefinition result = new Concrete.FunctionDefinition(kind, cRef, parameters(parameters), (Concrete.Expression) resultType, (Concrete.Expression) resultTypeLevel, (Concrete.FunctionBody) body);
    cRef.setDefinition(result);
    return result;
  }

  @Override
  public @NotNull ConcreteFunctionBody body(@NotNull ConcreteExpression term) {
    if (!(term instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.TermFunctionBody(myData, (Concrete.Expression) term);
  }

  @Override
  public @NotNull ConcreteFunctionBody body(@NotNull Collection<? extends ConcreteReferenceExpression> elim, @NotNull Collection<? extends ConcreteClause> clauses) {
    return new Concrete.ElimFunctionBody(myData, refExprs(elim), functionClauses(clauses));
  }

  private List<Concrete.ReferenceExpression> refExprs(@NotNull Collection<? extends ConcreteReferenceExpression> elim) {
    List<Concrete.ReferenceExpression> refs = new ArrayList<>(elim.size());
    for (ConcreteReferenceExpression ref : elim) {
      if (!(ref instanceof Concrete.ReferenceExpression)) {
        throw new IllegalArgumentException();
      }
      refs.add((Concrete.ReferenceExpression) ref);
    }
    return refs;
  }

  @Override
  public @NotNull ConcreteFunctionBody body(@NotNull Collection<? extends ConcreteClassElement> coclauses) {
    List<Concrete.CoClauseElement> elements = new ArrayList<>(coclauses.size());
    for (ConcreteClassElement coclause : coclauses) {
      if (!(coclause instanceof Concrete.CoClauseElement)) {
        throw new IllegalArgumentException();
      }
      elements.add((Concrete.CoClauseElement) coclause);
    }
    return new Concrete.CoelimFunctionBody(myData, elements);
  }

  @Override
  public @NotNull Concrete.DataDefinition data(@NotNull ArendRef ref, @NotNull Collection<? extends ConcreteParameter> parameters, boolean isTruncated, @Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel, @NotNull Collection<? extends ConcreteConstructorClause> clauses) {
    if (!(ref instanceof ConcreteLocatedReferable cRef)) {
      throw new IllegalArgumentException("The reference must be a global reference with a parent");
    }

    List<Concrete.ConstructorClause> constructorClauses = new ArrayList<>(clauses.size());
    for (ConcreteConstructorClause clause : clauses) {
      if (!(clause instanceof Concrete.ConstructorClause)) {
        throw new IllegalArgumentException();
      }
      constructorClauses.add((Concrete.ConstructorClause) clause);
    }

    cRef.setKind(GlobalReferable.Kind.DATA);
    Concrete.DataDefinition result = new Concrete.DataDefinition(cRef, null, null, typeParameters(parameters), null, isTruncated, pLevel == null && hLevel == null ? null : universe(pLevel, hLevel), constructorClauses);
    cRef.setDefinition(result);
    for (Concrete.ConstructorClause clause : constructorClauses) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        constructor.setDataType(result);
      }
    }
    return result;
  }

  @Override
  public @NotNull ConcreteConstructorClause clause(@Nullable Collection<? extends ConcretePattern> patterns, @NotNull Collection<? extends ConcreteConstructor> constructors) {
    List<Concrete.Constructor> cons = new ArrayList<>(constructors.size());
    for (ConcreteConstructor constructor : constructors) {
      if (!(constructor instanceof Concrete.Constructor)) {
        throw new IllegalArgumentException();
      }
      cons.add((Concrete.Constructor) constructor);
    }
    return new Concrete.ConstructorClause(myData, patterns(patterns), cons);
  }

  @Override
  public @NotNull ConcreteConstructor constructor(@NotNull ArendRef ref, @NotNull Collection<? extends ConcreteParameter> parameters, @NotNull Collection<? extends ConcreteReferenceExpression> elimRefs, @NotNull Collection<? extends ConcreteClause> clauses, boolean isCoerce) {
    if (!(ref instanceof ConcreteLocatedReferable cRef)) {
      throw new IllegalArgumentException("The reference must be a global reference with a parent");
    }

    cRef.setKind(GlobalReferable.Kind.CONSTRUCTOR);
    Concrete.Constructor result = new Concrete.Constructor(cRef, null, typeParameters(parameters), refExprs(elimRefs), functionClauses(clauses), isCoerce);
    cRef.setDefinition(result);
    return result;
  }

  @Override
  public @NotNull ConcreteDefinition classDef(@NotNull ArendRef ref, boolean isRecord, boolean withoutClassifying, @NotNull Collection<? extends ConcreteReferenceExpression> superClasses, @NotNull Collection<? extends ConcreteClassElement> elements) {
    if (!(ref instanceof ConcreteResolvedClassReferable cRef)) {
      throw new IllegalArgumentException("The reference must be a class reference");
    }

    List<Concrete.ClassElement> cElements = new ArrayList<>(elements.size());
    Concrete.ClassDefinition result = new Concrete.ClassDefinition(cRef, null, null, isRecord, withoutClassifying, refExprs(superClasses), cElements);

    for (ConcreteClassElement element : elements) {
      if (!(element instanceof Concrete.ClassElement)) {
        throw new IllegalArgumentException();
      }
      if (element instanceof Concrete.ClassField) {
        ((Concrete.ClassField) element).setParentClass(result);
      }
      cElements.add((Concrete.ClassElement) element);
    }

    cRef.setDefinition(result);
    return result;
  }

  @Override
  public @NotNull ConcreteClassElement field(@NotNull ArendRef ref, @NotNull ClassFieldKind kind, @NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, boolean isCoerce) {
    if (!(ref instanceof ConcreteClassFieldReferable cRef && resultType instanceof Concrete.Expression && (resultTypeLevel == null || resultTypeLevel instanceof Concrete.Expression))) {
      throw new IllegalArgumentException("The reference must be a global reference with a parent");
    }

    Concrete.ClassField result = new Concrete.ClassField(cRef, null, cRef.isExplicitField(), kind, typeParameters(parameters), (Concrete.Expression) resultType, (Concrete.Expression) resultTypeLevel, isCoerce);
    cRef.setDefinition(result);
    return result;
  }

  @Override
  public @NotNull ConcreteClassElement override(@NotNull ArendRef ref, @NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel) {
    if (!(ref instanceof Referable && resultType instanceof Concrete.Expression && (resultTypeLevel == null || resultTypeLevel instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.OverriddenField(myData, (Referable) ref, typeParameters(parameters), (Concrete.Expression) resultType, (Concrete.Expression) resultTypeLevel);
  }

  @Override
  public @NotNull ConcreteLevelParameters levelParameters(boolean isPLevels, @NotNull List<String> names, boolean isIncreasing) {
    List<LevelReferable> refs = new ArrayList<>(names.size());
    for (String name : names) {
      refs.add(new DataLevelReferable(myData, name, isPLevels));
    }
    return new Concrete.LevelParameters(myData, refs, isIncreasing);
  }

  @NotNull
  @Override
  public ArendRef local(@NotNull String name) {
    return new GeneratedLocalReferable(name);
  }

  @Override
  public @NotNull Referable localDeclaration(@NotNull ArendRef ref) {
    return makeLocalRef(ref);
  }

  @Override
  public @NotNull ArendRef global(@NotNull String name, @NotNull Precedence precedence) {
    return new GlobalReferableImpl(name, precedence);
  }

  @Override
  public @NotNull ArendRef global(@NotNull ArendRef parent, @NotNull String name, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrec) {
    if (!(parent instanceof LocatedReferable)) {
      throw new IllegalArgumentException();
    }
    return new ConcreteLocatedReferable(myData, name, precedence, alias, aliasPrec, (LocatedReferable) parent, GlobalReferable.Kind.OTHER);
  }

  @Override
  public @NotNull ArendRef classRef(@NotNull ArendRef parent, @NotNull String name, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrec) {
    if (!(parent instanceof LocatedReferable)) {
      throw new IllegalArgumentException();
    }
    return new ConcreteResolvedClassReferable(myData, name, precedence, alias, aliasPrec, (LocatedReferable) parent, new ArrayList<>());
  }

  @Override
  public @NotNull ArendRef fieldRef(@NotNull ArendRef parent, @NotNull String name, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrec, boolean isExplicit, boolean isParameter) {
    if (!(parent instanceof TCDefReferable)) {
      throw new IllegalArgumentException();
    }
    return new ConcreteClassFieldReferable(myData, name, precedence, alias, aliasPrec, !isParameter, isExplicit, isParameter, (TCDefReferable) parent);
  }

  private static Referable makeLocalRef(ArendRef ref) {
    if (!(ref == null || ref instanceof Referable) || ref instanceof LongUnresolvedReference) {
      throw new IllegalArgumentException();
    }
    return ref instanceof UnresolvedReference ? new DataLocalReferable(((UnresolvedReference) ref).getData(), ref.getRefName()) : (Referable) ref;
  }

  @NotNull
  @Override
  public Concrete.Parameter param(boolean explicit, @Nullable ArendRef ref) {
    return new Concrete.NameParameter(myData, explicit, makeLocalRef(ref));
  }

  @NotNull
  @Override
  public Concrete.TypeParameter param(boolean explicit, @NotNull Collection<? extends ArendRef> refs, @NotNull ConcreteExpression type) {
    return param(explicit, false, refs, type);
  }

  @Override
  public @NotNull Concrete.TypeParameter param(boolean explicit, @NotNull ConcreteExpression type) {
    return param(explicit, false, type);
  }

  @Override
  public @NotNull Concrete.TypeParameter param(boolean explicit, boolean isProperty, @NotNull ConcreteExpression type) {
    if (!(type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.TypeParameter(myData, explicit, (Concrete.Expression) type, isProperty);
  }

  @Override
  public @NotNull Concrete.TypeParameter param(boolean explicit, boolean isProperty, @NotNull Collection<? extends ArendRef> refs, @NotNull ConcreteExpression type) {
    if (!(type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    if (refs.isEmpty()) {
      return new Concrete.TypeParameter(myData, explicit, (Concrete.Expression) type, false);
    }
    List<Referable> cRefs = new ArrayList<>(refs.size());
    for (ArendRef ref : refs) {
      cRefs.add(makeLocalRef(ref));
    }
    return new Concrete.TelescopeParameter(myData, explicit, cRefs, (Concrete.Expression) type, false);
  }

  @NotNull
  @Override
  public ConcreteLetClause letClause(@NotNull ArendRef ref, @NotNull Collection<? extends ConcreteParameter> parameters, @Nullable ConcreteExpression type, @NotNull ConcreteExpression term) {
    if (!((type == null || type instanceof Concrete.Expression) && term instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClause(makeLocalRef(ref), parameters(parameters), (Concrete.Expression) type, (Concrete.Expression) term);
  }

  @NotNull
  @Override
  public ConcreteLetClause letClause(@NotNull ConcretePattern pattern, @Nullable ConcreteExpression type, @NotNull ConcreteExpression term) {
    if (!(pattern instanceof Concrete.Pattern && (type == null || type instanceof Concrete.Expression) && term instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClause((Concrete.Pattern) pattern, (Concrete.Expression) type, (Concrete.Expression) term);
  }

  @NotNull
  @Override
  public ConcreteClassElement implementation(@NotNull ArendRef field, @Nullable ConcreteExpression expression) {
    if (!(field instanceof Referable && (expression == null || expression instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ClassFieldImpl(myData, (Referable) field, (Concrete.Expression) expression, expression == null ? new Concrete.Coclauses(myData, Collections.emptyList()) : null);
  }

  @NotNull
  @Override
  public ConcreteClassElement implementation(@NotNull ArendRef field, @Nullable ConcreteExpression expression, @NotNull ArendRef classRef, @NotNull ConcreteClassElement... subclauses) {
    if (!(field instanceof Referable && (expression == null || expression instanceof Concrete.Expression) && classRef instanceof TCDefReferable)) {
      throw new IllegalArgumentException();
    }
    Concrete.ClassFieldImpl classFieldImpl = new Concrete.ClassFieldImpl(myData, (Referable) field, (Concrete.Expression) expression, new Concrete.Coclauses(myData, classFieldImpls(subclauses)));
    classFieldImpl.classRef = (TCDefReferable) classRef;
    return classFieldImpl;
  }

  @Override
  public @NotNull ConcreteClassElement implementation(@NotNull ArendRef field, @Nullable ConcreteExpression expression, @NotNull ArendRef classRef, @NotNull Collection<? extends ConcreteClassElement> subclauses) {
    if (!(field instanceof Referable && (expression == null || expression instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    Concrete.ClassFieldImpl classFieldImpl = new Concrete.ClassFieldImpl(myData, (Referable) field, (Concrete.Expression) expression, new Concrete.Coclauses(myData, classFieldImpls(subclauses)));
    classFieldImpl.classRef = (TCDefReferable) classRef;
    return classFieldImpl;
  }

  @NotNull
  @Override
  public ConcreteCaseArgument caseArg(@NotNull ConcreteExpression expression, @Nullable ArendRef asRef, @Nullable ConcreteExpression type) {
    if (!(expression instanceof Concrete.Expression && (type == null || type instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.CaseArgument((Concrete.Expression) expression, makeLocalRef(asRef), (Concrete.Expression) type);
  }

  @Override
  public @NotNull ConcreteCaseArgument caseArg(@NotNull ConcreteReferenceExpression expression, @Nullable ConcreteExpression type) {
    if (!(expression instanceof Concrete.ReferenceExpression && (type == null || type instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.CaseArgument((Concrete.ReferenceExpression) expression, (Concrete.Expression) type);
  }

  private List<Concrete.Pattern> patterns(Collection<? extends ConcretePattern> patterns) {
    if (patterns == null) return null;
    List<Concrete.Pattern> result = new ArrayList<>(patterns.size());
    for (ConcretePattern pattern : patterns) {
      if (!(pattern instanceof Concrete.Pattern)) {
        throw new IllegalArgumentException();
      }
      result.add((Concrete.Pattern) pattern);
    }
    return result;
  }

  @NotNull
  @Override
  public ConcreteClause clause(@NotNull Collection<? extends ConcretePattern> patterns, @Nullable ConcreteExpression expression) {
    if (!(expression == null || expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.FunctionClause(myData, patterns(patterns), (Concrete.Expression) expression);
  }

  @NotNull
  @Override
  public ConcretePattern refPattern(@Nullable ArendRef ref, @Nullable ConcreteExpression type) {
    if (!(type == null || type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.NamePattern(myData, true, makeLocalRef(ref), (Concrete.Expression) type);
  }

  @NotNull
  @Override
  public ConcretePattern tuplePattern(@NotNull ConcretePattern... subpatterns) {
    return new Concrete.TuplePattern(myData, true, patterns(Arrays.asList(subpatterns)), null);
  }

  @Override
  public @NotNull ConcretePattern tuplePattern(@NotNull Collection<? extends ConcretePattern> subpatterns) {
    return new Concrete.TuplePattern(myData, true, patterns(subpatterns), null);
  }

  @NotNull
  @Override
  public ConcretePattern numberPattern(int number) {
    if (number > Concrete.NumberPattern.MAX_VALUE) number = Concrete.NumberPattern.MAX_VALUE;
    if (number < -Concrete.NumberPattern.MAX_VALUE) number = -Concrete.NumberPattern.MAX_VALUE;
    return new Concrete.NumberPattern(myData, number, null);
  }

  @NotNull
  @Override
  public ConcretePattern conPattern(@NotNull ArendRef constructor, @NotNull ConcretePattern... subpatterns) {
    if (!(constructor instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ConstructorPattern(myData, true, myData, (Referable) constructor, patterns(Arrays.asList(subpatterns)), null);
  }

  @Override
  public @NotNull ConcretePattern conPattern(@NotNull ArendRef constructor, @NotNull Collection<? extends ConcretePattern> subpatterns) {
    if (!(constructor instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ConstructorPattern(myData, true, myData, (Referable) constructor, patterns(subpatterns), null);
  }

  @NotNull
  @Override
  public ConcreteLevel inf() {
    return new Concrete.InfLevelExpression(myData);
  }

  @NotNull
  @Override
  public ConcreteLevel lp() {
    return new Concrete.PLevelExpression(myData);
  }

  @NotNull
  @Override
  public ConcreteLevel lh() {
    return new Concrete.HLevelExpression(myData);
  }

  @NotNull
  @Override
  public ConcreteLevel numLevel(int level) {
    return new Concrete.NumberLevelExpression(myData, level);
  }

  @NotNull
  @Override
  public ConcreteLevel sucLevel(@NotNull ConcreteLevel level) {
    if (!(level instanceof Concrete.LevelExpression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.SucLevelExpression(myData, (Concrete.LevelExpression) level);
  }

  @NotNull
  @Override
  public ConcreteLevel maxLevel(@NotNull ConcreteLevel level1, @NotNull ConcreteLevel level2) {
    if (!(level1 instanceof Concrete.LevelExpression && level2 instanceof Concrete.LevelExpression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.MaxLevelExpression(myData, (Concrete.LevelExpression) level1, (Concrete.LevelExpression) level2);
  }

  @Override
  public @NotNull ConcreteLevel varLevel(@NotNull ArendRef ref) {
    if (!(ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.VarLevelExpression(myData, (Referable) ref);
  }

  @NotNull
  @Override
  public ConcreteFactory copy() {
    return new ConcreteFactoryImpl(myData);
  }

  @NotNull
  @Override
  public ConcreteFactory withData(@Nullable Object data) {
    Object actualData = data instanceof ConcreteSourceNode ? ((ConcreteSourceNode) data).getData() : data;
    return actualData == myData ? this : new ConcreteFactoryImpl(actualData);
  }
}
