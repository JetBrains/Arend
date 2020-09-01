package org.arend.extImpl;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.ReferenceExpression;
import org.arend.ext.concrete.*;
import org.arend.ext.concrete.expr.*;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.GoalSolver;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.naming.reference.*;
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

  @NotNull
  @Override
  public ConcreteReferenceExpression ref(@NotNull ArendRef ref, @Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel) {
    if (!(ref instanceof Referable && (pLevel == null || pLevel instanceof Concrete.LevelExpression) && (hLevel == null || hLevel instanceof Concrete.LevelExpression) )) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(myData, (Referable) ref, (Concrete.LevelExpression) pLevel, (Concrete.LevelExpression) hLevel);
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
    return new Concrete.ReferenceExpression(myData, new CoreReferable(name, TypecheckingResult.fromChecked(Objects.requireNonNull(expr))), null, null);
  }

  @NotNull
  @Override
  public ConcreteExpression meta(@NotNull String name, @NotNull MetaDefinition meta) {
    return new Concrete.ReferenceExpression(myData, new MetaReferable(Precedence.DEFAULT, name, null, "", meta, null, null), null, null);
  }

  @NotNull
  @Override
  public ConcreteExpression thisExpr() {
    return new Concrete.ThisExpression(myData, null);
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
  public ConcreteLamExpression lam(@NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression body) {
    if (!(body instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LamExpression(myData, parameters(parameters), (Concrete.Expression) body);
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
    return new Concrete.PiExpression(myData, typeParameters(parameters), (Concrete.Expression) codomain);
  }

  @NotNull
  @Override
  public ConcreteExpression universe(@Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel) {
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
    return new Concrete.SigmaExpression(myData, typeParameters(Arrays.asList(parameters)));
  }

  @Override
  public @NotNull ConcreteExpression sigma(@NotNull Collection<? extends ConcreteParameter> parameters) {
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
    List<Concrete.FunctionClause> cClauses = new ArrayList<>(clauses.length);
    for (ConcreteClause clause : clauses) {
      if (!(clause instanceof Concrete.Clause)) {
        throw new IllegalArgumentException();
      }
      if (!(clause instanceof Concrete.FunctionClause)) {
        throw new IllegalArgumentException("Expected a function clause");
      }
      cClauses.add((Concrete.FunctionClause) clause);
    }
    return caseExprC(isSCase, arguments, resultType, resultTypeLevel, cClauses);
  }

  @Override
  public @NotNull ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull Collection<? extends ConcreteClause> clauses) {
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
    return caseExprC(isSCase, arguments, resultType, resultTypeLevel, cClauses);
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
  public ConcreteExpression letExpr(boolean isStrict, @NotNull Collection<? extends ConcreteLetClause> clauses, @NotNull ConcreteExpression expression) {
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
    return new Concrete.LetExpression(myData, isStrict, cClauses, (Concrete.Expression) expression);
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

  @NotNull
  @Override
  public ConcreteExpression typed(@NotNull ConcreteExpression expression, @NotNull ConcreteExpression type) {
    if (!(expression instanceof Concrete.Expression && type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.TypedExpression(myData, (Concrete.Expression) expression, (Concrete.Expression) type);
  }

  @Override
  public @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, @NotNull Collection<? extends ConcreteArgument> arguments) {
    if (!(function instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    Concrete.Expression fun = (Concrete.Expression) function;
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
    if (!(function instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    Concrete.Expression fun = (Concrete.Expression) function;
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

  @NotNull
  @Override
  public ArendRef local(@NotNull String name) {
    return new LocalReferable(name);
  }

  private static Referable makeLocalRef(ArendRef ref) {
    if (!(ref == null || ref instanceof Referable) || ref instanceof LongUnresolvedReference) {
      throw new IllegalArgumentException();
    }
    return ref instanceof UnresolvedReference ? new DataLocalReferable(((UnresolvedReference) ref).getData(), ref.getRefName()) : (Referable) ref;
  }

  @NotNull
  @Override
  public ConcreteParameter param(boolean explicit, @Nullable ArendRef ref) {
    return new Concrete.NameParameter(myData, explicit, makeLocalRef(ref));
  }

  @NotNull
  @Override
  public ConcreteParameter param(boolean explicit, @NotNull Collection<? extends ArendRef> refs, @NotNull ConcreteExpression type) {
    if (!(type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    if (refs.isEmpty()) {
      return new Concrete.TypeParameter(myData, explicit, (Concrete.Expression) type);
    }
    List<Referable> cRefs = new ArrayList<>(refs.size());
    for (ArendRef ref : refs) {
      cRefs.add(makeLocalRef(ref));
    }
    return new Concrete.TelescopeParameter(myData, explicit, cRefs, (Concrete.Expression) type);
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
  public ConcreteLetClause letClause(@NotNull ConcreteSinglePattern pattern, @Nullable ConcreteExpression type, @NotNull ConcreteExpression term) {
    if (!(pattern instanceof Concrete.LetClausePattern && (type == null || type instanceof Concrete.Expression) && term instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClause((Concrete.LetClausePattern) pattern, (Concrete.Expression) type, (Concrete.Expression) term);
  }

  @NotNull
  @Override
  public ConcreteSinglePattern singlePatternRef(@Nullable ArendRef ref, @Nullable ConcreteExpression type) {
    if (!(type == null || type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClausePattern(makeLocalRef(ref), (Concrete.Expression) type);
  }

  @NotNull
  @Override
  public ConcreteSinglePattern singlePatternConstructor(@NotNull ConcreteSinglePattern... subpatterns) {
    List<Concrete.LetClausePattern> cPatterns = new ArrayList<>(subpatterns.length);
    for (ConcreteSinglePattern subpattern : subpatterns) {
      if (!(subpattern instanceof Concrete.LetClausePattern)) {
        throw new IllegalArgumentException();
      }
      cPatterns.add((Concrete.LetClausePattern) subpattern);
    }
    return new Concrete.LetClausePattern(myData, cPatterns);
  }

  @Override
  public @NotNull ConcreteSinglePattern singlePatternConstructor(@NotNull Collection<? extends ConcreteSinglePattern> subpatterns) {
    List<Concrete.LetClausePattern> cPatterns = new ArrayList<>(subpatterns.size());
    for (ConcreteSinglePattern subpattern : subpatterns) {
      if (!(subpattern instanceof Concrete.LetClausePattern)) {
        throw new IllegalArgumentException();
      }
      cPatterns.add((Concrete.LetClausePattern) subpattern);
    }
    return new Concrete.LetClausePattern(myData, cPatterns);
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
    return new Concrete.TuplePattern(myData, true, patterns(Arrays.asList(subpatterns)), new ArrayList<>(1));
  }

  @Override
  public @NotNull ConcretePattern tuplePattern(@NotNull Collection<? extends ConcretePattern> subpatterns) {
    return new Concrete.TuplePattern(myData, true, patterns(subpatterns), new ArrayList<>(1));
  }

  @NotNull
  @Override
  public ConcretePattern numberPattern(int number) {
    if (number > Concrete.NumberPattern.MAX_VALUE) number = Concrete.NumberPattern.MAX_VALUE;
    if (number < -Concrete.NumberPattern.MAX_VALUE) number = -Concrete.NumberPattern.MAX_VALUE;
    return new Concrete.NumberPattern(myData, number, new ArrayList<>(1));
  }

  @NotNull
  @Override
  public ConcretePattern conPattern(@NotNull ArendRef constructor, @NotNull ConcretePattern... subpatterns) {
    if (!(constructor instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ConstructorPattern(myData, true, (Referable) constructor, patterns(Arrays.asList(subpatterns)), new ArrayList<>(1));
  }

  @Override
  public @NotNull ConcretePattern conPattern(@NotNull ArendRef constructor, @NotNull Collection<? extends ConcretePattern> subpatterns) {
    if (!(constructor instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ConstructorPattern(myData, true, (Referable) constructor, patterns(subpatterns), new ArrayList<>(1));
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

  @NotNull
  @Override
  public ConcreteFactory copy() {
    return new ConcreteFactoryImpl(myData);
  }

  @NotNull
  @Override
  public ConcreteFactory withData(@Nullable Object data) {
    return new ConcreteFactoryImpl(data);
  }
}
