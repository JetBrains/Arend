package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class NormalizeVisitor implements ExpressionVisitor<Expression> {
  private final Mode myMode;

  public NormalizeVisitor(Mode mode) {
    myMode = mode;
  }

  private Expression visitApps(Expression expr, List<ArgumentExpression> exprs) {
    int numberOfLambdas = 0;
    while (expr instanceof LamExpression && numberOfLambdas < exprs.size()) {
      ++numberOfLambdas;
      expr = ((LamExpression) expr).getBody().normalize(Mode.WHNF);
    }
    if (numberOfLambdas > 0) {
      if (numberOfLambdas < exprs.size()) {
        ArgumentExpression[] exprs1 = new ArgumentExpression[exprs.size() - numberOfLambdas];
        List<Expression> exprs2 = new ArrayList<>(numberOfLambdas);
        for (int i = 0; i < exprs.size() - numberOfLambdas; ++i) {
          exprs1[i] = exprs.get(exprs.size() - numberOfLambdas - 1 - i);
        }
        for (int i = exprs.size() - numberOfLambdas; i < exprs.size(); ++i) {
          exprs2.add(exprs.get(i).getExpression());
        }
        expr = Apps(expr.subst(exprs2, 0), exprs1);
        return myMode == Mode.TOP ? expr : expr.accept(this);
      } else {
        List<Expression> jexprs = new ArrayList<>(exprs.size());
        for (ArgumentExpression expr1 : exprs) {
          jexprs.add(expr1.getExpression());
        }
        expr = expr.subst(jexprs, 0);
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
      if (myMode == Mode.NF) {
        expr = Apps(expr, new ArgumentExpression(exprs.get(i).getExpression().accept(this), exprs.get(i).isExplicit(), exprs.get(i).isHidden()));
      } else {
        expr = Apps(expr, exprs.get(i));
      }
    }
    return expr;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    List<ArgumentExpression> exprs = new ArrayList<>();
    return visitApps(expr.getFunctionArgs(exprs), exprs);
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

  public Expression applyDefCall(Definition def, Abstract.Definition.Fixity fixity, List<ArgumentExpression> args) {
    if (myMode == Mode.TOP) return null;
    if (fixity == Abstract.Definition.Fixity.INFIX && args.size() == 2 && args.get(0).isExplicit() && args.get(1).isExplicit()) {
      if (myMode == Mode.NF) {
        for (int i = 0; i < args.size(); ++i) {
          args.set(i, new ArgumentExpression(args.get(i).getExpression().accept(this), args.get(i).isExplicit(), args.get(i).isHidden()));
        }
      }
      Expression expr = BinOp(args.get(args.size() - 1), def, args.get(args.size() - 2));
      for (int i = args.size() - 3; i >= 0; --i) {
        expr = Apps(expr, args.get(i));
      }
      return expr;
    } else {
      Expression expr = DefCall(def);
      for (int i = args.size() - 1; i >= 0; --i) {
        if (myMode == Mode.NF) {
          expr = Apps(expr, new ArgumentExpression(args.get(i).getExpression().accept(this), args.get(i).isExplicit(), args.get(i).isHidden()));
        } else {
          expr = Apps(expr, args.get(i));
        }
      }
      return expr;
    }
  }

  public Expression visitDefCall(Definition def, Abstract.Definition.Fixity fixity, List<ArgumentExpression> args) {
    if (def instanceof FunctionDefinition) {
      List<TypeArgument> args1 = new ArrayList<>();
      splitArguments(def.getType(), args1);
      int numberOfArgs = numberOfVariables(args1);
      if (myMode == Mode.WHNF && numberOfArgs > args.size()) {
        return applyDefCall(def, fixity, args);
      }

      List<Expression> args2 = new ArrayList<>(numberOfArgs);
      for (int i = 0; i < Math.min(numberOfArgs, args.size()); ++i) {
        args2.add(args.get(i).getExpression().liftIndex(0, numberOfArgs - args.size()));
      }
      for (int i = numberOfArgs - args.size() - 1; i >= 0; --i) {
        args2.add(Index(i));
      }

      Expression result = ((FunctionDefinition) def).getTerm();
      Abstract.Definition.Arrow arrow = ((FunctionDefinition) def).getArrow();
      while (result instanceof ElimExpression && ((ElimExpression) result).getElimType() == Abstract.ElimExpression.ElimType.ELIM) {
        List<Expression> constructorArgs = new ArrayList<>();
        Expression expr = ((ElimExpression) result).getExpression().subst(args2, 0).normalize(Mode.WHNF).getFunction(constructorArgs);
        if (expr instanceof DefCallExpression && ((DefCallExpression) expr).getDefinition() instanceof Constructor) {
          Constructor constructor = (Constructor) ((DefCallExpression) expr).getDefinition();
          Clause clause = constructor.getIndex() < ((ElimExpression) result).getClauses().size() ? ((ElimExpression) result).getClauses().get(constructor.getIndex()) : null;
          if (clause != null && clause.getArguments().size() == constructorArgs.size()) {
            int var = ((IndexExpression) ((ElimExpression) result).getExpression()).getIndex();
            args2.remove(var);
            Collections.reverse(constructorArgs);
            args2.addAll(var, constructorArgs);
            result = clause.getExpression();
            arrow = clause.getArrow();
            continue;
          }
        }
        if (((ElimExpression) result).getOtherwise() != null) {
          arrow = ((ElimExpression) result).getOtherwise().getArrow();
          result = ((ElimExpression) result).getOtherwise().getExpression();
          continue;
        }
        return applyDefCall(def, fixity, args);
      }

      result = result.subst(args2, 0);
      if (arrow == Abstract.Definition.Arrow.LEFT) {
        result = result.normalize(Mode.TOP);
        if (result == null) {
          return applyDefCall(def, fixity, args);
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

    List<TypeArgument> splitArguments = new ArrayList<>();
    splitArguments(arguments, splitArguments);
    if (myMode == Mode.WHNF && splitArguments.size() >= args.size()) {
      return applyDefCall(def, fixity, args);
    }

    int argsSize = fixity == Abstract.Definition.Fixity.PREFIX ? args.size() : args.size() - 2;
    Expression result = fixity == Abstract.Definition.Fixity.PREFIX ? DefCall(def) : BinOp(args.get(args.size() - 1), def, args.get(args.size() - 2));
    for (int i = argsSize - 1; i >= 0; --i) {
      Expression arg = args.get(i).getExpression();
      if (myMode == Mode.NF) {
        arg = arg.accept(this);
      }
      if (splitArguments.size() > args.size()) {
        arg = arg.liftIndex(0, splitArguments.size() - args.size());
      }
      result = Apps(result, arg != args.get(i).getExpression() ? new ArgumentExpression(arg, args.get(i).isExplicit(), args.get(i).isHidden()) : args.get(i));
    }
    for (int i = splitArguments.size() - args.size() - 1; i >= 0; --i) {
      result = Apps(result, new ArgumentExpression(Index(i), splitArguments.get(splitArguments.size() - 1 - i).getExplicit(), !splitArguments.get(splitArguments.size() - 1 - i).getExplicit()));
    }
    result = addLambdas(arguments, args.size(), result);
    return result;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return visitDefCall(expr.getDefinition(), Abstract.Definition.Fixity.PREFIX, new ArrayList<ArgumentExpression>());
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    return myMode == Mode.TOP ? null : myMode == Mode.NF ? Lam(visitArguments(expr.getArguments()), expr.getBody().accept(this)) : expr;
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
  public Expression visitUniverse(UniverseExpression expr) {
    return myMode == Mode.TOP ? null : expr;
  }

  @Override
  public Expression visitVar(VarExpression expr) {
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
    List<ArgumentExpression> args = new ArrayList<>(2);
    args.add(expr.getRight());
    args.add(expr.getLeft());
    return visitDefCall(expr.getBinOp(), Abstract.Definition.Fixity.INFIX, args);
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    throw new IllegalStateException();
  }

  public enum Mode { WHNF, NF, TOP }
}
