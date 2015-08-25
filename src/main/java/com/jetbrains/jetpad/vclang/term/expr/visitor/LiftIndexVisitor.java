package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.OverriddenDefinition;
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

public class LiftIndexVisitor implements ExpressionVisitor<Expression> {
  private final int myFrom;
  private final int myOn;

  public LiftIndexVisitor(int from, int on) {
    myFrom = from;
    myOn = on;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    Expression fun = expr.getFunction().accept(this);
    if (fun == null) return null;
    Expression arg = expr.getArgument().getExpression().accept(this);
    if (arg == null) return null;
    return Apps(fun, new ArgumentExpression(arg, expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    if (expr.getExpression() == null && expr.getParameters() == null) return expr;
    Expression expr1 = null;
    if (expr.getExpression() != null) {
      expr1 = expr.getExpression().accept(this);
      if (expr1 == null) return null;
    }
    List<Expression> parameters = expr.getParameters() == null ? null : new ArrayList<Expression>(expr.getParameters().size());
    if (expr.getParameters() != null) {
      for (Expression parameter : expr.getParameters()) {
        Expression expr2 = parameter.accept(this);
        if (expr2 == null) return null;
        parameters.add(expr2);
      }
    }
    return DefCall(expr1, expr.getDefinition(), parameters);
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    if (expr.getIndex() < myFrom) return expr;
    if (expr.getIndex() + myOn >= myFrom) return Index(expr.getIndex() + myOn);
    return null;
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    List<Argument> arguments = new ArrayList<>(expr.getArguments().size());
    Integer from = visitArguments(expr.getArguments(), arguments);
    if (from == null) return null;
    Expression body = expr.getBody().liftIndex(from, myOn);
    return body == null ? null : Lam(arguments, body);
  }

  private Integer visitArguments(List<Argument> arguments, List<Argument> result, int from) {
    for (Argument argument : arguments) {
      if (argument instanceof NameArgument) {
        result.add(argument);
        ++from;
      } else
      if (argument instanceof TelescopeArgument) {
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        Expression arg = teleArgument.getType().liftIndex(from, myOn);
        if (arg == null) return null;
        result.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), arg));
        from += teleArgument.getNames().size();
      } else {
        throw new IllegalStateException();
      }
    }
    return from;
  }

  private Integer visitArguments(List<Argument> arguments, List<Argument> result) {
    return visitArguments(arguments, result, myFrom);
  }

  private int visitTypeArguments(List<TypeArgument> arguments, List<TypeArgument> result, int from) {
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        Expression arg = teleArgument.getType().liftIndex(from, myOn);
        if (arg == null) return -1;
        result.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), arg));
        from += teleArgument.getNames().size();
      } else {
        Expression arg = argument.getType().liftIndex(from, myOn);
        if (arg == null) return -1;
        result.add(new TypeArgument(argument.getExplicit(), arg));
        ++from;
      }
    }
    return from;
  }

  private int visitTypeArguments(List<TypeArgument> arguments, List<TypeArgument> result) {
    return visitTypeArguments(arguments, result, myFrom);
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    int from = visitTypeArguments(expr.getArguments(), result);
    if (from < 0) return null;
    Expression codomain = expr.getCodomain().liftIndex(from, myOn);
    return codomain == null ? null : Pi(result, codomain);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr) {
    if (expr.getExpr() == null) return expr;
    Expression expr1 = expr.accept(this);
    return expr1 == null ? null : new ErrorExpression(expr1, expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr) {
    return expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      Expression expr1 = field.accept(this);
      if (expr1 == null) return null;
      fields.add(expr1);
    }
    return Tuple(fields, (SigmaExpression) expr.getType().accept(this));
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    return visitTypeArguments(expr.getArguments(), result) < 0 ? null : Sigma(result);
  }

  private Clause visitClause(Clause clause, ElimExpression elimExpr) {
    return new Clause(clause.getPattern(), clause.getArrow(),
            clause.getExpression().liftIndex(myFrom + getNumArguments(clause.getPattern()), myOn), elimExpr);
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    List<Clause> clauses = new ArrayList<>();
    ElimExpression result = Elim((IndexExpression) expr.getExpression().liftIndex(myFrom, myOn), clauses);
    for (Clause clause : expr.getClauses()) {
      clauses.add(visitClause(clause, result));
    }
    return result;
  }

  @Override
  public Expression visitProj(ProjExpression expr) {
    Expression expr1 = expr.getExpression().accept(this);
    return expr1 == null ? null : Proj(expr1, expr.getField());
  }

  @Override
  public Expression visitClassExt(ClassExtExpression expr) {
    Map<FunctionDefinition, OverriddenDefinition> definitions = new HashMap<>();
    for (Map.Entry<FunctionDefinition, OverriddenDefinition> entry : expr.getDefinitionsMap().entrySet()) {
      List<Argument> arguments = new ArrayList<>(entry.getValue().getArguments().size());
      Integer from = visitArguments(entry.getValue().getArguments(), arguments);
      if (from == null) return null;

      Expression resultType = entry.getValue().getResultType() == null ? null : entry.getValue().getResultType().liftIndex(from, myOn);
      Expression term = entry.getValue().getTerm() == null ? null : entry.getValue().getTerm().liftIndex(from, myOn);
      definitions.put(entry.getKey(), new OverriddenDefinition(entry.getValue().getNamespace(), entry.getValue().getPrecedence(), arguments, resultType, entry.getValue().getArrow(), term, entry.getKey()));
    }
    return ClassExt(expr.getBaseClass(), definitions, expr.getUniverse());
  }

  @Override
  public Expression visitNew(NewExpression expr) {
    return New(expr.getExpression().accept(this));
  }

  @Override
  public Expression visitLet(LetExpression letExpression) {
    final List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    int from = myFrom;
    for (LetClause clause : letExpression.getClauses()) {
      clauses.add(visitLetClause(clause, from));
      if (clauses.get(clauses.size() - 1)== null)
        return null;
      from++;
    }
    final Expression expr = letExpression.getExpression().liftIndex(from, myOn);
    return expr == null ? null : Let(clauses, expr);
  }

  public LetClause visitLetClause(LetClause clause, Integer from) {
    final List<Argument> arguments = new ArrayList<>(clause.getArguments().size());
    from = visitArguments(clause.getArguments(), arguments, from);
    if (from == null) return null;
    final Expression resultType = clause.getResultType() == null ? null : clause.getResultType().liftIndex(from, myOn);
    final Expression term = clause.getTerm().liftIndex(from, myOn);
    return new LetClause(clause.getName(), arguments, resultType, clause.getArrow(), term);
  }

  public LetClause visitLetClause(LetClause clause) {
    return visitLetClause(clause, myFrom);
  }
}
