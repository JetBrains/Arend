package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

// TODO: Rewrite normalization using thunks
public class NormalizeVisitor implements ExpressionVisitor<Expression> {
  private final Mode myMode;

  public NormalizeVisitor(Mode mode) {
    myMode = mode;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    Expression function1 = expr.getFunction().accept(this);
    if (function1 instanceof LamExpression) {
      Expression body = ((LamExpression)function1).getBody();
      return body.subst(expr.getArgument(), 0).accept(this);
    }
    if (function1 instanceof AppExpression) {
      AppExpression appExpr1 = (AppExpression)function1;
      if (appExpr1.getFunction() instanceof AppExpression) {
        AppExpression appExpr2 = (AppExpression)appExpr1.getFunction();
        if (appExpr2.getFunction() instanceof NelimExpression) {
          Expression zeroClause = appExpr2.getArgument();
          Expression sucClause = appExpr1.getArgument();
          Expression caseExpr = expr.getArgument().accept(this);
          if (caseExpr instanceof ZeroExpression) {
            return myMode == Mode.WHNF ? zeroClause.accept(this) : zeroClause;
          }
          if (caseExpr instanceof AppExpression) {
            AppExpression appExpr3 = (AppExpression)caseExpr;
            if (appExpr3.getFunction() instanceof SucExpression) {
              Expression recursiveCall = Apps(appExpr1, appExpr3.getArgument());
              Expression result = Apps(sucClause, appExpr3.getArgument(), recursiveCall);
              return result.accept(this);
            }
          }
        }
      }
    }
    return Apps(function1, myMode == Mode.WHNF ? expr.getArgument() : expr.getArgument().accept(this));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    if (expr.getDefinition() instanceof FunctionDefinition) {
      return ((FunctionDefinition) expr.getDefinition()).getTerm().accept(this);
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    return expr;
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    if (myMode == Mode.NF) {
      List<Argument> arguments = new ArrayList<>(expr.getArguments().size());
      for (Argument argument : expr.getArguments()) {
        if (argument instanceof NameArgument) {
          arguments.add(argument);
        } else
        if (argument instanceof TelescopeArgument) {
          arguments.add(new TelescopeArgument(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), ((TypeArgument) argument).getType().accept(this)));
        } else {
          throw new IllegalStateException();
        }
      }
      return Lam(arguments, expr.getBody().accept(this));
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitNat(NatExpression expr) {
    return expr;
  }

  @Override
  public Expression visitNelim(NelimExpression expr) {
    return expr;
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    if (myMode == Mode.WHNF) {
      return expr;
    } else {
      List<TypeArgument> arguments = new ArrayList<>();
      for (TypeArgument argument : expr.getArguments()) {
        if (argument instanceof TelescopeArgument) {
          arguments.add(new TelescopeArgument(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), argument.getType().accept(this)));
        } else {
          arguments.add(new TypeArgument(argument.getExplicit(), argument.getType().accept(this)));
        }
      }
      return Pi(arguments, expr.getCodomain().accept(this));
    }
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
  public Expression visitHole(HoleExpression expr) {
    if (myMode == Mode.WHNF) {
      return expr;
    } else {
      return expr.getInstance(expr.expression() == null ? null : expr.expression().accept(this));
    }
  }

  public static enum Mode { WHNF, NF }
}
