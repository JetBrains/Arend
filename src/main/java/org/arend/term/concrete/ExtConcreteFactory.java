package org.arend.term.concrete;

import org.arend.ext.concrete.*;
import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ExtConcreteFactory implements ConcreteFactory {
  @Nonnull
  @Override
  public ConcreteExpression ref(@Nonnull ArendRef ref) {
    if (!(ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(null, (Referable) ref);
  }

  @Nonnull
  @Override
  public ConcreteExpression ref(@Nonnull ArendRef ref, @Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel) {
    if (!(ref instanceof Referable && (pLevel == null || pLevel instanceof Concrete.LevelExpression) && (hLevel == null || hLevel instanceof Concrete.LevelExpression) )) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ReferenceExpression(null, (Referable) ref, (Concrete.LevelExpression) pLevel, (Concrete.LevelExpression) hLevel);
  }

  @Nonnull
  @Override
  public ConcreteExpression thisExpr() {
    return new Concrete.ThisExpression(null, null);
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

  @Nonnull
  @Override
  public ConcreteExpression lam(@Nonnull Collection<? extends ConcreteParameter> parameters, @Nonnull ConcreteExpression body) {
    if (!(body instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LamExpression(null, parameters(parameters), (Concrete.Expression) body);
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

  @Nonnull
  @Override
  public ConcreteExpression pi(@Nonnull Collection<? extends ConcreteParameter> parameters, @Nonnull ConcreteExpression codomain) {
    if (!(codomain instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.PiExpression(null, typeParameters(parameters), (Concrete.Expression) codomain);
  }

  @Nonnull
  @Override
  public ConcreteExpression universe(@Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel) {
    if (!((pLevel == null || pLevel instanceof Concrete.LevelExpression) && (hLevel == null || hLevel instanceof Concrete.LevelExpression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.UniverseExpression(null, (Concrete.LevelExpression) pLevel, (Concrete.LevelExpression) hLevel);
  }

  @Nonnull
  @Override
  public ConcreteExpression hole() {
    return new Concrete.HoleExpression(null);
  }

  @Nonnull
  @Override
  public ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.GoalExpression(null, name, (Concrete.Expression) expression);
  }

  @Nonnull
  @Override
  public ConcreteExpression tuple(@Nonnull ConcreteExpression... expressions) {
    List<Concrete.Expression> fields = new ArrayList<>(expressions.length);
    for (ConcreteExpression expression : expressions) {
      if (!(expression instanceof Concrete.Expression)) {
        throw new IllegalArgumentException();
      }
      fields.add((Concrete.Expression) expression);
    }
    return new Concrete.TupleExpression(null, fields);
  }

  @Nonnull
  @Override
  public ConcreteExpression sigma(@Nonnull ConcreteParameter... parameters) {
    return new Concrete.SigmaExpression(null, typeParameters(Arrays.asList(parameters)));
  }

  @Nonnull
  @Override
  public ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @Nonnull ConcreteClause... clauses) {
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
    return new Concrete.CaseExpression(null, isSCase, cArgs, (Concrete.Expression) resultType, (Concrete.Expression) resultTypeLevel, cClauses);
  }

  @Nonnull
  @Override
  public ConcreteExpression eval(@Nonnull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.EvalExpression(null, false, (Concrete.Expression) expression);
  }

  @Nonnull
  @Override
  public ConcreteExpression peval(@Nonnull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.EvalExpression(null, true, (Concrete.Expression) expression);
  }

  @Nonnull
  @Override
  public ConcreteExpression proj(@Nonnull ConcreteExpression expression, int field) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ProjExpression(null, (Concrete.Expression) expression, field);
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

  @Nonnull
  @Override
  public ConcreteExpression classExt(@Nonnull ConcreteExpression expression, @Nonnull ConcreteClassElement... elements) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return Concrete.ClassExtExpression.make(null, (Concrete.Expression) expression, classFieldImpls(elements));
  }

  @Nonnull
  @Override
  public ConcreteExpression newExpr(@Nonnull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.NewExpression(null, (Concrete.Expression) expression);
  }

  @Nonnull
  @Override
  public ConcreteExpression letExpr(boolean isStrict, @Nonnull Collection<? extends ConcreteLetClause> clauses, @Nonnull ConcreteExpression expression) {
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
    return new Concrete.LetExpression(null, isStrict, cClauses, (Concrete.Expression) expression);
  }

  @Nonnull
  @Override
  public ConcreteExpression number(@Nonnull BigInteger number) {
    return new Concrete.NumericLiteral(null, number);
  }

  @Nonnull
  @Override
  public ConcreteExpression number(int number) {
    return new Concrete.NumericLiteral(null, BigInteger.valueOf(number));
  }

  @Nonnull
  @Override
  public ConcreteExpression typed(@Nonnull ConcreteExpression expression, @Nonnull ConcreteExpression type) {
    if (!(expression instanceof Concrete.Expression && type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.TypedExpression(null, (Concrete.Expression) expression, (Concrete.Expression) type);
  }

  @Nonnull
  @Override
  public ArendRef local(@Nonnull String name) {
    return new LocalReferable(name);
  }

  @Nonnull
  @Override
  public ConcreteParameter param(@Nullable ArendRef ref) {
    if (!(ref == null || ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.NameParameter(null, true, (Referable) ref);
  }

  @Nonnull
  @Override
  public ConcreteParameter param(@Nonnull Collection<? extends ArendRef> refs, @Nonnull ConcreteExpression type) {
    if (!(type instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    if (refs.isEmpty()) {
      return new Concrete.TypeParameter(null, true, (Concrete.Expression) type);
    }
    List<Referable> cRefs = new ArrayList<>(refs.size());
    for (ArendRef ref : refs) {
      if (!(ref instanceof Referable)) {
        throw new IllegalArgumentException();
      }
      cRefs.add((Referable) ref);
    }
    return new Concrete.TelescopeParameter(null, true, cRefs, (Concrete.Expression) type);
  }

  @Nonnull
  @Override
  public ConcreteLetClause letClause(@Nonnull ArendRef ref, @Nonnull Collection<? extends ConcreteParameter> parameters, @Nullable ConcreteExpression type, @Nonnull ConcreteExpression term) {
    if (!(ref instanceof Referable && (type == null || type instanceof Concrete.Expression) && term instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClause((Referable) ref, parameters(parameters), (Concrete.Expression) type, (Concrete.Expression) term);
  }

  @Nonnull
  @Override
  public ConcreteLetClause letClause(@Nonnull ConcreteSinglePattern pattern, @Nullable ConcreteExpression type, @Nonnull ConcreteExpression term) {
    if (!(pattern instanceof Concrete.LetClausePattern && (type == null || type instanceof Concrete.Expression) && term instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClause((Concrete.LetClausePattern) pattern, (Concrete.Expression) type, (Concrete.Expression) term);
  }

  @Nonnull
  @Override
  public ConcreteSinglePattern singlePatternRef(@Nullable ArendRef ref, @Nullable ConcreteExpression type) {
    if (!((ref == null || ref instanceof Referable) && (type == null || type instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.LetClausePattern((Referable) ref, (Concrete.Expression) type);
  }

  @Nonnull
  @Override
  public ConcreteSinglePattern singlePatternConstructor(@Nonnull ConcreteSinglePattern... subpatterns) {
    List<Concrete.LetClausePattern> cPatterns = new ArrayList<>(subpatterns.length);
    for (ConcreteSinglePattern subpattern : subpatterns) {
      if (!(subpattern instanceof Concrete.LetClausePattern)) {
        throw new IllegalArgumentException();
      }
      cPatterns.add((Concrete.LetClausePattern) subpattern);
    }
    return new Concrete.LetClausePattern(null, cPatterns);
  }

  @Nonnull
  @Override
  public ConcreteClassElement implementation(@Nonnull ArendRef field, @Nullable ConcreteExpression expression, ConcreteClassElement... subclauses) {
    if (!(field instanceof Referable && (expression == null || expression instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ClassFieldImpl(null, (Referable) field, (Concrete.Expression) expression, classFieldImpls(subclauses));
  }

  @Nonnull
  @Override
  public ConcreteCaseArgument caseArg(@Nonnull ConcreteExpression expression, @Nullable ArendRef asRef, @Nullable ConcreteExpression type) {
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

  @Nonnull
  @Override
  public ConcreteClause clause(@Nonnull Collection<? extends ConcretePattern> patterns, @Nullable ConcreteExpression expression) {
    if (!(expression == null || expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.FunctionClause(null, patterns(patterns), (Concrete.Expression) expression);
  }

  @Nonnull
  @Override
  public ConcretePattern refPattern(@Nullable ArendRef ref, @Nullable ConcreteExpression type) {
    if (!((ref == null || ref instanceof Referable) && (type == null || type instanceof Concrete.Expression))) {
      throw new IllegalArgumentException();
    }
    return new Concrete.NamePattern(null, true, (Referable) ref, (Concrete.Expression) type);
  }

  @Nonnull
  @Override
  public ConcretePattern tuplePattern(@Nonnull ConcretePattern... subpatterns) {
    return new Concrete.TuplePattern(null, true, patterns(Arrays.asList(subpatterns)), new ArrayList<>(1));
  }

  @Nonnull
  @Override
  public ConcretePattern numberPattern(int number) {
    if (number > Concrete.NumberPattern.MAX_VALUE) number = Concrete.NumberPattern.MAX_VALUE;
    if (number < Concrete.NumberPattern.MAX_VALUE) number = -Concrete.NumberPattern.MAX_VALUE;
    return new Concrete.NumberPattern(null, number, new ArrayList<>(1));
  }

  @Nonnull
  @Override
  public ConcretePattern conPattern(@Nonnull ArendRef constructor, @Nonnull ConcretePattern... subpatterns) {
    if (!(constructor instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.ConstructorPattern(null, true, (Referable) constructor, patterns(Arrays.asList(subpatterns)), new ArrayList<>(1));
  }

  @Nonnull
  @Override
  public ConcreteLevel inf() {
    return new Concrete.InfLevelExpression(null);
  }

  @Nonnull
  @Override
  public ConcreteLevel lp() {
    return new Concrete.PLevelExpression(null);
  }

  @Nonnull
  @Override
  public ConcreteLevel lh() {
    return new Concrete.HLevelExpression(null);
  }

  @Nonnull
  @Override
  public ConcreteLevel numLevel(int level) {
    return new Concrete.NumberLevelExpression(null, level);
  }

  @Nonnull
  @Override
  public ConcreteLevel sucLevel(@Nonnull ConcreteLevel level) {
    if (!(level instanceof Concrete.LevelExpression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.SucLevelExpression(null, (Concrete.LevelExpression) level);
  }

  @Nonnull
  @Override
  public ConcreteLevel maxLevel(@Nonnull ConcreteLevel level1, @Nonnull ConcreteLevel level2) {
    if (!(level1 instanceof Concrete.LevelExpression && level2 instanceof Concrete.LevelExpression)) {
      throw new IllegalArgumentException();
    }
    return new Concrete.MaxLevelExpression(null, (Concrete.LevelExpression) level1, (Concrete.LevelExpression) level2);
  }
}
