package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.*;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final Definition myParent;
  private final List<Binding> myLocalContext;
  private final List<TypeCheckingError> myErrors;
  private Side mySide;
  private final Set<Definition> myAbstractCalls;

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

  public static abstract class Result {
    public Expression expression;
    public List<CompareVisitor.Equation> equations;
  }

  public static class OKResult extends Result {
    public Expression type;

    public OKResult(Expression expression, Expression type, List<CompareVisitor.Equation> equations) {
      this.expression = expression;
      this.type = type;
      this.equations = equations;
    }
  }

  public static class InferErrorResult extends Result {
    public TypeCheckingError error;

    public InferErrorResult(InferHoleExpression hole, TypeCheckingError error, List<CompareVisitor.Equation> equations) {
      expression = hole;
      this.error = error;
      this.equations = equations;
    }
  }

  public enum Side { LHS, RHS }

  public CheckTypeVisitor(Definition parent, List<Binding> localContext, Set<Definition> abstractCalls, List<TypeCheckingError> errors, Side side) {
    myParent = parent;
    myLocalContext = localContext;
    myAbstractCalls = abstractCalls;
    myErrors = errors;
    mySide = side;
  }

  public void setSide(Side side) {
    mySide = side;
  }

  private Result checkResult(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
    }
    Expression actualNorm = result.type.normalize(NormalizeVisitor.Mode.NF);
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF);
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result1 = compare(expectedNorm, actualNorm, equations);
    if (result1 instanceof CompareVisitor.MaybeResult || result1.isOK() == CompareVisitor.CMP.GREATER || result1.isOK() == CompareVisitor.CMP.EQUALS) {
      if (result.equations != null) {
        result.equations.addAll(equations);
      } else {
        result.equations = equations;
      }
      expression.setWellTyped(result.expression);
      return result;
    } else {
      TypeCheckingError error = new TypeMismatchError(expectedNorm, actualNorm, expression, getNames(myLocalContext));
      expression.setWellTyped(Error(result.expression, error));
      myErrors.add(error);
      return null;
    }
  }

  private Result checkResultImplicit(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
    }

    if (result.expression instanceof DefCallExpression) {
      DefCallExpression defCall = (DefCallExpression) result.expression;
      if (defCall.getDefinition() instanceof Constructor && !((Constructor) defCall.getDefinition()).getDataType().getParameters().isEmpty()) {
        return typeCheckFunctionApps(expression, new ArrayList<Abstract.ArgumentExpression>(), expectedType, expression);
      }
    }

    if (numberOfVariables(result.type) > numberOfVariables(expectedType)) {
      return typeCheckFunctionApps(expression, new ArrayList<Abstract.ArgumentExpression>(), expectedType, expression);
    } else {
      return checkResult(expectedType, result, expression);
    }
  }

  private Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    } else {
      return expr.accept(this, expectedType);
    }
  }

  private int solveEquations(int size, Arg[] argsImp, Result[] resultArgs, List<CompareVisitor.Equation> equations, List<CompareVisitor.Equation> resultEquations, Abstract.Expression fun) {
    int found = size;
    for (CompareVisitor.Equation equation : equations) {
      for (int i = 0; i < size; ++i) {
        if (resultArgs[i] instanceof InferErrorResult && resultArgs[i].expression == equation.hole) {
          if (!(argsImp[i].expression instanceof Abstract.InferHoleExpression)) {
            if (argsImp[i].expression instanceof Expression) {
              Expression expr1 = ((Expression) argsImp[i].expression).normalize(NormalizeVisitor.Mode.NF);
              List<CompareVisitor.Equation> equations1 = new ArrayList<>();
              CompareVisitor.CMP cmp = compare(expr1, equation.expression, equations1).isOK();
              if (cmp != CompareVisitor.CMP.NOT_EQUIV) {
                if (cmp == CompareVisitor.CMP.GREATER) {
                  argsImp[i] = new Arg(argsImp[i].isExplicit, null, equation.expression);
                }
              } else {
                List<Abstract.Expression> options = new ArrayList<>(2);
                options.add(argsImp[i].expression);
                options.add(equation.expression);
                for (int j = i + 1; j < size; ++j) {
                  if (resultArgs[j] instanceof InferErrorResult && resultArgs[j].expression == equation.hole) {
                    boolean was = false;
                    for (Abstract.Expression option : options) {
                      cmp = compare(option, equation.expression, equations1).isOK();
                      if (cmp != CompareVisitor.CMP.NOT_EQUIV) {
                        was = true;
                        break;
                      }
                    }
                    if (!was) {
                      options.add(equation.expression);
                    }
                  }
                }
                myErrors.add(new InferredArgumentsMismatch(i + 1, options, fun, getNames(myLocalContext)));
                return -1;
              }
            } else {
              break;
            }
          }

          argsImp[i] = new Arg(argsImp[i].isExplicit, null, equation.expression);
          found = i < found ? i : found;
          break;
        }
      }
      resultEquations.add(equation);
    }
    return found;
  }

  private boolean typeCheckArgs(Arg[] argsImp, Result[] resultArgs, List<TypeArgument> signature, List<CompareVisitor.Equation> resultEquations, int startIndex, Abstract.Expression fun) {
    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] == null) {
        TypeCheckingError error = new ArgInferenceError(functionArg(i + 1), fun, getNames(myLocalContext), fun);
        resultArgs[i] = new InferErrorResult(new InferHoleExpression(error), error, null);
      }
    }

    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] instanceof OKResult || argsImp[i].expression instanceof Abstract.InferHoleExpression) continue;

      List<Expression> substExprs = new ArrayList<>(i);
      for (int j = i - 1; j >= 0; --j) {
        substExprs.add(resultArgs[j].expression);
      }
      Expression type = signature.get(i).getType().subst(substExprs, 0);

      Result result = typeCheck(argsImp[i].expression, type);
      if (result == null) {
        for (int j = i + 1; j < resultArgs.length; ++j) {
          if (!(argsImp[j].expression instanceof Abstract.InferHoleExpression)) {
            typeCheck(argsImp[j].expression, null);
          }
        }
        return false;
      }
      if (result instanceof OKResult) {
        resultArgs[i] = result;
        argsImp[i].expression = result.expression;
      } else {
        if (resultArgs[i] != null && resultArgs[i].expression instanceof InferHoleExpression) {
          resultArgs[i] = new InferErrorResult((InferHoleExpression) resultArgs[i].expression, ((InferErrorResult) result).error, result.equations);
        } else {
          resultArgs[i] = result;
        }
      }

      if (resultArgs[i].equations == null) continue;
      int found = solveEquations(i, argsImp, resultArgs, resultArgs[i].equations, resultEquations, fun);
      if (found < 0) return false;
      if (found != i) {
        i = found - 1;
      }
    }
    return true;
  }

  private Result typeCheckFunctionApps(Abstract.Expression fun, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression expression) {
    Result function = typeCheck(fun, null);
    if (function instanceof OKResult) {
      return typeCheckApps(fun, 0, (OKResult) function, args, expectedType, expression);
    } else {
      if (function instanceof InferErrorResult) {
        myErrors.add(((InferErrorResult) function).error);
      }
      for (Abstract.ArgumentExpression arg : args) {
        typeCheck(arg.getExpression(), null);
      }
      return null;
    }
  }

  private Result typeCheckApps(Abstract.Expression fun, int argsSkipped, OKResult okFunction, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression expression) {
    List<TypeArgument> signatureArguments = new ArrayList<>();
    DataDefinition dataType = null;
    if (okFunction.expression instanceof DefCallExpression) {
      Definition def = ((DefCallExpression) okFunction.expression).getDefinition();
      if (def instanceof Constructor) {
        dataType = ((Constructor) def).getDataType();
        splitArguments(dataType.getParameters(), signatureArguments);
      }
    }
    int parametersNumber = signatureArguments.size();

    Expression signatureResultType = splitArguments(okFunction.type, signatureArguments);
    Arg[] argsImp = new Arg[signatureArguments.size()];
    for (int i = 0; i < parametersNumber; ++i) {
      assert dataType != null;
      argsImp[i] = new Arg(!signatureArguments.get(i).getExplicit(), null, new InferHoleExpression(new ArgInferenceError(parameter(i + 1), fun, null, new StringPrettyPrintable(dataType.getName()))));
    }

    int i, j;
    for (i = parametersNumber, j = 0; i < signatureArguments.size() && j < args.size(); ++i, ++j) {
      if (args.get(j).isExplicit() == signatureArguments.get(i).getExplicit()) {
        argsImp[i] = new Arg(args.get(j).isHidden(), null, args.get(j).getExpression());
      } else
      if (args.get(j).isExplicit()) {
        argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(functionArg(j + 1), fun, getNames(myLocalContext), fun)));
        --j;
      } else {
        TypeCheckingError error = new TypeCheckingError("Unexpected implicit argument", args.get(j).getExpression(), getNames(myLocalContext));
        args.get(j).getExpression().setWellTyped(Error(null, error));
        myErrors.add(error);
        for (Abstract.ArgumentExpression arg : args) {
          typeCheck(arg.getExpression(), null);
        }
        return null;
      }
    }

    if (j < args.size() && signatureArguments.size() == 0) {
      TypeCheckingError error = new TypeCheckingError("Function expects " + (argsSkipped + i) + " arguments, but is applied to " + (argsSkipped + i + args.size() - j), fun, getNames(myLocalContext));
      fun.setWellTyped(Error(okFunction.expression, error));
      myErrors.add(error);
      for (Abstract.ArgumentExpression arg : args) {
        typeCheck(arg.getExpression(), null);
      }
      return null;
    }

    if (okFunction.expression instanceof DefCallExpression && ((DefCallExpression) okFunction.expression).getDefinition().equals(Prelude.PATH_CON) && args.size() == 1 && j == 1) {
      Expression argExpectedType = null;
      InferHoleExpression holeExpression = null;
      if (expectedType != null) {
        List<Expression> argsExpectedType = new ArrayList<>(3);
        Expression fexpectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF).getFunction(argsExpectedType);
        if (fexpectedType instanceof DefCallExpression && ((DefCallExpression) fexpectedType).getDefinition().equals(Prelude.PATH) && argsExpectedType.size() == 3) {
          if (argsExpectedType.get(2) instanceof InferHoleExpression) {
            holeExpression = (InferHoleExpression) argsExpectedType.get(2);
          } else {
            argExpectedType = Pi("i", DefCall(Prelude.INTERVAL), Apps(argsExpectedType.get(2).liftIndex(0, 1), Index(0)));
          }
        }
      }

      InferHoleExpression inferHoleExpr = new InferHoleExpression(new ArgInferenceError(type(), args.get(0).getExpression(), getNames(myLocalContext), args.get(0).getExpression()));
      if (argExpectedType == null) {
        argExpectedType = Pi("i", DefCall(Prelude.INTERVAL), inferHoleExpr);
      }

      Result argResult = typeCheck(args.get(0).getExpression(), argExpectedType);
      if (!(argResult instanceof OKResult)) return argResult;
      if (argResult.equations != null) {
        for (int k = 0; k < argResult.equations.size(); ++k) {
          if (argResult.equations.get(k).hole.equals(inferHoleExpr)) {
            argResult.equations.remove(k--);
          }
        }
      }
      PiExpression piType = (PiExpression) ((OKResult) argResult).type;

      List<TypeArgument> arguments = new ArrayList<>(piType.getArguments().size());
      if (piType.getArguments().get(0) instanceof TelescopeArgument) {
        List<String> names = ((TelescopeArgument) piType.getArguments().get(0)).getNames();
        if (names.size() > 1) {
          arguments.add(Tele(piType.getArguments().get(0).getExplicit(), names.subList(1, names.size()), piType.getArguments().get(0).getType()));
        }
      }
      if (piType.getArguments().size() > 1) {
        arguments.addAll(piType.getArguments().subList(1, piType.getArguments().size()));
      }

      Expression type = arguments.size() > 0 ? Pi(arguments, piType.getCodomain()) : piType.getCodomain();
      Expression resultType = Apps(DefCall(Prelude.PATH), Lam("i", type), Apps(argResult.expression, DefCall(Prelude.LEFT)), Apps(argResult.expression, DefCall(Prelude.RIGHT)));
      List<CompareVisitor.Equation> resultEquations = argResult.equations;
      if (holeExpression != null) {
        if (resultEquations == null) {
          resultEquations = new ArrayList<>(1);
        }
        resultEquations.add(new CompareVisitor.Equation(holeExpression, Lam("i", type.normalize(NormalizeVisitor.Mode.NF))));
      }

      return checkResult(expectedType, new OKResult(Apps(DefCall(Prelude.PATH_CON), argResult.expression), resultType, resultEquations), expression);
    }

    if (expectedType != null && j == args.size()) {
      for (; i < signatureArguments.size() - numberOfVariables(expectedType); ++i) {
        if (signatureArguments.get(i).getExplicit()) {
          break;
        } else {
          argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(functionArg(i + 1), fun, getNames(myLocalContext), fun)));
        }
      }
    }

    int argsNumber = i;
    Result[] resultArgs = new Result[argsNumber];
    List<CompareVisitor.Equation> resultEquations = new ArrayList<>();
    if (!typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, 0, fun)) {
      expression.setWellTyped(Error(null, myErrors.get(myErrors.size() - 1)));
      return null;
    }

    Expression resultType;
    if (signatureArguments.size() == argsNumber) {
      resultType = signatureResultType;
    } else {
      int size = signatureArguments.size() - argsNumber;
      List<TypeArgument> rest = new ArrayList<>(size);
      for (i = 0; i < size; ++i) {
        rest.add(signatureArguments.get(argsNumber + i));
      }
      resultType = Pi(rest, signatureResultType);
    }
    List<Expression> substExprs = new ArrayList<>(argsNumber);
    for (i = argsNumber - 1; i >= 0; --i) {
      substExprs.add(resultArgs[i].expression);
    }
    resultType = resultType.subst(substExprs, 0);

    int argIndex = 0;
    for (i = argsNumber - 1; i >= 0; --i) {
      if (!(resultArgs[i] instanceof OKResult)) {
        argIndex = i + 1;
        break;
      }
    }

    if (argIndex != 0 && expectedType != null && j == args.size() && expectedType.accept(new FindHoleVisitor()) == null) {
      Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF);
      Expression actualNorm = resultType.normalize(NormalizeVisitor.Mode.NF);
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      CompareVisitor.Result result = compare(actualNorm, expectedNorm, equations);

      if (result instanceof CompareVisitor.JustResult && result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
        Expression resultExpr = okFunction.expression;
        for (i = parametersNumber; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }

        TypeCheckingError error = new TypeMismatchError(expectedNorm, actualNorm, expression, getNames(myLocalContext));
        expression.setWellTyped(Error(resultExpr, error));
        myErrors.add(error);
        return null;
      }

      int found = solveEquations(argsNumber, argsImp, resultArgs, equations, resultEquations, fun);
      if (found < 0 || (found != argsNumber && !typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, found, fun))) {
        Expression resultExpr = okFunction.expression;
        for (i = parametersNumber; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i] == null ? new InferHoleExpression(null) : resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }
        expression.setWellTyped(Error(resultExpr, myErrors.get(myErrors.size() - 1)));
        return null;
      }

      argIndex = 0;
      for (i = argsNumber - 1; i >= 0; --i) {
        if (!(resultArgs[i] instanceof OKResult)) {
          argIndex = i + 1;
          break;
        }
      }

      if (argIndex == 0) {
        if (signatureArguments.size() == argsNumber) {
          resultType = signatureResultType;
        } else {
          int size = signatureArguments.size() - argsNumber;
          List<TypeArgument> rest = new ArrayList<>(size);
          for (i = 0; i < size; ++i) {
            rest.add(signatureArguments.get(argsNumber + i));
          }
          resultType = Pi(rest, signatureResultType);
        }
        substExprs = new ArrayList<>(argsNumber);
        for (i = argsNumber - 1; i >= 0; --i) {
          substExprs.add(resultArgs[i].expression);
        }
        resultType = resultType.subst(substExprs, 0);
      }
    }

    Expression resultExpr = okFunction.expression;
    for (i = parametersNumber; i < argsNumber; ++i) {
      resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
    }

    if (argIndex == 0) {
      if (j < args.size()) {
        List<Abstract.ArgumentExpression> restArgs = new ArrayList<>(args.size() - j);
        for (int k = j; k < args.size(); ++k) {
          restArgs.add(args.get(k));
        }
        return typeCheckApps(fun, argsSkipped + j, new OKResult(resultExpr, resultType, resultEquations), restArgs, expectedType, expression);
      } else {
        return checkResult(expectedType, new OKResult(resultExpr, resultType, resultEquations), expression);
      }
    } else {
      TypeCheckingError error;
      if (resultArgs[argIndex - 1] instanceof InferErrorResult) {
        error = ((InferErrorResult) resultArgs[argIndex - 1]).error;
      } else {
        if (argIndex > parametersNumber) {
          error = new ArgInferenceError(functionArg(argIndex - parametersNumber), fun, getNames(myLocalContext), fun);
        } else {
          error = new ArgInferenceError(parameter(argIndex), fun, null, new StringPrettyPrintable(((Constructor) ((DefCallExpression) okFunction.expression).getDefinition()).getDataType().getName()));
        }
      }
      expression.setWellTyped(Error(resultExpr, error));
      return new InferErrorResult(new InferHoleExpression(error), error, resultEquations);
    }
  }

  public OKResult checkType(Abstract.Expression expr, Expression expectedType) {
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;
    if (result instanceof OKResult) return (OKResult) result;
    InferErrorResult errorResult = (InferErrorResult) result;
    myErrors.add(errorResult.error);
    return null;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    return typeCheckFunctionApps(Abstract.getFunction(expr, args), args, expectedType, expr);
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    TypeCheckingError error = null;
    if (expr.getDefinition() instanceof FunctionDefinition && ((FunctionDefinition) expr.getDefinition()).typeHasErrors() || !(expr.getDefinition() instanceof FunctionDefinition) && expr.getDefinition().hasErrors()) {
      error = new HasErrors(expr.getDefinition().getName(), expr);
    } else {
      if (!expr.getDefinition().isRelativelyStatic(myParent)) {
        error = new TypeCheckingError("Non-static method call", expr, null);
      }
    }

    if (error != null) {
      expr.setWellTyped(Error(DefCall(expr.getDefinition()), error));
      myErrors.add(error);
      return null;
    } else {
      if (expr.getDefinition().isAbstract()) {
        myAbstractCalls.add(expr.getDefinition());
      }

      return checkResultImplicit(expectedType, new OKResult(DefCall(expr.getDefinition()), expr.getDefinition().getType(), null), expr);
    }
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getType().liftIndex(0, expr.getIndex() + 1);
    return checkResultImplicit(expectedType, new OKResult(Index(expr.getIndex()), actualType, null), expr);
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

    List<TypeArgument> piArgs = new ArrayList<>();
    int actualNumberOfPiArgs;
    Expression resultType;
    Expression fresultType;
    if (expectedType == null) {
      for (Arg ignored : lambdaArgs) {
        piArgs.add(null);
      }
      actualNumberOfPiArgs = 0;
      resultType = null;
      fresultType = null;
    } else {
      resultType = expectedType.splitAt(lambdaArgs.size(), piArgs).normalize(NormalizeVisitor.Mode.WHNF);
      fresultType = resultType.getFunction(new ArrayList<Expression>());
      actualNumberOfPiArgs = piArgs.size();
      if (fresultType instanceof InferHoleExpression) {
        for (int i = piArgs.size(); i < lambdaArgs.size(); ++i) {
          piArgs.add(null);
        }
      }
    }

    if (piArgs.size() < lambdaArgs.size()) {
      TypeCheckingError error = new TypeCheckingError("Expected a function of " + piArgs.size() + " arguments, but the lambda has " + lambdaArgs.size(), expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }

    List<TypeCheckingError> errors = new ArrayList<>(lambdaArgs.size());
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (piArgs.get(i) == null && lambdaArgs.get(i).expression == null) {
        TypeCheckingError error = new ArgInferenceError(lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
        errors.add(error);
        if (fresultType instanceof InferHoleExpression) {
          expr.setWellTyped(Error(null, error));
          return new InferErrorResult((InferHoleExpression) fresultType, error, null);
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression == null) {
        InferHoleExpression hole = piArgs.get(i).getType().accept(new FindHoleVisitor());
        if (hole != null) {
          if (!errors.isEmpty()) {
            break;
          } else {
            TypeCheckingError error = new ArgInferenceError(lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
            expr.setWellTyped(Error(null, error));
            return new InferErrorResult(hole, error, null);
          }
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression != null) {
        if (piArgs.get(i).getExplicit() != lambdaArgs.get(i).isExplicit) {
          errors.add(new TypeCheckingError((i + 1) + suffix(i + 1) + " argument of the lambda should be " + (piArgs.get(i).getExplicit() ? "explicit" : "implicit"), expr, getNames(myLocalContext)));
        }
      }
    }
    if (!errors.isEmpty()) {
      expr.setWellTyped(Error(null, errors.get(0)));
      myErrors.addAll(errors);
      return null;
    }

    List<TypeArgument> argumentTypes = new ArrayList<>(lambdaArgs.size());
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (lambdaArgs.get(i).expression != null) {
        Result argResult = typeCheck(lambdaArgs.get(i).expression, Universe());
        if (!(argResult instanceof OKResult)) {
          while (i-- > 0) {
            myLocalContext.remove(myLocalContext.size() - 1);
          }
          return argResult;
        }
        OKResult okArgResult = (OKResult) argResult;
        addLiftedEquations(okArgResult, resultEquations, i);
        argumentTypes.add(Tele(lambdaArgs.get(i).isExplicit, vars(lambdaArgs.get(i).name), okArgResult.expression));

        if (piArgs.get(i) != null) {
          Expression argExpectedType = piArgs.get(i).getType().normalize(NormalizeVisitor.Mode.NF);
          Expression argActualType = argumentTypes.get(i).getType().normalize(NormalizeVisitor.Mode.NF);
          List<CompareVisitor.Equation> equations = new ArrayList<>();
          CompareVisitor.Result result = compare(argExpectedType, argActualType, equations);
          if (result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
            errors.add(new TypeMismatchError(piArgs.get(i).getType(), lambdaArgs.get(i).expression, expr, getNames(myLocalContext)));
          } else {
            resultEquations.addAll(equations);
          }
        }
      } else {
        argumentTypes.add(Tele(piArgs.get(i).getExplicit(), vars(lambdaArgs.get(i).name), piArgs.get(i).getType()));
      }
      myLocalContext.add(new TypedBinding(lambdaArgs.get(i).name, argumentTypes.get(i).getType()));
    }

    Result bodyResult = typeCheck(expr.getBody(), fresultType instanceof InferHoleExpression ? null : resultType);

    for (int i = 0; i < lambdaArgs.size(); ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }

    if (!(bodyResult instanceof OKResult)) return bodyResult;
    OKResult okBodyResult = (OKResult) bodyResult;
    addLiftedEquations(okBodyResult, resultEquations, lambdaArgs.size());

    if (resultType instanceof InferHoleExpression) {
      Expression actualType = okBodyResult.type;
      if (lambdaArgs.size() > actualNumberOfPiArgs) {
        actualType = Pi(argumentTypes.subList(actualNumberOfPiArgs, lambdaArgs.size()), actualType);
      }
      Expression expr1 = actualType.normalize(NormalizeVisitor.Mode.NF).liftIndex(0, -actualNumberOfPiArgs);
      if (expr1 != null) {
        resultEquations.add(new CompareVisitor.Equation((InferHoleExpression) resultType, expr1));
      }
    }

    List<Argument> resultLambdaArgs = new ArrayList<>(argumentTypes.size());
    for (TypeArgument argumentType : argumentTypes) {
      resultLambdaArgs.add(argumentType);
    }
    OKResult result = new OKResult(Lam(resultLambdaArgs, okBodyResult.expression), Pi(argumentTypes, okBodyResult.type), resultEquations);
    expr.setWellTyped(result.expression);
    return result;
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    OKResult[] domainResults = new OKResult[expr.getArguments().size()];
    int numberOfVars = 0;
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArguments().get(i).getType(), Universe());
      if (!(result instanceof OKResult)) return result;
      domainResults[i] = (OKResult) result;
      addLiftedEquations(domainResults[i], equations, numberOfVars);
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        List<String> names = ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames();
        for (int j = 0; j < names.size(); ++j) {
          myLocalContext.add(new TypedBinding(names.get(j), domainResults[i].expression.liftIndex(0, j)));
          ++numberOfVars;
        }
      } else {
        myLocalContext.add(new TypedBinding(null, domainResults[i].expression));
        ++numberOfVars;
      }
    }

    Result codomainResult = typeCheck(expr.getCodomain(), Universe());
    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }
    if (!(codomainResult instanceof OKResult)) return codomainResult;
    OKResult okCodomainResult = (OKResult) codomainResult;

    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (int i = 0; i < domainResults.length; ++i) {
      Universe argUniverse = ((UniverseExpression) domainResults[i].type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        String msg = "Universe " + argUniverse + " of " + (i + 1) + suffix(i + 1) + " argument is not compatible with universe " + universe + " of previous arguments";
        TypeCheckingError error = new TypeCheckingError(msg, expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myErrors.add(error);
        return null;
      }
      universe = maxUniverse;
    }
    Universe codomainUniverse = ((UniverseExpression) okCodomainResult.type).getUniverse();
    Universe maxUniverse = universe.max(codomainUniverse);
    if (maxUniverse == null) {
      String msg = "Universe " + codomainUniverse + " the codomain is not compatible with universe " + universe + " of arguments";
      TypeCheckingError error = new TypeCheckingError(msg, expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }
    Expression actualType = new UniverseExpression(maxUniverse);

    addLiftedEquations(okCodomainResult, equations, numberOfVars);

    List<TypeArgument> resultArguments = new ArrayList<>(domainResults.length);
    for (int i = 0; i < domainResults.length; ++i) {
      resultArguments.add(argFromArgResult(expr.getArguments().get(i), domainResults[i]));
    }
    return checkResult(expectedType, new OKResult(Pi(resultArguments, okCodomainResult.expression), actualType, equations), expr);
  }

  private TypeArgument argFromArgResult(Abstract.TypeArgument arg, OKResult argResult) {
    if (arg instanceof Abstract.TelescopeArgument) {
      return new TelescopeArgument(arg.getExplicit(), ((Abstract.TelescopeArgument) arg).getNames(), argResult.expression);
    } else {
      return new TypeArgument(arg.getExplicit(), argResult.expression);
    }
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    return checkResult(expectedType, new OKResult(new UniverseExpression(expr.getUniverse()), new UniverseExpression(expr.getUniverse().succ()), null), expr);
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Expression expectedType) {
    ListIterator<Binding> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Binding def = it.previous();
      if (expr.getName().equals(def.getName())) {
        return checkResultImplicit(expectedType, new OKResult(Index(index), def.getType().liftIndex(0, index + 1), null), expr);
      }
      ++index;
    }

    NotInScopeError error = new NotInScopeError(expr, expr.getName());
    expr.setWellTyped(Error(null, error));
    myErrors.add(error);
    return null;
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    TypeCheckingError error = new GoalError(myLocalContext, expectedType.normalize(NormalizeVisitor.Mode.NF), expr);
    return new InferErrorResult(new InferHoleExpression(error), error, null);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    TypeCheckingError error = new ArgInferenceError(expression(), expr, getNames(myLocalContext), null);
    expr.setWellTyped(Error(null, error));
    myErrors.add(error);
    return null;
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    if (expectedType != null) {
      Expression expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
      if (expectedTypeNorm instanceof SigmaExpression) {
        InferHoleExpression hole = expectedTypeNorm.accept(new FindHoleVisitor());
        if (hole != null) {
          return new InferErrorResult(hole, hole.getError(), null);
        }

        List<TypeArgument> sigmaArgs = new ArrayList<>();
        splitArguments(((SigmaExpression) expectedTypeNorm).getArguments(), sigmaArgs);

        if (expr.getFields().size() != sigmaArgs.size()) {
          TypeCheckingError error = new TypeCheckingError("Expected a tuple with " + sigmaArgs.size() + " fields, but given " + expr.getFields().size(), expr, getNames(myLocalContext));
          expr.setWellTyped(Error(null, error));
          myErrors.add(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Expression expression = Tuple(fields);
        List<CompareVisitor.Equation> equations = new ArrayList<>();
        for (int i = 0; i < sigmaArgs.size(); ++i) {
          List<Expression> substExprs = new ArrayList<>(fields.size());
          for (int j = fields.size() - 1; j >= 0; --j) {
            substExprs.add(fields.get(j));
          }

          Expression expType = sigmaArgs.get(i).getType().subst(substExprs, 0);
          Result result = typeCheck(expr.getFields().get(i), expType);
          if (!(result instanceof OKResult)) return result;
          OKResult okResult = (OKResult) result;
          fields.add(okResult.expression);
          if (okResult.equations != null) {
            equations.addAll(okResult.equations);
          }
        }
        return new OKResult(expression, expectedType, equations);
      } else {
        Expression fexpectedTypeNorm = expectedTypeNorm.getFunction(new ArrayList<Expression>());
        if (fexpectedTypeNorm instanceof InferHoleExpression) {
          return new InferErrorResult((InferHoleExpression) fexpectedTypeNorm, ((InferHoleExpression) fexpectedTypeNorm).getError(), null);
        }

        TypeCheckingError error = new TypeMismatchError(expectedTypeNorm, Sigma(args(TypeArg(Var("?")), TypeArg(Var("?")))), expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myErrors.add(error);
        return null;
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    Expression expression = Tuple(fields);
    List<TypeArgument> arguments = new ArrayList<>(expr.getFields().size());
    Expression type = Sigma(arguments);
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (Abstract.Expression field : expr.getFields()) {
      Result result = typeCheck(field, null);
      if (!(result instanceof OKResult)) return result;
      OKResult okResult = (OKResult) result;
      fields.add(okResult.expression);
      arguments.add(TypeArg(okResult.type));
      if (okResult.equations != null) {
        equations.addAll(okResult.equations);
      }
    }
    return new OKResult(expression, type, equations);
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression expectedType) {
    OKResult[] domainResults = new OKResult[expr.getArguments().size()];
    int numberOfVars = 0;
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArguments().get(i).getType(), Universe());
      if (!(result instanceof OKResult)) return result;
      domainResults[i] = (OKResult) result;
      addLiftedEquations(domainResults[i], equations, numberOfVars);
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        List<String> names = ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames();
        for (int j = 0; j < names.size(); ++j) {
          myLocalContext.add(new TypedBinding(names.get(j), domainResults[i].expression.liftIndex(0, j)));
          ++numberOfVars;
        }
      } else {
        myLocalContext.add(new TypedBinding(null, domainResults[i].expression));
        ++numberOfVars;
      }
    }

    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }

    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (int i = 0; i < domainResults.length; ++i) {
      Universe argUniverse = ((UniverseExpression) domainResults[i].type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        String msg = "Universe " + argUniverse + " of " + (i + 1) + suffix(i + 1) + " argument is not compatible with universe " + universe + " of previous arguments";
        TypeCheckingError error = new TypeCheckingError(msg, expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myErrors.add(error);
        return null;
      }
      universe = maxUniverse;
    }
    Expression actualType = new UniverseExpression(universe);

    List<TypeArgument> resultArguments = new ArrayList<>(domainResults.length);
    for (int i = 0; i < domainResults.length; ++i) {
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        resultArguments.add(new TelescopeArgument(expr.getArguments().get(i).getExplicit(), ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames(), domainResults[i].expression));
      } else {
        resultArguments.add(new TypeArgument(expr.getArguments().get(i).getExplicit(), domainResults[i].expression));
      }
    }
    return checkResult(expectedType, new OKResult(Sigma(resultArguments), actualType, equations), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, Expression expectedType) {
    List<Abstract.ArgumentExpression> args = new ArrayList<>(2);
    args.add(expr.getLeft());
    args.add(expr.getRight());

    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : null;
    return typeCheckFunctionApps(new Concrete.DefCallExpression(position, expr.getBinOp()), args, expectedType, expr);
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression expectedType) {
    TypeCheckingError error = null;
    if (expr.getElimType() == Abstract.ElimExpression.ElimType.CASE) {
      error = new TypeCheckingError("Not implemented yet: \\case", expr, getNames(myLocalContext));
    }
    if (expectedType == null && error == null) {
      error = new TypeCheckingError("Cannot infer type of the expression", expr, getNames(myLocalContext));
    }
    if (mySide != Side.LHS && error == null) {
      error = new TypeCheckingError("\\elim is allowed only at the root of a definition", expr, getNames(myLocalContext));
    }

    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult exprOKResult = (OKResult) exprResult;
    if (!(exprOKResult.expression instanceof IndexExpression) && error == null) {
      error = new TypeCheckingError("\\elim can be applied only to a local variable", expr.getExpression(), getNames(myLocalContext));
    }

    List<Expression> parameters = new ArrayList<>();
    Expression type = exprOKResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    Expression ftype = type.getFunction(parameters);
    if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition) && error == null) {
      error = new TypeMismatchError("a data type", type, expr.getExpression(), getNames(myLocalContext));
    }

    if (error != null) {
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }

    int varIndex = ((IndexExpression) exprOKResult.expression).getIndex();
    DataDefinition dataType = (DataDefinition) ((DefCallExpression) ftype).getDefinition();
    List<Constructor> constructors = new ArrayList<>(dataType.getConstructors());
    List<Clause> clauses = new ArrayList<>(dataType.getConstructors().size());
    for (int i = 0; i < dataType.getConstructors().size(); ++i) {
      clauses.add(null);
    }

    Result errorResult = null;
    boolean wasError = false;

    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause == null) continue;

      int index;
      for (index = 0; index < constructors.size(); ++index) {
        if (constructors.get(index).getName().equals(clause.getName())) {
          break;
        }
      }
      if (index == constructors.size()) {
        for (index = 0; index < dataType.getConstructors().size(); ++index) {
          if (dataType.getConstructors().get(index).getName().equals(clause.getName())) {
            break;
          }
        }
        if (index == dataType.getConstructors().size()) {
          error = new NotInScopeError(clause, clause.getName() == null ? "" : clause.getName());
        } else {
          error = new TypeCheckingError("Overlapping pattern matching: " + dataType.getConstructors().get(index), clause, getNames(myLocalContext));
        }
        expr.setWellTyped(Error(null, error));
        myErrors.add(error);
        continue;
      }

      Constructor constructor = constructors.get(index);
      constructors.remove(index);

      if (constructor.hasErrors()) {
        error = new HasErrors(constructor.getName(), clause);
        clause.getExpression().setWellTyped(Error(null, error));
        myErrors.add(error);
        continue;
      }

      List<TypeArgument> constructorArguments = new ArrayList<>();
      splitArguments(constructor.getType(), constructorArguments);
      if (clause.getArguments().size() != constructorArguments.size()) {
        String msg = "Expected " + constructorArguments.size() + " arguments to " + constructor.getName() + ", but given " + clause.getArguments().size();
        error = new TypeCheckingError(msg, clause, getNames(myLocalContext));
        clause.getExpression().setWellTyped(Error(null, error));
        myErrors.add(error);
        continue;
      }

      List<NameArgument> arguments = new ArrayList<>(clause.getArguments().size());
      for (int i = 0; i < clause.getArguments().size(); ++i) {
        arguments.add(new NameArgument(clause.getArguments().get(i).getExplicit(), clause.getArguments().get(i).getName()));
      }

      Expression substExpr = DefCall(constructor);
      for (int j = constructorArguments.size() - 1; j >= 0; --j) {
        substExpr = Apps(substExpr, new ArgumentExpression(Index(j + varIndex), constructorArguments.get(j).getExplicit(), !constructorArguments.get(j).getExplicit()));
      }
      Expression clauseExpectedType = expectedType.liftIndex(varIndex + 1, constructorArguments.size()).subst(substExpr, varIndex);

      List<Binding> localContext = new ArrayList<>(myLocalContext.size() - 1 + clause.getArguments().size());
      for (int i = 0; i < myLocalContext.size() - 1 - varIndex; ++i) {
        localContext.add(myLocalContext.get(i));
      }
      for (int i = 0; i < constructorArguments.size(); ++i) {
        localContext.add(new TypedBinding(arguments.get(i).getName(), constructorArguments.get(i).getType().subst(parameters, 0)));
      }
      for (int i = myLocalContext.size() - varIndex; i < myLocalContext.size(); ++i) {
        int i0 = i - myLocalContext.size() + varIndex;
        localContext.add(new TypedBinding(myLocalContext.get(i).getName(), myLocalContext.get(i).getType().liftIndex(i0 + 1, constructorArguments.size()).subst(substExpr, i0)));
      }

      Side side = clause.getArrow() == Abstract.Definition.Arrow.RIGHT || !(clause.getExpression() instanceof Abstract.ElimExpression && ((Abstract.ElimExpression) clause.getExpression()).getElimType() == Abstract.ElimExpression.ElimType.ELIM) ? Side.RHS : Side.LHS;
      Result clauseResult = new CheckTypeVisitor(myParent, localContext, myAbstractCalls, myErrors, side).typeCheck(clause.getExpression(), clauseExpectedType);
      if (!(clauseResult instanceof OKResult)) {
        wasError = true;
        if (errorResult == null) {
          errorResult = clauseResult;
        } else
        if (clauseResult instanceof InferErrorResult) {
          myErrors.add(((InferErrorResult) errorResult).error);
          errorResult = clauseResult;
        }
      } else {
        clauses.set(constructor.getIndex(), new Clause(constructor, arguments, clause.getArrow(), clauseResult.expression, null));
      }
    }

    if (wasError) {
      return errorResult;
    }

    if (!constructors.isEmpty()) {
      String msg = "Incomplete pattern matching";
      if (!dataType.equals(Prelude.INTERVAL)) {
        msg += ". Unhandled constructors: ";
        for (int i = 0; i < constructors.size(); ++i) {
          if (i > 0) msg += ", ";
          msg += constructors.get(i).getName();
        }
      }
      error = new TypeCheckingError(msg + ".", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
    }

    Clause otherwise = null;
    if (expr.getOtherwise() != null) {
      Side side = expr.getOtherwise().getArrow() == Abstract.Definition.Arrow.RIGHT || !(expr.getOtherwise().getExpression() instanceof Abstract.ElimExpression && ((Abstract.ElimExpression) expr.getOtherwise().getExpression()).getElimType() == Abstract.ElimExpression.ElimType.ELIM) ? Side.RHS : Side.LHS;
      CheckTypeVisitor visitor = side != mySide ? new CheckTypeVisitor(myParent, myLocalContext, myAbstractCalls, myErrors, side) : this;
      Result clauseResult = visitor.typeCheck(expr.getOtherwise().getExpression(), expectedType);
      if (clauseResult instanceof InferErrorResult) {
        return clauseResult;
      }
      if (clauseResult != null) {
        otherwise = new Clause(null, null, expr.getOtherwise().getArrow(), clauseResult.expression, null);
      }
    }

    ElimExpression result = Elim(expr.getElimType(), (IndexExpression) exprOKResult.expression, clauses, otherwise);
    for (Clause clause : clauses) {
      if (clause != null) {
        clause.setElimExpression(result);
      }
    }
    if (otherwise != null) {
      otherwise.setElimExpression(result);
    }
    return new OKResult(result, expectedType, null);
  }

  @Override
  public Result visitFieldAcc(Abstract.FieldAccExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression type = okExprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    boolean notInScope = false;

    if (type instanceof ClassExtExpression || type instanceof DefCallExpression && ((DefCallExpression) type).getDefinition() instanceof ClassDefinition) {
      Definition parent = type instanceof ClassExtExpression ? ((ClassExtExpression) type).getBaseClass() : ((DefCallExpression) type).getDefinition();
      Definition child = parent.findChild(expr.getName());
      if (child != null) {
        if (child.hasErrors()) {
          TypeCheckingError error = new HasErrors(child.getName(), expr);
          expr.setWellTyped(Error(DefCall(child), error));
          myErrors.add(error);
          return null;
        } else {
          Definition resultDef = child;
          if (type instanceof ClassExtExpression && child instanceof FunctionDefinition) {
            OverriddenDefinition overridden = ((ClassExtExpression) type).getDefinitionsMap().get(child);
            if (overridden != null) {
              resultDef = overridden;
            }
          }
          Expression resultType = resultDef.getType();
          if (resultType == null) {
            resultType = child.getType();
          }
          return checkResult(expectedType, new OKResult(FieldAcc(okExprResult.expression, resultDef), resultType.accept(new ReplaceDefCallVisitor(parent, okExprResult.expression)), okExprResult.equations), expr);
        }
      }
      notInScope = true;
    }

    TypeCheckingError error;
    if (notInScope) {
      error = new NotInScopeError(expr, expr.getName());
    } else {
      error = new TypeCheckingError("Expected an expression of a class type", expr.getExpression(), getNames(myLocalContext));
    }
    expr.setWellTyped(Error(null, error));
    myErrors.add(error);
    return null;
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression type = okExprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
    if (!(type instanceof SigmaExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected an expression of a sigma type", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }

    List<TypeArgument> splitArgs = new ArrayList<>();
    splitArguments(((SigmaExpression) type).getArguments(), splitArgs);
    if (expr.getField() < 0 || expr.getField() >= splitArgs.size()) {
      TypeCheckingError error = new TypeCheckingError("Index " + (expr.getField() + 1) + " out of range", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }

    List<Expression> exprs = new ArrayList<>(expr.getField());
    for (int i = expr.getField() - 1; i >= 0; --i) {
      exprs.add(Proj(okExprResult.expression, i));
    }
    return checkResult(expectedType, new OKResult(Proj(okExprResult.expression, expr.getField()), splitArgs.get(expr.getField()).getType().subst(exprs, 0), okExprResult.equations), expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression expectedType) {
    TypeCheckingError error = null;
    if (expr.getBaseClass().hasErrors()) {
      error = new HasErrors(expr.getBaseClass().getName(), expr);
    } else {
      if (!expr.getBaseClass().isRelativelyStatic(myParent)) {
        error = new TypeCheckingError("Non-static method call", expr, getNames(myLocalContext));
      }
    }

    if (error != null) {
      expr.setWellTyped(Error(DefCall(expr.getBaseClass()), error));
      myErrors.add(error);
      return null;
    }

    if (expr.getDefinitions().isEmpty()) {
      return checkResultImplicit(expectedType, new OKResult(DefCall(expr.getBaseClass()), expr.getBaseClass().getType(), null), expr);
    }

    Map<String, FunctionDefinition> abstracts = new HashMap<>(expr.getBaseClass().getAbstracts());
    Map<FunctionDefinition, OverriddenDefinition> definitions = new HashMap<>();
    for (Abstract.FunctionDefinition definition : expr.getDefinitions()) {
      FunctionDefinition oldDefinition = abstracts.remove(definition.getName());
      if (oldDefinition == null) {
        myErrors.add(new TypeCheckingError(definition.getName() + " is not defined in " + expr.getBaseClass().getFullName(), definition, getNames(myLocalContext)));
      } else {
        OverriddenDefinition newDefinition = new OverriddenDefinition(definition.getName(), expr.getBaseClass(), definition.getPrecedence(), definition.getFixity(), definition.getArrow());
        newDefinition.setOverriddenFunction(oldDefinition);
        new DefinitionCheckTypeVisitor(newDefinition, myErrors).visitFunction(definition, myLocalContext);
        definitions.put(oldDefinition, newDefinition);
      }
    }

    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    for (FunctionDefinition definition : abstracts.values()) {
      universe = universe.max(definition.getUniverse());
    }
    return checkResultImplicit(expectedType, new OKResult(ClassExt(expr.getBaseClass(), definitions), new UniverseExpression(universe), null), expr);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression normExpr = okExprResult.expression.accept(new NormalizeVisitor(NormalizeVisitor.Mode.WHNF));
    if (!(normExpr instanceof DefCallExpression && ((DefCallExpression) normExpr).getDefinition() instanceof ClassDefinition || normExpr instanceof ClassExtExpression)) {
      TypeCheckingError error = new TypeCheckingError("Expected a class", expr.getExpression(), getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrors.add(error);
      return null;
    }
    return checkResultImplicit(expectedType, new OKResult(New(okExprResult.expression), normExpr, okExprResult.equations), expr);
  }

  private class CheckTypeContextSaver implements AutoCloseable {
    final int myOldContextSize = myLocalContext.size();

    @Override
    public void close() {
      trimToSize(myLocalContext, myOldContextSize);
    }
  }

  private Result visitLetClause(Abstract.LetClause clause) {
    List<Argument> args = new ArrayList<>();
    Expression resultType;
    Expression term;
    List<CompareVisitor.Equation> equations = new ArrayList<>();

    try (CheckTypeContextSaver saver = new CheckTypeContextSaver()) {

      int numVarsPassed = 0;
      for (int i = 0; i < clause.getArguments().size(); i++) {
        if (clause.getArguments().get(i) instanceof TypeArgument) {
          final TypeArgument typeArgument = (TypeArgument) clause.getArguments().get(i);
          final Result result = typeCheck(typeArgument.getType(), Universe());
          if (!(result instanceof OKResult)) return result;
          final OKResult okResult = (OKResult) result;
          args.add(argFromArgResult(typeArgument, okResult));
          addLiftedEquations(okResult, equations, numVarsPassed);
          if (typeArgument instanceof Abstract.TelescopeArgument) {
            List<String> names = ((Abstract.TelescopeArgument) typeArgument).getNames();
            for (int j = 0; j < names.size(); ++j) {
              myLocalContext.add(new TypedBinding(names.get(j), okResult.expression.liftIndex(0, j)));
              ++numVarsPassed;
            }
          } else {
            myLocalContext.add(new TypedBinding(null, okResult.expression));
            ++numVarsPassed;
          }
        } else {
          throw new IllegalStateException();
        }
      }

      Expression expectedType = null;
      if (clause.getResultType() != null) {
        Result result = typeCheck(clause.getResultType(), null);
        if (!(result instanceof OKResult)) return result;
        addLiftedEquations((OKResult) result, equations, numVarsPassed);
        expectedType = result.expression;
      }
      Result termResult = typeCheck(clause.getTerm(), expectedType);
      if (!(termResult instanceof OKResult)) return termResult;
      addLiftedEquations((OKResult) termResult, equations, numVarsPassed);

      term = ((OKResult) termResult).expression;
      resultType = ((OKResult) termResult).type;
    }

    myLocalContext.add(new LetClause(clause.getName(), args, resultType, clause.getArrow(), term));
    return new OKResult(null, null, equations);
  }

  private void addLiftedEquations(OKResult okResult, List<CompareVisitor.Equation> equations, int numVarsPassed) {
    if (okResult.equations != null) {
      for (CompareVisitor.Equation equation : okResult.equations) {
        Expression expr1 = equation.expression.liftIndex(0, -numVarsPassed);
        if (expr1 != null) {
          equations.add(new CompareVisitor.Equation(equation.hole, expr1));
        }
      }
    }
  }

  @Override
  public Result visitLet(Abstract.LetExpression letExpression, Expression expectedType) {
    try (CheckTypeContextSaver context = new CheckTypeContextSaver()) {
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      for (int i = 0; i < letExpression.getClauses().size(); i++) {
        final Result clauseResult = visitLetClause(letExpression.getClauses().get(i));
        if (!(clauseResult instanceof OKResult)) return clauseResult;
        addLiftedEquations((OKResult) clauseResult, equations, i);
      }
      final Result result = typeCheck(letExpression.getExpression(), expectedType);
      if (!(result instanceof OKResult)) return result;
      final OKResult okResult = (OKResult) result;
      // TODO: check for occurence of the let bound variable
      return okResult;
    }
  }
}
