package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Suc;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Zero;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class CompareVisitor implements AbstractExpressionVisitor<Expression, CompareVisitor.Result> {
  private final List<Equation> myEquations;

  public enum CMP { EQUIV, EQUALS, GREATER, LESS, NOT_EQUIV }

  public interface Result {
    CMP isOK();
  }

  private static class EquationsLifter implements AutoCloseable {
    final List<Equation> myEquations;
    int myOldEquationSize;
    int myOn;

    EquationsLifter(List<Equation> equations) {
      this(equations, 0);
    }

    EquationsLifter(List<Equation> equations, int on) {
      myEquations = equations;
      myOldEquationSize = equations.size();
      myOn = on;
    }

    private void doLift(int size, int on) {
      for (int j = size; j < myEquations.size(); ++j) {
        Expression expr1 = myEquations.get(j).expression.liftIndex(0, on);
        if (expr1 != null) {
          myEquations.get(j).expression = expr1;
        } else {
          myEquations.remove(j--);
        }
      }
    }

    void lift(int on) {
      doLift(myOldEquationSize, myOn);
      myOldEquationSize = myEquations.size();
      myOn += on;
    }

    @Override
    public void close() {
      doLift(myOldEquationSize, myOn);
    }
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

  private static void lamArgsToTypes(List<? extends Abstract.Argument> arguments, List<Abstract.Expression> types) {
    for (Abstract.Argument arg : arguments) {
      if (arg instanceof Abstract.TelescopeArgument) {
        Abstract.TelescopeArgument teleArg = (Abstract.TelescopeArgument) arg;
        for (String ignored : teleArg.getNames()) {
          types.add(teleArg.getType());
        }
      } else if (arg instanceof Abstract.NameArgument) {
        types.add(null);
      } else if (arg instanceof Abstract.TypeArgument) {
        types.add(((Abstract.TypeArgument) arg).getType());
      }
    }
  }

  private static Abstract.Expression lamArgs(Abstract.Expression expr, List<Abstract.Expression> args) {
    if (expr instanceof Abstract.LamExpression) {
      Abstract.LamExpression lamExpr = (Abstract.LamExpression) expr;
      lamArgsToTypes(lamExpr.getArguments(), args);
      return lamArgs(lamExpr.getBody(), args);
    } else {
      return expr;
    }
  }

  private static Abstract.Expression piArgs(Abstract.Expression expr, List<Abstract.TypeArgument> args) {
    if (expr instanceof Abstract.PiExpression) {
      Abstract.PiExpression piExpr = (Abstract.PiExpression) expr;
      for (Abstract.Argument arg : piExpr.getArguments()) {
        if (arg instanceof Abstract.TelescopeArgument) {
          args.add((Abstract.TelescopeArgument) arg);
        } else if (arg instanceof Abstract.TypeArgument) {
          args.add((Abstract.TypeArgument) arg);
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
    if (!(other instanceof AppExpression)) {
      return null;
    }
    List<Expression> arguments = new ArrayList<>(4);
    Expression function = other.getFunction(arguments);
    if (!(arguments.size() == 1 && function instanceof DefCallExpression && Prelude.isPathCon(((DefCallExpression) function).getResolvedName().toDefinition()) && arguments.get(0) instanceof LamExpression)) {
      return null;
    }

    List<Expression> args = new ArrayList<>();
    Expression expr1 = ((LamExpression) arguments.get(0)).getBody().getFunction(args);
    if (expr1 instanceof DefCallExpression && Prelude.isAt(((DefCallExpression) expr1).getResolvedName().toDefinition()) && args.size() == 5 && args.get(0) instanceof IndexExpression && ((IndexExpression) args.get(0)).getIndex() == 0) {
      Expression newOther = args.get(1).liftIndex(0, -1);
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
    Result pathResult = checkPath(expr, other);
    if (pathResult != null) return pathResult;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;

    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    Abstract.Expression expr1 = Abstract.getFunction(expr, args);
    if (expr1 instanceof Abstract.InferHoleExpression) {
      return new MaybeResult(expr1);
    }

    if (expr1 instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr1).getResolvedName() != null && Prelude.isPathCon(((Abstract.DefCallExpression) expr1).getResolvedName().toDefinition()) && args.size() == 1 && args.get(0).getExpression() instanceof Abstract.LamExpression) {
      List<Abstract.ArgumentExpression> args1 = new ArrayList<>();
      Abstract.Expression expr2 = Abstract.getFunction(((Abstract.LamExpression) args.get(0).getExpression()).getBody(), args1);
      if (expr2 instanceof Abstract.DefCallExpression && Prelude.isAt(((Abstract.DefCallExpression) expr2).getResolvedName().toDefinition()) && args1.size() == 5 && args1.get(4).getExpression() instanceof Abstract.IndexExpression && ((Abstract.IndexExpression) args1.get(4).getExpression()).getIndex() == 0) {
        List<Equation> equations = new ArrayList<>();
        Result result = args1.get(3).getExpression().accept(new CompareVisitor(equations), other.liftIndex(0, 1));
        if (result.isOK() != CMP.NOT_EQUIV) {
          myEquations.addAll(equations);
          return result;
        }
      }
    }

    List<Expression> otherArgs = new ArrayList<>();
    Expression other1 = other.getFunction(otherArgs);
    if (args.size() != otherArgs.size()) {
      return new JustResult(CMP.NOT_EQUIV);
    }

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

    if (expr instanceof DefCallExpression) {
      if (!(other instanceof DefCallExpression)) return new JustResult(CMP.NOT_EQUIV);
      DefCallExpression otherDefCall = (DefCallExpression) other;

      if (!expr.getResolvedName().equals(otherDefCall.getResolvedName())) {
        return new JustResult(CMP.NOT_EQUIV);
      }

      if (expr.getExpression() == null)
        return new JustResult(otherDefCall.getExpression() == null ? CMP.EQUALS : CMP.NOT_EQUIV);
      return new JustResult(expr.getExpression().equals(otherDefCall.getExpression()) ? CMP.EQUALS : CMP.NOT_EQUIV);
    } else {
      if (other instanceof VarExpression) {
        return new JustResult(expr.getExpression() == null && expr.getName().equals(((VarExpression) other).getName())
            ? CMP.EQUALS : CMP.NOT_EQUIV);
      }

      if (!(other instanceof DefCallExpression)) {
        return new JustResult(CMP.NOT_EQUIV);
      }

      DefCallExpression otherDecCall = (DefCallExpression) other;

      // No dot case
      if (expr.getExpression() == null) {
        return new JustResult(expr.getName().equals(otherDecCall.getName()) ? CMP.EQUALS : CMP.NOT_EQUIV);
      }

      // Dot case
      if (!expr.getName().equals(otherDecCall.getName())) {
        return new JustResult(CMP.NOT_EQUIV);
      }
      return new JustResult(expr.getExpression() == null ? CMP.EQUIV : CMP.NOT_EQUIV);
    }
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression other) {
    return new JustResult(CMP.EQUIV);
  }

  /*
  @Override
  public Result visitClassCall(Abstract.ClassExtExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    Result pathResult = checkPath(expr, other);
    if (pathResult != null) return pathResult;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;
    if (!(expr instanceof ClassCallExpression && other instanceof ClassCallExpression)) return new JustResult(CMP.NOT_EQUIV);

    ClassCallExpression classExt = (ClassCallExpression) expr;
    ClassCallExpression otherClassExt = (ClassCallExpression) other;
    if (classExt.getDefinition() != otherClassExt.getDefinition()) return new JustResult(CMP.NOT_EQUIV);
    if (classExt.getImplementStatements().size() != otherClassExt.getImplementStatements().size()) return new JustResult(CMP.NOT_EQUIV);
    ClassCallExpression smaller;
    ClassCallExpression bigger;
    if (classExt.getImplementStatements().size() > otherClassExt.getImplementStatements().size()) {
      smaller = otherClassExt;
      bigger = classExt;
    } else {
      smaller = classExt;
      bigger = otherClassExt;
    }

    CMP cmp = CMP.EQUALS;
    MaybeResult maybeResult = null;
    smaller_loop:
    for (ClassCallExpression.OverrideElem elem : smaller.getImplementStatements()) {
      for (ClassCallExpression.OverrideElem otherElem : bigger.getImplementStatements()) {
        // TODO
        if (elem.field == otherElem.field) {
          Result result = otherElem.type.accept(this, elem.type);
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

          result = otherElem.term.accept(this, elem.term);
          if (result.isOK() != CMP.EQUALS) {
            return new JustResult(CMP.NOT_EQUIV);
          }

          continue smaller_loop;
        }
      }
      return new JustResult(CMP.NOT_EQUIV);
    }
    return maybeResult == null ? new JustResult(cmp) : maybeResult;
  }
  */

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
    return checkLam(expr, other, new ArrayList<Abstract.Expression>(), new ArrayList<Abstract.Expression>());
  }

  private Result checkLam(Abstract.Expression expr, Expression other, List<Abstract.Expression> args1, List<Abstract.Expression> args2) {
    Abstract.Expression body1 = lamArgs(expr, args1);
    Expression body2 = (Expression) lamArgs(other, args2);

    if (args1.size() == 0 && args2.size() == 0) return null;

    if (args1.size() < args2.size()) {
      for (int i = 0; i < args2.size() - args1.size(); ++i) {
        if (!(body2 instanceof AppExpression && ((AppExpression) body2).getArgument().getExpression() instanceof IndexExpression && ((IndexExpression) ((AppExpression) body2).getArgument().getExpression()).getIndex() == i)) {
          return new JustResult(CMP.NOT_EQUIV);
        }
        body2 = ((AppExpression) body2).getFunction();
      }

      body2 = body2.liftIndex(0, args1.size() - args2.size());
      if (body2 == null) {
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
      Expression expr1 = equations.get(i).expression.liftIndex(0, -args1.size());
      if (expr1 != null) {
        equations.get(i).expression = expr1;
      } else {
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
        Expression expr1 = equations.get(j).expression.liftIndex(0, -i);
        if (expr1 != null) {
          equations.get(j).expression = expr1;
        } else {
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
    try (EquationsLifter lifter = new EquationsLifter(myEquations)) {
      if (expr == other) return new JustResult(CMP.EQUALS);
      if (!(other instanceof PiExpression)) return new JustResult(CMP.NOT_EQUIV);

      List<Abstract.TypeArgument> args1 = new ArrayList<>();
      Abstract.Expression codomain1 = piArgs(expr, args1);
      List<TypeArgument> args2 = new ArrayList<>(numberOfVariables(args1));
      Expression codomain2 = other.splitAt(numberOfVariables(args1), args2, null);

      Result maybeResult = null;
      CMP cmp = CMP.EQUALS;

      Result argsResult = compareTypeArguments(args1, args2);
      if (argsResult.isOK() == CMP.NOT_EQUIV) {
        if (argsResult instanceof MaybeResult) {
          maybeResult = argsResult;
        } else {
          return argsResult;
        }
      }
      cmp = and(cmp, argsResult.isOK());


      lifter.lift(-numberOfVariables(args1));

      Result codomainResult = codomain1.accept(this, codomain2);
      if (codomainResult.isOK() == CMP.NOT_EQUIV) {
        if (codomainResult instanceof MaybeResult) {
          if (maybeResult == null) {
            maybeResult = codomainResult;
          }
        } else {
          return codomainResult;
        }
      }

      cmp = and(not(cmp), codomainResult.isOK());
      return maybeResult != null ? maybeResult : new JustResult(cmp);
    }
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
  public Result visitError(Abstract.ErrorExpression expr, Expression other) {
    return new JustResult(CMP.EQUALS);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression other) {
    myEquations.add(new Equation(expr, other));
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
      if (otherTuple.getFields().get(i) instanceof ProjExpression && ((ProjExpression) otherTuple.getFields().get(i)).getField() == i) {
        Result result = expr.accept(visitor, ((ProjExpression) otherTuple.getFields().get(i)).getExpression());
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
        if (expr.getFields().get(i) instanceof Abstract.ProjExpression && ((Abstract.ProjExpression) expr.getFields().get(i)).getField() == i) {
          Result result = ((Abstract.ProjExpression) expr.getFields().get(i)).getExpression().accept(this, other);
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
      Result result = expr.getFields().get(i).accept(this, otherTuple.getFields().get(i));
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
    return compareTypeArguments(expr.getArguments(), ((SigmaExpression) other).getArguments());
  }

  private List<Abstract.Expression> splitConcreteTypeArgumentsTypes(List<? extends Abstract.TypeArgument> arguments) {
    List<Abstract.Expression> args = new ArrayList<>();
    for (Abstract.TypeArgument arg : arguments) {
      if (arg instanceof Abstract.TelescopeArgument) {
        for (String ignored : ((Abstract.TelescopeArgument) arg).getNames()) {
          args.add(arg.getType());
        }
      } else {
        args.add(arg.getType());
      }
    }
    return args;
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
    if (!(other instanceof AppExpression)) return new JustResult(CMP.NOT_EQUIV);
    AppExpression otherApp1 = (AppExpression) other;
    if (!(otherApp1.getFunction() instanceof AppExpression)) return new JustResult(CMP.NOT_EQUIV);
    AppExpression otherApp2 = (AppExpression) otherApp1.getFunction();
    if (!(otherApp2.getFunction() instanceof DefCallExpression)) return new JustResult(CMP.NOT_EQUIV);
    DefCallExpression otherDefCall = (DefCallExpression) otherApp2.getFunction();

    if (!expr.getResolvedBinOpName().equals(otherDefCall.getResolvedName())) return new JustResult(CMP.NOT_EQUIV);
    Result result = expr.getLeft().accept(this, otherApp2.getArgument().getExpression());
    if (result.isOK() == CMP.NOT_EQUIV) return result;
    Result result1 = expr.getRight().accept(this, otherApp1.getArgument().getExpression());
    if (result1.isOK() == CMP.NOT_EQUIV) return result1;
    return new JustResult(and(result.isOK(), result1.isOK()));
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Expression other) {
    return expr.getSequence().isEmpty() ? expr.getLeft().accept(this, other) : new JustResult(CMP.NOT_EQUIV);
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    Result pathResult = checkPath(expr, other);
    if (pathResult != null) return pathResult;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;
    if (!(other instanceof ProjExpression)) return new JustResult(CMP.NOT_EQUIV);

    ProjExpression otherProj = (ProjExpression) other;
    if (expr.getField() != otherProj.getField()) return new JustResult(CMP.NOT_EQUIV);
    return expr.getExpression().accept(this, otherProj.getExpression());
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression other) {
    if (expr == other) return new JustResult(CMP.EQUALS);
    Result pathResult = checkPath(expr, other);
    if (pathResult != null) return pathResult;
    Result tupleResult = checkTuple(expr, other);
    if (tupleResult != null) return tupleResult;
    Result lamResult = checkLam(expr, other);
    if (lamResult != null) return lamResult;
    if (!(other instanceof NewExpression)) return new JustResult(CMP.NOT_EQUIV);
    return expr.getExpression().accept(this, ((NewExpression) other).getExpression());
  }

  Result compareTypeArguments(List<? extends Abstract.TypeArgument> args1, List<TypeArgument> args2) {
    try (EquationsLifter lifter = new EquationsLifter(myEquations)) {
      List<Abstract.Expression> types1 = splitConcreteTypeArgumentsTypes(args1);
      List<TypeArgument> args2Splitted = splitArguments(args2);

      if (types1.size() != args2Splitted.size())
        return new JustResult(CMP.NOT_EQUIV);
      CMP cmp = CMP.EQUALS;
      MaybeResult maybeResult = null;
      for (int i = 0; i < types1.size(); ++i) {

        Result result;
        if (i > 0 && types1.get(i) == types1.get(i - 1)) {
          Expression downLiftedType2 = args2Splitted.get(i).getType().liftIndex(0, -1);
          if (downLiftedType2 == null) {
            return new JustResult(CMP.NOT_EQUIV);
          }
          lifter.lift(1);
          result = types1.get(i).accept(this, downLiftedType2);
        } else {
          result = types1.get(i).accept(this, args2Splitted.get(i).getType());
        }


        if (result.isOK() == CMP.NOT_EQUIV) {
          if (result instanceof MaybeResult) {
            if (maybeResult == null)
              maybeResult = (MaybeResult) result;
          } else {
            return result;
          }
        }
        cmp = and(cmp, result.isOK());
        lifter.lift(-1);
      }

      return maybeResult == null ? new JustResult(cmp) : maybeResult;
    }
  }

  private Result visitLet(List<LetClause> clauses, Expression expr, List<LetClause> otherClauses, Expression other) {
    try (EquationsLifter lifter = new EquationsLifter(myEquations)) {
      if (otherClauses.size() != clauses.size())
        return new JustResult(CMP.NOT_EQUIV);

      CMP cmp = CMP.EQUALS;
      for (int i = 0; i < clauses.size(); i++) {
        List<Abstract.TypeArgument> letTypeArgs = new ArrayList<>();
        List<TypeArgument> otherTypeArgs = new ArrayList<>();
        for (Abstract.Argument arg : clauses.get(i).getArguments()) {
          letTypeArgs.add((Abstract.TypeArgument) arg);
        }
        for (Argument arg : otherClauses.get(i).getArguments()) {
          otherTypeArgs.add((TypeArgument) arg);
        }

        Result result = compareTypeArguments(letTypeArgs, otherTypeArgs);
        if (result.isOK() != CMP.EQUIV && result.isOK() != CMP.EQUALS)
          return new JustResult(CMP.NOT_EQUIV);
        cmp = and(cmp, result.isOK());

        try (EquationsLifter ignore = new EquationsLifter(myEquations, letTypeArgs.size())) {
          result = new JustResult(CMP.EQUIV); // clauses.get(i).getElimTree().accept(this, otherClauses.get(i).getElimTree());
        }

        if (result.isOK() != CMP.EQUIV && result.isOK() != CMP.EQUALS)
          return new JustResult(CMP.NOT_EQUIV);
        cmp = and(cmp, result.isOK());

        lifter.lift(-1);
      }
      Result result = expr.accept(this, other);
      if (result.isOK() == CMP.NOT_EQUIV)
        return result;
      return new JustResult(and(cmp, result.isOK()));
    }
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Expression other) {
    if (expr == other)
      return new JustResult(CMP.EQUALS);
    if (!(other instanceof LetExpression))
      return new JustResult(CMP.NOT_EQUIV);
    if (!(expr instanceof LetExpression))
      return new JustResult(CMP.NOT_EQUIV);
    LetExpression otherLet = ((LetExpression) other).mergeNestedLets();
    LetExpression letExpression = (LetExpression) expr;

    List<LetClause> exprLetClauses = new ArrayList<>(letExpression.getClauses());
    Expression exprExpression = letExpression.getExpression();
    while (exprExpression instanceof LetExpression) {
      exprLetClauses.addAll(((LetExpression) exprExpression).getClauses());
      exprExpression = ((LetExpression) exprExpression).getExpression();
    }
    return visitLet(exprLetClauses, exprExpression, otherLet.getClauses(), otherLet.getExpression());
  }

  @Override
  public Result visitNumericLiteral(Abstract.NumericLiteral expr, Expression other) {
    if (expr == other) {
      return new JustResult(CMP.EQUALS);
    }
    Expression expr1 = Zero();
    int number = expr.getNumber();
    for (int i = 0; i < number; ++i) {
      expr1 = Suc(expr1);
    }
    return new JustResult(expr1.equals(other) ? CMP.EQUALS : CMP.NOT_EQUIV);
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression other) {
    return new JustResult(CMP.NOT_EQUIV);
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Expression params) {
    return new JustResult(CMP.NOT_EQUIV);
  }
}
