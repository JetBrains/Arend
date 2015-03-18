package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.typechecking.NotInScopeError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeInferenceError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeMismatchError;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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
    Expression actualNorm = result.type.normalize();
    Expression expectedNorm = expectedType.normalize();
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

  private Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    } else {
      return expr.accept(this, expectedType);
    }
  }

  /*
  public Expression visitApps(Abstract.Expression fun, List<Expression> args) {
    if (fun instanceof Abstract.NelimExpression) {
      Expression type = args.get(0).accept(this);
      return Pi(Pi(Nat(), Pi(type, type)), Pi(Nat(), type));
    }
    Expression funType = fun.accept(this).normalize();
    return null;
  }
  */

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    Result result;
    if (expr.getFunction() instanceof Abstract.NelimExpression) {
      Result argument = typeCheck(expr.getArgument(), null);
      if (argument == null) return null;
      result = new Result(Apps(Nelim(), argument.expression), Pi(Pi(Nat(), Pi(argument.type, argument.type)), Pi(Nat(), argument.type)));
    } else {
      Result function = typeCheck(expr.getFunction(), null);
      if (function == null) {
        typeCheck(expr.getArgument(), null);
        return null;
      }
      Expression functionType = function.type.normalize();
      if (functionType instanceof PiExpression) {
        PiExpression piType = (PiExpression) functionType;
        Result argument = typeCheck(expr.getArgument(), piType.getDomain());
        if (argument == null) return null;
        result = new Result(Apps(function.expression, argument.expression), piType.getCodomain().subst(argument.expression, 0));
      } else {
        TypeCheckingError error = new TypeMismatchError(Pi(Var("_"), Var("_")), functionType, expr.getFunction());
        expr.getFunction().setWellTyped(Error(function.expression, error));
        myErrors.add(error);
        typeCheck(expr.getArgument(), null);
        return null;
      }
    }
    return checkResult(expectedType, result, expr);
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
    Expression expectedNorm = expectedType.normalize();
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
