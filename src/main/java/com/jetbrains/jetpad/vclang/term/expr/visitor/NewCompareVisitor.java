package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.Equations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class NewCompareVisitor extends BaseExpressionVisitor<Expression, NewCompareVisitor.Result> {
  private final Equations myEquations;
  private CMP myCMP;

  private NewCompareVisitor(Equations equations, CMP cmp) {
    myEquations = equations;
    myCMP = cmp;
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

  private Expression lamEtaReduce(Expression expr, int vars) {
    for (int i = 0; i < vars; i++) {
      if (!(expr instanceof AppExpression)) {
        return null;
      }
      AppExpression appExpr = (AppExpression) expr;
      if (!(appExpr.getArgument().getExpression() instanceof IndexExpression) || ((IndexExpression) appExpr.getArgument().getExpression()).getIndex() != i) {
        return null;
      }
      expr = appExpr.getFunction();
    }
    return expr.liftIndex(0, -vars);
  }

  private Expression pathEtaReduce(AppExpression expr) {
    if (!(expr.getFunction() instanceof ConCallExpression && ((ConCallExpression) expr.getFunction()).getDefinition() != Prelude.PATH_CON)) {
      return null;
    }
    NumberOfLambdas numberOfLambdas = getNumberOfLambdas(expr.getArgument().getExpression());
    if (numberOfLambdas.number < 1) {
      return null;
    }
    Expression atExpr = lamEtaReduce(numberOfLambdas.body, numberOfLambdas.number - 1);
    if (atExpr == null) {
      return null;
    }

    List<Expression> atArgs = new ArrayList<>(5);
    Expression atFun = atExpr.getFunction(atArgs);
    if (!(atArgs.size() == 5 && atArgs.get(0) instanceof IndexExpression && ((IndexExpression) atArgs.get(0)).getIndex() == 0 && atFun instanceof FunCallExpression && ((FunCallExpression) atFun).getDefinition() == Prelude.AT)) {
      return null;
    }
    return atArgs.get(1).liftIndex(0, -1);
  }

  public Result compare(Expression expr1, Expression expr2) {
    if (expr1 == expr2 || expr1 instanceof ErrorExpression || expr2 instanceof ErrorExpression) {
      return Result.YES;
    }

    if (expr1 instanceof LamExpression || expr2 instanceof LamExpression) {
      NumberOfLambdas number1 = getNumberOfLambdas(expr1);
      NumberOfLambdas number2 = getNumberOfLambdas(expr2);
      boolean firstGreater = number1.number >= number2.number;
      NumberOfLambdas maxNumber = firstGreater ? number1 : number2;
      NumberOfLambdas minNumber = firstGreater ? number2 : number1;
      Expression maxBody = maxNumber.body;

      int diff = maxNumber.number - minNumber.number;
      maxBody = lamEtaReduce(maxBody, diff);
      if (maxBody == null) {
        return Result.NO;
      }
      Equations equations = new Equations();
      Result result = new NewCompareVisitor(equations, CMP.EQ).compare(firstGreater ? maxBody : minNumber.body, firstGreater ? minNumber.body : maxBody);
      if (result == Result.NO) {
        return Result.NO;
      }
      if (!equations.lift(-diff)) {
        return Result.MAYBE;
      }
      myEquations.add(equations);
      return result;
    }

    if (expr1 instanceof AppExpression) {
      Expression expr = pathEtaReduce((AppExpression) expr1);
      if (expr != null) {
        myCMP = CMP.EQ;
        return compare(expr, expr2);
      }
    }
    if (expr2 instanceof AppExpression) {
      Expression expr = pathEtaReduce((AppExpression) expr2);
      if (expr != null) {
        myCMP = CMP.EQ;
        return compare(expr1, expr);
      }
    }

    // TODO: inference var in expr2
    return expr1.accept(this, expr2);
  }

  private boolean isInferVar(Expression expr) {
    // TODO
    return expr instanceof IndexExpression;
  }

  @Override
  public Result visitApp(AppExpression expr1, Expression expr2) {
    List<Expression> args1 = new ArrayList<>();
    Expression fun1 = expr1.getFunction(args1);
    List<Expression> args2 = new ArrayList<>(args1.size());
    Expression fun2 = expr1.getFunction(args2);
    if (isInferVar(fun1) || isInferVar(fun2)) {
      return Result.MAYBE;
    }
    if (args1.size() != args2.size()) {
      return Result.NO;
    }

    myCMP = CMP.EQ;
    Result result = compare(fun1, fun2);
    if (result == Result.NO) {
      return Result.NO;
    }
    for (int i = 0; i < args1.size(); i++) {
      result = compare(args1.get(i), args2.get(i)).and(result);
      if (result == Result.NO) {
        return Result.NO;
      }
    }
    return result;
  }

  @Override
  public Result visitDefCall(DefCallExpression expr1, Expression expr2) {
    if (!(expr2 instanceof DefCallExpression)) return Result.NO;
    return expr1.getDefinition() == ((DefCallExpression) expr2).getDefinition() ? Result.YES : Result.NO;
  }

  @Override
  public Result visitClassCall(ClassCallExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ClassCallExpression) || expr1.getDefinition() != ((ClassCallExpression) expr2).getDefinition()) return Result.NO;
    Map<ClassField, ClassCallExpression.ImplementStatement> implStats1 = expr1.getImplementStatements();
    Map<ClassField, ClassCallExpression.ImplementStatement> implStats2 = ((ClassCallExpression) expr2).getImplementStatements();
    if (myCMP == CMP.EQ && implStats1.size() != implStats2.size() ||
        myCMP == CMP.LE && implStats1.size() <  implStats2.size() ||
        myCMP == CMP.GE && implStats1.size() >  implStats2.size()) {
      return Result.NO;
    }
    Map<ClassField, ClassCallExpression.ImplementStatement> minImplStats = implStats1.size() <= implStats2.size() ? implStats1 : implStats2;
    Map<ClassField, ClassCallExpression.ImplementStatement> maxImplStats = implStats1.size() <= implStats2.size() ? implStats2 : implStats1;

    CMP oldCMP = myCMP;
    Result result = Result.YES;
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> entry : minImplStats.entrySet()) {
      ClassCallExpression.ImplementStatement maxStat = maxImplStats.get(entry.getKey());
      if (maxStat == null) {
        return Result.NO;
      }
      ClassCallExpression.ImplementStatement implStat1 = implStats1.size() <= implStats2.size() ? entry.getValue() : maxStat;
      ClassCallExpression.ImplementStatement implStat2 = implStats1.size() <= implStats2.size() ? maxStat : entry.getValue();

      if (implStat1.term != null && implStat2.term != null) {
        myCMP = CMP.EQ;
        result = compare(implStat1.term, implStat2.term).and(result);
        if (result == Result.NO) {
          return Result.NO;
        }
      } else
      if (implStat1.term != null || implStat2.term != null) {
        return Result.NO;
      }
      myCMP = oldCMP;

      if (implStat1.type == null && implStat2.type == null) {
        continue;
      }
      Expression type1 = implStat1.type;
      Expression type2 = implStat2.type;
      if (type1 == null) {
        if (myCMP == CMP.GE) {
          continue;
        }
        type1 = entry.getKey().getBaseType();
      }
      if (type2 == null) {
        if (myCMP == CMP.LE) {
          continue;
        }
        type2 = entry.getKey().getBaseType();
      }
      result = compare(type1, type2).and(result);
      if (result == Result.NO) {
        return Result.NO;
      }
    }
    return result;
  }

  @Override
  public Result visitIndex(IndexExpression expr1, Expression expr2) {
    // TODO: inference variables
    if (!(expr2 instanceof IndexExpression)) return Result.NO;
    return expr1.getIndex() == ((IndexExpression) expr2).getIndex() ? Result.YES : Result.NO;
  }

  @Override
  public Result visitLam(LamExpression expr1, Expression expr2) {
    throw new IllegalStateException();
  }

  private Result compareTypeArguments(List<? extends Argument> args1, List<? extends Argument> args2, NewCompareVisitor visitor) {
    if (args1.size() != args2.size()) {
      return Result.NO;
    }

    Result result = Result.YES;
    for (int i = 0; i < args1.size(); i++) {
      if (args1.get(i) instanceof TypeArgument && args2.get(i) instanceof TypeArgument) {
        result = visitor.compare(((TypeArgument) args1.get(i)).getType(), ((TypeArgument) args2.get(i)).getType()).and(result);
      }
      if (result == Result.NO) {
        return Result.NO;
      }
      if (!visitor.myEquations.lift(-i)) {
        return Result.MAYBE;
      }
      myEquations.add(visitor.myEquations);
      visitor.myEquations.clear();
    }

    return result;
  }

  @Override
  public Result visitPi(PiExpression expr1, Expression expr2) {
    if (!(expr2 instanceof PiExpression)) return Result.NO;
    List<TypeArgument> args1 = new ArrayList<>();
    splitArguments(expr1, args1, null);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    splitArguments(expr2, args2, null);

    Equations equations = new Equations();
    NewCompareVisitor visitor = new NewCompareVisitor(equations, CMP.EQ);
    Result result = compareTypeArguments(args1, args2, visitor);
    if (result == Result.NO) {
      return Result.NO;
    }

    result = visitor.compare(expr1.getCodomain(), ((PiExpression) expr2).getCodomain()).and(result);
    if (result == Result.NO) {
      return Result.NO;
    }
    if (!equations.lift(-args1.size())) {
      return Result.MAYBE;
    }
    myEquations.add(equations);
    return result;
  }

  @Override
  public Result visitUniverse(UniverseExpression expr1, Expression expr2) {
    if (!(expr2 instanceof UniverseExpression)) return Result.NO;
    Universe.Cmp result = expr1.getUniverse().compare(((UniverseExpression) expr2).getUniverse());
    return result == Universe.Cmp.EQUALS || result == myCMP.toUniverseCmp() ? Result.YES : Result.NO;
  }

  @Override
  public Result visitInferHole(InferHoleExpression expr1, Expression expr2) {
    return null;
  }

  @Override
  public Result visitError(ErrorExpression expr1, Expression expr2) {
    return Result.NO;
  }

  @Override
  public Result visitTuple(TupleExpression expr1, Expression expr2) {
    if (!(expr2 instanceof TupleExpression)) return Result.NO;
    TupleExpression tupleExpr2 = (TupleExpression) expr2;
    if (expr1.getFields().size() != tupleExpr2.getFields().size()) {
      return Result.NO;
    }

    myCMP = CMP.EQ;
    Result result = Result.YES;
    for (int i = 0; i < expr1.getFields().size(); i++) {
      result = compare(expr1.getFields().get(i), tupleExpr2.getFields().get(i)).and(result);
      if (result == Result.NO) {
        return Result.NO;
      }
    }
    return result;
  }

  @Override
  public Result visitSigma(SigmaExpression expr1, Expression expr2) {
    if (!(expr2 instanceof SigmaExpression)) return Result.NO;
    List<TypeArgument> args1 = new ArrayList<>();
    splitArguments(expr1.getArguments(), args1);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    splitArguments(((SigmaExpression) expr2).getArguments(), args2);
    return compareTypeArguments(args1, args2, new NewCompareVisitor(new Equations(), CMP.EQ));
  }

  private Result visitClause(Clause clause1, Clause clause2) {
    if (clause1 == clause2) return Result.YES;
    if (clause1 == null || clause2 == null) return Result.NO;

    if (!clause2.getPatterns().equals(clause1.getPatterns()) || clause1.getArrow() != clause2.getArrow()) {
      return Result.NO;
    }

    return compare(clause1.getExpression(), clause2.getExpression());
  }

  @Override
  public Result visitElim(ElimExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ElimExpression)) return Result.NO;

    ElimExpression elimExpr2 = (ElimExpression) expr2;
    if (expr1.getClauses().size() != elimExpr2.getClauses().size() || expr1.getExpressions().size() != elimExpr2.getExpressions().size()) {
      return Result.NO;
    }

    myCMP = CMP.EQ;
    Result result = Result.YES;
    for (int i = 0; i < expr1.getExpressions().size(); i++) {
      result = compare(expr1.getExpressions().get(i), elimExpr2.getExpressions().get(i)).and(result);
      if (result == Result.NO) {
        return Result.NO;
      }
    }

    for (int i = 0; i < expr1.getClauses().size(); i++) {
      result = visitClause(expr1.getClauses().get(i), elimExpr2.getClauses().get(i)).and(result);
      if (result == Result.NO) {
        return Result.NO;
      }
    }

    return result;
  }

  @Override
  public Result visitProj(ProjExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ProjExpression)) return Result.NO;
    ProjExpression projExpr2 = (ProjExpression) expr2;
    if (expr1.getField() != projExpr2.getField()) {
      return Result.NO;
    }
    myCMP = CMP.EQ;
    return compare(expr1.getExpression(), projExpr2.getExpression());
  }

  @Override
  public Result visitNew(NewExpression expr1, Expression expr2) {
    if (!(expr2 instanceof NewExpression)) return Result.NO;
    myCMP = CMP.EQ;
    return compare(expr1.getExpression(), ((NewExpression) expr2).getExpression());
  }

  @Override
  public Result visitLet(LetExpression expr1, Expression expr2) {
    if (!(expr2 instanceof LetExpression)) {
      return Result.NO;
    }
    LetExpression letExpr1 = expr1.mergeNestedLets();
    LetExpression letExpr2 = ((LetExpression) expr2).mergeNestedLets();
    if (letExpr1.getClauses().size() != letExpr2.getClauses().size()) {
      return Result.NO;
    }

    Result result = Result.YES;
    Equations equations = new Equations();
    NewCompareVisitor visitor = new NewCompareVisitor(equations, CMP.EQ);
    for (int i = 0; i < letExpr1.getClauses().size(); i++) {
      List<TypeArgument> args1 = new ArrayList<>();
      splitArguments(letExpr1.getClauses().get(i).getArguments(), args1);
      List<TypeArgument> args2 = new ArrayList<>(args1.size());
      splitArguments(letExpr1.getClauses().get(i).getArguments(), args2);

      result = compareTypeArguments(args1, args2, visitor).and(result);
      if (result == Result.NO) {
        return Result.NO;
      }

      result = visitor.compare(letExpr1.getClauses().get(i).getTerm(), letExpr2.getClauses().get(i).getTerm()).and(result);
      if (result == Result.NO) {
        return Result.NO;
      }
      if (!equations.lift(-(args1.size() + i))) {
        return Result.MAYBE;
      }
      myEquations.add(equations);
      equations.clear();
    }

    visitor.myCMP = myCMP;
    result = visitor.compare(letExpr1.getExpression(), letExpr2.getExpression()).and(result);
    if (result == Result.NO) {
      return Result.NO;
    }
    if (!equations.lift(-letExpr1.getClauses().size())) {
      return Result.MAYBE;
    }
    myEquations.add(equations);
    return result;
  }

  public enum CMP {
    LE, EQ, GE;

    public Universe.Cmp toUniverseCmp() {
      switch (this) {
        case LE: return Universe.Cmp.LESS;
        case EQ: return Universe.Cmp.EQUALS;
        case GE: return Universe.Cmp.GREATER;
      }
      throw new IllegalStateException();
    }
  }

  public enum Result {
    YES, NO, MAYBE;

    public Result and(Result result) {
      return this == NO || result == NO ? NO : this == MAYBE || result == MAYBE ? MAYBE : YES;
    }
  }
}
