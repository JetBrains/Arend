package com.jetbrains.jetpad.vclang.term.visitor;

import com.google.common.collect.Lists;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.typechecking.NotInScopeError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeInferenceError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeMismatchError;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.Error;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final Map<String, Definition> myGlobalContext;
  private final List<Definition> myLocalContext;
  private final List<TypeCheckingError> myErrors;

  public static class Result {
    public Expression expression;
    public Expression type;

    public Result(Expression expression, Expression type) {
      this.expression = expression;
      this.type = type;
    }
  }

  public CheckTypeVisitor(Map<String, Definition> globalContext, List<Definition> localContext, List<TypeCheckingError> errors) {
    myGlobalContext = globalContext;
    myLocalContext = localContext;
    myErrors = errors;
  }

  private Result checkResult(Expression expectedType, Result result, Abstract.Expression expression) {
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
    }
    Expression actualNorm = result.type.normalize(NormalizeVisitor.Mode.NF);
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF);
    if (expectedNorm.equals(actualNorm)) {
      expression.setWellTyped(result.expression);
      return result;
    } else {
      TypeCheckingError error = new TypeMismatchError(expectedNorm, actualNorm, expression);
      expression.setWellTyped(Error(result.expression, error));
      myErrors.add(error);
      return null;
    }
  }

  public Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    } else {
      return expr.accept(this, expectedType);
    }
  }

  private Result visitApps(Abstract.Expression fun, List<Abstract.Expression> args) {
    if (fun instanceof Abstract.NelimExpression) {
      Result argument = typeCheck(args.get(0), null);
      if (argument == null) return null;
      return new Result(Apps(Nelim(), argument.expression), Pi(Pi(Nat(), Pi(argument.type, argument.type)), Pi(Nat(), argument.type)));
    } else {
      Result function = typeCheck(fun, null);
      if (function == null) {
        for (Abstract.Expression arg : args) {
          typeCheck(arg, null);
        }
        return null;
      }
      Expression resultExpr = function.expression;
      Expression resultType = function.type;
      Iterator<Abstract.Expression> it = args.iterator();
      while (it.hasNext()) {
        resultType = resultType.normalize(NormalizeVisitor.Mode.WHNF);
        if (resultType instanceof PiExpression) {
          PiExpression piType = (PiExpression) resultType;
          Result argument = typeCheck(it.next(), piType.getDomain());
          if (argument == null) return null;
          resultExpr = Apps(resultExpr, argument.expression);
          resultType = piType.getCodomain().subst(argument.expression, 0);
        } else {
          TypeCheckingError error = new TypeMismatchError(Pi(Var("_"), Var("_")), resultType, fun);
          fun.setWellTyped(Error(function.expression, error));
          myErrors.add(error);
          while (it.hasNext()) {
            typeCheck(it.next(), null);
          }
          return null;
        }
      }
      return new Result(resultExpr, resultType);
    }
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    List<Abstract.Expression> args = new ArrayList<>();
    Abstract.Expression fexpr;
    for (fexpr = expr; fexpr instanceof Abstract.AppExpression; fexpr = ((Abstract.AppExpression) fexpr).getFunction()) {
      args.add(((Abstract.AppExpression) fexpr).getArgument());
    }
    return checkResult(expectedType, visitApps(fexpr, Lists.reverse(args)), expr);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(DefCall(expr.getDefinition()), expr.getDefinition().getSignature().getType()), expr);
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getSignature().getType().liftIndex(0, expr.getIndex() + 1);
    return checkResult(expectedType, new Result(Index(expr.getIndex()), actualType), expr);
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    if (expectedType == null) {
      TypeCheckingError error = new TypeInferenceError(expr);
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
    if (expectedNorm instanceof PiExpression) {
      PiExpression type = (PiExpression)expectedNorm;
      // TODO: This is ugly. Fix it.
      myLocalContext.add(new FunctionDefinition(expr.getVariable(), new Signature(type.getDomain()), new VarExpression(expr.getVariable())));
      Result body = typeCheck(expr.getBody(), type.getCodomain());
      myLocalContext.remove(myLocalContext.size() - 1);
      if (body == null) return null;
      Result result = new Result(Lam(expr.getVariable(), body.expression), Pi(type.isExplicit(), type.getVariable(), type.getDomain(), body.type));
      expr.setWellTyped(result.expression);
      return result;
    } else {
      TypeCheckingError error = new TypeMismatchError(expectedNorm, Pi(Var("_"), Var("_")), expr);
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }
  }

  @Override
  public Result visitNat(Abstract.NatExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(Nat(), Universe(0)), expr);
  }

  @Override
  public Result visitNelim(Abstract.NelimExpression expr, Expression expectedType) {
    TypeCheckingError error = new TypeCheckingError("Expected at least one argument to N-elim", expr);
    expr.setWellTyped(Error(null, error));
    myErrors.add(error);
    return null;
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    Result domainResult = typeCheck(expr.getDomain(), Universe(-1));
    if (domainResult == null) return null;
    // TODO: This is ugly. Fix it.
    myLocalContext.add(new FunctionDefinition(expr.getVariable(), new Signature(domainResult.expression), Var(expr.getVariable())));
    Result codomainResult = typeCheck(expr.getCodomain(), Universe(-1));
    myLocalContext.remove(myLocalContext.size() - 1);
    if (codomainResult == null) return null;
    Expression actualType = Universe(Math.max(((UniverseExpression) domainResult.type).getLevel(), ((UniverseExpression) codomainResult.type).getLevel()));
    return checkResult(expectedType, new Result(Pi(expr.isExplicit(), expr.getVariable(), domainResult.expression, codomainResult.expression), actualType), expr);
  }

  @Override
  public Result visitSuc(Abstract.SucExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(Suc(), Pi(Nat(), Nat())), expr);
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(Universe(expr.getLevel()), Universe(expr.getLevel() == -1 ? -1 : expr.getLevel() + 1)), expr);
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Expression expectedType) {
    ListIterator<Definition> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Definition def = it.previous();
      if (expr.getName().equals(def.getName())) {
        return checkResult(expectedType, new Result(Index(index), def.getSignature().getType().liftIndex(0, index + 1)), expr);
      }
      ++index;
    }
    Definition def = myGlobalContext.get(expr.getName());
    if (def == null) {
      NotInScopeError error = new NotInScopeError(expr);
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    } else {
      return checkResult(expectedType, new Result(DefCall(def), def.getSignature().getType()), expr);
    }
  }

  @Override
  public Result visitZero(Abstract.ZeroExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(Zero(), Nat()), expr);
  }
}
