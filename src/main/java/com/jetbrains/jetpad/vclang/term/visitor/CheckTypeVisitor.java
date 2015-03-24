package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Argument;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.typechecking.NotInScopeError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeInferenceError;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeMismatchError;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.Error;
import static com.jetbrains.jetpad.vclang.term.visitor.CompareVisitor.and;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final Map<String, Definition> myGlobalContext;
  private final List<Definition> myLocalContext;
  private final List<TypeCheckingError> myErrors;

  private static class Arg {
    Abstract.Expression expression;
    boolean isExplicit;

    Arg(Abstract.Expression expression, boolean isExplicit) {
      this.expression = expression;
      this.isExplicit = isExplicit;
    }
  }

  private static class ImplicitArgumentExpression extends HoleExpression {
    public ImplicitArgumentExpression() {
      super(null);
    }

    @Override
    public HoleExpression getInstance(Expression expr) {
      return new ImplicitArgumentExpression();
    }
  }

  public static class Result {
    public Expression expression;
    public Expression type;
    public CompareVisitor.Result result;

    public Result(Expression expression, Expression type, CompareVisitor.Result result) {
      this.expression = expression;
      this.type = type;
      this.result = result;
    }
  }

  public CheckTypeVisitor(Map<String, Definition> globalContext, List<Definition> localContext, List<TypeCheckingError> errors) {
    myGlobalContext = globalContext;
    myLocalContext = localContext;
    myErrors = errors;
  }

  private Result checkResult(Expression expectedType, Result result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
    }
    Expression actualNorm = result.type.normalize(NormalizeVisitor.Mode.NF);
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF);
    result.result = and(result.result, compare(expectedNorm, actualNorm));
    if (result.result == CompareVisitor.Result.NOT_OK) {
      TypeCheckingError error = new TypeMismatchError(expectedNorm, actualNorm, expression);
      expression.setWellTyped(Error(result.expression, error));
      myErrors.add(error);
      return null;
    } else {
      expression.setWellTyped(result.expression);
      return result;
    }
  }

  public Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    } else {
      return expr.accept(this, expectedType);
    }
  }

  private Result typeCheckApps(Abstract.Expression fun, Arg[] args) {
    if (fun instanceof Abstract.NelimExpression) {
      Result argument = typeCheck(args[0].expression, null);
      if (argument == null) return null;
      return new Result(Apps(Nelim(), argument.expression), Pi(Pi(Nat(), Pi(argument.type, argument.type)), Pi(Nat(), argument.type)), argument.result);
    } else {
      Result function = typeCheck(fun, null);
      if (function == null) {
        for (Arg arg : args) {
          typeCheck(arg.expression, null);
        }
        return null;
      }

      Signature signature = new Signature(function.type);
      Result[] resultArgs = new Result[signature.getArguments().length];
      int argsNumber = Math.min(args.length, signature.getArguments().length);
      for (int i = 0; i < argsNumber; ++i) {
        Expression type = signature.getArgument(i).getType();
        for (int j = i - 1; j >= 0; --j) {
          type = type.subst(resultArgs[j].expression, 0);
        }
        if (args[i].isExplicit == signature.getArgument(i).isExplicit()) {
          resultArgs[i] = typeCheck(args[i].expression, type);
          if (resultArgs[i] == null) {
            for (int j = i + 1; j < args.length; ++j) {
              typeCheck(args[j].expression, null);
            }
            return null;
          }
        } else
        if (args[i].isExplicit) {
          // TODO: Infer arguments
        } else {
          TypeCheckingError error = new TypeCheckingError("Unexpected implicit argument", args[i].expression);
          args[i].expression.setWellTyped(Error(null, error));
          myErrors.add(error);
          for (int j = i + 1; j < args.length; ++j) {
            typeCheck(args[j].expression, null);
          }
          return null;
        }
      }

      if (args.length > signature.getArguments().length) {
        TypeCheckingError error = new TypeCheckingError("Function expects " + signature.getArguments().length + " arguments, but is applied to " + args.length, fun);
        fun.setWellTyped(Error(function.expression, error));
        myErrors.add(error);
        for (int i = signature.getArguments().length; i < args.length; ++i) {
          typeCheck(args[i].expression, null);
        }
        return null;
      }

      Expression resultType;
      if (signature.getArguments().length == args.length) {
        resultType = signature.getResultType();
      } else {
        Argument[] rest = new Argument[signature.getArguments().length - args.length];
        for (int i = 0; i < rest.length; ++i) {
          rest[i] = signature.getArgument(args.length + i);
        }
        resultType = new Signature(rest, signature.getResultType()).getType();
      }
      for (int i = argsNumber - 1; i >= 0; --i) {
        resultType = resultType.subst(resultArgs[i].expression, 0);
      }

      Expression resultExpr = function.expression;
      for (Result result : resultArgs) {
        resultExpr = Apps(resultExpr, result.expression);
      }

      CompareVisitor.Result resultResult = function.result;
      for (Result result : resultArgs) {
        resultResult = and(resultResult, result.result);
      }
      return new Result(resultExpr, resultType, resultResult);
    }
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    List<Arg> args = new ArrayList<>();
    Abstract.Expression fexpr;
    for (fexpr = expr; fexpr instanceof Abstract.AppExpression; fexpr = ((Abstract.AppExpression) fexpr).getFunction()) {
      args.add(new Arg(((Abstract.AppExpression) fexpr).getArgument(), expr.isExplicit()));
    }

    Arg[] argsArray = new Arg[args.size()];
    for (int i = 0; i < argsArray.length; ++i) {
      argsArray[i] = args.get(argsArray.length - 1 - i);
    }
    return checkResult(expectedType, typeCheckApps(fexpr, argsArray), expr);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(DefCall(expr.getDefinition()), expr.getDefinition().getSignature().getType(), CompareVisitor.Result.OK), expr);
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getSignature().getType().liftIndex(0, expr.getIndex() + 1);
    return checkResult(expectedType, new Result(Index(expr.getIndex()), actualType, CompareVisitor.Result.OK), expr);
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
      Result result = new Result(Lam(expr.getVariable(), body.expression), Pi(type.isExplicit(), type.getVariable(), type.getDomain(), body.type), body.result);
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
    return checkResult(expectedType, new Result(Nat(), Universe(0), CompareVisitor.Result.OK), expr);
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
    return checkResult(expectedType, new Result(Pi(expr.isExplicit(), expr.getVariable(), domainResult.expression, codomainResult.expression), actualType, and(domainResult.result, codomainResult.result)), expr);
  }

  @Override
  public Result visitSuc(Abstract.SucExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(Suc(), Pi(Nat(), Nat()), CompareVisitor.Result.OK), expr);
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(Universe(expr.getLevel()), Universe(expr.getLevel() == -1 ? -1 : expr.getLevel() + 1), CompareVisitor.Result.OK), expr);
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Expression expectedType) {
    ListIterator<Definition> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Definition def = it.previous();
      if (expr.getName().equals(def.getName())) {
        return checkResult(expectedType, new Result(Index(index), def.getSignature().getType().liftIndex(0, index + 1), CompareVisitor.Result.OK), expr);
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
      return checkResult(expectedType, new Result(DefCall(def), def.getSignature().getType(), CompareVisitor.Result.OK), expr);
    }
  }

  @Override
  public Result visitZero(Abstract.ZeroExpression expr, Expression expectedType) {
    return checkResult(expectedType, new Result(Zero(), Nat(), CompareVisitor.Result.OK), expr);
  }

  @Override
  public Result visitHole(Abstract.HoleExpression expr, Expression expectedType) {
    // TODO
    return null;
  }
}
