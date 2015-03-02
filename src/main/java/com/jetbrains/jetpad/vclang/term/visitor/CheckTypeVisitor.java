package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.NotInScopeException;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingException;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeInferenceException;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeMismatchException;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private Map<String, Definition> myGlobalContext;
  private List<Definition> myLocalContext;

  public static class Result {
    public Expression expression;
    public Expression type;

    public Result(Expression expression, Expression type) {
      this.expression = expression;
      this.type = type;
    }
  }

  private void checkType(Expression expectedType, Expression actualType, Abstract.Expression expression) throws TypeCheckingException {
    if (expectedType == null) return;
    Expression actualNorm = actualType.normalize();
    Expression expectedNorm = expectedType.normalize();
    if (!expectedNorm.equals(actualNorm)) {
      throw new TypeMismatchException(expectedNorm, actualNorm, expression);
    }
  }

  public CheckTypeVisitor(Map<String, Definition> globalContext, List<Definition> localContext) {
    myGlobalContext = globalContext;
    myLocalContext = localContext;
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
      Result argument = expr.getArgument().accept(this, null);
      result = new Result(Apps(Nelim(), argument.expression), Pi(Pi(Nat(), Pi(argument.type, argument.type)), Pi(Nat(), argument.type)));
    } else {
      Result function = expr.getFunction().accept(this, null);
      Expression functionType = function.type.normalize();
      if (functionType instanceof PiExpression) {
        PiExpression piType = (PiExpression) functionType;
        Result argument = expr.getArgument().accept(this, piType.getDomain());
        result = new Result(Apps(function.expression, argument.expression), piType.getCodomain().subst(argument.expression, 0));
      } else {
        throw new TypeMismatchException(Pi(Var("_"), Var("_")), functionType, expr.getFunction());
      }
    }
    checkType(expectedType, result.type, expr.getArgument());
    return result;
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    Expression actualType = expr.getDefinition().getSignature().getType();
    checkType(expectedType, actualType, expr);
    return new Result(DefCall(expr.getDefinition()), actualType);
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getSignature().getType().liftIndex(0, expr.getIndex() + 1);
    checkType(expectedType, actualType, expr);
    return new Result(Index(expr.getIndex()), actualType);
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    if (expectedType == null) {
      throw new TypeInferenceException(expr);
    }
    Expression expectedNorm = expectedType.normalize();
    if (expectedNorm instanceof PiExpression) {
      PiExpression type = (PiExpression)expectedNorm;
      // TODO: This is ugly. Fix it.
      myLocalContext.add(new FunctionDefinition(expr.getVariable(), new Signature(type.getDomain()), new VarExpression(expr.getVariable())));
      Result body = expr.getBody().accept(this, type.getCodomain());
      myLocalContext.remove(myLocalContext.size() - 1);
      return new Result(Lam(expr.getVariable(), body.expression), Pi(type.isExplicit(), type.getVariable(), type.getDomain(), body.type));
    } else {
      throw new TypeMismatchException(expectedNorm, Pi(Var("_"), Var("_")), expr);
    }
  }

  @Override
  public Result visitNat(Abstract.NatExpression expr, Expression expectedType) {
    Expression actualType = Universe(0);
    checkType(expectedType, actualType, expr);
    return new Result(Nat(), actualType);
  }

  @Override
  public Result visitNelim(Abstract.NelimExpression expr, Expression expectedType) {
    expectedType = expectedType.normalize();
    if (expectedType instanceof PiExpression) {
      Expression type = ((PiExpression)expectedType).getDomain();
      Expression actualType = Pi(type, Pi(Pi(Nat(), Pi(type, type)), Pi(Nat(), type)));
      checkType(expectedType, actualType, expr);
      return new Result(Nelim(), actualType);
    } else {
      throw new TypeInferenceException(expr);
    }
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    Result domainResult = expr.getDomain().accept(this, Universe(-1));
    // TODO: This is ugly. Fix it.
    myLocalContext.add(new FunctionDefinition(expr.getVariable(), new Signature(domainResult.expression), Var(expr.getVariable())));
    Result codomainResult = expr.getCodomain().accept(this, Universe(-1));
    myLocalContext.remove(myLocalContext.size() - 1);
    return new Result(Pi(expr.isExplicit(), expr.getVariable(), domainResult.expression, codomainResult.expression), Universe(Math.max(((UniverseExpression) domainResult.type).getLevel(), ((UniverseExpression) codomainResult.type).getLevel())));
  }

  @Override
  public Result visitSuc(Abstract.SucExpression expr, Expression expectedType) {
    Expression actualType = Pi(Nat(), Nat());
    checkType(expectedType, actualType, expr);
    return new Result(Suc(), actualType);
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    Expression actualType = Universe(expr.getLevel() == -1 ? -1 : expr.getLevel() + 1);
    checkType(expectedType, actualType, expr);
    return new Result(Universe(expr.getLevel()), actualType);
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Expression expectedType) {
    ListIterator<Definition> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Definition def = it.previous();
      if (expr.getName().equals(def.getName())) {
        Expression actualType = def.getSignature().getType().liftIndex(0, index + 1);
        checkType(expectedType, actualType, expr);
        return new Result(Index(index), actualType);
      }
      ++index;
    }
    Definition def = myGlobalContext.get(expr.getName());
    if (def == null) {
      throw new NotInScopeException(expr.getName());
    }
    Expression actualType = def.getSignature().getType();
    checkType(expectedType, actualType, expr);
    return new Result(DefCall(def), actualType);
  }

  @Override
  public Result visitZero(Abstract.ZeroExpression expr, Expression expectedType) {
    checkType(expectedType, Nat(), Zero());
    return new Result(Zero(), Nat());
  }
}
