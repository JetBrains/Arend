package org.arend.core.expr;

import org.arend.core.definition.ClassField;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.let.LetClausePattern;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LetExpression extends Expression {
  private final boolean myStrict;
  private final List<LetClause> myClauses;
  private final Expression myExpression;

  public LetExpression(boolean isStrict, List<LetClause> clauses, Expression expression) {
    myStrict = isStrict;
    myClauses = clauses;
    myExpression = expression;
  }

  public boolean isStrict() {
    return myStrict;
  }

  public List<LetClause> getClauses() {
    return myClauses;
  }

  private Expression normalizeClauseExpression(LetClausePattern pattern, Expression expression) {
    expression = expression.normalize(NormalizeVisitor.Mode.WHNF);
    if (!pattern.isMatching()) {
      return expression;
    }

    TupleExpression tuple = expression.checkedCast(TupleExpression.class);
    if (tuple != null) {
      if (tuple.getFields().size() != pattern.getPatterns().size()) {
        return expression;
      }

      List<Expression> fields = new ArrayList<>(tuple.getFields().size());
      for (int i = 0; i < pattern.getPatterns().size(); i++) {
        fields.add(normalizeClauseExpression(pattern.getPatterns().get(i), tuple.getFields().get(i)));
      }
      return new TupleExpression(fields, tuple.getSigmaType());
    }

    NewExpression newExpr = expression.checkedCast(NewExpression.class);
    if (newExpr != null && pattern.getFields() != null && pattern.getFields().size() == pattern.getPatterns().size()) {
      ClassCallExpression classCall = newExpr.getExpression();
      Map<ClassField, Expression> implementations = new HashMap<>();
      for (int i = 0; i < pattern.getPatterns().size(); i++) {
        ClassField classField = pattern.getFields().get(i);
        implementations.put(classField, normalizeClauseExpression(pattern.getPatterns().get(i), classCall.getImplementedHere().get(classField)));
      }
      for (Map.Entry<ClassField, Expression> entry : classCall.getImplementedHere().entrySet()) {
        implementations.putIfAbsent(entry.getKey(), entry.getValue());
      }
      return new NewExpression(new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), implementations, Sort.PROP, false));
    }

    return expression;
  }

  public ExprSubstitution getClausesSubstitution() {
    ExprSubstitution substitution = new ExprSubstitution();
    for (LetClause clause : myClauses) {
      substitution.add(clause, normalizeClauseExpression(clause.getPattern(), clause.getExpression().subst(substitution)));
    }
    return substitution;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }

  @Override
  public boolean isWHNF() {
    return false;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
