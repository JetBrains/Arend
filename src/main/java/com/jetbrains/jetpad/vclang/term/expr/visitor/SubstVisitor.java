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

public class SubstVisitor implements ExpressionVisitor<Expression> {
  private final List<Expression> mySubstExprs;
  private final int myFrom;

  public SubstVisitor(List<Expression> substExprs, int from) {
    mySubstExprs = substExprs;
    myFrom = from;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    return Apps(expr.getFunction().accept(this), new ArgumentExpression(expr.getArgument().getExpression().accept(this), expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public DefCallExpression visitDefCall(DefCallExpression expr) {
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
    if (expr.getIndex() < myFrom) return Index(expr.getIndex());
    if (expr.getIndex() >= mySubstExprs.size() + myFrom) return Index(expr.getIndex() - mySubstExprs.size());
    return mySubstExprs.get(expr.getIndex() - myFrom);
  }

  @Override
  public Expression visitLam(LamExpression expr) {
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

  List<TypeArgument> visitTypeArguments(List<TypeArgument> arguments, SubstVisitorContext ctx) {
    List<TypeArgument> result = new ArrayList<>(arguments.size());
    for (TypeArgument arg : arguments) {
      result.add(visitTypeArgument(arg, ctx));
    }
    return result;
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    SubstVisitorContext ctx = new SubstVisitorContext(mySubstExprs, myFrom);
    return Pi(visitTypeArguments(expr.getArguments(), ctx), ctx.subst(expr.getCodomain()));
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr) {
    return expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this), expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr) {
    return expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this));
    }
    return Tuple(fields, (SigmaExpression) expr.getType().accept(this));
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    return Sigma(visitTypeArguments(expr.getArguments(), new SubstVisitorContext(mySubstExprs, myFrom)));
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    throw new IllegalStateException();
  }

  @Override
  public Expression visitProj(ProjExpression expr) {
    return Proj(expr.getExpression().accept(this), expr.getField());
  }

  @Override
  public Expression visitClassExt(ClassExtExpression expr) {
    Map<FunctionDefinition, OverriddenDefinition> definitions = new HashMap<>();
    for (Map.Entry<FunctionDefinition, OverriddenDefinition> entry : expr.getDefinitionsMap().entrySet()) {
      FunctionDefinition function = entry.getValue();
      List<Argument> arguments = new ArrayList<>(function.getArguments().size());
      Expression[] result = visitLamArguments(function.getArguments(), arguments, function.getResultType(), function.getTerm());
      definitions.put(entry.getKey(), new OverriddenDefinition(function.getParentNamespace(), function.getName(), function.getStaticNamespace(), function.getPrecedence(), arguments, result[0], function.getArrow(), result[1], entry.getKey()));
    }
    return ClassExt(visitDefCall(expr.getBaseClassExpression()), definitions, expr.getUniverse());
  }

  @Override
  public Expression visitNew(NewExpression expr) {
    return New(expr.getExpression().accept(this));
  }

  @Override
  public LetExpression visitLet(LetExpression letExpression) {
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
    final List<Argument> arguments = visitArguments(clause.getArguments(), localCtx);
    final Expression resultType = clause.getResultType() == null ? null : localCtx.subst(clause.getResultType());
    final Expression expression = localCtx.subst(clause.getTerm());
    return new LetClause(clause.getName(), arguments, resultType, clause.getArrow(), expression);
  }
}
