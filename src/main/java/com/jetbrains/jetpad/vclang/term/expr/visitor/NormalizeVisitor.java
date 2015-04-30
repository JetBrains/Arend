package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

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

  // TODO: Fix normalization of function calls with <=.
  private Expression visitDefCall(Expression expr, Definition function, Expression... expressions) {
    if (function instanceof FunctionDefinition) {
      return Apps(((FunctionDefinition) function).getTerm(), expressions).accept(this);
    } else {
      return expr;
    }
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return visitDefCall(expr, expr.getDefinition());
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    return expr;
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    return myMode == Mode.NF ? Lam(visitArguments(expr.getArguments()), expr.getBody().accept(this)) : expr;
  }

  @Override
  public Expression visitNat(NatExpression expr) {
    return expr;
  }

  @Override
  public Expression visitNelim(NelimExpression expr) {
    return expr;
  }

  private List<Argument> visitArguments(List<Argument> arguments) {
    List<Argument> result = new ArrayList<>(arguments.size());
    for (Argument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        result.add(new TelescopeArgument(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), ((TelescopeArgument) argument).getType().accept(this)));
      } else {
        if (argument instanceof TypeArgument) {
          result.add(new TypeArgument(argument.getExplicit(), ((TypeArgument) argument).getType().accept(this)));
        } else {
          result.add(argument);
        }
      }
    }
    return result;
  }

  private List<TypeArgument> visitTypeArguments(List<TypeArgument> arguments) {
    List<TypeArgument> result = new ArrayList<>(arguments.size());
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        result.add(new TelescopeArgument(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), argument.getType().accept(this)));
      } else {
        result.add(new TypeArgument(argument.getExplicit(), argument.getType().accept(this)));
      }
    }
    return result;
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    if (myMode == Mode.WHNF) return expr;
    return Pi(visitTypeArguments(expr.getArguments()), expr.getCodomain().accept(this));
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
    return myMode == Mode.WHNF || expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this), expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr) {
    return expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr) {
    if (myMode == Mode.WHNF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this));
    }
    return Tuple(fields);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    if (myMode == Mode.WHNF) return expr;
    return Sigma(visitTypeArguments(expr.getArguments()));
  }

  @Override
  public Expression visitBinOp(BinOpExpression expr) {
    return visitDefCall(expr, expr.getBinOp(), expr.getLeft(), expr.getRight());
  }

  // TODO: Fix normalization of eliminators with <=.
  @Override
  public Expression visitElim(ElimExpression expr) {
    List<Expression> args = new ArrayList<>();
    Expression fun = expr.getExpression().normalize(Mode.WHNF).getFunction(args);
    if (!(fun instanceof DefCallExpression && ((DefCallExpression) fun).getDefinition() instanceof Constructor)) {
      return myMode == Mode.WHNF ? expr : visitElimNF(expr);
    }

    Constructor constructor = (Constructor) ((DefCallExpression) fun).getDefinition();
    for (Clause clause : expr.getClauses()) {
      if (clause.getConstructor().equals(constructor) && clause.getArguments().size() == args.size()) {
        Expression result = clause.getExpression();
        for (int i = 0; i < args.size(); ++i) {
          result = result.subst(args.get(i).liftIndex(0, args.size() - 1 - i), 0);
        }
        return result.accept(this);
      }
    }
    return myMode == Mode.WHNF ? expr : visitElimNF(expr);
  }

  private ElimExpression visitElimNF(ElimExpression expr) {
    List<Clause> clauses = new ArrayList<>(expr.getClauses().size());
    for (Clause clause : expr.getClauses()) {
      clauses.add(new Clause(clause.getConstructor(), visitArguments(clause.getArguments()), clause.getArrow(), clause.getExpression().accept(this)));
    }
    return Elim(expr.getElimType(), expr.getExpression().accept(this), clauses);
  }

  public enum Mode { WHNF, NF }
}
