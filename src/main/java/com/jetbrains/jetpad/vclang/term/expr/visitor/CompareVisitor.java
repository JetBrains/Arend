package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

public class CompareVisitor implements AbstractExpressionVisitor<Expression, Boolean> {
  private final List<Equation> myEquations;
  private CMP myCmp;

  public enum CMP { EQ, GEQ, LEQ }

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
    public Abstract.Expression expression;

    public Equation(Abstract.InferHoleExpression hole, Abstract.Expression expression) {
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

  @Override
  public Boolean visitApp(Abstract.AppExpression expr, Expression other) {
    if (expr == other) return true;
    if (!(other instanceof AppExpression)) return false;
    AppExpression otherApp = (AppExpression) other;
    return expr.getFunction().accept(this, otherApp.getFunction()) && expr.getArgument().accept(this, otherApp.getArgument());
  }

  @Override
  public Boolean visitDefCall(Abstract.DefCallExpression expr, Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.DefCallExpression && expr.getDefinition().equals(((Abstract.DefCallExpression) other).getDefinition());
  }

  @Override
  public Boolean visitIndex(Abstract.IndexExpression expr, Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.IndexExpression && expr.getIndex() == ((Abstract.IndexExpression) other).getIndex();
  }

  @Override
  public Boolean visitLam(Abstract.LamExpression expr, Expression other) {
    if (expr == other) return true;
    if (!(other instanceof Abstract.LamExpression)) return false;
    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression body1 = lamArgs(expr, args1);
    List<Abstract.Expression> args2 = new ArrayList<>();
    Abstract.Expression body2 = lamArgs(other, args2);
    if (args1.size() != args2.size() || !body1.accept(this, (Expression) body2)) return false;
    for (int i = 0; i < args1.size(); ++i) {
      if (args1.get(i) != null && args2.get(i) != null && !args1.get(i).accept(this, (Expression) args2.get(i))) return false;
    }
    return true;
  }

  @Override
  public Boolean visitNat(Abstract.NatExpression expr, Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.NatExpression;
  }

  @Override
  public Boolean visitNelim(Abstract.NelimExpression expr, Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.NelimExpression;
  }

  @Override
  public Boolean visitPi(Abstract.PiExpression expr, Expression other) {
    if (expr == other) return true;
    if (!(other instanceof Abstract.PiExpression)) return false;

    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression codomain1 = piArgs(expr, args1);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    Expression codomain2 = other.splitAt(args1.size(), args2);
    if (args1.size() != args2.size()) return false;

    myCmp = not(myCmp);
    for (int i = 0; i < args1.size(); ++i) {
      if (i > 0 && args1.get(i) == args1.get(i - 1) && args2.get(i).getType() == args2.get(i - 1).getType()) continue;
      if (!args1.get(i).accept(this, args2.get(i).getType())) return false;
    }
    myCmp = not(myCmp);

    return codomain1.accept(this, codomain2);
  }

  @Override
  public Boolean visitSuc(Abstract.SucExpression expr, Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.SucExpression;
  }

  @Override
  public Boolean visitUniverse(Abstract.UniverseExpression expr, Expression other) {
    if (expr == other) return true;
    if (!(other instanceof Abstract.UniverseExpression)) return false;
    Universe.Cmp cmp = expr.getUniverse().compare(((Abstract.UniverseExpression) other).getUniverse());
    if (myCmp == CMP.LEQ) return cmp == Universe.Cmp.EQUALS || cmp == Universe.Cmp.LESS;
    if (myCmp == CMP.GEQ) return cmp == Universe.Cmp.EQUALS || cmp == Universe.Cmp.GREATER;
    return cmp == Universe.Cmp.EQUALS;
  }

  @Override
  public Boolean visitVar(Abstract.VarExpression expr, Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.VarExpression && expr.getName().equals(((Abstract.VarExpression) other).getName());
  }

  @Override
  public Boolean visitZero(Abstract.ZeroExpression expr, Expression other) {
    if (expr == other) return true;
    return other instanceof Abstract.ZeroExpression;
  }

  @Override
  public Boolean visitError(Abstract.ErrorExpression expr, Expression other) {
    return true;
  }

  @Override
  public Boolean visitInferHole(Abstract.InferHoleExpression expr, Expression other) {
    myEquations.add(new Equation(expr, other));
    return true;
  }

  @Override
  public Boolean visitTuple(Abstract.TupleExpression expr, Expression other) {
    if (expr == other) return true;
    if (!(other instanceof TupleExpression)) return false;

    TupleExpression otherTuple = (TupleExpression) other;
    if (expr.getFields().size() != otherTuple.getFields().size()) return false;
    for (int i = 0; i < expr.getFields().size(); ++i) {
      if (!expr.getField(i).accept(this, otherTuple.getField(i))) return false;
    }
    return true;
  }

  @Override
  public Boolean visitSigma(Abstract.SigmaExpression expr, Expression other) {
    if (expr == other) return true;
    if (!(other instanceof SigmaExpression)) return false;

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

    if (args.size() != otherArgs.size()) return false;
    for (int i = 0; i < args.size(); ++i) {
      if (i > 0 && args.get(i) == args.get(i - 1) && otherArgs.get(i) == otherArgs.get(i - 1)) continue;
      if (!args.get(i).accept(this, otherArgs.get(i))) return false;
    }
    return true;
  }

  @Override
  public Boolean visitBinOp(Abstract.BinOpExpression expr, Expression other) {
    if (expr == other) return true;
    if (!(other instanceof BinOpExpression)) return false;

    BinOpExpression otherBinOp = (BinOpExpression) other;
    return expr.getBinOp().equals(otherBinOp.getBinOp()) && expr.getLeft().accept(this, otherBinOp.getLeft()) && expr.getRight().accept(this, otherBinOp.getRight());
  }

  @Override
  public Boolean visitElim(Abstract.ElimExpression expr, Expression other) {
    if (expr == other) return true;
    if (!(other instanceof ElimExpression)) return false;

    ElimExpression otherElim = (ElimExpression) other;
    if (expr.getElimType() != otherElim.getElimType() || expr.getClauses().size() != otherElim.getClauses().size() || !expr.getExpression().accept(this, otherElim.getExpression())) return false;
    for (int i = 0; i < expr.getClauses().size(); ++i) {
      if (!visitClause(expr.getClause(i), otherElim.getClause(i))) return false;
    }
    return true;
  }

  public boolean visitClause(Abstract.Clause clause, Clause other) {
    if (!other.getName().equals(clause.getName()) || clause.getArrow() != other.getArrow()) return false;
    List<Abstract.Expression> args1 = new ArrayList<>();
    Abstract.Expression expr1 = lamArgs(clause.getExpression(), args1);
    List<Abstract.Expression> args2 = new ArrayList<>();
    Abstract.Expression expr2 = lamArgs(other.getExpression(), args2);
    if (args1.size() != args2.size() || !expr1.accept(this, (Expression) expr2)) return false;
    for (int i = 0; i < args1.size(); ++i) {
      if (args1.get(i) != null && args2.get(i) != null && !args1.get(i).accept(this, (Expression) args2.get(i))) return false;
    }
    return true;
  }
}
