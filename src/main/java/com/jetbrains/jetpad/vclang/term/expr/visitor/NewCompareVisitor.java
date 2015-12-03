package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.CompareResult;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.Equations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class NewCompareVisitor extends BaseExpressionVisitor<Expression, CompareResult> {
  private final Equations myEquations;

  private NewCompareVisitor(Equations equations) {
    myEquations = equations;
  }

  private class NumberOfLambdas {
    int number;
    Expression body;

    public NumberOfLambdas(int number, Expression body) {
      this.number = number;
      this.body = body;
    }
  }

  private NumberOfLambdas getNumberOfLambdas(Expression expr) {
    if (!(expr instanceof LamExpression)) {
      return new NumberOfLambdas(0, expr);
    }
    LamExpression lamExpr = (LamExpression) expr;
    NumberOfLambdas result = getNumberOfLambdas(lamExpr.getBody());
    for (Argument arg : lamExpr.getArguments()) {
      if (arg instanceof TelescopeArgument) {
        result.number += ((TelescopeArgument) arg).getNames().size();
      } else {
        result.number++;
      }
    }
    return result;
  }

  public CompareResult compare(Expression expr1, Expression expr2) {
    if (expr1 == expr2 || expr1 instanceof ErrorExpression || expr2 instanceof ErrorExpression) {
      return CompareResult.EQUIV;
    }

    if (expr1 instanceof LamExpression || expr2 instanceof LamExpression) {
      NumberOfLambdas number1 = getNumberOfLambdas(expr1);
      NumberOfLambdas number2 = getNumberOfLambdas(expr2);
      boolean firstGreater = number1.number >= number2.number;
      NumberOfLambdas maxNumber = firstGreater ? number1 : number2;
      NumberOfLambdas minNumber = firstGreater ? number2 : number1;
      Expression maxBody = maxNumber.body;
      int diff = maxNumber.number - minNumber.number;
      for (int i = 0; i < diff; i++) {
        if (!(maxBody instanceof AppExpression)) {
          return CompareResult.NOT_EQUIV;
        }
        AppExpression appExpr = (AppExpression) maxBody;
        if (!(appExpr.getArgument().getExpression() instanceof IndexExpression) || ((IndexExpression) appExpr.getArgument().getExpression()).getIndex() != i) {
          return CompareResult.NOT_EQUIV;
        }
        maxBody = appExpr.getFunction();
      }
      maxBody = maxBody.liftIndex(0, -diff);
      if (maxBody == null) {
        return CompareResult.NOT_EQUIV;
      }
      Equations equations = new Equations();
      CompareResult result = new NewCompareVisitor(equations).compare(firstGreater ? maxBody : minNumber.body, firstGreater ? minNumber.body : maxBody).mustBeEquiv();
      if (!equations.lift(-diff)) {
        return CompareResult.NOT_EQUIV;
      }
      myEquations.add(equations);
      return result;
    }

    // TODO: eta equivalence for paths and tuples
    // TODO: inference var in expr2
    return expr1.accept(this, expr2);
  }

  private boolean isInferVar(Expression expr) {
    // TODO
    return expr instanceof IndexExpression;
  }

  @Override
  public CompareResult visitApp(AppExpression expr1, Expression expr2) {
    List<Expression> args1 = new ArrayList<>();
    Expression fun1 = expr1.getFunction(args1);
    List<Expression> args2 = new ArrayList<>(args1.size());
    Expression fun2 = expr1.getFunction(args2);
    if (isInferVar(fun1) || isInferVar(fun2)) {
      return CompareResult.MAYBE_EQUIV;
    }
    if (args1.size() != args2.size()) {
      return CompareResult.NOT_EQUIV;
    }

    CompareResult result = compare(fun1, fun2).mustBeEquiv();
    if (result == CompareResult.NOT_EQUIV) {
      return CompareResult.NOT_EQUIV;
    }
    for (int i = 0; i < args1.size(); i++) {
      result = compare(args1.get(i), args2.get(i)).mustBeEquiv().and(result);
      if (result == CompareResult.NOT_EQUIV) {
        return CompareResult.NOT_EQUIV;
      }
    }
    return result;
  }

  @Override
  public CompareResult visitDefCall(DefCallExpression expr1, Expression expr2) {
    if (!(expr2 instanceof DefCallExpression)) return CompareResult.NOT_EQUIV;
    return expr1.getDefinition() == ((DefCallExpression) expr2).getDefinition() ? CompareResult.EQUIV : CompareResult.NOT_EQUIV;
  }

  @Override
  public CompareResult visitClassCall(ClassCallExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ClassCallExpression) || expr1.getDefinition() != ((ClassCallExpression) expr2).getDefinition()) return CompareResult.NOT_EQUIV;
    Map<ClassField, ClassCallExpression.ImplementStatement> implStats1 = expr1.getImplementStatements();
    Map<ClassField, ClassCallExpression.ImplementStatement> implStats2 = ((ClassCallExpression) expr2).getImplementStatements();

    CompareResult result = CompareResult.EQUIV;
    for (ClassField field : expr1.getDefinition().getFields()) {
      ClassCallExpression.ImplementStatement implStat1 = implStats1.get(field);
      ClassCallExpression.ImplementStatement implStat2 = implStats2.get(field);

      if (implStat1 == null && implStat2 == null) {
        continue;
      }
      if (implStat2 == null) {
        result = CompareResult.LESS.and(result);
        if (result == CompareResult.NOT_EQUIV) {
          return CompareResult.NOT_EQUIV;
        }
      } else
      if (implStat1 == null) {
        result = CompareResult.GREATER.and(result);
        if (result == CompareResult.NOT_EQUIV) {
          return CompareResult.NOT_EQUIV;
        }
      } else {
        if (implStat1.term != null && implStat2.term != null) {
          CompareResult result1 = compare(implStat1.term, implStat2.term).mustBeEquiv();
          result = result1.and(result);
          if (result == CompareResult.NOT_EQUIV) {
            return CompareResult.NOT_EQUIV;
          }
        } else
        if (implStat1.term != null || implStat2.term != null) {
          return CompareResult.NOT_EQUIV;
        }

        if (implStat1.type == null && implStat2.type == null) {
          continue;
        }
        Expression type1 = implStat1.type;
        Expression type2 = implStat2.type;
        if (type1 == null) {
          if (!result.isEquiv()) {
            continue;
          }
          type1 = field.getBaseType();
        }
        if (type2 == null) {
          if (!result.isEquiv()) {
            continue;
          }
          type2 = field.getBaseType();
        }
        result = compare(type1, type2).and(result);
        if (result == CompareResult.NOT_EQUIV) {
          return CompareResult.NOT_EQUIV;
        }
      }
    }
    return result;
  }

  @Override
  public CompareResult visitIndex(IndexExpression expr1, Expression expr2) {
    // TODO: inference variables
    if (!(expr2 instanceof IndexExpression)) return CompareResult.NOT_EQUIV;
    return expr1.getIndex() == ((IndexExpression) expr2).getIndex() ? CompareResult.EQUIV : CompareResult.NOT_EQUIV;
  }

  @Override
  public CompareResult visitLam(LamExpression expr1, Expression expr2) {
    throw new IllegalStateException();
  }

  private CompareResult compareTypeArguments(List<? extends Argument> args1, List<? extends Argument> args2, NewCompareVisitor visitor) {
    if (args1.size() != args2.size()) {
      return CompareResult.NOT_EQUIV;
    }

    CompareResult result = CompareResult.EQUIV;
    for (int i = 0; i < args1.size(); i++) {
      if (args1.get(i) instanceof TypeArgument && args2.get(i) instanceof TypeArgument) {
        result = visitor.compare(((TypeArgument) args1.get(i)).getType(), ((TypeArgument) args2.get(i)).getType()).mustBeEquiv().and(result);
      }
      if (result == CompareResult.NOT_EQUIV) {
        return CompareResult.NOT_EQUIV;
      }
      if (!visitor.myEquations.lift(-i)) {
        return CompareResult.NOT_EQUIV;
      }
      myEquations.add(visitor.myEquations);
      visitor.myEquations.clear();
    }

    return result;
  }

  @Override
  public CompareResult visitPi(PiExpression expr1, Expression expr2) {
    if (!(expr2 instanceof PiExpression)) return CompareResult.NOT_EQUIV;
    List<TypeArgument> args1 = new ArrayList<>();
    splitArguments(expr1, args1, null);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    splitArguments(expr2, args2, null);

    Equations equations = new Equations();
    NewCompareVisitor visitor = new NewCompareVisitor(equations);
    CompareResult result = compareTypeArguments(args1, args2, visitor);
    if (result == CompareResult.NOT_EQUIV) {
      return CompareResult.NOT_EQUIV;
    }

    result = visitor.compare(expr1.getCodomain(), ((PiExpression) expr2).getCodomain()).mustBeEquiv().and(result);
    if (result == CompareResult.NOT_EQUIV) {
      return CompareResult.NOT_EQUIV;
    }
    if (!equations.lift(-args1.size())) {
      return CompareResult.NOT_EQUIV;
    }
    myEquations.add(equations);
    return result;
  }

  @Override
  public CompareResult visitUniverse(UniverseExpression expr1, Expression expr2) {
    if (!(expr2 instanceof UniverseExpression)) return CompareResult.NOT_EQUIV;
    switch (expr1.getUniverse().compare(((UniverseExpression) expr2).getUniverse())) {
      case EQUALS:
        return CompareResult.EQUIV;
      case LESS:
        return CompareResult.LESS;
      case GREATER:
        return CompareResult.GREATER;
      case NOT_COMPARABLE:
        return CompareResult.NOT_EQUIV;
    }
    throw new IllegalStateException();
  }

  @Override
  public CompareResult visitInferHole(InferHoleExpression expr1, Expression expr2) {
    return null;
  }

  @Override
  public CompareResult visitError(ErrorExpression expr1, Expression expr2) {
    return CompareResult.NOT_EQUIV;
  }

  @Override
  public CompareResult visitTuple(TupleExpression expr1, Expression expr2) {
    throw new IllegalStateException();
  }

  @Override
  public CompareResult visitSigma(SigmaExpression expr1, Expression expr2) {
    if (!(expr2 instanceof SigmaExpression)) return CompareResult.NOT_EQUIV;
    List<TypeArgument> args1 = new ArrayList<>();
    splitArguments(expr1.getArguments(), args1);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    splitArguments(((SigmaExpression) expr2).getArguments(), args2);
    return compareTypeArguments(args1, args2, new NewCompareVisitor(new Equations()));
  }

  private CompareResult visitClause(Clause clause1, Clause clause2) {
    if (clause1 == clause2) return CompareResult.EQUIV;
    if (clause1 == null || clause2 == null) return CompareResult.NOT_EQUIV;

    if (!clause2.getPatterns().equals(clause1.getPatterns()) || clause1.getArrow() != clause2.getArrow()) {
      return CompareResult.NOT_EQUIV;
    }

    return compare(clause1.getExpression(), clause2.getExpression()).mustBeEquiv();
  }

  @Override
  public CompareResult visitElim(ElimExpression expr1, Expression expr2) {
    if (expr1 == expr2) return CompareResult.EQUIV;
    if (!(expr2 instanceof ElimExpression)) return CompareResult.NOT_EQUIV;

    ElimExpression elimExpr2 = (ElimExpression) expr2;
    if (expr1.getClauses().size() != elimExpr2.getClauses().size() || expr1.getExpressions().size() != elimExpr2.getExpressions().size()) {
      return CompareResult.NOT_EQUIV;
    }

    CompareResult result = CompareResult.EQUIV;
    for (int i = 0; i < expr1.getExpressions().size(); i++) {
      result = compare(expr1.getExpressions().get(i), elimExpr2.getExpressions().get(i)).mustBeEquiv().and(result);
      if (result == CompareResult.NOT_EQUIV) {
        return CompareResult.NOT_EQUIV;
      }
    }

    for (int i = 0; i < expr1.getClauses().size(); i++) {
      result = visitClause(expr1.getClauses().get(i), elimExpr2.getClauses().get(i)).mustBeEquiv().and(result);
      if (result == CompareResult.NOT_EQUIV) {
        return CompareResult.NOT_EQUIV;
      }
    }

    return result;
  }

  @Override
  public CompareResult visitProj(ProjExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ProjExpression)) return CompareResult.NOT_EQUIV;
    ProjExpression projExpr2 = (ProjExpression) expr2;
    if (expr1.getField() != projExpr2.getField()) {
      return CompareResult.NOT_EQUIV;
    }
    return compare(expr1.getExpression(), projExpr2.getExpression()).mustBeEquiv();
  }

  @Override
  public CompareResult visitNew(NewExpression expr1, Expression expr2) {
    if (!(expr2 instanceof NewExpression)) return CompareResult.NOT_EQUIV;
    return compare(expr1.getExpression(), ((NewExpression) expr2).getExpression()).mustBeEquiv();
  }

  @Override
  public CompareResult visitLet(LetExpression expr1, Expression expr2) {
    if (!(expr2 instanceof LetExpression)) {
      return CompareResult.NOT_EQUIV;
    }
    LetExpression letExpr1 = expr1.mergeNestedLets();
    LetExpression letExpr2 = ((LetExpression) expr2).mergeNestedLets();
    if (letExpr1.getClauses().size() != letExpr2.getClauses().size()) {
      return CompareResult.NOT_EQUIV;
    }

    CompareResult result = CompareResult.EQUIV;
    for (int i = 0; i < letExpr1.getClauses().size(); i++) {
      // TODO
      List<TypeArgument> argsT1 = new ArrayList<>();
      for (Argument argument : letExpr1.getClauses().get(i).getArguments()) {
        if (argument instanceof TypeArgument) {
          argsT1.add((TypeArgument) argument);
        }
      }
      List<TypeArgument> argsT2 = new ArrayList<>();
      for (Argument argument : letExpr2.getClauses().get(i).getArguments()) {
        if (argument instanceof TypeArgument) {
          argsT2.add((TypeArgument) argument);
        }
      }
      List<TypeArgument> args1 = new ArrayList<>();
      splitArguments(argsT1, args1);
      List<TypeArgument> args2 = new ArrayList<>(args1.size());
      splitArguments(argsT2, args2);

      Equations equations = new Equations();
      NewCompareVisitor visitor = new NewCompareVisitor(equations);
      result = compareTypeArguments(args1, args2, visitor).mustBeEquiv().and(result);
      if (result == CompareResult.NOT_EQUIV) {
        return CompareResult.NOT_EQUIV;
      }

      result = visitor.compare(letExpr1.getExpression(), letExpr2.getExpression()).mustBeEquiv().and(result);
      if (result == CompareResult.NOT_EQUIV) {
        return CompareResult.NOT_EQUIV;
      }
      if (!equations.lift(-args1.size())) {
        return CompareResult.NOT_EQUIV;
      }
      myEquations.add(equations);
    }

    return compare(letExpr1.getExpression(), letExpr2.getExpression()).mustBeEquiv().and(result);
  }
}
