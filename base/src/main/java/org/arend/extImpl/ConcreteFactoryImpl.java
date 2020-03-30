package org.arend.extImpl;

import org.arend.ext.concrete.*;
import org.arend.ext.concrete.expr.ConcreteCaseArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.CheckedExpression;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.naming.reference.CoreReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ConcreteFactoryImpl implements ConcreteFactory {
  private Object myData;

  public ConcreteFactoryImpl(Object data) {
    myData = data;
  }

  @NotNull
  @Override
  public ConcreteExpression ref(@NotNull ArendRef ref) {
    if (!(ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(myData, (Referable) ref);
  }

  @NotNull
  @Override
  public ConcreteExpression ref(@NotNull ArendRef ref, @Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel) {
    if (!(ref instanceof Referable && (pLevel == null || pLevel instanceof Concrete.LevelExpression) && (hLevel == null || hLevel instanceof Concrete.LevelExpression) )) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(myData, (Referable) ref, (Concrete.LevelExpression) pLevel, (Concrete.LevelExpression) hLevel);
  }

  @NotNull
  @Override
  public ConcreteExpression core(String name, @NotNull CheckedExpression expr) {
    return new Concrete.ReferenceExpression(myData, new CoreReferable(name, expr), null, null);
  }

  @NotNull
  @Override
  public ConcreteExpression meta(String name, @NotNull MetaDefinition meta) {
    return new Concrete.ReferenceExpression(myData, new MetaReferable(Precedence.DEFAULT, name, "", meta), null, null);
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
  public ConcreteExpression lam(@NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression body) {
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

  @NotNull
  @Override
  public ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.GoalExpression(myData, name, (Concrete.Expression) expression);
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

  @NotNull
  @Override
  public ConcreteExpression sigma(@NotNull ConcreteParameter... parameters) {
    return new Concrete.SigmaExpression(myData, typeParameters(Arrays.asList(parameters)));
  }

  @NotNull
  @Override
  public ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull ConcreteClause... clauses) {
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
    return new Concrete.CaseExpression(myData, isSCase, cArgs, (Concrete.Expression) resultType, (Concrete.Expression) resultTypeLevel, cClauses);
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

  @NotNull
  @Override
  public ConcreteExpression classExt(@NotNull ConcreteExpression expression, @NotNull ConcreteClassElement... elements) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return Concrete.ClassExtExpression.make(myData, (Concrete.Expression) expression, classFieldImpls(elements));
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

  @NotNull
  @Override
  public ArendRef local(@NotNull String name) {
    return new LocalReferable(name);
  }

  @NotNull
  @Override
  public ConcreteParameter param(@Nullable ArendRef ref) {
    if (!(ref == null || ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.NameParameter(myData, true, (Referable) ref);
  }

  @NotNull
  @Override
  public ConcreteParameter param(@NotNull Collection<? extends ArendRef> refs, @NotNull ConcreteExpression type) {
    if (!(type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    if (refs.isEmpty()) {
      return new Concrete.TypeParameter(myData, true, (Concrete.Expression) type);
    }
    List<Referable> cRefs = new ArrayList<>(refs.size());
    for (ArendRef ref : refs) {
      if (!(ref instanceof Referable)) {
        throw new IllegalArgumentException();
      }
      cRefs.add((Referable) ref);
    }
    return new Concrete.TelescopeParameter(myData, true, cRefs, (Concrete.Expression) type);
  }

  @NotNull
  @Override
  public ConcreteLetClause letClause(@NotNull ArendRef ref, @NotNull Collection<? extends ConcreteParameter> parameters, @Nullable ConcreteExpression type, @NotNull ConcreteExpression term) {
    if (!(ref instanceof Referable && (type == null || type instanceof Concrete.Expression) && term instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClause((Referable) ref, parameters(parameters), (Concrete.Expression) type, (Concrete.Expression) term);
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
    if (!((ref == null || ref instanceof Referable) && (type == null || type instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClausePattern((Referable) ref, (Concrete.Expression) type);
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

  @NotNull
  @Override
  public ConcreteClassElement implementation(@NotNull ArendRef field, @Nullable ConcreteExpression expression, ConcreteClassElement... subclauses) {
    if (!(field instanceof Referable && (expression == null || expression instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ClassFieldImpl(myData, (Referable) field, (Concrete.Expression) expression, classFieldImpls(subclauses));
  }

  @NotNull
  @Override
  public ConcreteCaseArgument caseArg(@NotNull ConcreteExpression expression, @Nullable ArendRef asRef, @Nullable ConcreteExpression type) {
    if (!(expression instanceof Concrete.Expression && (asRef == null || asRef instanceof Referable) && (type == null || type instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.CaseArgument((Concrete.Expression) expression, (Referable) asRef, (Concrete.Expression) type);
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
    if (!((ref == null || ref instanceof Referable) && (type == null || type instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.NamePattern(myData, true, (Referable) ref, (Concrete.Expression) type);
  }

  @NotNull
  @Override
  public ConcretePattern tuplePattern(@NotNull ConcretePattern... subpatterns) {
    return new Concrete.TuplePattern(myData, true, patterns(Arrays.asList(subpatterns)), new ArrayList<>(1));
  }

  @NotNull
  @Override
  public ConcretePattern numberPattern(int number) {
    if (number > Concrete.NumberPattern.MAX_VALUE) number = Concrete.NumberPattern.MAX_VALUE;
    if (number < Concrete.NumberPattern.MAX_VALUE) number = -Concrete.NumberPattern.MAX_VALUE;
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
    myData = data;
    return this;
  }
}
