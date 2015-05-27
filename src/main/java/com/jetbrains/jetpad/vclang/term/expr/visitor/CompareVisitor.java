package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

public class CompareVisitor implements AbstractExpressionVisitor<Expression, CompareVisitor.Result> {
  private final List<Equation> myEquations;

  public enum CMP { EQUIV, EQUALS, GREATER, LESS, NOT_EQUIV }

  public interface Result {
    CMP isOK();
  }

  public static class JustResult implements Result {
    private final CMP myResult;

    public JustResult(CMP result) {
      myResult = result;
    }

    @Override
    public CMP isOK() {
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
    public CMP isOK() {
      return CMP.NOT_EQUIV;
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
          for (int i = 0; i < teleArg.getNames().size(); ++i) {
            args.add(teleArg.getType() instanceof Expression ? ((Expression) teleArg.getType()).liftIndex(0, i) : teleArg.getType());
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

  public CompareVisitor(List<Equation> equations) {
    myEquations = equations;
  }

  public List<Equation> equations() {
    return myEquations;
  }

  private CMP and(CMP cmp1, CMP cmp2) {
    if (cmp2 == CMP.NOT_EQUIV) return CMP.NOT_EQUIV;
    switch (cmp1) {
      case EQUIV:
        return CMP.EQUIV;
      case EQUALS:
        return cmp2;
      case GREATER:
        if (cmp2 == CMP.GREATER || cmp2 == CMP.EQUALS) {
          return CMP.GREATER;
        } else {
          return CMP.EQUIV;
        }
      case LESS:
        if (cmp2 == CMP.LESS || cmp2 == CMP.EQUALS) {
          return CMP.LESS;
        } else {
          return CMP.EQUIV;
        }
    }
    return CMP.NOT_EQUIV;
  }

  private CMP not(CMP cmp) {
    switch (cmp) {
      case EQUIV:
        return CMP.EQUIV;
      case EQUALS:
        return CMP.EQUALS;
      case GREATER:
        return CMP.LESS;
      case LESS:
        return CMP.GREATER;
    }
    return CMP.NOT_EQUIV;
  }

  private Result checkPath(Abstract.Expression expr, Expression other) {
    if (!(other instanceof AppExpression && ((AppExpression) other).getFunction() instanceof DefCallExpression && ((DefCallExpression) ((AppExpression) other).getFunction()).getDefinition().equals(Prelude.PATH_CON) && ((AppExpression) other).getArgument().getExpression() instanceof LamExpression)) {
      return null;
    }

    List<Expression> args = new ArrayList<>();
    Expression expr1 = ((LamExpression) ((AppExpression) other).getArgument().getExpression()).getBody().getFunction(args);
    if (expr1 instanceof DefCallExpression && ((DefCallExpression) expr1).getDefinition().equals(Prelude.AT) && args.size() == 5 && args.get(0) instanceof IndexExpression && ((IndexExpression) args.get(0)).getIndex() == 0) {
      Expression newOther = null;
      try {
        newOther = args.get(1).liftIndex(0, -1);
      } catch (LiftIndexVisitor.NegativeIndexException ignored) {}

      if (newOther != null) {
        List<Equation> equations = new ArrayList<>();
        Result result = expr.accept(new CompareVisitor(equations), newOther);
        if (result instanceof MaybeResult || result.isOK() != CMP.NOT_EQUIV) {
          myEquations.addAll(equations);
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    if (!(expr.getFunction() instanceof DefCallExpression && ((DefCallExpression) expr.getFunction()).getDefinition().equals(Prelude.PATH_CON))) {
      Result result = checkPath(expr, other);
      if (result != null) return result;
    }
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;

    if (expr.getFunction() instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr.getFunction()).getDefinition().equals(Prelude.PATH_CON)) {
      if (expr.getArgument().getExpression() instanceof Abstract.LamExpression) {
        List<Abstract.ArgumentExpression> args = new ArrayList<>();
        Abstract.Expression expr1 = Abstract.getFunction(((Abstract.LamExpression) expr.getArgument().getExpression()).getBody(), args);
        if (expr1 instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr1).getDefinition().equals(Prelude.AT) && args.size() == 5 && args.get(4).getExpression() instanceof Abstract.IndexExpression && ((Abstract.IndexExpression) args.get(4).getExpression()).getIndex() == 0) {
          List<Equation> equations = new ArrayList<>();
          Result result = args.get(3).getExpression().accept(new CompareVisitor(equations), other.liftIndex(0, 1));
          if (result.isOK() != CMP.NOT_EQUIV) {
            myEquations.addAll(equations);
            return result;
          }
        }
      }
    }

    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    Abstract.Expression expr1 = Abstract.getFunction(expr, args);
    if (expr1 instanceof Abstract.InferHoleExpression) {
      return new MaybeResult(expr1);
    }

    List<Expression> otherArgs = new ArrayList<>();
    Expression other1 = other.getFunction(otherArgs);
    if (args.size() != otherArgs.size()) return new JustResult(CMP.NOT_EQUIV);

    int equationsNumber = myEquations.size();
    Result result = expr1.accept(this, other1);
    if (result.isOK() == CMP.NOT_EQUIV) return result;
    if (myEquations.size() > equationsNumber) {
      while (myEquations.size() > equationsNumber) {
        myEquations.remove(myEquations.size() - 1);
      }
      return new MaybeResult(expr1);
    }

    CMP cmp = result.isOK();
    MaybeResult maybeResult = null;
    for (int i = 0; i < args.size(); ++i) {
      result = args.get(i).getExpression().accept(this, otherArgs.get(args.size() - 1 - i));
      if (result.isOK() == CMP.NOT_EQUIV) {
        if (result instanceof MaybeResult) {
          if (maybeResult == null) {
            maybeResult = (MaybeResult) result;
          }
        } else {
          return result;
        }
      }
      cmp = and(cmp, result.isOK());
    }
    return maybeResult == null ? new JustResult(cmp) : maybeResult;
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    Result result = checkPath(expr, other);
    if (result != null) return result;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;
    return new JustResult(other instanceof Abstract.DefCallExpression && expr.getDefinition().equals(((Abstract.DefCallExpression) other).getDefinition()) ? CMP.EQUALS : CMP.NOT_EQUIV);
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    Result result = checkPath(expr, other);
    if (result != null) return result;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;
    return new JustResult(other instanceof Abstract.IndexExpression && expr.getIndex() == ((Abstract.IndexExpression) other).getIndex() ? CMP.EQUALS : CMP.NOT_EQUIV);
  }

  private Result checkLam(Abstract.Expression expr, Expression other) {
    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression body1 = lamArgs(expr, args1);
    List<Abstract.Expression> args2 = new ArrayList<>();
    Expression body2 = (Expression) lamArgs(other, args2);

    if (args1.size() == 0 && args2.size() == 0) return null;

    if (args1.size() < args2.size()) {
      for (int i = 0; i < args2.size() - args1.size(); ++i) {
        if (!(body2 instanceof AppExpression && ((AppExpression) body2).getArgument().getExpression() instanceof IndexExpression && ((IndexExpression) ((AppExpression) body2).getArgument().getExpression()).getIndex() == i)) {
          return new JustResult(CMP.NOT_EQUIV);
        }
        body2 = ((AppExpression) body2).getFunction();
      }

      try {
        body2 = body2.liftIndex(0, args1.size() - args2.size());
      } catch (LiftIndexVisitor.NegativeIndexException ignored) {
        return new JustResult(CMP.NOT_EQUIV);
      }
    }

    if (args2.size() < args1.size()) {
      for (int i = 0; i < args1.size() - args2.size(); ++i) {
        if (!(body1 instanceof Abstract.AppExpression && ((Abstract.AppExpression) body1).getArgument().getExpression() instanceof Abstract.IndexExpression && ((Abstract.IndexExpression) ((Abstract.AppExpression) body1).getArgument().getExpression()).getIndex() == i)) {
          return new JustResult(CMP.NOT_EQUIV);
        }
        body1 = ((Abstract.AppExpression) body1).getFunction();
      }

      body2 = body2.liftIndex(0, args1.size() - args2.size());
    }

    List<Equation> equations = new ArrayList<>();
    CompareVisitor visitor = new CompareVisitor(equations);
    Result result = body1.accept(visitor, body2);
    if (result.isOK() == CMP.NOT_EQUIV && result instanceof JustResult) return result;
    for (int i = 0; i < equations.size(); ++i) {
      try {
        equations.get(i).expression = equations.get(i).expression.liftIndex(0, -args1.size());
      } catch (LiftIndexVisitor.NegativeIndexException ignored) {
        equations.remove(i--);
      }
    }

    CMP cmp = result.isOK();
    MaybeResult maybeResult = result instanceof MaybeResult ? (MaybeResult) result : null;
    for (int i = 0; i < Math.min(args1.size(), args2.size()); ++i) {
      int equationsNumber = equations.size();
      if (args1.get(i) != null && args2.get(i) != null) {
        Result result1 = args1.get(i).accept(visitor, (Expression) args2.get(i));
        if (result1.isOK() == CMP.NOT_EQUIV) {
          if (result1 instanceof MaybeResult) {
            if (maybeResult == null) {
              maybeResult = (MaybeResult) result1;
            }
          } else {
            return result1;
          }
        }
        cmp = and(cmp, result1.isOK());
      }
      for (int j = equationsNumber; j < equations.size(); ++j) {
        try {
          equations.get(j).expression = equations.get(j).expression.liftIndex(0, -i);
        } catch (LiftIndexVisitor.NegativeIndexException ignored) {
          equations.remove(j--);
        }
      }
    }

    if (maybeResult != null || cmp != CMP.NOT_EQUIV) {
      myEquations.addAll(equations);
    }

    return maybeResult == null ? new JustResult(cmp) : maybeResult;
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression other) {
    return expr == other ? new JustResult(CMP.EQUALS) : checkLam(expr, other);
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    if (!(other instanceof PiExpression)) return new JustResult(CMP.NOT_EQUIV);

    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression codomain1 = piArgs(expr, args1);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    Expression codomain2 = other.splitAt(args1.size(), args2);
    if (args1.size() != args2.size()) return new JustResult(CMP.NOT_EQUIV);

    int equationsNumber;

    CMP cmp = CMP.EQUALS;
    for (int i = 0; i < args1.size(); ++i) {
      if (i > 0 && args1.get(i) == args1.get(i - 1) && args2.get(i).getType() == args2.get(i - 1).getType()) continue;

      equationsNumber = myEquations.size();
      Result result = args1.get(i).accept(this, args2.get(i).getType());
      if (result.isOK() == CMP.NOT_EQUIV) return result;
      cmp = and(cmp, result.isOK());
      for (int j = equationsNumber; j < myEquations.size(); ++j) {
        try {
          myEquations.get(j).expression = myEquations.get(j).expression.liftIndex(0, -i);
        } catch (LiftIndexVisitor.NegativeIndexException ignored) {
          myEquations.remove(j--);
        }
      }
    }

    equationsNumber = myEquations.size();
    Result result = codomain1.accept(this, codomain2);
    for (int i = equationsNumber; i < myEquations.size(); ++i) {
      try {
        myEquations.get(i).expression = myEquations.get(i).expression.liftIndex(0, -args1.size());
      } catch (LiftIndexVisitor.NegativeIndexException ignored) {
        myEquations.remove(i--);
      }
    }
    return result.isOK() == CMP.NOT_EQUIV ? result : new JustResult(and(not(cmp), result.isOK()));
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    if (!(other instanceof Abstract.UniverseExpression)) return new JustResult(CMP.NOT_EQUIV);
    switch (expr.getUniverse().compare(((Abstract.UniverseExpression) other).getUniverse())) {
      case EQUALS:
        return new JustResult(CMP.EQUALS);
      case LESS:
        return new JustResult(CMP.LESS);
      case GREATER:
        return new JustResult(CMP.GREATER);
    }
    return new JustResult(CMP.NOT_EQUIV);
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    Result result = checkPath(expr, other);
    if (result != null) return result;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;
    return new JustResult(other instanceof Abstract.VarExpression && expr.getName().equals(((Abstract.VarExpression) other).getName()) ? CMP.EQUALS : CMP.NOT_EQUIV);
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression other) {
    return new JustResult(CMP.EQUALS);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression other) {
    myEquations.add(new Equation(expr, other.normalize(NormalizeVisitor.Mode.NF)));
    return new JustResult(CMP.EQUALS);
  }

  private Result checkTuple(Abstract.Expression expr, Expression other) {
    if (!(other instanceof TupleExpression)) return null;
    TupleExpression otherTuple = (TupleExpression) other;

    List<Equation> equations = new ArrayList<>();
    CompareVisitor visitor = new CompareVisitor(equations);
    CMP cmp = CMP.EQUALS;
    MaybeResult maybeResult = null;
    for (int i = 0; i < otherTuple.getFields().size(); ++i) {
      if (otherTuple.getField(i) instanceof FieldAccExpression && ((FieldAccExpression) otherTuple.getField(i)).getIndex() == i) {
        Result result = expr.accept(visitor, ((FieldAccExpression) otherTuple.getField(i)).getExpression());
        if (result.isOK() == CMP.NOT_EQUIV) {
          if (result instanceof MaybeResult) {
            if (maybeResult == null) {
              maybeResult = (MaybeResult) result;
            }
          } else {
            return null;
          }
        }
        cmp = and(cmp, result.isOK());
      } else {
        return null;
      }
    }

    if (maybeResult != null || cmp != CMP.NOT_EQUIV) {
      myEquations.addAll(equations);
    }

    return maybeResult == null ? new JustResult(cmp) : maybeResult;
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    if (!(other instanceof TupleExpression)) {
      CMP cmp = CMP.EQUALS;
      MaybeResult maybeResult = null;
      for (int i = 0; i < expr.getFields().size(); ++i) {
        if (expr.getField(i) instanceof Abstract.FieldAccExpression && ((Abstract.FieldAccExpression) expr.getField(i)).getIndex() == i) {
          Result result = ((Abstract.FieldAccExpression) expr.getField(i)).getExpression().accept(this, other);
          if (result.isOK() == CMP.NOT_EQUIV) {
            if (result instanceof MaybeResult) {
              if (maybeResult == null) {
                maybeResult = (MaybeResult) result;
              }
            } else {
              return result;
            }
          }
          cmp = and(cmp, result.isOK());
        } else {
          return new JustResult(CMP.NOT_EQUIV);
        }
      }
      return maybeResult == null ? new JustResult(cmp) : maybeResult;
    }

    TupleExpression otherTuple = (TupleExpression) other;
    if (expr.getFields().size() != otherTuple.getFields().size()) return new JustResult(CMP.NOT_EQUIV);

    CMP cmp = CMP.EQUALS;
    MaybeResult maybeResult = null;
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = expr.getField(i).accept(this, otherTuple.getField(i));
      if (result.isOK() == CMP.NOT_EQUIV) {
        if (result instanceof MaybeResult) {
          if (maybeResult == null) {
            maybeResult = (MaybeResult) result;
          }
        } else {
          return result;
        }
      }
      cmp = and(cmp, result.isOK());
    }
    return maybeResult == null ? new JustResult(cmp) : maybeResult;
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    if (!(other instanceof SigmaExpression)) return new JustResult(CMP.NOT_EQUIV);

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

    if (args.size() != otherArgs.size()) return new JustResult(CMP.NOT_EQUIV);
    CMP cmp = CMP.EQUALS;
    MaybeResult maybeResult = null;
    for (int i = 0; i < args.size(); ++i) {
      if (i > 0 && args.get(i) == args.get(i - 1) && otherArgs.get(i) == otherArgs.get(i - 1)) continue;

      int equationsNumber = myEquations.size();
      Result result = args.get(i).accept(this, otherArgs.get(i));
      if (result.isOK() == CMP.NOT_EQUIV) {
        if (result instanceof MaybeResult) {
          if (maybeResult == null) {
            maybeResult = (MaybeResult) result;
          }
        } else {
          return result;
        }
      }
      cmp = and(cmp, result.isOK());

      for (int j = equationsNumber; j < myEquations.size(); ++j) {
        try {
          myEquations.get(j).expression = myEquations.get(j).expression.liftIndex(0, -i);
        } catch (LiftIndexVisitor.NegativeIndexException ignored) {
          myEquations.remove(j--);
        }
      }
    }

    return maybeResult == null ? new JustResult(cmp) : maybeResult;
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    Result pathResult = checkPath(expr, other);
    if (pathResult != null) return pathResult;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;
    if (!(other instanceof BinOpExpression)) return new JustResult(CMP.NOT_EQUIV);

    BinOpExpression otherBinOp = (BinOpExpression) other;
    if (!expr.getBinOp().equals(otherBinOp.getBinOp())) return new JustResult(CMP.NOT_EQUIV);
    Result result = expr.getLeft().getExpression().accept(this, otherBinOp.getLeft().getExpression());
    if (result.isOK() == CMP.NOT_EQUIV) return result;
    Result result1 = expr.getRight().getExpression().accept(this, otherBinOp.getRight().getExpression());
    if (result1.isOK() == CMP.NOT_EQUIV) return result1;
    return new JustResult(and(result.isOK(), result1.isOK()));
  }

  @Override
  public Result visitFieldAcc(Abstract.FieldAccExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    Result pathResult = checkPath(expr, other);
    if (pathResult != null) return pathResult;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;
    if (!(other instanceof FieldAccExpression)) return new JustResult(CMP.NOT_EQUIV);

    FieldAccExpression otherFieldAcc = (FieldAccExpression) other;
    Result result = expr.getExpression().accept(this, otherFieldAcc.getExpression());
    if (result.isOK() == CMP.NOT_EQUIV) return result;
    if (expr.getDefinition() != null) {
      if (!expr.getDefinition().equals(otherFieldAcc.getDefinition())) return new JustResult(CMP.NOT_EQUIV);
    } else
    if (expr.getIndex() != -1) {
      if (expr.getIndex() != otherFieldAcc.getIndex()) return new JustResult(CMP.NOT_EQUIV);
    } else {
      if (!expr.getName().equals(otherFieldAcc.getName())) return new JustResult(CMP.NOT_EQUIV);
    }
    return result;
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    if (!(other instanceof ElimExpression)) return new JustResult(CMP.NOT_EQUIV);

    ElimExpression otherElim = (ElimExpression) other;
    if (expr.getElimType() != otherElim.getElimType() || expr.getClauses().size() != otherElim.getClauses().size()) {
      return new JustResult(CMP.NOT_EQUIV);
    }
    Result result = expr.getExpression().accept(this, otherElim.getExpression());
    if (result.isOK() == CMP.NOT_EQUIV) return result;

    CMP cmp = result.isOK();
    MaybeResult maybeResult = null;
    for (int i = 0; i < expr.getClauses().size(); ++i) {
      result = visitClause(expr.getClause(i), otherElim.getClause(i));
      if (result.isOK() == CMP.NOT_EQUIV) {
        if (result instanceof MaybeResult) {
          if (maybeResult == null) {
            maybeResult = (MaybeResult) result;
          }
        } else {
          return result;
        }
      }
      cmp = and(cmp, result.isOK());
    }

    if (expr.getOtherwise() == otherElim.getOtherwise()) return maybeResult == null ? new JustResult(cmp) : maybeResult;
    if (expr.getOtherwise() == null || otherElim.getOtherwise() == null || expr.getOtherwise().getArrow() != otherElim.getOtherwise().getArrow()) return new JustResult(CMP.NOT_EQUIV);
    result = expr.getExpression().accept(this, otherElim.getOtherwise().getExpression());
    if (result.isOK() == CMP.NOT_EQUIV) {
      if (result instanceof MaybeResult) {
        if (maybeResult == null) {
          maybeResult = (MaybeResult) result;
        }
      } else {
        return result;
      }
    }
    return maybeResult == null ? new JustResult(and(cmp, result.isOK())) : maybeResult;
  }

  public Result visitClause(Abstract.Clause clause, Clause other) {
    if (clause == other) return new JustResult(CMP.EQUALS);
    if (clause == null || other == null) return new JustResult(CMP.NOT_EQUIV);

    if (!other.getName().equals(clause.getName()) || clause.getArrow() != other.getArrow()) return new JustResult(CMP.NOT_EQUIV);
    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression expr1 = lamArgs(clause.getExpression(), args1);
    List<Abstract.Expression> args2 = new ArrayList<>();
    Abstract.Expression expr2 = lamArgs(other.getExpression(), args2);
    if (args1.size() != args2.size()) return new JustResult(CMP.NOT_EQUIV);
    Result result = expr1.accept(this, (Expression) expr2);
    if (result.isOK() == CMP.NOT_EQUIV) return result;

    CMP cmp = result.isOK();
    MaybeResult maybeResult = null;
    for (int i = 0; i < args1.size(); ++i) {
      if (args1.get(i) != null && args2.get(i) != null) {
        result = args1.get(i).accept(this, (Expression) args2.get(i));
        if (result.isOK() == CMP.NOT_EQUIV) {
          if (result instanceof MaybeResult) {
            if (maybeResult == null) {
              maybeResult = (MaybeResult) result;
            }
          } else {
            return result;
          }
        }
        cmp = and(cmp, result.isOK());
      }
    }

    return maybeResult == null ? new JustResult(cmp) : maybeResult;
  }
}
