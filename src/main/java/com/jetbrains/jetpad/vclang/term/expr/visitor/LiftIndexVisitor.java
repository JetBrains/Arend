package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.getNumArguments;

public class LiftIndexVisitor extends BaseExpressionVisitor<Integer, Expression> {
  private final int myOn;

  public LiftIndexVisitor(int on) {
    myOn = on;
  }

  @Override
  public Expression visitApp(AppExpression expr, Integer from) {
    Expression fun = expr.getFunction().accept(this, from);
    if (fun == null) return null;
    Expression arg = expr.getArgument().getExpression().accept(this, from);
    if (arg == null) return null;
    return Apps(fun, new ArgumentExpression(arg, expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public DefCallExpression visitDefCall(DefCallExpression expr, Integer from) {
    return expr;
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Integer from) {
    if (expr.getParameters().isEmpty()) return expr;
    List<Expression> parameters = new ArrayList<>(expr.getParameters().size());
    for (Expression parameter : expr.getParameters()) {
      Expression expr2 = parameter.accept(this, from);
      if (expr2 == null) return null;
      parameters.add(expr2);
    }
    return ConCall(expr.getDefinition(), parameters);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Integer from) {
    Map<ClassField, ClassCallExpression.ImplementStatement> statements = new HashMap<>();
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      statements.put(elem.getKey(), new ClassCallExpression.ImplementStatement(elem.getValue().type == null ? null : elem.getValue().type.accept(this, from), elem.getValue().term == null ? null : elem.getValue().term.accept(this, from)));
    }
    return ClassCall(expr.getDefinition(), statements);
  }

  @Override
  public Expression visitIndex(IndexExpression expr, Integer from) {
    if (expr.getIndex() < from) return expr;
    if (expr.getIndex() + myOn >= from) return Index(expr.getIndex() + myOn);
    return null;
  }

  @Override
  public Expression visitLam(LamExpression expr, Integer from) {
    List<Argument> arguments = new ArrayList<>(expr.getArguments().size());
    from = visitArguments(expr.getArguments(), arguments, from);
    if (from == null) return null;
    Expression body = expr.getBody().accept(this, from);
    return body == null ? null : Lam(arguments, body);
  }

  private Integer visitArguments(List<Argument> arguments, List<Argument> result, Integer from) {
    for (Argument argument : arguments) {
      if (argument instanceof NameArgument) {
        result.add(argument);
        ++from;
      } else
      if (argument instanceof TelescopeArgument) {
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        Expression arg = teleArgument.getType().accept(this, from);
        if (arg == null) return null;
        result.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), arg));
        from += teleArgument.getNames().size();
      } else {
        throw new IllegalStateException();
      }
    }
    return from;
  }

  private int visitTypeArguments(List<TypeArgument> arguments, List<TypeArgument> result, Integer from) {
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        Expression arg = teleArgument.getType().accept(this, from);
        if (arg == null) return -1;
        result.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), arg));
        from += teleArgument.getNames().size();
      } else {
        Expression arg = argument.getType().accept(this, from);
        if (arg == null) return -1;
        result.add(new TypeArgument(argument.getExplicit(), arg));
        ++from;
      }
    }
    return from;
  }

  @Override
  public Expression visitPi(PiExpression expr, Integer from) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    from = visitTypeArguments(expr.getArguments(), result, from);
    if (from < 0) return null;
    Expression codomain = expr.getCodomain().accept(this, from);
    return codomain == null ? null : Pi(result, codomain);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Integer from) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Integer from) {
    if (expr.getExpr() == null) return expr;
    Expression expr1 = expr.accept(this, from);
    return expr1 == null ? null : new ErrorExpression(expr1, expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr, Integer from) {
    return expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Integer from) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      Expression expr1 = field.accept(this, from);
      if (expr1 == null) return null;
      fields.add(expr1);
    }
    return Tuple(fields, (SigmaExpression) expr.getType().accept(this, from));
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Integer from) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    return visitTypeArguments(expr.getArguments(), result, from) < 0 ? null : Sigma(result);
  }

  private Clause visitClause(Clause clause, ElimExpression elimExpr, Integer from) {
    int liftShift = 0;
    for (int i = 0; i < clause.getPatterns().size(); i++) {
      if (elimExpr.getExpressions().get(i).getIndex() < from) {
        liftShift += getNumArguments(clause.getPatterns().get(i)) - 1;
      }
    }
    return new Clause(clause.getPatterns(), clause.getArrow(),
            clause.getExpression().accept(this, from + liftShift), elimExpr);
  }

  @Override
  public Expression visitElim(ElimExpression expr, Integer from) {
    List<Clause> clauses = new ArrayList<>();
    List<IndexExpression> expressions = new ArrayList<>(expr.getExpressions().size());
    ElimExpression result = Elim(expressions, clauses);
    for (IndexExpression var : expr.getExpressions())
      expressions.add((IndexExpression) var.accept(this, from));
    for (Clause clause : expr.getClauses())
      clauses.add(visitClause(clause, result, from));
    return result;
  }

  @Override
  public Expression visitProj(ProjExpression expr, Integer from) {
    Expression expr1 = expr.getExpression().accept(this, from);
    return expr1 == null ? null : Proj(expr1, expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Integer from) {
    return New(expr.getExpression().accept(this, from));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Integer from) {
    final List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      clauses.add(visitLetClause(clause, from));
      if (clauses.get(clauses.size() - 1) == null)
        return null;
      from++;
    }
    final Expression expr = letExpression.getExpression().accept(this, from);
    return expr == null ? null : Let(clauses, expr);
  }

  public LetClause visitLetClause(LetClause clause, Integer from) {
    final List<Argument> arguments = new ArrayList<>(clause.getArguments().size());
    from = visitArguments(clause.getArguments(), arguments, from);
    if (from == null) return null;
    final Expression resultType = clause.getResultType() == null ? null : clause.getResultType().accept(this, from);
    final Expression term = clause.getTerm().accept(this, from);
    return new LetClause(clause.getName(), arguments, resultType, clause.getArrow(), term);
  }
}
