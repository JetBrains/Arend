package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.getNumArguments;

public class LiftIndexVisitor extends BaseExpressionVisitor<Expression> {
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
  public DefCallExpression visitDefCall(DefCallExpression expr) {
    return expr;
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr) {
    if (expr.getParameters().isEmpty()) return expr;
    List<Expression> parameters = new ArrayList<>(expr.getParameters().size());
    for (Expression parameter : expr.getParameters()) {
      Expression expr2 = parameter.accept(this);
      if (expr2 == null) return null;
      parameters.add(expr2);
    }
    return ConCall(expr.getDefinition(), parameters);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr) {
    List<ClassCallExpression.OverrideElem> elems = new ArrayList<>(expr.getOverrideElems().size());
    for (ClassCallExpression.OverrideElem elem : expr.getOverrideElems()) {
      elems.add(new ClassCallExpression.OverrideElem(elem.field, elem.type == null ? null : elem.type.accept(this), elem.term == null ? null : elem.term.accept(this)));
    }
    return ClassCall(expr.getDefinition(), elems, expr.getUniverse());
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
    int liftShift = 0;
    for (int i = 0; i < clause.getPatterns().size(); i++) {
      if (elimExpr.getExpressions().get(i).getIndex() < myFrom) {
        liftShift += getNumArguments(clause.getPatterns().get(i)) - 1;
      }
    }
    return new Clause(clause.getPatterns(), clause.getArrow(),
            clause.getExpression().liftIndex(myFrom + liftShift, myOn), elimExpr);
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    List<Clause> clauses = new ArrayList<>();
    List<IndexExpression> expressions = new ArrayList<>(expr.getExpressions().size());
    ElimExpression result = Elim(expressions, clauses);
    for (IndexExpression var : expr.getExpressions())
      expressions.add((IndexExpression) var.liftIndex(myFrom, myOn));
    for (Clause clause : expr.getClauses())
      clauses.add(visitClause(clause, result));
    return result;
  }

  @Override
  public Expression visitProj(ProjExpression expr) {
    Expression expr1 = expr.getExpression().accept(this);
    return expr1 == null ? null : Proj(expr1, expr.getField());
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
