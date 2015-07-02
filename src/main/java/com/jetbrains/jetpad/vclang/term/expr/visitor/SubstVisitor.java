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
  public Expression visitDefCall(DefCallExpression expr) {
    return expr;
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

  public Expression[] visitLamArguments(List<Argument> inputArgs, List<Argument> outputArgs, Expression... exprs) {
    List<Expression> substExprs = new ArrayList<>(mySubstExprs);
    int from = myFrom;
    int on = 0;
    for (Argument argument : inputArgs) {
      if (argument instanceof NameArgument) {
        outputArgs.add(argument);
        ++on;
      } else
      if (argument instanceof TelescopeArgument) {
        from += on;
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        for (int i = 0; i < substExprs.size(); ++i) {
          substExprs.set(i, substExprs.get(i).liftIndex(0, on));
        }
        outputArgs.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), teleArgument.getType().subst(substExprs, from)));
        on = teleArgument.getNames().size();
      } else {
        throw new IllegalStateException();
      }
    }

    for (int i = 0; i < substExprs.size(); ++i) {
      substExprs.set(i, substExprs.get(i).liftIndex(0, on));
    }

    Expression[] result = new Expression[exprs.length];
    for (int i = 0; i < exprs.length; ++i) {
      result[i] = exprs[i] == null ? null : exprs[i].subst(substExprs, from + on);
    }
    return result;
  }

  private Expression visitArguments(List<TypeArgument> arguments, Expression codomain) {
    List<Expression> substExprs = new ArrayList<>(mySubstExprs);
    int from = myFrom;
    List<TypeArgument> result = new ArrayList<>();
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        List<String> names = ((TelescopeArgument) argument).getNames();
        result.add(new TelescopeArgument(argument.getExplicit(), names, argument.getType().subst(substExprs, from)));
        for (int i = 0; i < substExprs.size(); ++i) {
          substExprs.set(i, substExprs.get(i).liftIndex(0, names.size()));
        }
        from += names.size();
      } else {
        result.add(new TypeArgument(argument.getExplicit(), argument.getType().subst(substExprs, from)));
        for (int i = 0; i < substExprs.size(); ++i) {
          substExprs.set(i, substExprs.get(i).liftIndex(0, 1));
        }
        ++from;
      }
    }
    return codomain == null ? Sigma(result) : Pi(result, codomain.subst(substExprs, from));
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    return visitArguments(expr.getArguments(), expr.getCodomain());
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
    return Tuple(fields);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    return visitArguments(expr.getArguments(), null);
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    throw new IllegalStateException();
  }

  @Override
  public Expression visitFieldAcc(FieldAccExpression expr) {
    return FieldAcc(expr.getExpression().accept(this), expr.getField());
  }

  @Override
  public Expression visitProj(ProjExpression expr) {
    return Proj(expr.getExpression().accept(this), expr.getField());
  }

  @Override
  public Expression visitClassExt(ClassExtExpression expr) {
    Map<FunctionDefinition, OverriddenDefinition> definitions = new HashMap<>();
    for (Map.Entry<FunctionDefinition, OverriddenDefinition> entry : expr.getDefinitionsMap().entrySet()) {
      List<Argument> arguments = new ArrayList<>(entry.getValue().getArguments().size());
      Expression[] result = visitLamArguments(entry.getValue().getArguments(), arguments, entry.getValue().getResultType(), entry.getValue().getTerm());
      definitions.put(entry.getKey(), new OverriddenDefinition(entry.getValue().getName(), entry.getValue().getParent(), entry.getValue().getPrecedence(), entry.getValue().getFixity(), arguments, result[0], entry.getValue().getArrow(), result[1], entry.getKey()));
    }
    return ClassExt(expr.getBaseClass(), definitions);
  }

  @Override
  public Expression visitNew(NewExpression expr) {
    return New(expr.getExpression().accept(this));
  }
}
