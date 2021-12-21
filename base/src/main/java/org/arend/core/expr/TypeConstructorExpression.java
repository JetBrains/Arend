package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.Levels;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreTypeConstructorExpression;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TypeConstructorExpression extends Expression implements CoreTypeConstructorExpression {
  private final FunctionDefinition myDefinition;
  private Levels myLevels;
  private final int myClauseIndex;
  private final List<Expression> myClauseArguments;
  private Expression myArgument;

  private TypeConstructorExpression(FunctionDefinition definition, Levels levels, int clauseIndex, List<Expression> clauseArguments, Expression argument) {
    myDefinition = definition;
    myLevels = levels;
    myClauseIndex = clauseIndex;
    myClauseArguments = clauseArguments;
    myArgument = argument;
  }

  public static Expression make(FunctionDefinition definition, Levels levels, int clauseIndex, List<Expression> clauseArguments, Expression argument) {
    TypeDestructorExpression typeDestructor = argument == null ? null : argument.cast(TypeDestructorExpression.class);
    return typeDestructor != null && definition == typeDestructor.getDefinition() ? typeDestructor.getArgument() : new TypeConstructorExpression(definition, levels, clauseIndex, clauseArguments, argument);
  }

  public static Expression match(FunCallExpression funCall, Expression argument) {
    if (funCall.getDefinition().getKind() != CoreFunctionDefinition.Kind.TYPE) return null;
    if (funCall.getDefinition().getActualBody() instanceof Expression) {
      return make(funCall.getDefinition(), funCall.getLevels(), -1, new ArrayList<>(funCall.getDefCallArguments()), argument);
    }
    if (!(funCall.getDefinition().getActualBody() instanceof ElimBody)) return null;

    List<? extends ElimClause<Pattern>> clauses = ((ElimBody) funCall.getDefinition().getActualBody()).getClauses();
    for (int i = 0; i < clauses.size(); i++) {
      List<Expression> result = new ArrayList<>();
      List<ExpressionPattern> patterns = Pattern.toExpressionPatterns(clauses.get(i).getPatterns(), funCall.getDefinition().getParameters());
      if (patterns == null) continue;
      if (ExpressionPattern.match(patterns, funCall.getDefCallArguments(), result) == Decision.YES) {
        return make(funCall.getDefinition(), funCall.getLevels(), i, result, argument);
      }
    }

    return null;
  }

  @Override
  public @NotNull FunctionDefinition getDefinition() {
    return myDefinition;
  }

  @Override
  public @NotNull Levels getLevels() {
    return myLevels;
  }

  public void setLevels(Levels levels) {
    myLevels = levels;
  }

  @Override
  public int getClauseIndex() {
    return myClauseIndex;
  }

  @Override
  public @NotNull List<Expression> getClauseArguments() {
    return myClauseArguments;
  }

  @Override
  public @NotNull Expression getArgument() {
    return myArgument;
  }

  public void setArgument(Expression argument) {
    myArgument = argument;
  }

  public static Expression unfoldType(Expression type) {
    type = type.normalize(NormalizationMode.WHNF);
    while (type instanceof FunCallExpression && ((FunCallExpression) type).getDefinition().getKind() == CoreFunctionDefinition.Kind.TYPE) {
      Expression next = match((FunCallExpression) type, null);
      if (next == null) return type;
      type = ((TypeConstructorExpression) next).getArgumentType().normalize(NormalizationMode.WHNF);
    }
    return type;
  }

  public static Expression unfoldExpression(Expression expr) {
    expr = expr.normalize(NormalizationMode.WHNF);
    while (expr instanceof TypeConstructorExpression) {
      expr = ((TypeConstructorExpression) expr).getArgument().normalize(NormalizationMode.WHNF);
    }
    return expr;
  }

  public static TypecheckingResult unfoldResult(TypecheckingResult result) {
    Expression expr = result.expression;
    Expression type = result.type;
    Expression typeNorm = type.normalize(NormalizationMode.WHNF);
    while (typeNorm instanceof FunCallExpression && ((FunCallExpression) typeNorm).getDefinition().getKind() == CoreFunctionDefinition.Kind.TYPE) {
      FunCallExpression funCall = (FunCallExpression) typeNorm;
      Expression next = match(funCall, null);
      if (next == null) break;
      type = ((TypeConstructorExpression) next).getArgumentType();
      expr = TypeDestructorExpression.match(funCall, expr);
      typeNorm = type.normalize(NormalizationMode.WHNF);
    }
    return new TypecheckingResult(expr, type);
  }

  @Override
  public @NotNull Expression getArgumentType() {
    Body body = myDefinition.getActualBody();
    if (body instanceof Expression) {
      return ((Expression) body).subst(new ExprSubstitution().add(myDefinition.getParameters(), myClauseArguments), myLevels.makeSubstitution(myDefinition));
    } else {
      ElimClause<Pattern> clause = ((ElimBody) Objects.requireNonNull(body)).getClauses().get(myClauseIndex);
      return Objects.requireNonNull(clause.getExpression()).subst(new ExprSubstitution().add(Pattern.getFirstBinding(clause.getPatterns()), myClauseArguments), myLevels.makeSubstitution(myDefinition));
    }
  }

  @Override
  public FunCallExpression getType() {
    Body body = myDefinition.getActualBody();
    if (body instanceof Expression) {
      return FunCallExpression.makeFunCall(myDefinition, myLevels, myClauseArguments);
    } else {
      ElimClause<Pattern> clause = ((ElimBody) Objects.requireNonNull(body)).getClauses().get(myClauseIndex);
      List<ExpressionPattern> patterns = Pattern.toExpressionPatterns(clause.getPatterns(), myDefinition.getParameters());
      List<Expression> defCallArguments = ExpressionPattern.applyClauseArguments(patterns, myClauseArguments, myLevels.makeSubstitution(myDefinition));
      return FunCallExpression.makeFunCall(myDefinition, myLevels, defCallArguments);
    }
  }

  @Override
  public @NotNull FunCallExpression computeType() {
    return getType();
  }

  public void substSort(LevelSubstitution substitution) {
    myLevels = myLevels.subst(substitution);
  }

  @Override
  public @NotNull DependentLink getParameters() {
    return myDefinition.getActualBody() instanceof ElimBody ? Pattern.getFirstBinding(((ElimBody) myDefinition.getActualBody()).getClauses().get(myClauseIndex).getPatterns()) : myDefinition.getParameters();
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTypeConstructor(this, params);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTypeConstructor(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitTypeConstructor(this, param1, param2);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
