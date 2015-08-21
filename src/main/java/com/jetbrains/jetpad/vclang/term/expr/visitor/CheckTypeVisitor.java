package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.error.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.Utils;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.*;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.Name;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandArgs;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.patternMatchAll;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final Definition myParent;
  private final List<Binding> myLocalContext;
  private final ModuleLoader myModuleLoader;
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

  public static class LetClauseResult extends Result {
    LetClause letClause;

    public LetClauseResult(LetClause letClause, List<CompareVisitor.Equation> equations) {
      this.letClause = letClause;
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

  public CheckTypeVisitor(Definition parent, List<Binding> localContext, Set<Definition> abstractCalls, ModuleLoader moduleLoader, Side side) {
    myParent = parent;
    myLocalContext = localContext;
    myAbstractCalls = abstractCalls;
    myModuleLoader = moduleLoader;
    mySide = side;
  }

  private Result checkResult(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
    }
    Expression actualNorm = result.type.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
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
      TypeCheckingError error = new TypeMismatchError(myParent, expectedNorm, actualNorm, expression, getNames(myLocalContext));
      expression.setWellTyped(Error(result.expression, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
      return null;
    }
  }

  private Result checkResultImplicit(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
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
              Expression expr1 = ((Expression) argsImp[i].expression).normalize(NormalizeVisitor.Mode.NF, myLocalContext);
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
                myModuleLoader.getTypeCheckingErrors().add(new InferredArgumentsMismatch(myParent, i + 1, options, fun, getNames(myLocalContext)));
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

  private boolean typeCheckArgs(Arg[] argsImp, Result[] resultArgs, List<TypeArgument> signature, List<CompareVisitor.Equation> resultEquations, int startIndex, int parametersNumber, Abstract.Expression fun) {
    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] == null) {
        TypeCheckingError error = new ArgInferenceError(myParent, i < parametersNumber ? parameter(i + 1) : functionArg(i - parametersNumber + 1), fun, getNames(myLocalContext), fun);
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

      Result result;
      /* if (argsImp[i].expression instanceof Expression) {
        List<Expression> context = new ArrayList<>(myLocalContext.size());
        for (Binding binding : myLocalContext) {
          context.add(binding.getType());
        }
        Expression actualType = ((Expression) argsImp[i].expression).getType(context);
        result = checkResult(type, new OKResult((Expression) argsImp[i].expression, actualType, null), argsImp[i].expression);
      } else { */
        result = typeCheck(argsImp[i].expression, type);
      // }
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
    Result function;
    if (fun instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) fun).getDefinition() instanceof Constructor && !((Constructor) ((Abstract.DefCallExpression) fun).getDefinition()).getDataType().getParameters().isEmpty()) {
      function = typeCheckDefCall((Abstract.DefCallExpression) fun, null);
      if (function instanceof OKResult) {
        return typeCheckApps(fun, 0, (OKResult) function, args, expectedType, expression);
      }
    } else {
      function = typeCheck(fun, null);
      if (function instanceof OKResult) {
        return typeCheckApps(fun, 0, (OKResult) function, args, expectedType, expression);
      }
    }

    if (function instanceof InferErrorResult) {
      myModuleLoader.getTypeCheckingErrors().add(((InferErrorResult) function).error);
    }
    for (Abstract.ArgumentExpression arg : args) {
      typeCheck(arg.getExpression(), null);
    }
    return null;
  }

  private Result typeCheckApps(Abstract.Expression fun, int argsSkipped, OKResult okFunction, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression expression) {
    List<TypeArgument> signatureArguments = new ArrayList<>();
    int parametersNumber = 0;
    if (okFunction.expression instanceof DefCallExpression) {
      Definition def = ((DefCallExpression) okFunction.expression).getDefinition();
      if (def instanceof Constructor) {
        if (((Constructor) def).getPatterns() == null) {
          parametersNumber = numberOfVariables(((Constructor) def).getDataType().getParameters());
        } else {
          parametersNumber = expandArgs(((Constructor) def).getPatterns(), ((Constructor) def).getDataType().getParameters()).size();
        }
      }
    }

    Expression signatureResultType = splitArguments(okFunction.type, signatureArguments);
    assert parametersNumber <= signatureArguments.size();
    Arg[] argsImp = new Arg[signatureArguments.size()];
    int i, j;
    for (i = 0; i < parametersNumber; ++i) {
      argsImp[i] = new Arg(false, null, new InferHoleExpression(new ArgInferenceError(myParent, parameter(i + 1), fun, getNames(myLocalContext), fun)));
    }
    for (j = 0; i < signatureArguments.size() && j < args.size(); ++i) {
      if (args.get(j).isExplicit() == signatureArguments.get(i).getExplicit()) {
        argsImp[i] = new Arg(args.get(j).isHidden(), null, args.get(j).getExpression());
        ++j;
      } else
      if (args.get(j).isExplicit()) {
        argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(myParent, functionArg(i - parametersNumber + 1), fun, getNames(myLocalContext), fun)));
      } else {
        TypeCheckingError error = new TypeCheckingError(myParent, "Unexpected implicit argument", args.get(j).getExpression(), getNames(myLocalContext));
        args.get(j).getExpression().setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        for (Abstract.ArgumentExpression arg : args) {
          typeCheck(arg.getExpression(), null);
        }
        return null;
      }
    }

    if (j < args.size() && signatureArguments.isEmpty()) {
      TypeCheckingError error = new TypeCheckingError(myParent, "Function expects " + (argsSkipped + i) + " arguments, but is applied to " + (argsSkipped + i + args.size() - j), fun, getNames(myLocalContext));
      fun.setWellTyped(Error(okFunction.expression, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
      for (Abstract.ArgumentExpression arg : args) {
        typeCheck(arg.getExpression(), null);
      }
      return null;
    }

    if (okFunction.expression instanceof DefCallExpression && ((DefCallExpression) okFunction.expression).getDefinition() == Prelude.PATH_CON && args.size() == 1 && j == 1) {
      Expression argExpectedType = null;
      InferHoleExpression holeExpression = null;
      if (expectedType != null) {
        List<Expression> argsExpectedType = new ArrayList<>(3);
        Expression fexpectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext).getFunction(argsExpectedType);
        if (fexpectedType instanceof DefCallExpression && ((DefCallExpression) fexpectedType).getDefinition().equals(Prelude.PATH) && argsExpectedType.size() == 3) {
          if (argsExpectedType.get(2) instanceof InferHoleExpression) {
            holeExpression = (InferHoleExpression) argsExpectedType.get(2);
          } else {
            argExpectedType = Pi("i", DefCall(Prelude.INTERVAL), Apps(argsExpectedType.get(2).liftIndex(0, 1), Index(0)));
          }
        }
      }

      InferHoleExpression inferHoleExpr = null;
      if (argExpectedType == null) {
        inferHoleExpr = new InferHoleExpression(new ArgInferenceError(myParent, type(), args.get(0).getExpression(), getNames(myLocalContext), args.get(0).getExpression()));
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
      Expression parameter1 = Lam(lamArgs(Tele(vars("i"), DefCall(Prelude.INTERVAL))), type);
      Expression parameter2 = Apps(argResult.expression, DefCall(Prelude.LEFT));
      Expression parameter3 = Apps(argResult.expression, DefCall(Prelude.RIGHT));
      Expression resultType = Apps(DefCall(Prelude.PATH), parameter1, parameter2, parameter3);
      List<CompareVisitor.Equation> resultEquations = argResult.equations;
      if (holeExpression != null) {
        if (resultEquations == null) {
          resultEquations = new ArrayList<>(1);
        }
        resultEquations.add(new CompareVisitor.Equation(holeExpression, Lam(lamArgs(Tele(vars("i"), DefCall(Prelude.INTERVAL))), type.normalize(NormalizeVisitor.Mode.NF, myLocalContext))));
      }

      List<Expression> parameters = new ArrayList<>(3);
      parameters.add(parameter1);
      parameters.add(parameter2);
      parameters.add(parameter3);
      Expression resultExpr = Apps(DefCall(null, Prelude.PATH_CON, parameters), new ArgumentExpression(argResult.expression, true, false));
      return checkResult(expectedType, new OKResult(resultExpr, resultType, resultEquations), expression);
    }

    if (expectedType != null && j == args.size()) {
      for (; i < signatureArguments.size() - numberOfVariables(expectedType); ++i) {
        if (signatureArguments.get(i).getExplicit()) {
          break;
        } else {
          argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(myParent, functionArg(i + 1), fun, getNames(myLocalContext), fun)));
        }
      }
    }

    int argsNumber = i;
    Result[] resultArgs = new Result[argsNumber];
    List<CompareVisitor.Equation> resultEquations = new ArrayList<>();
    if (!typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, 0, parametersNumber, fun)) {
      expression.setWellTyped(Error(null, myModuleLoader.getTypeCheckingErrors().get(myModuleLoader.getTypeCheckingErrors().size() - 1)));
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
      Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
      Expression actualNorm = resultType.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      CompareVisitor.Result result = compare(actualNorm, expectedNorm, equations);

      if (result instanceof CompareVisitor.JustResult && result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
        Expression resultExpr = okFunction.expression;
        for (i = parametersNumber; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }

        TypeCheckingError error = new TypeMismatchError(myParent, expectedNorm, actualNorm, expression, getNames(myLocalContext));
        expression.setWellTyped(Error(resultExpr, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        return null;
      }

      int found = solveEquations(argsNumber, argsImp, resultArgs, equations, resultEquations, fun);
      if (found < 0 || (found != argsNumber && !typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, found, parametersNumber, fun))) {
        Expression resultExpr = okFunction.expression;
        for (i = parametersNumber; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i] == null ? new InferHoleExpression(null) : resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }
        expression.setWellTyped(Error(resultExpr, myModuleLoader.getTypeCheckingErrors().get(myModuleLoader.getTypeCheckingErrors().size() - 1)));
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
    List<Expression> parameters = null;
    if (parametersNumber > 0) {
      parameters = new ArrayList<>(parametersNumber);
      ((DefCallExpression) resultExpr).setParameters(parameters);
    }
    for (i = 0; i < parametersNumber; ++i) {
      assert parameters != null;
      parameters.add(resultArgs[i].expression);
    }
    for (; i < argsNumber; ++i) {
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
          error = new ArgInferenceError(myParent, functionArg(argIndex - parametersNumber), fun, getNames(myLocalContext), fun);
        } else {
          error = new ArgInferenceError(myParent, parameter(argIndex), fun, null, new StringPrettyPrintable(((Constructor) ((DefCallExpression) okFunction.expression).getDefinition()).getDataType().getName()));
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
    myModuleLoader.getTypeCheckingErrors().add(errorResult.error);
    return null;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    return typeCheckFunctionApps(Abstract.getFunction(expr, args), args, expectedType, expr);
  }

  private Result typeCheckDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    TypeCheckingError error;
    ClassDefinition parent = null;
    OKResult result = null;
    if (expr.getDefinition() == null) {
      assert expr.getExpression() != null;

      Result exprResult = typeCheck(expr.getExpression(), null);
      if (!(exprResult instanceof OKResult)) return exprResult;
      OKResult okExprResult = (OKResult) exprResult;
      Expression type = okExprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
      boolean notInScope = false;

      if (type instanceof ClassExtExpression || type instanceof DefCallExpression && ((DefCallExpression) type).getDefinition() instanceof ClassDefinition) {
        parent = type instanceof ClassExtExpression ? ((ClassExtExpression) type).getBaseClass() : (ClassDefinition) ((DefCallExpression) type).getDefinition();
        Definition child = parent.getField(expr.getName().name);
        if (child != null) {
          if (child.hasErrors()) {
            error = new HasErrors(myParent, child.getName(), expr);
            expr.setWellTyped(Error(DefCall(child), error));
            myModuleLoader.getTypeCheckingErrors().add(error);
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
            result = new OKResult(DefCall(okExprResult.expression, resultDef), resultType, okExprResult.equations);
          }
        }
        notInScope = true;
      } else if (okExprResult.type instanceof UniverseExpression) {
        Expression expression = okExprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
        if (expression instanceof DefCallExpression && ((DefCallExpression) expression).getDefinition() instanceof ClassDefinition) {
          Definition field = ((DefCallExpression) expression).getDefinition().getStaticField(expr.getName().name);
          if (field == null) {
            notInScope = true;
          } else {
            result = new OKResult(DefCall(field), field.getType(), okExprResult.equations);
          }
        } else {
          List<Expression> arguments = new ArrayList<>();
          Expression function = expression.getFunction(arguments);
          Collections.reverse(arguments);
          if (function instanceof DefCallExpression && ((DefCallExpression) function).getDefinition() instanceof DataDefinition) {
            Constructor constructor = (Constructor) ((DefCallExpression) function).getDefinition().getStaticField(expr.getName().name);
            if (constructor == null) {
              notInScope = true;
            } else {
              if (constructor.getPatterns() != null) {
                Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), arguments, myLocalContext);
                if (matchResult.expressions == null) {
                  error = new TypeCheckingError(myParent, "Constructor is not appropriate, failed to match data type parameters. " +
                      "Expected " + matchResult.failedPattern + ", got " + matchResult.actualExpression, expr, getNames(myLocalContext));
                  myModuleLoader.getTypeCheckingErrors().add(error);
                  expr.setWellTyped(Error(null, error));
                  return null;
                }
                arguments = matchResult.expressions;
              }
              Collections.reverse(arguments);
              Expression resultType = constructor.getType().subst(arguments, 0);
              Collections.reverse(arguments);
              return checkResultImplicit(expectedType, new OKResult(DefCall(null, constructor, arguments), resultType, okExprResult.equations), expr);
            }
          }
        }
      }

      if (result == null) {
        if (notInScope) {
          error = new NotInScopeError(myParent, expr, expr.getName());
        } else {
          error = new TypeCheckingError(myParent, "Expected an expression of a class type or a data type", expr.getExpression(), getNames(myLocalContext));
        }
        expr.setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        return null;
      }
    } else {
      if (expr.getDefinition() instanceof FunctionDefinition && ((FunctionDefinition) expr.getDefinition()).typeHasErrors() || !(expr.getDefinition() instanceof FunctionDefinition) && expr.getDefinition().hasErrors()) {
        error = new HasErrors(myParent, expr.getName(), expr);
        expr.setWellTyped(Error(DefCall(expr.getDefinition()), error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        return null;
      }

      if (expr.getDefinition().isAbstract()) {
        myAbstractCalls.add(expr.getDefinition());
      }

      result = new OKResult(DefCall(expr.getDefinition()), expr.getDefinition().getType(), null);
    }

    if (result.expression instanceof DefCallExpression) {
      if (((DefCallExpression) result.expression).getDefinition() instanceof Constructor) {
        Constructor constructor = ((Constructor) ((DefCallExpression) result.expression).getDefinition());
        List<TypeArgument> parameters = constructor.getDataType().getParameters();
        if (constructor.getPatterns() != null) {
          parameters = expandArgs(constructor.getPatterns(), parameters);
        }

        if (parameters != null && !parameters.isEmpty()) {
          result.type = Pi(parameters, result.type);
        }
      }
      if (((DefCallExpression) result.expression).getExpression() != null && parent != null) {
        result.type = result.type.accept(new ReplaceDefCallVisitor(parent, ((DefCallExpression) result.expression).getExpression()));
      }
    }
    return result;
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    Result result = typeCheckDefCall(expr, expectedType);
    if (result instanceof OKResult && result.expression instanceof DefCallExpression) {
      DefCallExpression defCall = (DefCallExpression) result.expression;
      if (defCall.getDefinition() instanceof Constructor) {
        if (defCall.getParameters() != null) {
          return result;
        }
        if (!((Constructor) defCall.getDefinition()).getDataType().getParameters().isEmpty()) {
          return typeCheckApps(expr, 0, (OKResult) result, new ArrayList<Abstract.ArgumentExpression>(0), expectedType, expr);
        }
      }
    }
    return result instanceof OKResult ? checkResultImplicit(expectedType, (OKResult) result, expr) : result;
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    OKResult result = getIndex(expr);
    result.type = result.type.liftIndex(0, ((IndexExpression) result.expression).getIndex() + 1);
    return checkResultImplicit(expectedType, result, expr);
  }

  private OKResult getIndex(Abstract.IndexExpression expr) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getType();
    return new OKResult(Index(expr.getIndex()), actualType, null);
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
        resultType = expectedType.splitAt(lambdaArgs.size(), piArgs).normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
        fresultType = resultType.getFunction(new ArrayList<Expression>());
        actualNumberOfPiArgs = piArgs.size();
        if (fresultType instanceof InferHoleExpression) {
          for (int i = piArgs.size(); i < lambdaArgs.size(); ++i) {
            piArgs.add(null);
          }
        }
      }

    if (piArgs.size() < lambdaArgs.size()) {
      TypeCheckingError error = new TypeCheckingError(myParent, "Expected a function of " + piArgs.size() + " arguments, but the lambda has " + lambdaArgs.size(), expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
      return null;
    }

    List<TypeCheckingError> errors = new ArrayList<>(lambdaArgs.size());
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (piArgs.get(i) == null && lambdaArgs.get(i).expression == null) {
        TypeCheckingError error = new ArgInferenceError(myParent, lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
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
            TypeCheckingError error = new ArgInferenceError(myParent, lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
            expr.setWellTyped(Error(null, error));
            return new InferErrorResult(hole, error, null);
          }
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression != null) {
        if (piArgs.get(i).getExplicit() != lambdaArgs.get(i).isExplicit) {
          errors.add(new TypeCheckingError(myParent, (i + 1) + suffix(i + 1) + " argument of the lambda should be " + (piArgs.get(i).getExplicit() ? "explicit" : "implicit"), expr, getNames(myLocalContext)));
        }
      }
    }
    if (!errors.isEmpty()) {
      expr.setWellTyped(Error(null, errors.get(0)));
      myModuleLoader.getTypeCheckingErrors().addAll(errors);
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
          Expression argExpectedType = piArgs.get(i).getType().normalize(NormalizeVisitor.Mode.NF, myLocalContext);
          Expression argActualType = argumentTypes.get(i).getType().normalize(NormalizeVisitor.Mode.NF, myLocalContext);
          List<CompareVisitor.Equation> equations = new ArrayList<>();
          CompareVisitor.Result result = compare(argExpectedType, argActualType, equations);
          if (result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
            errors.add(new TypeMismatchError(myParent, piArgs.get(i).getType(), lambdaArgs.get(i).expression, expr, getNames(myLocalContext)));
          } else {
            for (int j = 0; j < equations.size(); ++j) {
              Expression expression = equations.get(j).expression.liftIndex(0, -i);
              if (expression == null) {
                equations.remove(j--);
              } else {
                equations.set(j, new CompareVisitor.Equation(equations.get(j).hole, expression));
              }
            }
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
        Expression expr1 = actualType.normalize(NormalizeVisitor.Mode.NF, myLocalContext).liftIndex(0, -actualNumberOfPiArgs);
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
        myLocalContext.add(new TypedBinding((Name) null, domainResults[i].expression));
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
        TypeCheckingError error = new TypeCheckingError(myParent, msg, expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        return null;
      }
      universe = maxUniverse;
    }
    Universe codomainUniverse = ((UniverseExpression) okCodomainResult.type).getUniverse();
    Universe maxUniverse = universe.max(codomainUniverse);
    if (maxUniverse == null) {
      String msg = "Universe " + codomainUniverse + " the codomain is not compatible with universe " + universe + " of arguments";
      TypeCheckingError error = new TypeCheckingError(myParent, msg, expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
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
    OKResult result = getLocalVar(expr);
    if (result == null) {
      return null;
    }
    result.type = result.type.liftIndex(0, ((IndexExpression) result.expression).getIndex() + 1);
    return checkResultImplicit(expectedType, result, expr);
  }

  private OKResult getLocalVar(Abstract.VarExpression expr) {
    ListIterator<Binding> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Binding def = it.previous();
      if (expr.getName().equals(def.getName() == null ? null : def.getName().name)) {
        return new OKResult(Index(index), def.getType(), null);
      }
      ++index;
    }

    NotInScopeError error = new NotInScopeError(myParent, expr, expr.getName());
    expr.setWellTyped(Error(null, error));
    myModuleLoader.getTypeCheckingErrors().add(error);
    return null;
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    TypeCheckingError error = new GoalError(myParent, myLocalContext, expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext), expr);
    return new InferErrorResult(new InferHoleExpression(error), error, null);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    TypeCheckingError error = new ArgInferenceError(myParent, expression(), expr, getNames(myLocalContext), null);
    expr.setWellTyped(Error(null, error));
    myModuleLoader.getTypeCheckingErrors().add(error);
    return null;
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType != null) {
      expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      if (!(expectedTypeNorm instanceof SigmaExpression || expectedType instanceof InferHoleExpression)) {
        Expression fExpectedTypeNorm = expectedTypeNorm.getFunction(new ArrayList<Expression>());
        if (fExpectedTypeNorm instanceof InferHoleExpression) {
          return new InferErrorResult((InferHoleExpression) fExpectedTypeNorm, ((InferHoleExpression) fExpectedTypeNorm).getError(), null);
        }

        TypeCheckingError error = new TypeMismatchError(myParent, expectedTypeNorm, Sigma(args(TypeArg(Error(null, null)), TypeArg(Error(null, null)))), expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        return null;
      }

      if (expectedTypeNorm instanceof SigmaExpression) {
        InferHoleExpression hole = expectedTypeNorm.accept(new FindHoleVisitor());
        if (hole != null) {
          return new InferErrorResult(hole, hole.getError(), null);
        }

        List<TypeArgument> sigmaArgs = new ArrayList<>();
        splitArguments(((SigmaExpression) expectedTypeNorm).getArguments(), sigmaArgs);

        if (expr.getFields().size() != sigmaArgs.size()) {
          TypeCheckingError error = new TypeCheckingError(myParent, "Expected a tuple with " + sigmaArgs.size() + " fields, but given " + expr.getFields().size(), expr, getNames(myLocalContext));
          expr.setWellTyped(Error(null, error));
          myModuleLoader.getTypeCheckingErrors().add(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Expression expression = Tuple(fields, (SigmaExpression) expectedTypeNorm);
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
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    List<TypeArgument> arguments = new ArrayList<>(expr.getFields().size());
    SigmaExpression type = Sigma(arguments);
    Expression expression = Tuple(fields, type);
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (!(result instanceof OKResult)) return result;
      OKResult okResult = (OKResult) result;
      fields.add(okResult.expression);
      arguments.add(TypeArg(okResult.type.liftIndex(0, i)));
      if (okResult.equations != null) {
        equations.addAll(okResult.equations);
      }
    }

    if (expectedTypeNorm instanceof InferHoleExpression) {
      equations.add(new CompareVisitor.Equation((InferHoleExpression) expectedTypeNorm, type));
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
        myLocalContext.add(new TypedBinding((Name) null, domainResults[i].expression));
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
        TypeCheckingError error = new TypeCheckingError(myParent, msg, expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
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
    return typeCheckFunctionApps(new Concrete.DefCallExpression(position, null, expr.getBinOp()), args, expectedType, expr);
  }

  public static class ExpandPatternResult {
    public final Expression expression;
    public final Pattern pattern;
    public final int numBindings;

    public ExpandPatternResult(Expression expression, Pattern pattern, int numBindings) {
      this.expression = expression;
      this.pattern = pattern;
      this.numBindings = numBindings;
    }
  }

  private ExpandPatternResult expandPattern(Abstract.Pattern pattern, Binding binding, Abstract.Expression expr) {
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      if (name == null) {
        myLocalContext.add(binding);
      } else {
        myLocalContext.add(new TypedBinding(((Abstract.NamePattern) pattern).getName(), binding.getType()));
      }
      return new ExpandPatternResult(Index(0), new NamePattern(name, pattern.getExplicit()), 1);
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      TypeCheckingError error;
      Abstract.ConstructorPattern constructorPattern = (Abstract.ConstructorPattern) pattern;

      List<Expression> parameters = new ArrayList<>();
      Expression type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      Expression ftype = type.getFunction(parameters);
      Collections.reverse(parameters);
      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        error = new TypeMismatchError(myParent, "a data type", type, expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        return null;
      }
      DataDefinition dataType = (DataDefinition) ((DefCallExpression) ftype).getDefinition();

      Constructor constructor = null;
      for (int index = 0; index < dataType.getConstructors().size(); ++index) {
        if (dataType.getConstructors().get(index).getName().equals(constructorPattern.getConstructorName())) {
          constructor = dataType.getConstructors().get(index);
        }
      }

      if (constructor == null) {
        error = new NotInScopeError(myParent, pattern, constructorPattern.getConstructorName());
        myModuleLoader.getTypeCheckingErrors().add(error);
        expr.setWellTyped(Error(null, error));
        return null;
      }

      if (constructor.hasErrors()) {
        error = new HasErrors(myParent, constructor.getName(), pattern);
        myModuleLoader.getTypeCheckingErrors().add(error);
        expr.setWellTyped(Error(null, error));
        return null;
      }

      List<Expression> matchedParameters;
      if (constructor.getPatterns() != null) {
        Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), parameters, myLocalContext);
        if (matchResult.expressions == null) {
          error = new TypeCheckingError(myParent, "Constructor is not appropriate, failed to match data type parameters. " +
              "Expected " + matchResult.failedPattern + ", got " + matchResult.actualExpression, pattern, getNames(myLocalContext));
          myModuleLoader.getTypeCheckingErrors().add(error);
          expr.setWellTyped(Error(null, error));
          return null;
        }
        matchedParameters = matchResult.expressions;
      } else {
        matchedParameters = parameters;
      }
      Collections.reverse(matchedParameters);
      List<TypeArgument> constructorArguments = new ArrayList<>();
      splitArguments(constructor.getType().subst(matchedParameters, 0), constructorArguments);

      Utils.ProcessImplicitResult implicitResult = processImplicit(constructorPattern.getArguments(), constructorArguments);
      if (implicitResult.patterns == null) {
        if (implicitResult.wrongImplicitPosition < constructorPattern.getArguments().size()) {
          error = new TypeCheckingError(myParent, "Unexpected implicit argument", constructorPattern.getArguments().get(implicitResult.wrongImplicitPosition), getNames(myLocalContext));
        } else {
          error = new TypeCheckingError(myParent, "Too few explicit arguments, expected: " + implicitResult.numExplicit, constructorPattern, getNames(myLocalContext));
        }
        myModuleLoader.getTypeCheckingErrors().add(error);
        expr.setWellTyped(Error(null, error));
        return null;
      }
      List<Abstract.Pattern> patterns = implicitResult.patterns;

      List<Pattern> resultPatterns = new ArrayList<>();
      List<Expression> substituteExpressions = new ArrayList<>();
      int numBindings = 0;
      Expression substExpression = DefCall(null, constructor, parameters);
      for (int i = 0; i < constructorArguments.size(); ++i) {
        ExpandPatternResult result = expandPattern(patterns.get(i), new TypedBinding((String) null, constructorArguments.get(i).getType().subst(substituteExpressions, 0)), expr);
        if (result == null)
          return null;
        substituteExpressions.add(result.expression);
        substExpression = Apps(substExpression, result.expression);
        resultPatterns.add(result.pattern);
        numBindings += result.numBindings;
      }

      return new ExpandPatternResult(substExpression, new ConstructorPattern(constructor, resultPatterns, pattern.getExplicit()), numBindings);
    } else {
      throw new IllegalStateException();
    }
  }

  public ExpandPatternResult expandPatternOn(Abstract.Pattern pattern, int varIndex, Abstract.Expression origExpr) {
    int varContextIndex = myLocalContext.size() - 1 - varIndex;

    Binding binding = myLocalContext.get(varContextIndex);
    List<Binding> tail = new ArrayList<>(myLocalContext.subList(varContextIndex + 1, myLocalContext.size()));
    myLocalContext.subList(varContextIndex, myLocalContext.size()).clear();

    ExpandPatternResult result = expandPattern(pattern, binding, origExpr);
    if (result == null)
      return null;
    for (int i = 0; i < tail.size(); i++) {
      // TODO: check let clause... move subst to binding?
      Expression type = tail.get(i).getType();
      Expression expression = result.expression.liftIndex(0, i);
      if (result.numBindings > 0) {
        type = type.liftIndex(i + 1, result.numBindings - 1).subst(expression, i);
      } else {
        type = type.subst(expression.liftIndex(0, 1), i).liftIndex(i + 1, result.numBindings - 1);
      }
      myLocalContext.add(new TypedBinding(tail.get(i).getName(), type));
    }

    return result;
  }

  public OKResult lookupLocalVar(Abstract.Expression expression) {
    OKResult exprOKResult;
    if (expression instanceof Abstract.VarExpression) {
      exprOKResult = getLocalVar((Abstract.VarExpression) expression);
      if (exprOKResult == null) return null;
    } else if (expression instanceof Abstract.IndexExpression) {
      exprOKResult = getIndex((Abstract.IndexExpression) expression);
    } else {
      throw new IllegalStateException();
    }
    return exprOKResult;
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression expectedType) {
    TypeCheckingError error = null;
    if (expectedType == null) {
      error = new TypeCheckingError(myParent, "Cannot infer type of the expression", expr, getNames(myLocalContext));
    }
    if (mySide != Side.LHS && error == null) {
      error = new TypeCheckingError(myParent, "\\elim is allowed only at the root of a definition", expr, getNames(myLocalContext));
    }

    if (!(expr.getExpression() instanceof Abstract.IndexExpression || expr.getExpression() instanceof Abstract.VarExpression)) {
      error = new TypeCheckingError(myParent, "\\elim can be applied only to a local variable", expr.getExpression(), getNames(myLocalContext));
      myModuleLoader.getTypeCheckingErrors().add(error);
      expr.setWellTyped(Error(null, error));
      return null;
    }

    OKResult exprOKResult = lookupLocalVar(expr.getExpression());

    if (exprOKResult == null) {
      return null;
    }

    List<Expression> parameters = new ArrayList<>();
    Expression type = exprOKResult.type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
    Expression ftype = type.getFunction(parameters);
    Collections.reverse(parameters);
    if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition) && error == null) {
      error = new TypeMismatchError(myParent, "a data type", type, expr.getExpression(), getNames(myLocalContext));
    }

    if (error != null) {
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
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

    clause_loop:
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
          error = new NotInScopeError(myParent, clause, clause.getName() == null ? "" : clause.getName().getPrefixName());
        } else {
          error = new TypeCheckingError(myParent, "Overlapping pattern matching: " + dataType.getConstructors().get(index), clause, getNames(myLocalContext));
        }
        expr.setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        continue;
      }

      Constructor constructor = constructors.get(index);
      constructors.remove(index);

      if (constructor.hasErrors()) {
        error = new HasErrors(myParent, constructor.getName(), clause);
        clause.getExpression().setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        continue;
      }

      List<TypeArgument> constructorArguments = new ArrayList<>();
      splitArguments(constructor.getType(), constructorArguments);

      List<NameArgument> arguments = new ArrayList<>(clause.getArguments().size());
      int indexI = 0, indexJ = 0;
      for (; indexI < clause.getArguments().size() && indexJ < constructorArguments.size(); ++indexJ) {
        if (clause.getArguments().get(indexI).getExplicit() == constructorArguments.get(indexJ).getExplicit()) {
          arguments.add(new NameArgument(clause.getArguments().get(indexI).getExplicit(), clause.getArguments().get(indexI).getName()));
          ++indexI;
        } else
        if (!clause.getArguments().get(indexI).getExplicit()) {
          error = new TypeCheckingError(myParent, "Unexpected implicit argument", clause.getArguments().get(indexI), getNames(myLocalContext));
          clause.getExpression().setWellTyped(Error(null, error));
          myModuleLoader.getTypeCheckingErrors().add(error);
          continue clause_loop;
        } else {
          String name = constructorArguments.get(indexJ) instanceof TelescopeArgument ? ((TelescopeArgument) constructorArguments.get(indexJ)).getNames().get(0) : null;
          arguments.add(new NameArgument(false, name));
        }
      }

      if (indexI < clause.getArguments().size()) {
        String msg = "Expected " + constructorArguments.size() + " arguments to " + constructor.getName() + ", but given " + (constructorArguments.size() + clause.getArguments().size() - indexI);
        error = new TypeCheckingError(myParent, msg, clause, getNames(myLocalContext));
        clause.getExpression().setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        continue;
      }

      int explicitCount = 0;
      for (; indexJ < constructorArguments.size(); ++indexJ) {
        if (constructorArguments.get(indexJ).getExplicit()) {
          ++explicitCount;
        } else {
          String name = constructorArguments.get(indexJ) instanceof TelescopeArgument ? ((TelescopeArgument) constructorArguments.get(indexJ)).getNames().get(0) : null;
          arguments.add(new NameArgument(false, name));
        }
      }
      if (explicitCount > 0) {
        String msg = "Expected " + constructorArguments.size() + " arguments to " + constructor.getName() + ", but given " + (constructorArguments.size() - explicitCount);
        error = new TypeCheckingError(myParent, msg, clause, getNames(myLocalContext));
        clause.getExpression().setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        continue;
      }

      Expression substExpr = DefCall(null, constructor, parameters);
      for (int j = 0; j < constructorArguments.size(); ++j) {
        substExpr = Apps(substExpr, new ArgumentExpression(Index(constructorArguments.size() - 1 - j), constructorArguments.get(j).getExplicit(), !constructorArguments.get(j).getExplicit()));
      }

      List<Binding> localContext = new ArrayList<>(myLocalContext.size() - 1 + constructorArguments.size());
      for (int i = 0; i < myLocalContext.size() - 1 - varIndex; ++i) {
        localContext.add(myLocalContext.get(i));
      }
      if (!constructorArguments.isEmpty()) {
        List<Expression> parameters1 = new ArrayList<>(parameters);
        Collections.reverse(parameters1);
        for (int i = 0; i < constructorArguments.size(); ++i) {
          if (i > 0) {
            for (int j = 0; j < parameters1.size(); ++j) {
              parameters1.set(j, parameters1.get(j).liftIndex(0, 1));
            }
          }
          localContext.add(new TypedBinding(arguments.get(i).getName(), constructorArguments.get(i).getType().subst(parameters1, i)));
        }
      }
      for (int i = myLocalContext.size() - varIndex; i < myLocalContext.size(); ++i) {
        int i0 = i - myLocalContext.size() + varIndex;
        localContext.add(new TypedBinding(myLocalContext.get(i).getName(), myLocalContext.get(i).getType().liftIndex(i0 + 1, constructorArguments.size()).subst(substExpr, i0)));
        substExpr = substExpr.liftIndex(0, 1);
      }
      Expression clauseExpectedType = expectedType.liftIndex(varIndex + 1, constructorArguments.size()).subst(substExpr, varIndex);

      Side side = clause.getArrow() == Abstract.Definition.Arrow.RIGHT || !(clause.getExpression() instanceof Abstract.ElimExpression) ? Side.RHS : Side.LHS;
      Result clauseResult = new CheckTypeVisitor(myParent, localContext, myAbstractCalls, myModuleLoader, side).typeCheck(clause.getExpression(), clauseExpectedType);
      if (!(clauseResult instanceof OKResult)) {
        wasError = true;
        if (errorResult == null) {
          errorResult = clauseResult;
        } else if (clauseResult instanceof InferErrorResult) {
          myModuleLoader.getTypeCheckingErrors().add(((InferErrorResult) errorResult).error);
          errorResult = clauseResult;
        }
      } else {
        clauses.set(constructor.getIndex(), new Clause(constructor, arguments, clause.getArrow(), clauseResult.expression, null));
      }
    }

    if (wasError) {
      return errorResult;
    }

    if (!constructors.isEmpty() && expr.getOtherwise() == null) {
      String msg = "Incomplete pattern matching";
      if (!dataType.equals(Prelude.INTERVAL)) {
        msg += ". Unhandled constructors: ";
        for (int i = 0; i < constructors.size(); ++i) {
          if (i > 0) msg += ", ";
          msg += constructors.get(i).getName().getPrefixName();
        }
      }
      error = new TypeCheckingError(myParent, msg, expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
    }
    if (constructors.isEmpty() && expr.getOtherwise() != null) {
      String msg = "Overlapping pattern matching";
      error = new TypeCheckingError(myParent, msg, expr.getOtherwise(), getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
    }

    Clause otherwise = null;
    if (expr.getOtherwise() != null) {
      Side side = expr.getOtherwise().getArrow() == Abstract.Definition.Arrow.RIGHT || !(expr.getOtherwise().getExpression() instanceof Abstract.ElimExpression) ? Side.RHS : Side.LHS;
      CheckTypeVisitor visitor = side != mySide ? new CheckTypeVisitor(myParent, myLocalContext, myAbstractCalls, myModuleLoader, side) : this;
      Result clauseResult = visitor.typeCheck(expr.getOtherwise().getExpression(), expectedType);
      if (clauseResult instanceof InferErrorResult) {
        return clauseResult;
      }
      if (clauseResult != null) {
        otherwise = new Clause(null, null, expr.getOtherwise().getArrow(), clauseResult.expression, null);
      }
    }

    ElimExpression result = Elim((IndexExpression) exprOKResult.expression, clauses, otherwise);
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
  public Result visitCase(Abstract.CaseExpression expr, Expression expectedType) {
    if (expectedType == null) {
      TypeCheckingError error = new TypeCheckingError(myParent, "Cannot infer type of the expression", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
      return null;
    }

    List<CompareVisitor.Equation> equations = new ArrayList<>();

    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult exprOKResult = (OKResult) exprResult;
    equations.addAll(exprOKResult.equations);
    Abstract.ElimExpression elim = wrapCaseToElim(expr);

    myLocalContext.add(new TypedBinding((Name) null, exprOKResult.type));
    Result elimResult = elim.accept(new CheckTypeVisitor(myParent, myLocalContext, myAbstractCalls, myModuleLoader, Side.LHS), expectedType.liftIndex(0, 1));
    if (!(elimResult instanceof OKResult)) return elimResult;
    OKResult elimOKResult = (OKResult) elimResult;
    addLiftedEquations(elimOKResult, equations, 1);
    myLocalContext.remove(myLocalContext.size() - 1);

    LetExpression letExpression = Let(lets(let("caseF", lamArgs(Tele(vars("caseA"), exprOKResult.type)), elimOKResult.type,
                    Abstract.Definition.Arrow.LEFT, elimOKResult.expression)), Apps(Index(0), exprOKResult.expression));

    expr.setWellTyped(exprOKResult.type.liftIndex(0, -1));
    return new OKResult(letExpression, exprOKResult.type.liftIndex(0, -1), equations);
  }


  private Abstract.ElimExpression wrapCaseToElim(final Abstract.CaseExpression expr) {
    return new Abstract.ElimExpression() {
      @Override
      public Abstract.Expression getExpression() {
        return Index(0);
      }

      @Override
      public List<? extends Abstract.Clause> getClauses() {
        return expr.getClauses();
      }

      @Override
      public Abstract.Clause getOtherwise() {
        return expr.getOtherwise();
      }

      @Override
      public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
        return visitor.visitElim(this, params);
      }

      @Override
      public void setWellTyped(Expression wellTyped) {

      }

      @Override
      public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
        accept(new PrettyPrintVisitor(builder, names, 0), prec);
      }
    };
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression type = okExprResult.type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
    if (!(type instanceof SigmaExpression)) {
      TypeCheckingError error = new TypeCheckingError(myParent, "Expected an expression of a sigma type", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
      return null;
    }

    List<TypeArgument> splitArgs = new ArrayList<>();
    splitArguments(((SigmaExpression) type).getArguments(), splitArgs);
    if (expr.getField() < 0 || expr.getField() >= splitArgs.size()) {
      TypeCheckingError error = new TypeCheckingError(myParent, "Index " + (expr.getField() + 1) + " out of range", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
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
    if (expr.getBaseClass().hasErrors()) {
      TypeCheckingError error = new HasErrors(myParent, expr.getBaseClass().getName(), expr);
      expr.setWellTyped(Error(DefCall(expr.getBaseClass()), error));
      myModuleLoader.getTypeCheckingErrors().add(error);
      return null;
    }

    if (expr.getDefinitions().isEmpty()) {
      return checkResultImplicit(expectedType, new OKResult(DefCall(expr.getBaseClass()), expr.getBaseClass().getType(), null), expr);
    }

    // TODO
    Map<String, FunctionDefinition> abstracts = new HashMap<>();
    for (Definition definition : expr.getBaseClass().getFields()) {
      if (definition instanceof FunctionDefinition && definition.isAbstract()) {
        abstracts.put(definition.getName().name, (FunctionDefinition) definition);
      }
    }

    Map<FunctionDefinition, OverriddenDefinition> definitions = new HashMap<>();
    for (Abstract.FunctionDefinition definition : expr.getDefinitions()) {
      FunctionDefinition oldDefinition = abstracts.remove(definition.getName().name);
      if (oldDefinition == null) {
        myModuleLoader.getTypeCheckingErrors().add(new TypeCheckingError(myParent, definition.getName() + " is not defined in " + expr.getBaseClass().getFullName(), definition, getNames(myLocalContext)));
      } else {
        OverriddenDefinition newDefinition = (OverriddenDefinition) TypeChecking.typeCheckFunctionBegin(myModuleLoader, expr.getBaseClass(), definition, myLocalContext, oldDefinition);
        if (newDefinition == null) return null;
        TypeChecking.typeCheckFunctionEnd(myModuleLoader, definition.getTerm(), newDefinition, myLocalContext, oldDefinition, false);
        definitions.put(oldDefinition, newDefinition);
      }
    }

    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    for (FunctionDefinition definition : abstracts.values()) {
      universe = universe.max(definition.getUniverse());
    }
    return checkResultImplicit(expectedType, new OKResult(ClassExt(expr.getBaseClass(), definitions, universe), new UniverseExpression(universe), null), expr);
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression normExpr = okExprResult.expression.accept(new NormalizeVisitor(NormalizeVisitor.Mode.WHNF, myLocalContext));
    if (!(normExpr instanceof DefCallExpression && ((DefCallExpression) normExpr).getDefinition() instanceof ClassDefinition || normExpr instanceof ClassExtExpression)) {
      TypeCheckingError error = new TypeCheckingError(myParent, "Expected a class", expr.getExpression(), getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myModuleLoader.getTypeCheckingErrors().add(error);
      return null;
    }
    return checkResultImplicit(expectedType, new OKResult(New(okExprResult.expression), normExpr, okExprResult.equations), expr);
  }

  private Result typeCheckLetClause(Abstract.LetClause clause) {
    List<Argument> args = new ArrayList<>();
    Expression resultType;
    Expression term;
    List<CompareVisitor.Equation> equations = new ArrayList<>();

    try (ContextSaver saver = new ContextSaver(myLocalContext)) {
      int numVarsPassed = 0;
      for (int i = 0; i < clause.getArguments().size(); i++) {
        if (clause.getArguments().get(i) instanceof Abstract.TypeArgument) {
          Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) clause.getArguments().get(i);
          Result result = typeCheck(typeArgument.getType(), Universe());
          if (!(result instanceof OKResult)) return result;
          OKResult okResult = (OKResult) result;
          args.add(argFromArgResult(typeArgument, okResult));
          addLiftedEquations(okResult, equations, numVarsPassed);
          if (typeArgument instanceof Abstract.TelescopeArgument) {
            List<String> names = ((Abstract.TelescopeArgument) typeArgument).getNames();
            for (int j = 0; j < names.size(); ++j) {
              myLocalContext.add(new TypedBinding(names.get(j), okResult.expression.liftIndex(0, j)));
              ++numVarsPassed;
            }
          } else {
            myLocalContext.add(new TypedBinding((Name) null, okResult.expression));
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
        addLiftedEquations(result, equations, numVarsPassed);
        expectedType = result.expression;
      }
      Result termResult = clause.getTerm().accept(new CheckTypeVisitor(myParent, myLocalContext, myAbstractCalls, myModuleLoader, Side.LHS), expectedType);
      if (!(termResult instanceof OKResult)) return termResult;
      addLiftedEquations(termResult, equations, numVarsPassed);

      term = ((OKResult) termResult).expression;
      resultType = ((OKResult) termResult).type;
    }

    LetClause result = new LetClause(clause.getName(), args, resultType, clause.getArrow(), term);
    myLocalContext.add(result);
    return new LetClauseResult(result, equations);
  }

  private void addLiftedEquations(Result okResult, List<CompareVisitor.Equation> equations, int numVarsPassed) {
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
  public Result visitLet(Abstract.LetExpression expr, Expression expectedType) {
    OKResult finalResult;
    try (ContextSaver saver = new ContextSaver(myLocalContext)) {
      List<LetClause> clauses = new ArrayList<>();
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      for (int i = 0; i < expr.getClauses().size(); i++) {
        Result clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (!(clauseResult instanceof LetClauseResult)) return clauseResult;
        addLiftedEquations(clauseResult, equations, i);
        clauses.add(((LetClauseResult) clauseResult).letClause);
      }
      Result result = typeCheck(expr.getExpression(), expectedType == null ? null : expectedType.liftIndex(0, expr.getClauses().size()));
      if (!(result instanceof OKResult)) return result;
      OKResult okResult = (OKResult) result;
      addLiftedEquations(okResult, equations, expr.getClauses().size());

      Expression normalizedResultType = okResult.type.normalize(NormalizeVisitor.Mode.NF, myLocalContext).liftIndex(0, -expr.getClauses().size());
      if (normalizedResultType == null) {
        TypeCheckingError error = new TypeCheckingError(myParent, "Let result type depends on a bound variable.", expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myModuleLoader.getTypeCheckingErrors().add(error);
        return null;
      }
      finalResult = new OKResult(Let(clauses, okResult.expression), normalizedResultType, equations);
    }
    return finalResult;
  }
}
