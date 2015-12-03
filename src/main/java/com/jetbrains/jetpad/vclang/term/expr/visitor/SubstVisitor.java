package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class SubstVisitor extends BaseExpressionVisitor<Void, Expression> {
  private final List<Expression> mySubstExprs;
  private final int myFrom;

  public SubstVisitor(List<Expression> substExprs, int from) {
    mySubstExprs = substExprs;
    myFrom = from;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    return Apps(expr.getFunction().accept(this, null), new ArgumentExpression(expr.getArgument().getExpression().accept(this, null), expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public DefCallExpression visitDefCall(DefCallExpression expr, Void params) {
    return expr;
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Void params) {
    if (expr.getParameters().isEmpty()) return expr;
    List<Expression> parameters = new ArrayList<>(expr.getParameters().size());
    for (Expression parameter : expr.getParameters()) {
      Expression expr2 = parameter.accept(this, null);
      if (expr2 == null) return null;
      parameters.add(expr2);
    }
    return ConCall(expr.getDefinition(), parameters);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    Map<ClassField, ClassCallExpression.ImplementStatement> statements = new HashMap<>();
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      statements.put(elem.getKey(), new ClassCallExpression.ImplementStatement(elem.getValue().type == null ? null : elem.getValue().type.accept(this, null), elem.getValue().term == null ? null : elem.getValue().term.accept(this, null)));
    }
    return ClassCall(expr.getDefinition(), statements);
  }

  @Override
  public Expression visitIndex(IndexExpression expr, Void params) {
    if (expr.getIndex() < myFrom) return Index(expr.getIndex());
    if (expr.getIndex() >= mySubstExprs.size() + myFrom) return Index(expr.getIndex() - mySubstExprs.size());
    return mySubstExprs.get(expr.getIndex() - myFrom);
  }

  @Override
  public Expression visitLam(LamExpression expr, Void params) {
    List<Argument> arguments = new ArrayList<>(expr.getArguments().size());
    Expression[] result = visitLamArguments(expr.getArguments(), arguments, expr.getBody());
    return Lam(arguments, result[0]);
  }

  private Expression[] visitLamArguments(List<Argument> inputArgs, List<Argument> outputArgs, Expression... exprs) {
    SubstVisitorContext ctx = new SubstVisitorContext(mySubstExprs, myFrom);
    outputArgs.addAll(visitArguments(inputArgs, ctx));

    Expression[] result = new Expression[exprs.length];
    for (int i = 0; i < exprs.length; ++i) {
      result[i] = exprs[i] == null ? null : ctx.subst(exprs[i]);
    }
    return result;
  }

  public static class SubstVisitorContext {
    private int myFrom;
    private final List<Expression> mySubstExprs;

    public SubstVisitorContext(List<? extends Expression> substExprs, int from) {
      this.myFrom = from;
      this.mySubstExprs = new ArrayList<>(substExprs);
    }

    private void lift(int on) {
      for (int i = 0; i < mySubstExprs.size(); ++i) {
        mySubstExprs.set(i, mySubstExprs.get(i).liftIndex(0, on));
      }
      myFrom += on;
    }

    Expression subst(Expression expr) {
      return expr.subst(mySubstExprs, myFrom);
    }

  }

  static TypeArgument visitTypeArgument(TypeArgument argument, SubstVisitorContext ctx) {
    TypeArgument result;
    if (argument instanceof TelescopeArgument) {
      List<String> names = ((TelescopeArgument) argument).getNames();
      result = new TelescopeArgument(argument.getExplicit(), names, ctx.subst(argument.getType()));
      ctx.lift(names.size());
    } else {
      result = new TypeArgument(argument.getExplicit(), ctx.subst(argument.getType()));
      ctx.lift(1);
    }
    return result;
  }

  static Argument visitArgument(Argument argument, SubstVisitorContext ctx) {
    Argument result;
    if (argument instanceof NameArgument) {
      result = argument;
      ctx.lift(1);
    } else if (argument instanceof TypeArgument) {
      result = visitTypeArgument((TypeArgument) argument, ctx);
    } else {
      throw new IllegalStateException();
    }
    return result;
  }

  static List<Argument> visitArguments(List<Argument> arguments, SubstVisitorContext ctx) {
    List<Argument> result = new ArrayList<>(arguments.size());
    for (Argument arg : arguments) {
      result.add(visitArgument(arg, ctx));
    }
    return result;
  }

  static List<TypeArgument> visitTypeArguments(List<TypeArgument> arguments, SubstVisitorContext ctx) {
    List<TypeArgument> result = new ArrayList<>(arguments.size());
    for (TypeArgument arg : arguments) {
      result.add(visitTypeArgument(arg, ctx));
    }
    return result;
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    SubstVisitorContext ctx = new SubstVisitorContext(mySubstExprs, myFrom);
    return Pi(visitTypeArguments(expr.getArguments(), ctx), ctx.subst(expr.getCodomain()));
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, null), expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return Tuple(fields, (SigmaExpression) expr.getType().accept(this, null));
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    return Sigma(visitTypeArguments(expr.getArguments(), new SubstVisitorContext(mySubstExprs, myFrom)));
  }

  @Override
  public Expression visitElim(ElimExpression expr, Void params) {
    List<IndexExpression> expressions = new ArrayList<>();
    for (IndexExpression index : expr.getExpressions()) {
      assert index.getIndex() < myFrom;
      expressions.add((IndexExpression) index.accept(this, null));
    }
    List<Clause> clauses = new ArrayList<>();
    ElimExpression result = new ElimExpression(expressions, clauses);
    for (Clause clause : expr.getClauses()) {
      clauses.add(new Clause(clause.getPatterns(), clause.getArrow(), clause.getExpression().subst(mySubstExprs,
          myFrom + Utils.getNumArguments(clause.getPatterns()) - clause.getPatterns().size()), result));
    }

    return result;
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return Proj(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return New(expr.getExpression().accept(this, null));
  }

  @Override
  public LetExpression visitLet(LetExpression letExpression, Void params) {
    final List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    final SubstVisitorContext ctx = new SubstVisitorContext(mySubstExprs, myFrom);

    for (LetClause clause : letExpression.getClauses()) {
      clauses.add(visitLetClause(clause, ctx.mySubstExprs, ctx.myFrom));
      ctx.lift(1);
    }

    final Expression expr = letExpression.getExpression().subst(ctx.mySubstExprs, ctx.myFrom);
    return Let(clauses, expr);
  }

  public static LetClause visitLetClause(LetClause clause, List<Expression> substExprs, int from) {
    final SubstVisitorContext localCtx = new SubstVisitorContext(substExprs, from);
    final List<TypeArgument> arguments = visitTypeArguments(clause.getArguments(), localCtx);
    final Expression resultType = clause.getResultType() == null ? null : localCtx.subst(clause.getResultType());
    final Expression expression = localCtx.subst(clause.getTerm());
    return new LetClause(clause.getName(), arguments, resultType, clause.getArrow(), expression);
  }
}
