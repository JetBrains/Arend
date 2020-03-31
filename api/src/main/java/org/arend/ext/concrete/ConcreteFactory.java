package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteCaseArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.typechecking.CheckedExpression;
import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Collection;

public interface ConcreteFactory {
  @NotNull ConcreteExpression ref(@NotNull ArendRef ref);
  @NotNull ConcreteExpression ref(@NotNull ArendRef ref, @Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel);
  @NotNull ConcreteExpression core(String name, @NotNull CheckedExpression expr);
  @NotNull ConcreteExpression meta(String name, @NotNull MetaDefinition meta);
  @NotNull ConcreteExpression thisExpr();
  @NotNull ConcreteExpression lam(@NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression body);
  @NotNull ConcreteExpression pi(@NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression codomain);
  @NotNull ConcreteExpression universe(@Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel);
  @NotNull ConcreteExpression hole();
  @NotNull ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression);
  @NotNull ConcreteExpression tuple(@NotNull ConcreteExpression... expressions);
  @NotNull ConcreteExpression sigma(@NotNull ConcreteParameter... parameters);
  @NotNull ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull ConcreteClause... clauses);
  @NotNull ConcreteExpression eval(@NotNull ConcreteExpression expression);
  @NotNull ConcreteExpression peval(@NotNull ConcreteExpression expression);
  @NotNull ConcreteExpression proj(@NotNull ConcreteExpression expression, int field);
  @NotNull ConcreteExpression classExt(@NotNull ConcreteExpression expression, @NotNull ConcreteClassElement... elements);
  @NotNull ConcreteExpression newExpr(@NotNull ConcreteExpression expression);
  @NotNull ConcreteExpression letExpr(boolean isStrict, @NotNull Collection<? extends ConcreteLetClause> clauses, @NotNull ConcreteExpression expression);
  @NotNull ConcreteExpression number(@NotNull BigInteger number);
  @NotNull ConcreteExpression number(int number);
  @NotNull ConcreteExpression typed(@NotNull ConcreteExpression expression, @NotNull ConcreteExpression type);
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, @NotNull Collection<? extends ConcreteArgument> arguments);
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, boolean isExplicit, @NotNull Collection<? extends ConcreteExpression> arguments);
  @NotNull ConcreteAppBuilder appBuilder(@NotNull ConcreteExpression function);

  @NotNull ArendRef local(@NotNull String name);
  @NotNull ConcreteParameter param(@Nullable ArendRef ref);
  @NotNull ConcreteParameter param(@NotNull Collection<? extends ArendRef> refs, @NotNull ConcreteExpression type);

  @NotNull ConcreteLetClause letClause(@NotNull ArendRef ref, @NotNull Collection<? extends ConcreteParameter> parameters, @Nullable ConcreteExpression type, @NotNull ConcreteExpression term);
  @NotNull ConcreteLetClause letClause(@NotNull ConcreteSinglePattern pattern, @Nullable ConcreteExpression type, @NotNull ConcreteExpression term);
  @NotNull ConcreteSinglePattern singlePatternRef(@Nullable ArendRef ref, @Nullable ConcreteExpression type);
  @NotNull ConcreteSinglePattern singlePatternConstructor(@NotNull ConcreteSinglePattern... subpatterns);

  @NotNull ConcreteClassElement implementation(@NotNull ArendRef field, @Nullable ConcreteExpression expression, ConcreteClassElement... subclauses);

  @NotNull ConcreteCaseArgument caseArg(@NotNull ConcreteExpression expression, @Nullable ArendRef asRef, @Nullable ConcreteExpression type);
  @NotNull ConcreteClause clause(@NotNull Collection<? extends ConcretePattern> patterns, @Nullable ConcreteExpression expression);
  @NotNull ConcretePattern refPattern(@Nullable ArendRef ref, @Nullable ConcreteExpression type);
  @NotNull ConcretePattern tuplePattern(@NotNull ConcretePattern... subpatterns);
  @NotNull ConcretePattern numberPattern(int number);
  @NotNull ConcretePattern conPattern(@NotNull ArendRef constructor, @NotNull ConcretePattern... subpatterns);

  @NotNull ConcreteLevel inf();
  @NotNull ConcreteLevel lp();
  @NotNull ConcreteLevel lh();
  @NotNull ConcreteLevel numLevel(int level);
  @NotNull ConcreteLevel sucLevel(@NotNull ConcreteLevel level);
  @NotNull ConcreteLevel maxLevel(@NotNull ConcreteLevel level1, @NotNull ConcreteLevel level2);

  @NotNull ConcreteFactory copy();
  @NotNull ConcreteFactory withData(@Nullable Object data);
}
