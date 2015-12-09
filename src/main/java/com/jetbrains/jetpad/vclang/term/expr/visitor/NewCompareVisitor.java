package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class NewCompareVisitor extends BaseExpressionVisitor<Expression, Boolean> {
  private final List<Binding> myContext;
  private final Equations myEquations;
  private Equations.CMP myCMP;

  private NewCompareVisitor(Equations equations, Equations.CMP cmp, List<Binding> context) {
    myEquations = equations;
    myContext = context;
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

  public static Boolean compare(Equations equations, Equations.CMP cmp, List<Binding> context, Expression expr1, Expression expr2) {
    return new NewCompareVisitor(equations, cmp, context).compare(expr1, expr2);
  }

  private NumberOfLambdas getNumberOfLambdas(Expression expr, boolean modifyContext) {
    if (!(expr instanceof LamExpression)) {
      return new NumberOfLambdas(0, expr);
    }
    LamExpression lamExpr = (LamExpression) expr;
    NumberOfLambdas result = getNumberOfLambdas(lamExpr.getBody(), modifyContext);
    for (TelescopeArgument arg : lamExpr.getArguments()) {
      if (modifyContext) {
        for (int i = 0; i < arg.getNames().size(); i++) {
          myContext.add(new TypedBinding(arg.getNames().get(i), arg.getType().liftIndex(0, i)));
        }
      }
      result.number += arg.getNames().size();
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

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      NumberOfLambdas numberOfLambdas = getNumberOfLambdas(expr.getArgument().getExpression(), true);
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
  }

  private Boolean compare(Expression expr1, Expression expr2) {
    if (expr1 == expr2 || expr1 instanceof ErrorExpression || expr2 instanceof ErrorExpression) {
      return true;
    }

    if (expr1 instanceof LamExpression || expr2 instanceof LamExpression) {
      try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
        NumberOfLambdas number1 = getNumberOfLambdas(expr1, false);
        NumberOfLambdas number2 = getNumberOfLambdas(expr2, true);
        boolean firstGreater = number1.number >= number2.number;
        NumberOfLambdas maxNumber = firstGreater ? number1 : number2;
        NumberOfLambdas minNumber = firstGreater ? number2 : number1;
        Expression maxBody = maxNumber.body;

        int diff = maxNumber.number - minNumber.number;
        maxBody = lamEtaReduce(maxBody, diff);
        if (maxBody == null) {
          return false;
        }
        Equations equations = myEquations.newInstance();
        if (!new NewCompareVisitor(equations, Equations.CMP.EQ, myContext).compare(firstGreater ? maxBody : minNumber.body, firstGreater ? minNumber.body : maxBody)) {
          return false;
        }
        equations.lift(-diff);
        return myEquations.add(equations);
      }
    }

    if (expr1 instanceof AppExpression) {
      Expression expr = pathEtaReduce((AppExpression) expr1);
      if (expr != null) {
        myCMP = Equations.CMP.EQ;
        return compare(expr, expr2);
      }
    }
    if (expr2 instanceof AppExpression) {
      Expression expr = pathEtaReduce((AppExpression) expr2);
      if (expr != null) {
        myCMP = Equations.CMP.EQ;
        return compare(expr1, expr);
      }
    }

    if (expr2 instanceof IndexExpression) {
      return compareIndex((IndexExpression) expr2, expr1, myCMP.not());
    }
    return expr1.accept(this, expr2);
  }

  private boolean checkIsInferVar(Expression fun1, List<Expression> args1, Expression expr2, Equations.CMP cmp) {
    if (!(fun1 instanceof IndexExpression)) {
      return false;
    }
    int index = ((IndexExpression) fun1).getIndex();
    if (index < myContext.size() && myContext.get(myContext.size() - 1 - index).isInference()) {
      myEquations.add((IndexExpression) fun1, args1, expr2, cmp);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Boolean visitApp(AppExpression expr1, Expression expr2) {
    List<Expression> args1 = new ArrayList<>();
    Expression fun1 = expr1.getFunction(args1);
    if (checkIsInferVar(fun1, args1, expr2, myCMP)) {
      return true;
    }

    List<Expression> args2 = new ArrayList<>(args1.size());
    Expression fun2 = expr1.getFunction(args2);
    if (checkIsInferVar(fun2, args2, expr1, myCMP.not())) {
      return true;
    }

    if (args1.size() != args2.size()) {
      return false;
    }

    myCMP = Equations.CMP.EQ;
    if (!compare(fun1, fun2)) {
      return false;
    }
    for (int i = 0; i < args1.size(); i++) {
      if (!compare(args1.get(i), args2.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitDefCall(DefCallExpression expr1, Expression expr2) {
    return expr2 instanceof DefCallExpression && expr1.getDefinition() == ((DefCallExpression) expr2).getDefinition();
  }

  @Override
  public Boolean visitClassCall(ClassCallExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ClassCallExpression) || expr1.getDefinition() != ((ClassCallExpression) expr2).getDefinition()) return false;
    Map<ClassField, ClassCallExpression.ImplementStatement> implStats1 = expr1.getImplementStatements();
    Map<ClassField, ClassCallExpression.ImplementStatement> implStats2 = ((ClassCallExpression) expr2).getImplementStatements();
    if (myCMP == Equations.CMP.EQ && implStats1.size() != implStats2.size() ||
        myCMP == Equations.CMP.LE && implStats1.size() <  implStats2.size() ||
        myCMP == Equations.CMP.GE && implStats1.size() >  implStats2.size()) {
      return false;
    }
    Map<ClassField, ClassCallExpression.ImplementStatement> minImplStats = implStats1.size() <= implStats2.size() ? implStats1 : implStats2;
    Map<ClassField, ClassCallExpression.ImplementStatement> maxImplStats = implStats1.size() <= implStats2.size() ? implStats2 : implStats1;

    Equations.CMP oldCMP = myCMP;
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> entry : minImplStats.entrySet()) {
      ClassCallExpression.ImplementStatement maxStat = maxImplStats.get(entry.getKey());
      if (maxStat == null) {
        return false;
      }
      ClassCallExpression.ImplementStatement implStat1 = implStats1.size() <= implStats2.size() ? entry.getValue() : maxStat;
      ClassCallExpression.ImplementStatement implStat2 = implStats1.size() <= implStats2.size() ? maxStat : entry.getValue();

      if (implStat1.term != null && implStat2.term != null) {
        myCMP = Equations.CMP.EQ;
        if (!compare(implStat1.term, implStat2.term)) {
          return false;
        }
      } else
      if (implStat1.term != null || implStat2.term != null) {
        return false;
      }
      myCMP = oldCMP;

      if (implStat1.type == null && implStat2.type == null) {
        continue;
      }
      Expression type1 = implStat1.type;
      Expression type2 = implStat2.type;
      if (type1 == null) {
        if (myCMP == Equations.CMP.GE) {
          continue;
        }
        type1 = entry.getKey().getBaseType();
      }
      if (type2 == null) {
        if (myCMP == Equations.CMP.LE) {
          continue;
        }
        type2 = entry.getKey().getBaseType();
      }
      if (!compare(type1, type2)) {
        return false;
      }
    }
    return true;
  }

  private Boolean compareIndex(IndexExpression expr1, Expression expr2, Equations.CMP cmp) {
    if (expr2 instanceof IndexExpression && expr1.getIndex() == ((IndexExpression) expr2).getIndex()) return true;
    assert expr1.getIndex() < myContext.size();
    Binding binding = myContext.get(myContext.size() - expr1.getIndex() - 1);
    return binding.isInference() && myEquations.add(expr1.getIndex(), expr2, cmp);
  }

  @Override
  public Boolean visitIndex(IndexExpression expr1, Expression expr2) {
    return compareIndex(expr1, expr2, myCMP);
  }

  @Override
  public Boolean visitLam(LamExpression expr1, Expression expr2) {
    throw new IllegalStateException();
  }

  private Boolean compareTypeArguments(List<TypeArgument> args1, List<TypeArgument> args2, NewCompareVisitor visitor) {
    if (args1.size() != args2.size()) {
      return false;
    }

    for (int i = 0; i < args1.size(); i++) {
      if (!visitor.compare(args1.get(i).getType(), args2.get(i).getType())) {
        return false;
      }
      visitor.myEquations.lift(-i);
      if (!myEquations.add(visitor.myEquations)) {
        return false;
      }
      visitor.myEquations.clear();
      Name name =
          args1.get(i) instanceof TelescopeArgument ? new Name(((TelescopeArgument) args1.get(i)).getNames().get(0)) :
          args2.get(i) instanceof TelescopeArgument ? new Name(((TelescopeArgument) args2.get(i)).getNames().get(0)) :
          null;
      myContext.add(new TypedBinding(name, args1.get(i).getType()));
    }

    return true;
  }

  @Override
  public Boolean visitPi(PiExpression expr1, Expression expr2) {
    if (!(expr2 instanceof PiExpression)) return false;
    List<TypeArgument> args1 = new ArrayList<>();
    splitArguments(expr1, args1, null);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    splitArguments(expr2, args2, null);

    Equations equations = myEquations.newInstance();
    NewCompareVisitor visitor = new NewCompareVisitor(equations, Equations.CMP.EQ, myContext);
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      if (!compareTypeArguments(args1, args2, visitor)) {
        return false;
      }

      if (!visitor.compare(expr1.getCodomain(), ((PiExpression) expr2).getCodomain())) {
        return false;
      }
      equations.lift(-args1.size());
      return myEquations.add(equations);
    }
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr1, Expression expr2) {
    if (!(expr2 instanceof UniverseExpression)) return false;
    Universe.Cmp result = expr1.getUniverse().compare(((UniverseExpression) expr2).getUniverse());
    return result == Universe.Cmp.EQUALS || result == myCMP.toUniverseCmp();
  }

  @Override
  public Boolean visitInferHole(InferHoleExpression expr1, Expression expr2) {
    return null;
  }

  @Override
  public Boolean visitError(ErrorExpression expr1, Expression expr2) {
    return false;
  }

  @Override
  public Boolean visitTuple(TupleExpression expr1, Expression expr2) {
    if (!(expr2 instanceof TupleExpression)) return false;
    TupleExpression tupleExpr2 = (TupleExpression) expr2;
    if (expr1.getFields().size() != tupleExpr2.getFields().size()) {
      return false;
    }

    myCMP = Equations.CMP.EQ;
    for (int i = 0; i < expr1.getFields().size(); i++) {
      if (!compare(expr1.getFields().get(i), tupleExpr2.getFields().get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitSigma(SigmaExpression expr1, Expression expr2) {
    if (!(expr2 instanceof SigmaExpression)) return false;
    List<TypeArgument> args1 = new ArrayList<>();
    splitArguments(expr1.getArguments(), args1);
    List<TypeArgument> args2 = new ArrayList<>(args1.size());
    splitArguments(((SigmaExpression) expr2).getArguments(), args2);
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      return compareTypeArguments(args1, args2, new NewCompareVisitor(myEquations.newInstance(), Equations.CMP.EQ, myContext));
    }
  }

  private Boolean visitClause(Clause clause1, Clause clause2) {
    if (clause1 == clause2) return true;
    if (clause1 == null || clause2 == null) return false;

    if (!clause2.getPatterns().equals(clause1.getPatterns()) || clause1.getArrow() != clause2.getArrow()) {
      return false;
    }

    return compare(clause1.getExpression(), clause2.getExpression());
  }

  @Override
  public Boolean visitElim(ElimExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ElimExpression)) return false;

    ElimExpression elimExpr2 = (ElimExpression) expr2;
    if (expr1.getClauses().size() != elimExpr2.getClauses().size() || expr1.getExpressions().size() != elimExpr2.getExpressions().size()) {
      return false;
    }

    myCMP = Equations.CMP.EQ;
    for (int i = 0; i < expr1.getExpressions().size(); i++) {
      if (!compare(expr1.getExpressions().get(i), elimExpr2.getExpressions().get(i))) {
        return false;
      }
    }

    for (int i = 0; i < expr1.getClauses().size(); i++) {
      if (!visitClause(expr1.getClauses().get(i), elimExpr2.getClauses().get(i))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitProj(ProjExpression expr1, Expression expr2) {
    if (!(expr2 instanceof ProjExpression)) return false;
    ProjExpression projExpr2 = (ProjExpression) expr2;
    if (expr1.getField() != projExpr2.getField()) {
      return false;
    }
    myCMP = Equations.CMP.EQ;
    return compare(expr1.getExpression(), projExpr2.getExpression());
  }

  @Override
  public Boolean visitNew(NewExpression expr1, Expression expr2) {
    if (!(expr2 instanceof NewExpression)) return false;
    myCMP = Equations.CMP.EQ;
    return compare(expr1.getExpression(), ((NewExpression) expr2).getExpression());
  }

  @Override
  public Boolean visitLet(LetExpression expr1, Expression expr2) {
    if (!(expr2 instanceof LetExpression)) {
      return false;
    }
    LetExpression letExpr1 = expr1.mergeNestedLets();
    LetExpression letExpr2 = ((LetExpression) expr2).mergeNestedLets();
    if (letExpr1.getClauses().size() != letExpr2.getClauses().size()) {
      return false;
    }

    Equations equations = myEquations.newInstance();
    NewCompareVisitor visitor = new NewCompareVisitor(equations, Equations.CMP.EQ, myContext);
    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (int i = 0; i < letExpr1.getClauses().size(); i++) {
        List<TypeArgument> args1 = new ArrayList<>();
        splitArguments(letExpr1.getClauses().get(i).getArguments(), args1);
        List<TypeArgument> args2 = new ArrayList<>(args1.size());
        splitArguments(letExpr1.getClauses().get(i).getArguments(), args2);

        if (!compareTypeArguments(args1, args2, visitor)) {
          return false;
        }

        if (!visitor.compare(letExpr1.getClauses().get(i).getTerm(), letExpr2.getClauses().get(i).getTerm())) {
          return false;
        }
        equations.lift(-(args1.size() + i));
        if (!myEquations.add(equations)) {
          return false;
        }
        equations.clear();
      }

      visitor.myCMP = myCMP;
      if (!visitor.compare(letExpr1.getExpression(), letExpr2.getExpression())) {
        return false;
      }
    }
    equations.lift(-letExpr1.getClauses().size());
    return myEquations.add(equations);
  }
}
