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
    return expr;
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
    Integer from = visitLamArguments(expr.getArguments(), arguments);
    if (from == null) return null;
    Expression body = expr.getBody().liftIndex(from, myOn);
    return body == null ? null : Lam(arguments, body);
  }

  private Integer visitLamArguments(List<Argument> arguments, List<Argument> result) {
    int from = myFrom;
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

  private int visitArguments(List<TypeArgument> arguments, List<TypeArgument> result) {
    int from = myFrom;
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

  @Override
  public Expression visitPi(PiExpression expr) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    int from = visitArguments(expr.getArguments(), result);
    if (from < 0) return null;
    Expression codomain = expr.getCodomain().liftIndex(from, myOn);
    return codomain == null ? null : Pi(result, codomain);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr) {
    return expr;
  }

  @Override
  public Expression visitVar(VarExpression expr) {
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
    return Tuple(fields);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    return visitArguments(expr.getArguments(), result) < 0 ? null : Sigma(result);
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    throw new IllegalStateException();
  }

  @Override
  public Expression visitFieldAcc(FieldAccExpression expr) {
    Expression expr1 = expr.getExpression().accept(this);
    return expr1 == null ? null : FieldAcc(expr1, expr.getField());
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
      Integer from = visitLamArguments(entry.getValue().getArguments(), arguments);
      if (from == null) return null;

      Expression resultType = entry.getValue().getResultType() == null ? null : entry.getValue().getResultType().liftIndex(from, myOn);
      Expression term = entry.getValue().getTerm() == null ? null : entry.getValue().getTerm().liftIndex(from, myOn);
      definitions.put(entry.getKey(), new OverriddenDefinition(entry.getValue().getName(), entry.getValue().getParent(), entry.getValue().getPrecedence(), entry.getValue().getFixity(), arguments, resultType, entry.getValue().getArrow(), term, entry.getKey()));
    }
    return ClassExt(expr.getBaseClass(), definitions);
  }

  @Override
  public Expression visitNew(NewExpression expr) {
    return New(expr.getExpression().accept(this));
  }
}
