package org.arend.core.expr;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.pattern.Pattern;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreTypeDestructorExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TypeDestructorExpression extends Expression implements CoreTypeDestructorExpression {
  private final FunctionDefinition myDefinition;
  private final Expression myArgument;

  public TypeDestructorExpression(FunctionDefinition definition, Expression argument) {
    myDefinition = definition;
    myArgument = argument;
  }

  public static Expression make(FunctionDefinition definition, Expression argument) {
    TypeConstructorExpression typeConstructor = argument == null ? null : argument.cast(TypeConstructorExpression.class);
    return typeConstructor != null && definition == typeConstructor.getDefinition() ? typeConstructor.getArgument() : new TypeDestructorExpression(definition, argument);
  }

  public static Expression match(FunCallExpression funCall, Expression argument) {
    if (funCall.getDefinition().getKind() != CoreFunctionDefinition.Kind.TYPE) return null;
    if (funCall.getDefinition().getActualBody() instanceof Expression) {
      return make(funCall.getDefinition(), argument);
    }
    if (!(funCall.getDefinition().getActualBody() instanceof ElimBody)) return null;

    List<? extends ElimClause<Pattern>> clauses = ((ElimBody) funCall.getDefinition().getActualBody()).getClauses();
    for (ElimClause<Pattern> clause : clauses) {
      List<Expression> result = new ArrayList<>();
      List<ExpressionPattern> patterns = Pattern.toExpressionPatterns(clause.getPatterns(), funCall.getDefinition().getParameters());
      if (patterns == null) continue;
      if (ExpressionPattern.match(patterns, funCall.getDefCallArguments(), result) == Decision.YES) {
        return make(funCall.getDefinition(), argument);
      }
    }

    return null;
  }

  @Override
  public @NotNull FunctionDefinition getDefinition() {
    return myDefinition;
  }

  @Override
  public @NotNull Expression getArgument() {
    return myArgument;
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTypeDestructor(this, params);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTypeDestructor(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitTypeDestructor(this, param1, param2);
  }

  @Override
  public Decision isWHNF() {
    return myArgument.isInstance(TypeConstructorExpression.class) ? Decision.NO : myArgument.isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return myArgument.isInstance(TypeConstructorExpression.class) ? null : myArgument.getStuckExpression();
  }
}
