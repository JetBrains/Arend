package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.term.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final Map<String, Definition> myGlobalContext;
  private final List<Binding> myLocalContext;
  private final List<TypeCheckingError> myErrors;

  private static class Arg {
    boolean isExplicit;
    String name;
    Abstract.Expression expression;

    Arg(boolean isExplicit, String name, Abstract.Expression expression) {
      this.isExplicit = isExplicit;
      this.name = name;
      this.expression = expression;
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

  public CheckTypeVisitor(Map<String, Definition> globalContext, List<Binding> localContext, List<TypeCheckingError> errors) {
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
    result.equations = compare(expectedNorm, actualNorm, CompareVisitor.CMP.GEQ);
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
      if (args[j].isExplicit == signature.getArgument(i).getExplicit()) {
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
        resultArgs[i] = new InferErrorResult((InferHoleExpression) argsImp[i], new ArgInferenceError("to function", fun, i + 1));
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
      TypeArgument[] rest = new TypeArgument[signature.getArguments().length - argsNumber];
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
      resultExpr = App(resultExpr, resultArgs[i].expression, signature.getArgument(i).getExplicit());
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
      TypeCheckingError error = new ArgInferenceError("to function", fun, argIndex);
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
      args.add(new Arg(appfexpr.isExplicit(), null, appfexpr.getArgument()));
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
    List<CompareVisitor.Equation> resultEquations = new ArrayList<>();

    List<Arg> lambdaArgs = new ArrayList<>();
    for (Abstract.Argument arg : expr.getArguments()) {
      if (arg instanceof Abstract.NameArgument) {
        lambdaArgs.add(new Arg(arg.getExplicit(), ((Abstract.NameArgument) arg).getName(), null));
      } else {
        for (String name : ((Abstract.TelescopeArgument) arg).getNames()) {
          lambdaArgs.add(new Arg(arg.getExplicit(), name, ((Abstract.TelescopeArgument) arg).getType()));
        }
      }
    }

    List<Arg> piArgs = new ArrayList<>();
    int actualNumberOfPiArgs;
    Expression resultType;
    if (expectedType == null) {
      for (Arg ignored : lambdaArgs) {
        piArgs.add(null);
      }
      actualNumberOfPiArgs = 0;
      resultType = null;
    } else {
      int i = 0;
      resultType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      argsLoop:
      while (resultType instanceof PiExpression) {
        PiExpression piType = (PiExpression) resultType;
        for (int j = 0; j < piType.getArguments().size(); ++j) {
          TypeArgument additionalArg = null;
          if (piType.getArgument(j) instanceof TelescopeArgument) {
            TelescopeArgument teleArg = (TelescopeArgument) piType.getArgument(j);
            for (int k = 0; k < teleArg.getNames().size(); ++k) {
              piArgs.add(new Arg(teleArg.getExplicit(), null, teleArg.getType()));
              if (++i >= lambdaArgs.size() && k + 1 < teleArg.getNames().size()) {
                List<String> names = new ArrayList<>(teleArg.getNames().size() - ++k);
                for (; k < teleArg.getNames().size(); ++k) {
                  names.add(teleArg.getName(k));
                }
                additionalArg = new TelescopeArgument(teleArg.getExplicit(), names, teleArg.getType());
                break;
              }
            }
          } else {
            piArgs.add(new Arg(piType.getArgument(j).getExplicit(), null, piType.getArgument(j).getType()));
            ++i;
          }
          if (i >= lambdaArgs.size()) {
            int size = piType.getArguments().size() - ++j + (additionalArg == null ? 0 : 1);
            if (size == 0) {
              resultType = piType.getCodomain();
            } else {
              List<TypeArgument> arguments = new ArrayList<>(size);
              if (additionalArg != null) {
                arguments.add(additionalArg);
              }
              for (; j < piType.getArguments().size(); ++j) {
                arguments.add(piType.getArgument(j));
              }
              resultType = Pi(arguments, piType.getCodomain());
            }
            break argsLoop;
          }
        }
        if (i >= lambdaArgs.size()) {
          resultType = piType.getCodomain();
          break;
        }
        resultType = piType.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
      }
      actualNumberOfPiArgs = piArgs.size();
      if (resultType instanceof InferHoleExpression) {
        for (; i < lambdaArgs.size(); ++i) {
          piArgs.add(null);
        }
      }
    }

    if (piArgs.size() < lambdaArgs.size()) {
      TypeCheckingError error = new TypeCheckingError("Expected a function of " + piArgs.size() + " arguments, but the lambda has " + lambdaArgs.size(), expr);
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }

    List<TypeCheckingError> errors = new ArrayList<>(lambdaArgs.size());
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (piArgs.get(i) == null && lambdaArgs.get(i).expression == null) {
        errors.add(new ArgInferenceError("of the lambda", expr, i));
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression == null) {
        InferHoleExpression hole = ((Expression) piArgs.get(i).expression).accept(new FindHoleVisitor());
        if (hole != null) {
          if (!errors.isEmpty()) {
            break;
          } else {
            return new InferErrorResult(hole, new ArgInferenceError("of the lambda", expr, i));
          }
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression != null) {
        if (piArgs.get(i).isExplicit != lambdaArgs.get(i).isExplicit) {
          errors.add(new TypeCheckingError(i + ArgInferenceError.suffix(i) + " argument of the lambda should be " + (piArgs.get(i).isExplicit ? "explicit" : "implicit"), expr));
        }
      }
    }
    if (!errors.isEmpty()) {
      expr.setWellTyped(Error(null, errors.get(0)));
      myErrors.addAll(errors);
      return null;
    }

    Expression[] argumentTypes = new Expression[lambdaArgs.size()];
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (lambdaArgs.get(i).expression != null) {
        Result argResult = typeCheck(lambdaArgs.get(i).expression, Universe(-1));
        if (!(argResult instanceof OKResult)) {
          while (i-- > 0) {
            myLocalContext.remove(myLocalContext.size() - 1);
          }
          return argResult;
        }
        OKResult okArgResult = (OKResult) argResult;
        if (okArgResult.equations != null) {
          for (CompareVisitor.Equation equation : okArgResult.equations) {
            try {
              if (equation.expression instanceof Expression) {
                resultEquations.add(new CompareVisitor.Equation(equation.hole, ((Expression) equation.expression).liftIndex(0, -i)));
              }
            } catch (LiftIndexVisitor.NegativeIndexException ignored) {
            }
          }
        }
        argumentTypes[i] = okArgResult.expression;

        if (piArgs.get(i) != null) {
          Expression argExpectedType = ((Expression) piArgs.get(i).expression).normalize(NormalizeVisitor.Mode.NF);
          Expression argActualType = argumentTypes[i].normalize(NormalizeVisitor.Mode.NF);
          List<CompareVisitor.Equation> equations = compare(argExpectedType, argActualType, CompareVisitor.CMP.LEQ);
          if (equations == null) {
            errors.add(new TypeMismatchError(piArgs.get(i).expression, lambdaArgs.get(i).expression, expr));
          } else {
            resultEquations.addAll(equations);
          }
        }
      } else {
        argumentTypes[i] = (Expression) piArgs.get(i).expression;
      }
      myLocalContext.add(new Binding(lambdaArgs.get(i).name, new Signature(argumentTypes[i])));
    }

    Result bodyResult = typeCheck(expr.getBody(), resultType instanceof InferHoleExpression ? null : resultType);

    for (int i = 0; i < lambdaArgs.size(); ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }

    if (!(bodyResult instanceof OKResult)) return bodyResult;
    OKResult okBodyResult = (OKResult) bodyResult;
    if (okBodyResult.equations != null) {
      for (CompareVisitor.Equation equation : okBodyResult.equations) {
        try {
          if (equation.expression instanceof Expression) {
            resultEquations.add(new CompareVisitor.Equation(equation.hole, ((Expression) equation.expression).liftIndex(0, -lambdaArgs.size())));
          }
        } catch (LiftIndexVisitor.NegativeIndexException ignored) {
        }
      }
    }

    if (resultType instanceof InferHoleExpression) {
      // TODO: Add an equation for resultType.
    }

    List<Argument> resultLambdaArgs = new ArrayList<>(argumentTypes.length);
    List<TypeArgument> resultPiArgs = new ArrayList<>(argumentTypes.length);
    for (int i = 0; i < argumentTypes.length; ++i) {
      TelescopeArgument arg = Tele(lambdaArgs.get(i).isExplicit, vars(lambdaArgs.get(i).name), argumentTypes[i]);
      resultLambdaArgs.add(arg);
      resultPiArgs.add(arg);
    }
    OKResult result = new OKResult(Lam(resultLambdaArgs, okBodyResult.expression), Pi(resultPiArgs, okBodyResult.type), resultEquations);
    expr.setWellTyped(result.expression);
    return result;
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
    OKResult[] domainResults = new OKResult[expr.getArguments().size()];
    int numberOfVars = 0;
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArgument(i).getType(), Universe(-1));
      if (!(result instanceof OKResult)) return result;
      domainResults[i] = (OKResult) result;
      if (domainResults[i].equations != null) {
        for (CompareVisitor.Equation equation : domainResults[i].equations) {
          try {
            if (equation.expression instanceof Expression) {
              equations.add(new CompareVisitor.Equation(equation.hole, ((Expression) equation.expression).liftIndex(0, -numberOfVars)));
            }
          } catch (LiftIndexVisitor.NegativeIndexException ignored) {
          }
        }
      }
      if (expr.getArgument(i) instanceof Abstract.TelescopeArgument) {
        for (String name : ((Abstract.TelescopeArgument) expr.getArgument(i)).getNames()) {
          myLocalContext.add(new Binding(name, new Signature(domainResults[i].expression)));
          ++numberOfVars;
        }
      } else {
        myLocalContext.add(new Binding(null, new Signature(domainResults[i].expression)));
        ++numberOfVars;
      }
    }

    Result codomainResult = typeCheck(expr.getCodomain(), Universe(-1));
    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }
    if (!(codomainResult instanceof OKResult)) return codomainResult;
    OKResult okCodomainResult = (OKResult) codomainResult;
    int level = ((UniverseExpression) okCodomainResult.type).getLevel();
    for (OKResult domainResult : domainResults) {
      level = Math.max(((UniverseExpression) domainResult.type).getLevel(), level);
    }
    Expression actualType = Universe(level);

    if (okCodomainResult.equations != null) {
      for (CompareVisitor.Equation equation : okCodomainResult.equations) {
        try {
          if (equation.expression instanceof Expression) {
            okCodomainResult.equations.add(new CompareVisitor.Equation(equation.hole, ((Expression) equation.expression).liftIndex(0, -numberOfVars)));
          }
        } catch (LiftIndexVisitor.NegativeIndexException ignored) {
        }
      }
    }

    List<TypeArgument> resultArguments = new ArrayList<>(domainResults.length);
    for (int i = 0; i < domainResults.length; ++i) {
      if (expr.getArgument(i) instanceof Abstract.TelescopeArgument) {
        resultArguments.add(new TelescopeArgument(expr.getArgument(i).getExplicit(), ((Abstract.TelescopeArgument) expr.getArgument(i)).getNames(), domainResults[i].expression));
      } else {
        resultArguments.add(new TypeArgument(expr.getArgument(i).getExplicit(), domainResults[i].expression));
      }
    }
    return checkResult(expectedType, new OKResult(Pi(resultArguments, okCodomainResult.expression), actualType, equations), expr);
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
    ListIterator<Binding> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Binding def = it.previous();
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
    // TODO: Type checking of holes?
    return null;
  }
}
