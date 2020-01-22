package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteCaseArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.typechecking.CheckedExpression;
import org.arend.ext.typechecking.MetaDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;

public interface ConcreteFactory {
  @Nonnull ConcreteExpression ref(@Nonnull ArendRef ref);
  @Nonnull ConcreteExpression ref(@Nonnull ArendRef ref, @Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel);
  @Nonnull ConcreteExpression core(String name, @Nonnull CheckedExpression expr);
  @Nonnull ConcreteExpression meta(String name, @Nonnull MetaDefinition meta);
  @Nonnull ConcreteExpression thisExpr();
  @Nonnull ConcreteExpression lam(@Nonnull Collection<? extends ConcreteParameter> parameters, @Nonnull ConcreteExpression body);
  @Nonnull ConcreteExpression pi(@Nonnull Collection<? extends ConcreteParameter> parameters, @Nonnull ConcreteExpression codomain);
  @Nonnull ConcreteExpression universe(@Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel);
  @Nonnull ConcreteExpression hole();
  @Nonnull ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression);
  @Nonnull ConcreteExpression tuple(@Nonnull ConcreteExpression... expressions);
  @Nonnull ConcreteExpression sigma(@Nonnull ConcreteParameter... parameters);
  @Nonnull ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @Nonnull ConcreteClause... clauses);
  @Nonnull ConcreteExpression eval(@Nonnull ConcreteExpression expression);
  @Nonnull ConcreteExpression peval(@Nonnull ConcreteExpression expression);
  @Nonnull ConcreteExpression proj(@Nonnull ConcreteExpression expression, int field);
  @Nonnull ConcreteExpression classExt(@Nonnull ConcreteExpression expression, @Nonnull ConcreteClassElement... elements);
  @Nonnull ConcreteExpression newExpr(@Nonnull ConcreteExpression expression);
  @Nonnull ConcreteExpression letExpr(boolean isStrict, @Nonnull Collection<? extends ConcreteLetClause> clauses, @Nonnull ConcreteExpression expression);
  @Nonnull ConcreteExpression number(@Nonnull BigInteger number);
  @Nonnull ConcreteExpression number(int number);
  @Nonnull ConcreteExpression typed(@Nonnull ConcreteExpression expression, @Nonnull ConcreteExpression type);

  @Nonnull ArendRef local(@Nonnull String name);
  @Nonnull ConcreteParameter param(@Nullable ArendRef ref);
  @Nonnull ConcreteParameter param(@Nonnull Collection<? extends ArendRef> refs, @Nonnull ConcreteExpression type);

  @Nonnull ConcreteLetClause letClause(@Nonnull ArendRef ref, @Nonnull Collection<? extends ConcreteParameter> parameters, @Nullable ConcreteExpression type, @Nonnull ConcreteExpression term);
  @Nonnull ConcreteLetClause letClause(@Nonnull ConcreteSinglePattern pattern, @Nullable ConcreteExpression type, @Nonnull ConcreteExpression term);
  @Nonnull ConcreteSinglePattern singlePatternRef(@Nullable ArendRef ref, @Nullable ConcreteExpression type);
  @Nonnull ConcreteSinglePattern singlePatternConstructor(@Nonnull ConcreteSinglePattern... subpatterns);

  @Nonnull ConcreteClassElement implementation(@Nonnull ArendRef field, @Nullable ConcreteExpression expression, ConcreteClassElement... subclauses);

  @Nonnull ConcreteCaseArgument caseArg(@Nonnull ConcreteExpression expression, @Nullable ArendRef asRef, @Nullable ConcreteExpression type);
  @Nonnull ConcreteClause clause(@Nonnull Collection<? extends ConcretePattern> patterns, @Nullable ConcreteExpression expression);
  @Nonnull ConcretePattern refPattern(@Nullable ArendRef ref, @Nullable ConcreteExpression type);
  @Nonnull ConcretePattern tuplePattern(@Nonnull ConcretePattern... subpatterns);
  @Nonnull ConcretePattern numberPattern(int number);
  @Nonnull ConcretePattern conPattern(@Nonnull ArendRef constructor, @Nonnull ConcretePattern... subpatterns);

  @Nonnull ConcreteLevel inf();
  @Nonnull ConcreteLevel lp();
  @Nonnull ConcreteLevel lh();
  @Nonnull ConcreteLevel numLevel(int level);
  @Nonnull ConcreteLevel sucLevel(@Nonnull ConcreteLevel level);
  @Nonnull ConcreteLevel maxLevel(@Nonnull ConcreteLevel level1, @Nonnull ConcreteLevel level2);

  @Nonnull ConcreteFactory copy();
  @Nonnull ConcreteFactory withData(@Nullable Object data);
}
