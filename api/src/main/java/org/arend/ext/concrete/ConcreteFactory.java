package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.*;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.error.GeneralError;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.GoalSolver;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

/**
 * ConcreteFactory can be used to create concrete expressions, which can be checked by {@link org.arend.ext.typechecking.ExpressionTypechecker}
 */
public interface ConcreteFactory {
  @NotNull ConcreteReferenceExpression ref(@NotNull ArendRef ref);
  @NotNull ConcreteReferenceExpression ref(@NotNull ArendRef ref, @Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel);
  @NotNull ConcreteReferenceExpression ref(@NotNull CoreBinding ref);
  @NotNull ConcreteExpression core(@NotNull TypedExpression expr);
  @NotNull ConcreteExpression core(@Nullable String name, @NotNull TypedExpression expr);
  @NotNull ConcreteExpression meta(@NotNull String name, @NotNull MetaDefinition meta);
  @NotNull ConcreteExpression thisExpr();
  @NotNull ConcreteExpression lam(@NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression body);
  @NotNull ConcreteExpression pi(@NotNull Collection<? extends ConcreteParameter> parameters, @NotNull ConcreteExpression codomain);
  @NotNull ConcreteExpression arr(@NotNull ConcreteExpression domain, @NotNull ConcreteExpression codomain);
  @NotNull ConcreteExpression universe(@Nullable ConcreteLevel pLevel, @Nullable ConcreteLevel hLevel);
  @NotNull ConcreteExpression hole();
  @NotNull ConcreteExpression error(@Nullable GeneralError error);
  @NotNull ConcreteExpression goal();
  @NotNull ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression);
  @NotNull ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression, @Nullable GoalSolver goalSolver);
  @NotNull ConcreteExpression goal(@Nullable String name, @Nullable ConcreteExpression expression, @Nullable GoalSolver goalSolver, @NotNull List<GeneralError> errors);
  @NotNull ConcreteExpression tuple(@NotNull ConcreteExpression... expressions);
  @NotNull ConcreteExpression tuple(@NotNull Collection<? extends ConcreteExpression> expressions);
  @NotNull ConcreteExpression sigma(@NotNull ConcreteParameter... parameters);
  @NotNull ConcreteExpression sigma(@NotNull List<? extends ConcreteParameter> parameters);
  @NotNull ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull ConcreteClause... clauses);
  @NotNull ConcreteExpression caseExpr(boolean isSCase, Collection<? extends ConcreteCaseArgument> arguments, @Nullable ConcreteExpression resultType, @Nullable ConcreteExpression resultTypeLevel, @NotNull Collection<? extends ConcreteClause> clauses);
  @NotNull ConcreteExpression eval(@NotNull ConcreteExpression expression);
  @NotNull ConcreteExpression peval(@NotNull ConcreteExpression expression);
  @NotNull ConcreteExpression proj(@NotNull ConcreteExpression expression, int field);
  @NotNull ConcreteExpression classExt(@NotNull ConcreteExpression expression, @NotNull ConcreteClassElement... elements);
  @NotNull ConcreteExpression classExt(@NotNull ConcreteExpression expression, @NotNull Collection<? extends ConcreteClassElement> elements);
  @NotNull ConcreteExpression newExpr(@NotNull ConcreteExpression expression);
  @NotNull ConcreteExpression letExpr(boolean isHave, boolean isStrict, @NotNull Collection<? extends ConcreteLetClause> clauses, @NotNull ConcreteExpression expression);
  @NotNull ConcreteExpression number(@NotNull BigInteger number);
  @NotNull ConcreteExpression number(int number);
  @NotNull ConcreteExpression typed(@NotNull ConcreteExpression expression, @NotNull ConcreteExpression type);
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, @NotNull Collection<? extends ConcreteArgument> arguments);
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, boolean isExplicit, @NotNull Collection<? extends ConcreteExpression> arguments);
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, ConcreteArgument... arguments);
  @NotNull ConcreteExpression app(@NotNull ConcreteExpression function, boolean isExplicit, ConcreteExpression... arguments);
  @NotNull ConcreteArgument arg(@NotNull ConcreteExpression expression, boolean isExplicit);
  @NotNull ConcreteAppBuilder appBuilder(@NotNull ConcreteExpression function);

  @NotNull ArendRef local(@NotNull String name);
  @NotNull ArendRef localDeclaration(@NotNull ArendRef ref);
  @NotNull ArendRef global(@NotNull String name, @NotNull Precedence precedence);
  @NotNull ConcreteParameter param(boolean explicit, @Nullable ArendRef ref);
  @NotNull ConcreteParameter param(boolean explicit, @NotNull Collection<? extends ArendRef> refs, @NotNull ConcreteExpression type);
  @NotNull ConcreteParameter param(boolean explicit, @NotNull ConcreteExpression type);

  default @NotNull ConcreteParameter param(@Nullable ArendRef ref) {
    return param(true, ref);
  }

  default @NotNull ConcreteParameter param(@NotNull Collection<? extends ArendRef> refs, @NotNull ConcreteExpression type) {
    return param(true, refs, type);
  }

  @NotNull ConcreteLetClause letClause(@NotNull ArendRef ref, @NotNull Collection<? extends ConcreteParameter> parameters, @Nullable ConcreteExpression type, @NotNull ConcreteExpression term);
  @NotNull ConcreteLetClause letClause(@NotNull ConcreteSinglePattern pattern, @Nullable ConcreteExpression type, @NotNull ConcreteExpression term);
  @NotNull ConcreteSinglePattern singlePatternRef(@Nullable ArendRef ref, @Nullable ConcreteExpression type);
  @NotNull ConcreteSinglePattern singlePatternConstructor(@NotNull ConcreteSinglePattern... subpatterns);
  @NotNull ConcreteSinglePattern singlePatternConstructor(@NotNull Collection<? extends ConcreteSinglePattern> subpatterns);

  @NotNull ConcreteClassElement implementation(@NotNull ArendRef field, @Nullable ConcreteExpression expression);
  @NotNull ConcreteClassElement implementation(@NotNull ArendRef field, @Nullable ConcreteExpression expression, @NotNull ArendRef classRef, @NotNull ConcreteClassElement... subclauses);
  @NotNull ConcreteClassElement implementation(@NotNull ArendRef field, @Nullable ConcreteExpression expression, @NotNull ArendRef classRef, @NotNull Collection<? extends ConcreteClassElement> subclauses);

  @NotNull ConcreteCaseArgument caseArg(@NotNull ConcreteExpression expression, @Nullable ArendRef asRef, @Nullable ConcreteExpression type);
  @NotNull ConcreteCaseArgument caseArg(@NotNull ConcreteReferenceExpression expression, @Nullable ConcreteExpression type);
  @NotNull ConcreteClause clause(@NotNull Collection<? extends ConcretePattern> patterns, @Nullable ConcreteExpression expression);
  @NotNull ConcretePattern refPattern(@Nullable ArendRef ref, @Nullable ConcreteExpression type);
  @NotNull ConcretePattern tuplePattern(@NotNull ConcretePattern... subpatterns);
  @NotNull ConcretePattern tuplePattern(@NotNull Collection<? extends ConcretePattern> subpatterns);
  @NotNull ConcretePattern numberPattern(int number);
  @NotNull ConcretePattern conPattern(@NotNull ArendRef constructor, @NotNull ConcretePattern... subpatterns);
  @NotNull ConcretePattern conPattern(@NotNull ArendRef constructor, @NotNull Collection<? extends ConcretePattern> subpatterns);

  @NotNull ConcreteLevel inf();
  @NotNull ConcreteLevel lp();
  @NotNull ConcreteLevel lh();
  @NotNull ConcreteLevel numLevel(int level);
  @NotNull ConcreteLevel sucLevel(@NotNull ConcreteLevel level);
  @NotNull ConcreteLevel maxLevel(@NotNull ConcreteLevel level1, @NotNull ConcreteLevel level2);

  @NotNull ConcreteFactory copy();
  @NotNull ConcreteFactory withData(@Nullable Object data);
}
