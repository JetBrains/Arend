package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

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
    return Apps(expr.getFunction().accept(this), expr.getArgument().accept(this));
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
    List<Argument> arguments = new ArrayList<>();
    List<Expression> substExprs = new ArrayList<>(mySubstExprs);
    int from = myFrom;
    int on = 0;
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof NameArgument) {
        arguments.add(argument);
        ++on;
      } else
      if (argument instanceof TelescopeArgument) {
        from += on;
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        for (int i = 0; i < substExprs.size(); ++i) {
          substExprs.set(i, substExprs.get(i).liftIndex(0, on));
        }
        arguments.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), teleArgument.getType().subst(substExprs, from)));
        on = teleArgument.getNames().size();
      } else {
        throw new IllegalStateException();
      }
    }

    for (int i = 0; i < substExprs.size(); ++i) {
      substExprs.set(i, substExprs.get(i).liftIndex(0, on));
    }
    return Lam(arguments, expr.getBody().subst(substExprs, from + on));
  }

  @Override
  public Expression visitNat(NatExpression expr) {
    return expr;
  }

  @Override
  public Expression visitNelim(NelimExpression expr) {
    return expr;
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
  public Expression visitSuc(SucExpression expr) {
    return expr;
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
  public Expression visitZero(ZeroExpression expr) {
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
  public Expression visitBinOp(BinOpExpression expr) {
    return BinOp(expr.getLeft().accept(this), expr.getBinOp(), expr.getRight().accept(this));
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    List<Clause> clauses = new ArrayList<>(expr.getClauses().size());
    for (Clause clause : expr.getClauses()) {
      List<Argument> arguments = new ArrayList<>();
      List<Expression> substExprs = new ArrayList<>(mySubstExprs);
      int from = myFrom;
      int on = 0;
      for (Argument argument : clause.getArguments()) {
        if (argument instanceof NameArgument) {
          arguments.add(argument);
          ++on;
        } else
        if (argument instanceof TelescopeArgument) {
          from += on;
          TelescopeArgument teleArgument = (TelescopeArgument) argument;
          for (int i = 0; i < substExprs.size(); ++i) {
            substExprs.set(i, substExprs.get(i).liftIndex(0, on));
          }
          arguments.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), teleArgument.getType().subst(substExprs, from)));
          on = teleArgument.getNames().size();
        } else {
          throw new IllegalStateException();
        }
      }

      for (int i = 0; i < substExprs.size(); ++i) {
        substExprs.set(i, substExprs.get(i).liftIndex(0, on));
      }
      clauses.add(new Clause(clause.getConstructor(), arguments, clause.getArrow(), clause.getExpression().subst(substExprs, from + on)));
    }
    return Elim(expr.getElimType(), expr.getExpression().accept(this), clauses);
  }
}
