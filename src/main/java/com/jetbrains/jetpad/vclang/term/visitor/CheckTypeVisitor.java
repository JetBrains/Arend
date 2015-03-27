package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Argument;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.typechecking.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.Error;

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

  static class InferHoleExpression extends HoleExpression {
    public InferHoleExpression() {
      super(null);
    }

    @Override
    public InferHoleExpression getInstance(Expression expr) {
      return this;
    }
  }

  public static abstract class Result {
    public Expression expression;
  }

  public static class OKResult extends Result {
    public Expression type;
    public List<CompareVisitor.Equation> equations;

    public OKResult(Expression expression, Expression type, List<CompareVisitor.Equation> equations) {
      this.expression = expression;
      this.type = type;
      this.equations = equations;
    }
  }

  public static class InferErrorResult extends Result {
    public TypeCheckingError error;

    public InferErrorResult(InferHoleExpression hole, TypeCheckingError error) {
      expression = hole;
      this.error = error;
    }
  }

  public CheckTypeVisitor(Map<String, Definition> globalContext, List<Definition> localContext, List<TypeCheckingError> errors) {
    myGlobalContext = globalContext;
    myLocalContext = localContext;
    myErrors = errors;
  }

  private OKResult checkResult(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
    }
    Expression actualNorm = result.type.normalize(NormalizeVisitor.Mode.NF);
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF);
    result.equations = compare(expectedNorm, actualNorm);
    if (result.equations == null) {
      TypeCheckingError error = new TypeMismatchError(expectedNorm, actualNorm, expression);
      expression.setWellTyped(Error(result.expression, error));
      myErrors.add(error);
      return null;
    } else {
      expression.setWellTyped(result.expression);
      return result;
    }
  }

  private Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    } else {
      return expr.accept(this, expectedType);
    }
  }

  private Result typeCheckApps(Abstract.Expression fun, Arg[] args, Expression expectedType, Abstract.Expression expression) {
    if (fun instanceof Abstract.NelimExpression) {
      Result argument = typeCheck(args[0].expression, null);
      if (!(argument instanceof OKResult)) return argument;
      OKResult okArgument = (OKResult) argument;
      return new OKResult(Apps(Nelim(), okArgument.expression), Pi(Pi(Nat(), Pi(okArgument.type, okArgument.type)), Pi(Nat(), okArgument.type)), null);
    }

    Result function = typeCheck(fun, null);
    if (!(function instanceof OKResult)) {
      if (function instanceof InferErrorResult) {
        myErrors.add(((InferErrorResult) function).error);
      }
      for (Arg arg : args) {
        typeCheck(arg.expression, null);
      }
      return null;
    }
    OKResult okFunction = (OKResult) function;

    Signature signature = new Signature(okFunction.type);
    Abstract.Expression[] argsImp = new Abstract.Expression[signature.getArguments().length];
    int i, j;
    for (i = 0, j = 0; i < signature.getArguments().length && j < args.length; ++i, ++j) {
      if (args[j].isExplicit == signature.getArgument(i).isExplicit()) {
        argsImp[i] = args[j].expression;
      } else
      if (args[j].isExplicit) {
        argsImp[i] = new InferHoleExpression();
        --j;
      } else {
        TypeCheckingError error = new TypeCheckingError("Unexpected implicit argument", args[j].expression);
        args[j].expression.setWellTyped(Error(null, error));
        myErrors.add(error);
        for (Arg arg : args) {
          typeCheck(arg.expression, null);
        }
        return null;
      }
    }

    if (j < args.length) {
      TypeCheckingError error = new TypeCheckingError("Function expects " + i + " arguments, but is applied to " + (i + args.length - j), fun);
      fun.setWellTyped(Error(okFunction.expression, error));
      myErrors.add(error);
      for (Arg arg : args) {
        typeCheck(arg.expression, null);
      }
      return null;
    }

    int argsNumber = i;
    Result[] resultArgs = new Result[argsNumber];
    List<CompareVisitor.Equation> resultEquations = new ArrayList<>();
    for (i = 0; i < argsNumber; ++i) {
      if (resultArgs[i] instanceof OKResult) continue;

      if (argsImp[i] instanceof InferHoleExpression) {
        resultArgs[i] = new InferErrorResult((InferHoleExpression) argsImp[i], new ArgInferenceError(fun, i + 1));
        continue;
      }

      Expression type = signature.getArgument(i).getType();
      for (j = i - 1; j >= 0; --j) {
        type = type.subst(resultArgs[j].expression, 0);
      }

      resultArgs[i] = typeCheck(argsImp[i], type);
      if (resultArgs[i] == null) {
        for (j = i + 1; j < argsNumber; ++j) {
          if (!(argsImp[j] instanceof InferHoleExpression)) {
            typeCheck(argsImp[j], null);
          }
        }
        return null;
      }
      if (resultArgs[i] instanceof InferErrorResult) {
        // TODO: Try to infer implicit arguments from other arguments
        myErrors.add(((InferErrorResult) resultArgs[i]).error);
        return null;
      }

      OKResult okResult = (OKResult) resultArgs[i];
      if (okResult.equations == null) continue;
      int found = i;
      for (CompareVisitor.Equation equation : okResult.equations) {
        boolean foundEq = false;
        for (j = 0; j < i; ++j) {
          if (resultArgs[j] instanceof InferErrorResult && ((InferErrorResult) resultArgs[j]).expression == equation.hole) {
            argsImp[j] = equation.expression;
            found = j < found ? j : found;
            foundEq = true;
          }
        }
        if (!foundEq) {
          resultEquations.add(equation);
        }
      }
      if (found != i) {
        i = found - 1;
      }
    }

    // TODO: Infer tail implicit arguments.

    Expression resultType;
    if (signature.getArguments().length == argsNumber) {
      resultType = signature.getResultType();
    } else {
      Argument[] rest = new Argument[signature.getArguments().length - argsNumber];
      for (i = 0; i < rest.length; ++i) {
        rest[i] = signature.getArgument(argsNumber + i);
      }
      resultType = new Signature(rest, signature.getResultType()).getType();
    }
    for (i = argsNumber - 1; i >= 0; --i) {
      resultType = resultType.subst(resultArgs[i].expression, 0);
    }
    Expression resultExpr = okFunction.expression;
    for (i = 0; i < argsNumber; ++i) {
      resultExpr = App(resultExpr, resultArgs[i].expression, signature.getArgument(i).isExplicit());
    }
    OKResult result = checkResult(expectedType, new OKResult(resultExpr, resultType, null), expression);

    // TODO: Infer implicit arguments from the goal.

    int argIndex = 0;
    for (i = 0; i < argsNumber; ++i) {
      if (!(resultArgs[i] instanceof OKResult)) {
        argIndex = i + 1;
        break;
      }
    }
    if (argIndex == 0) {
      if (result.equations == null) {
        result.equations = resultEquations;
      } else {
        result.equations.addAll(resultEquations);
      }
      return result;
    } else {
      TypeCheckingError error = new ArgInferenceError(fun, argIndex);
      expression.setWellTyped(Error(result.expression, error));
      myErrors.add(error);
      return null;
    }
  }

  public OKResult checkType(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) return null;
    Result result = expr.accept(this, expectedType);
    if (result == null) return null;
    if (result instanceof OKResult) return (OKResult) result;
    throw new IllegalStateException();
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    List<Arg> args = new ArrayList<>();
    Abstract.Expression fexpr;
    for (fexpr = expr; fexpr instanceof Abstract.AppExpression; fexpr = ((Abstract.AppExpression) fexpr).getFunction()) {
      Abstract.AppExpression appfexpr = (Abstract.AppExpression) fexpr;
      args.add(new Arg(appfexpr.getArgument(), appfexpr.isExplicit()));
    }

    Arg[] argsArray = new Arg[args.size()];
    for (int i = 0; i < argsArray.length; ++i) {
      argsArray[i] = args.get(argsArray.length - 1 - i);
    }
    return typeCheckApps(fexpr, argsArray, expectedType, expr);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    return checkResult(expectedType, new OKResult(DefCall(expr.getDefinition()), expr.getDefinition().getSignature().getType(), null), expr);
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getSignature().getType().liftIndex(0, expr.getIndex() + 1);
    return checkResult(expectedType, new OKResult(Index(expr.getIndex()), actualType, null), expr);
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
      PiExpression type = (PiExpression) expectedNorm;
      InferHoleExpression hole = type.getDomain().accept(new FindHoleVisitor());
      if (hole == null) {
        // TODO: This is ugly. Fix it.
        myLocalContext.add(new FunctionDefinition(expr.getVariable(), new Signature(type.getDomain()), Var(expr.getVariable())));
        Result body = typeCheck(expr.getBody(), type.getCodomain());
        myLocalContext.remove(myLocalContext.size() - 1);
        if (!(body instanceof OKResult)) return body;
        OKResult okBody = (OKResult) body;
        // TODO: replace null with okBody.equations
        OKResult result = new OKResult(Lam(expr.getVariable(), okBody.expression), Pi(type.isExplicit(), type.getVariable(), type.getDomain(), okBody.type), null);
        expr.setWellTyped(result.expression);
        return result;
      } else {
        return new InferErrorResult(hole, new TypeInferenceError(new VarExpression(expr.getVariable())));
      }
    }

    if (expectedNorm instanceof InferHoleExpression) {
      return new InferErrorResult((InferHoleExpression) expectedNorm, new TypeInferenceError(new VarExpression(expr.getVariable())));
    }

    TypeCheckingError error = new TypeMismatchError(expectedNorm, Pi(Var("?"), Var("?")), expr);
    expr.setWellTyped(Error(null, error));
    myErrors.add(error);
    return null;
  }

  @Override
  public Result visitNat(Abstract.NatExpression expr, Expression expectedType) {
    return checkResult(expectedType, new OKResult(Nat(), Universe(0), null), expr);
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
    if (!(domainResult instanceof OKResult)) return domainResult;
    OKResult okDomainResult = (OKResult) domainResult;
    // TODO: This is ugly. Fix it.
    myLocalContext.add(new FunctionDefinition(expr.getVariable(), new Signature(okDomainResult.expression), Var(expr.getVariable())));
    Result codomainResult = typeCheck(expr.getCodomain(), Universe(-1));
    myLocalContext.remove(myLocalContext.size() - 1);
    if (!(codomainResult instanceof OKResult)) return codomainResult;
    OKResult okCodomainResult = (OKResult) codomainResult;
    Expression actualType = Universe(Math.max(((UniverseExpression) okDomainResult.type).getLevel(), ((UniverseExpression) okCodomainResult.type).getLevel()));
    // TODO: Add okCodomain.equations
    return checkResult(expectedType, new OKResult(Pi(expr.isExplicit(), expr.getVariable(), okDomainResult.expression, okCodomainResult.expression), actualType, okDomainResult.equations), expr);
  }

  @Override
  public Result visitSuc(Abstract.SucExpression expr, Expression expectedType) {
    return checkResult(expectedType, new OKResult(Suc(), Pi(Nat(), Nat()), null), expr);
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    return checkResult(expectedType, new OKResult(Universe(expr.getLevel()), Universe(expr.getLevel() == -1 ? -1 : expr.getLevel() + 1), null), expr);
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Expression expectedType) {
    ListIterator<Definition> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Definition def = it.previous();
      if (expr.getName().equals(def.getName())) {
        return checkResult(expectedType, new OKResult(Index(index), def.getSignature().getType().liftIndex(0, index + 1), null), expr);
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
      return checkResult(expectedType, new OKResult(DefCall(def), def.getSignature().getType(), null), expr);
    }
  }

  @Override
  public Result visitZero(Abstract.ZeroExpression expr, Expression expectedType) {
    return checkResult(expectedType, new OKResult(Zero(), Nat(), null), expr);
  }

  @Override
  public Result visitHole(Abstract.HoleExpression expr, Expression expectedType) {
    // TODO
    return null;
  }
}
