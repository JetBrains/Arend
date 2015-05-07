package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;

public class NormalizeVisitor implements ExpressionVisitor<Expression> {
  private final Mode myMode;

  public NormalizeVisitor(Mode mode) {
    myMode = mode;
  }

  private Expression visitApps(Expression expr, List<Expression> exprs) {
    int numberOfLambdas = 0;
    while (expr instanceof LamExpression && numberOfLambdas < exprs.size()) {
      ++numberOfLambdas;
      expr = ((LamExpression) expr).getBody().normalize(Mode.WHNF);
    }
    if (numberOfLambdas > 0) {
      if (numberOfLambdas < exprs.size()) {
        Expression[] exprs1 = new Expression[exprs.size() - numberOfLambdas];
        List<Expression> exprs2 = new ArrayList<>(numberOfLambdas);
        for (int i = 0; i < exprs.size() - numberOfLambdas; ++i) {
          exprs1[i] = exprs.get(exprs.size() - numberOfLambdas - 1 - i);
        }
        for (int i = exprs.size() - numberOfLambdas; i < exprs.size(); ++i) {
          exprs2.add(exprs.get(i));
        }
        expr = Apps(expr.subst(exprs2, 0), exprs1);
        return myMode == Mode.TOP ? expr : expr.accept(this);
      } else {
        expr = expr.subst(exprs, 0);
        return myMode == Mode.TOP ? expr : expr.accept(this);
      }
    }

    if (expr instanceof DefCallExpression) {
      return visitDefCall(((DefCallExpression) expr).getDefinition(), Abstract.Definition.Fixity.PREFIX, exprs);
    }
    if (expr instanceof BinOpExpression) {
      exprs.add(((BinOpExpression) expr).getRight());
      exprs.add(((BinOpExpression) expr).getLeft());
      return visitDefCall(((BinOpExpression) expr).getBinOp(), Abstract.Definition.Fixity.PREFIX, exprs);
    }

    if (myMode == Mode.TOP) return null;
    if (myMode == Mode.NF) {
      expr = expr.accept(this);
    }
    for (int i = exprs.size() - 1; i >= 0; --i) {
      expr = Apps(expr, myMode == Mode.NF ? exprs.get(i).accept(this) : exprs.get(i));
    }
    return expr;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    List<Expression> exprs = new ArrayList<>();
    return visitApps(expr.getFunction(exprs), exprs);
  }

  private Expression addLambdas(List<? extends TypeArgument> args1, int drop, Expression expr) {
    List<Argument> arguments = new ArrayList<>();
    int j = 0, i = 0;
    if (i < drop) {
      for (; j < args1.size(); ++j) {
        if (args1.get(j) instanceof TelescopeArgument) {
          List<String> names = ((TelescopeArgument) args1.get(j)).getNames();
          i += names.size();
          if (i > drop) {
            arguments.add(Tele(names.subList(i - drop, names.size()), args1.get(j).getType()));
            ++j;
            break;
          }
          if (i == drop) {
            ++j;
            break;
          }
        } else {
          if (++i == drop) {
            ++j;
            break;
          }
        }
      }
    }
    for (; j < args1.size(); ++j) {
      if (args1.get(j) instanceof TelescopeArgument) {
        arguments.add(args1.get(j));
      } else {
        arguments.add(Tele(args1.get(j).getExplicit(), vars("x"), args1.get(j).getType()));
      }
    }

    return arguments.isEmpty() ? expr : Lam(arguments, expr);
  }

  public Expression applyDefCall(Definition def, Abstract.Definition.Fixity fixity, List<Expression> args) {
    if (fixity == Abstract.Definition.Fixity.PREFIX) {
      Expression expr = DefCall(def);
      for (int i = args.size() - 1; i >= 0; --i) {
        expr = Apps(expr, myMode == Mode.NF ? args.get(i).accept(this) : args.get(i));
      }
      return expr;
    } else {
      Expression expr = BinOp(args.get(args.size() - 1), def, args.get(args.size() - 2));
      for (int i = args.size() - 3; i >= 0; --i) {
        expr = Apps(expr, myMode == Mode.NF ? args.get(i).accept(this) : args.get(i));
      }
      return expr;
    }
  }

  public Expression visitDefCall(Definition def, Abstract.Definition.Fixity fixity, List<Expression> args) {
    if (def instanceof FunctionDefinition) {
      List<TelescopeArgument> args1 = ((FunctionDefinition) def).getArguments();
      int numberOfArgs = numberOfVariables(args1);
      if (myMode == Mode.WHNF && numberOfArgs > args.size()) {
        return applyDefCall(def, fixity, args);
      }

      List<Expression> args2 = new ArrayList<>(numberOfArgs);
      for (int i = 0; i < Math.min(numberOfArgs, args.size()); ++i) {
        args2.add(args.get(i).liftIndex(0, numberOfArgs - args.size()));
      }
      for (int i = numberOfArgs - args.size() - 1; i >= 0; --i) {
        args2.add(Index(i));
      }

      Expression result = ((FunctionDefinition) def).getTerm().subst(args2, 0);
      if (((FunctionDefinition) def).getArrow() == Abstract.Definition.Arrow.LEFT) {
        result = result.normalize(Mode.TOP);
        if (result == null) {
          return myMode == Mode.TOP ? null : applyDefCall(def, fixity, args);
        }
      }

      if (numberOfArgs <= args.size()) {
        for (int i = numberOfArgs; i < args.size(); ++i) {
          result = Apps(result, args.get(i));
        }
      } else {
        result = addLambdas(args1, args.size(), result);
      }

      return myMode == Mode.TOP ? result : result.accept(this);
    }

    if (myMode == Mode.TOP) return null;

    List<TypeArgument> arguments;
    if (def instanceof DataDefinition) {
      arguments = ((DataDefinition) def).getParameters();
    } else
    if (def instanceof Constructor) {
      arguments = ((Constructor) def).getArguments();
    } else {
      throw new IllegalStateException();
    }

    int numberOfArgs = numberOfVariables(arguments);
    if (myMode == Mode.WHNF && numberOfArgs >= args.size()) {
      return applyDefCall(def, fixity, args);
    }

    int argsSize = fixity == Abstract.Definition.Fixity.PREFIX ? args.size() : args.size() - 2;
    Expression result = fixity == Abstract.Definition.Fixity.PREFIX ? DefCall(def) : BinOp(args.get(args.size() - 1), def, args.get(args.size() - 2));
    for (int i = argsSize - 1; i >= 0; --i) {
      Expression arg = myMode == Mode.NF ? args.get(i).accept(this) : args.get(i);
      result = Apps(result, numberOfArgs > args.size() ? arg.liftIndex(0, numberOfArgs - args.size()) : arg);
    }
    for (int i = numberOfArgs - args.size() - 1; i >= 0; --i) {
      result = Apps(result, Index(i));
    }
    result = addLambdas(arguments, args.size(), result);
    return result;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return visitDefCall(expr.getDefinition(), Abstract.Definition.Fixity.PREFIX, new ArrayList<Expression>());
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    return myMode == Mode.TOP ? null : myMode == Mode.NF ? Lam(visitArguments(expr.getArguments()), expr.getBody().accept(this)) : expr;
  }

  @Override
  public Expression visitNat(NatExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitNelim(NelimExpression expr) {
    return myMode == Mode.TOP ? null : expr;
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
    return myMode == Mode.TOP ? null : myMode == Mode.NF ? Pi(visitTypeArguments(expr.getArguments()), expr.getCodomain().accept(this)) : expr;
  }

  @Override
  public Expression visitSuc(SucExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitVar(VarExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitZero(ZeroExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr) {
    return myMode == Mode.TOP ? null : myMode != Mode.NF || expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this), expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr) {
    if (myMode == Mode.TOP) return null;
    if (myMode != Mode.NF) return expr;
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this));
    }
    return Tuple(fields);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    return myMode == Mode.TOP ? null : myMode == Mode.NF ? Sigma(visitTypeArguments(expr.getArguments())) : expr;
  }

  @Override
  public Expression visitBinOp(BinOpExpression expr) {
    List<Expression> args = new ArrayList<>(2);
    args.add(expr.getRight());
    args.add(expr.getLeft());
    return visitDefCall(expr.getBinOp(), Abstract.Definition.Fixity.INFIX, args);
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    List<Expression> args = new ArrayList<>();
    Expression fun = expr.getExpression().normalize(Mode.WHNF).getFunction(args);
    if (!(fun instanceof DefCallExpression && ((DefCallExpression) fun).getDefinition() instanceof Constructor)) {
      return myMode == Mode.NF ? visitElimNF(expr) : myMode == Mode.TOP ? null : expr;
    }

    Constructor constructor = (Constructor) ((DefCallExpression) fun).getDefinition();
    Clause clause = expr.getClauses().get(constructor.getIndex());
    if (clause != null && clause.getArguments().size() == args.size()) {
      Expression result = clause.getExpression().subst(args, 0);
      return myMode == Mode.TOP ? result : result.accept(this);
    } else {
      return myMode == Mode.NF ? visitElimNF(expr) : myMode == Mode.TOP ? null : expr;
    }
  }

  private ElimExpression visitElimNF(ElimExpression expr) {
    List<Clause> clauses = new ArrayList<>(expr.getClauses().size());
    for (Clause clause : expr.getClauses()) {
      clauses.add(new Clause(clause.getConstructor(), visitArguments(clause.getArguments()), clause.getArrow(), clause.getExpression().accept(this)));
    }
    return Elim(expr.getElimType(), expr.getExpression().accept(this), clauses);
  }

  public enum Mode { WHNF, NF, TOP }
}
