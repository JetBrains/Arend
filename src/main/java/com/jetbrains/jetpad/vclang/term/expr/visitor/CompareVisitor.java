package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

public class CompareVisitor implements AbstractExpressionVisitor<Expression, CompareVisitor.Result> {
  private final List<Equation> myEquations;
  private CMP myCmp;

  public enum CMP { EQ, GEQ, LEQ }

  public interface Result {
    boolean isOK();
  }

  public static class JustResult implements Result {
    private final boolean myResult;

    public JustResult(boolean result) {
      myResult = result;
    }

    public boolean getResult() {
      return myResult;
    }

    @Override
    public boolean isOK() {
      return myResult;
    }
  }

  public static class MaybeResult implements Result {
    Abstract.Expression myExpression;

    public MaybeResult(Abstract.Expression expression) {
      myExpression = expression;
    }

    public Abstract.Expression getExpression() {
      return myExpression;
    }

    @Override
    public boolean isOK() {
      return false;
    }
  }

  private static CMP not(CMP cmp) {
    switch (cmp) {
      case EQ:
        return CMP.EQ;
      case GEQ:
        return CMP.LEQ;
      case LEQ:
        return CMP.GEQ;
      default:
        throw new IllegalStateException();
    }
  }

  private static Abstract.Expression lamArgs(Abstract.Expression expr, List<Abstract.Expression> args) {
    if (expr instanceof Abstract.LamExpression) {
      Abstract.LamExpression lamExpr = (Abstract.LamExpression) expr;
      for (Abstract.Argument arg : lamExpr.getArguments()) {
        if (arg instanceof Abstract.TelescopeArgument) {
          Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) arg;
          for (String ignored : teleArg.getNames()) {
            args.add(teleArg.getType());
          }
        } else
        if (arg instanceof Abstract.NameArgument) {
          args.add(null);
        }
      }
      return lamArgs(lamExpr.getBody(), args);
    } else {
      return expr;
    }
  }

  private static Abstract.Expression piArgs(Abstract.Expression expr, List<Abstract.Expression> args) {
    if (expr instanceof Abstract.PiExpression) {
      Abstract.PiExpression piExpr = (Abstract.PiExpression) expr;
      for (Abstract.Argument arg : piExpr.getArguments()) {
        if (arg instanceof Abstract.TelescopeArgument) {
          Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) arg;
          for (String ignored : teleArg.getNames()) {
            args.add(teleArg.getType());
          }
        } else
        if (arg instanceof Abstract.TypeArgument) {
          args.add(((Abstract.TypeArgument) arg).getType());
        }
      }
      return piArgs(piExpr.getCodomain(), args);
    } else {
      return expr;
    }
  }

  public static class Equation {
    public Abstract.InferHoleExpression hole;
    public Expression expression;

    public Equation(Abstract.InferHoleExpression hole, Expression expression) {
      this.hole = hole;
      this.expression = expression;
    }
  }

  public CompareVisitor(CMP cmp, List<Equation> equations) {
    myCmp = cmp;
    myEquations = equations;
  }

  public List<Equation> equations() {
    return myEquations;
  }

  private boolean checkPath(Abstract.Expression expr, Expression other) {
    if (!(other instanceof AppExpression && ((AppExpression) other).getFunction() instanceof DefCallExpression && ((DefCallExpression) ((AppExpression) other).getFunction()).getDefinition().equals(Prelude.PATH_CON) && ((AppExpression) other).getArgument().getExpression() instanceof LamExpression)) {
      return false;
    }

    List<Expression> args = new ArrayList<>();
    Expression expr1 = ((LamExpression) ((AppExpression) other).getArgument().getExpression()).getBody().getFunction(args);
    if (expr1 instanceof DefCallExpression && ((DefCallExpression) expr1).getDefinition().equals(Prelude.AT) && args.size() == 5 && args.get(0) instanceof IndexExpression && ((IndexExpression) args.get(0)).getIndex() == 0) {
      Expression newOther = null;
      try {
        newOther = args.get(1).liftIndex(0, -1);
      } catch (LiftIndexVisitor.NegativeIndexException ignored) {}

      if (newOther != null) {
        return expr.accept(this, newOther).isOK();
      }
    }
    return false;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression other) {
    if (expr == other || checkPath(expr, other) || checkLam(expr, other)) return new JustResult(true);

    if (expr.getFunction() instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr.getFunction()).getDefinition().equals(Prelude.PATH_CON)) {
      if (expr.getArgument().getExpression() instanceof Abstract.LamExpression) {
        List<Abstract.ArgumentExpression> args = new ArrayList<>();
        Abstract.Expression expr1 = Abstract.getFunction(((Abstract.LamExpression) expr.getArgument().getExpression()).getBody(), args);
        if (expr1 instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr1).getDefinition().equals(Prelude.AT) && args.size() == 5 && args.get(4).getExpression() instanceof Abstract.IndexExpression && ((Abstract.IndexExpression) args.get(4).getExpression()).getIndex() == 0) {
          Result result = args.get(3).getExpression().accept(this, other.liftIndex(0, 1));
          if (result.isOK()) return result;
        }
      }
    }

    if (!(other instanceof AppExpression)) return new JustResult(false);

    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    Abstract.Expression expr1 = Abstract.getFunction(expr, args);
    List<Expression> otherArgs = new ArrayList<>();
    Expression other1 = other.getFunction(otherArgs);

    int equationsNumber = myEquations.size();
    Result result = expr1.accept(this, other1);
    if (!result.isOK()) return result;
    if (myEquations.size() > equationsNumber) {
      while (myEquations.size() > equationsNumber) {
        myEquations.remove(myEquations.size() - 1);
      }
      return new MaybeResult(expr1);
    }
    if (args.size() != otherArgs.size()) return new JustResult(false);

    for (int i = 0; i < args.size(); ++i) {
      result = args.get(i).getExpression().accept(this, otherArgs.get(args.size() - 1 - i));
      if (!result.isOK()) return result;
    }
    return new JustResult(true);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression other) {
    return new JustResult(expr == other || checkPath(expr, other) || checkLam(expr, other) || other instanceof Abstract.DefCallExpression && expr.getDefinition().equals(((Abstract.DefCallExpression) other).getDefinition()));
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression other) {
    return new JustResult(expr == other || checkPath(expr, other) || checkLam(expr, other) || other instanceof Abstract.IndexExpression && expr.getIndex() == ((Abstract.IndexExpression) other).getIndex());
  }

  private boolean checkLam(Abstract.Expression expr, Expression other) {
    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression body1 = lamArgs(expr, args1);
    List<Abstract.Expression> args2 = new ArrayList<>();
    Expression body2 = (Expression) lamArgs(other, args2);

    if (args1.size() == 0 && args2.size() == 0) return false;

    if (args1.size() < args2.size()) {
      for (int i = 0; i < args2.size() - args1.size(); ++i) {
        if (!(body2 instanceof AppExpression && ((AppExpression) body2).getArgument().getExpression() instanceof IndexExpression && ((IndexExpression) ((AppExpression) body2).getArgument().getExpression()).getIndex() == i)) {
          return false;
        }
        body2 = ((AppExpression) body2).getFunction();
      }

      try {
        body2 = body2.liftIndex(0, args1.size() - args2.size());
      } catch (LiftIndexVisitor.NegativeIndexException ignored) {
        return false;
      }
    }

    if (args2.size() < args1.size()) {
      for (int i = 0; i < args1.size() - args2.size(); ++i) {
        if (!(body1 instanceof Abstract.AppExpression && ((Abstract.AppExpression) body1).getArgument().getExpression() instanceof Abstract.IndexExpression && ((Abstract.IndexExpression) ((Abstract.AppExpression) body1).getArgument().getExpression()).getIndex() == i)) {
          return false;
        }
        body1 = ((Abstract.AppExpression) body1).getFunction();
      }

      body2 = body2.liftIndex(0, args1.size() - args2.size());
    }

    int equationsNumber = myEquations.size();
    if (!body1.accept(this, body2).isOK()) return false;
    for (int i = equationsNumber; i < myEquations.size(); ++i) {
      try {
        myEquations.get(i).expression = myEquations.get(i).expression.liftIndex(0, -args1.size());
      } catch (LiftIndexVisitor.NegativeIndexException ignored) {
        myEquations.remove(i--);
      }
    }

    for (int i = 0; i < args1.size(); ++i) {
      equationsNumber = myEquations.size();
      if (args1.get(i) != null && args2.get(i) != null && !args1.get(i).accept(this, (Expression) args2.get(i)).isOK()) return false;
      for (int j = equationsNumber; j < myEquations.size(); ++j) {
        try {
          myEquations.get(j).expression = myEquations.get(j).expression.liftIndex(0, -i);
        } catch (LiftIndexVisitor.NegativeIndexException ignored) {
          myEquations.remove(j--);
        }
      }
    }

    return true;
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression other) {
    return new JustResult(expr == other || checkLam(expr, other));
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression other) {
    if (expr == other) return new JustResult(true);
    if (!(other instanceof PiExpression)) return new JustResult(false);

    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression codomain1 = piArgs(expr, args1);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    Expression codomain2 = other.splitAt(args1.size(), args2);
    if (args1.size() != args2.size()) return new JustResult(false);

    int equationsNumber;

    myCmp = not(myCmp);
    for (int i = 0; i < args1.size(); ++i) {
      if (i > 0 && args1.get(i) == args1.get(i - 1) && args2.get(i).getType() == args2.get(i - 1).getType()) continue;

      equationsNumber = myEquations.size();
      Result result = args1.get(i).accept(this, args2.get(i).getType());
      if (!result.isOK()) return result;
      for (int j = equationsNumber; j < myEquations.size(); ++j) {
        try {
          myEquations.get(j).expression = myEquations.get(j).expression.liftIndex(0, -i);
        } catch (LiftIndexVisitor.NegativeIndexException ignored) {
          myEquations.remove(j--);
        }
      }
    }
    myCmp = not(myCmp);

    equationsNumber = myEquations.size();
    Result result = codomain1.accept(this, codomain2);
    for (int i = equationsNumber; i < myEquations.size(); ++i) {
      try {
        myEquations.get(i).expression = myEquations.get(i).expression.liftIndex(0, -args1.size());
      } catch (LiftIndexVisitor.NegativeIndexException ignored) {
        myEquations.remove(i--);
      }
    }
    return result;
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression other) {
    if (expr == other) return new JustResult(true);
    if (!(other instanceof Abstract.UniverseExpression)) return new JustResult(false);
    Universe.Cmp cmp = expr.getUniverse().compare(((Abstract.UniverseExpression) other).getUniverse());
    if (myCmp == CMP.LEQ) return new JustResult(cmp == Universe.Cmp.EQUALS || cmp == Universe.Cmp.LESS);
    if (myCmp == CMP.GEQ) return new JustResult(cmp == Universe.Cmp.EQUALS || cmp == Universe.Cmp.GREATER);
    return new JustResult(cmp == Universe.Cmp.EQUALS);
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Expression other) {
    return new JustResult(expr == other || checkPath(expr, other) || checkLam(expr, other) || other instanceof Abstract.VarExpression && expr.getName().equals(((Abstract.VarExpression) other).getName()));
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression other) {
    return new JustResult(true);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression other) {
    myEquations.add(new Equation(expr, other.normalize(NormalizeVisitor.Mode.NF)));
    return new JustResult(true);
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression other) {
    if (expr == other) return new JustResult(true);
    if (!(other instanceof TupleExpression)) return new JustResult(false);

    TupleExpression otherTuple = (TupleExpression) other;
    if (expr.getFields().size() != otherTuple.getFields().size()) return new JustResult(false);
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = expr.getField(i).accept(this, otherTuple.getField(i));
      if (!result.isOK()) return result;
    }
    return new JustResult(true);
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression other) {
    if (expr == other) return new JustResult(true);
    if (!(other instanceof SigmaExpression)) return new JustResult(false);

    List<Abstract.Expression> args = new ArrayList<>();
    for (Abstract.TypeArgument arg : expr.getArguments()) {
      if (arg instanceof Abstract.TelescopeArgument) {
        for (String ignored : ((Abstract.TelescopeArgument) arg).getNames()) {
          args.add(arg.getType());
        }
      } else {
        args.add(arg.getType());
      }
    }

    SigmaExpression otherSigma = (SigmaExpression) other;
    List<Expression> otherArgs = new ArrayList<>();
    for (TypeArgument arg : otherSigma.getArguments()) {
      if (arg instanceof TelescopeArgument) {
        for (String ignored : ((TelescopeArgument) arg).getNames()) {
          otherArgs.add(arg.getType());
        }
      } else {
        otherArgs.add(arg.getType());
      }
    }

    if (args.size() != otherArgs.size()) return new JustResult(false);
    for (int i = 0; i < args.size(); ++i) {
      if (i > 0 && args.get(i) == args.get(i - 1) && otherArgs.get(i) == otherArgs.get(i - 1)) continue;

      int equationsNumber = myEquations.size();
      Result result = args.get(i).accept(this, otherArgs.get(i));
      if (!result.isOK()) return result;
      for (int j = equationsNumber; j < myEquations.size(); ++j) {
        try {
          myEquations.get(j).expression = myEquations.get(j).expression.liftIndex(0, -i);
        } catch (LiftIndexVisitor.NegativeIndexException ignored) {
          myEquations.remove(j--);
        }
      }
    }
    return new JustResult(true);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression other) {
    if (expr == other || checkPath(expr, other) || checkLam(expr, other)) return new JustResult(true);
    if (!(other instanceof BinOpExpression)) return new JustResult(false);

    BinOpExpression otherBinOp = (BinOpExpression) other;
    if (!expr.getBinOp().equals(otherBinOp.getBinOp())) return new JustResult(false);
    Result result = expr.getLeft().getExpression().accept(this, otherBinOp.getLeft().getExpression());
    if (!result.isOK()) return result;
    return expr.getRight().getExpression().accept(this, otherBinOp.getRight().getExpression());
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression other) {
    if (expr == other) return new JustResult(true);
    if (!(other instanceof ElimExpression)) return new JustResult(false);

    ElimExpression otherElim = (ElimExpression) other;
    if (expr.getElimType() != otherElim.getElimType() || expr.getClauses().size() != otherElim.getClauses().size()) {
      return new JustResult(false);
    }
    Result result = expr.getExpression().accept(this, otherElim.getExpression());
    if (!result.isOK()) return result;

    for (int i = 0; i < expr.getClauses().size(); ++i) {
      result = visitClause(expr.getClause(i), otherElim.getClause(i));
      if (!result.isOK()) return result;
    }
    if (expr.getOtherwise() == otherElim.getOtherwise()) return new JustResult(true);
    if (expr.getOtherwise() == null || otherElim.getOtherwise() == null || expr.getOtherwise().getArrow() != otherElim.getOtherwise().getArrow()) return new JustResult(false);
    return expr.getExpression().accept(this, otherElim.getOtherwise().getExpression());
  }

  public Result visitClause(Abstract.Clause clause, Clause other) {
    if (clause == other) return new JustResult(true);
    if (clause == null || other == null) return new JustResult(false);

    if (!other.getName().equals(clause.getName()) || clause.getArrow() != other.getArrow()) return new JustResult(false);
    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression expr1 = lamArgs(clause.getExpression(), args1);
    List<Abstract.Expression> args2 = new ArrayList<>();
    Abstract.Expression expr2 = lamArgs(other.getExpression(), args2);
    if (args1.size() != args2.size()) return new JustResult(false);
    Result result = expr1.accept(this, (Expression) expr2);
    if (!result.isOK()) return result;
    for (int i = 0; i < args1.size(); ++i) {
      if (args1.get(i) != null && args2.get(i) != null) {
        result = args1.get(i).accept(this, (Expression) args2.get(i));
        if (!result.isOK()) return result;
      }
    }
    return new JustResult(true);
  }
}
