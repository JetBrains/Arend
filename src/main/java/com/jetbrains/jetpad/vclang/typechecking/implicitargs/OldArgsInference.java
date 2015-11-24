package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.FindHoleVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.InferredArgumentsMismatch;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandConstructorParameters;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError.getNames;

public class OldArgsInference extends RowImplicitArgsInference {
  public OldArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  @Override
  public CheckTypeVisitor.Result inferRow(CheckTypeVisitor.OKResult fun, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression funExpr, Abstract.Expression expr) {
    return typeCheckApps(funExpr, 0, fun, args, expectedType, expr);
  }

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

  private static class SolveEquationsResult {
    int index;
    TypeCheckingError error;

    public SolveEquationsResult(int index, TypeCheckingError error) {
      this.index = index;
      this.error = error;
    }
  }

  private SolveEquationsResult solveEquations(int size, Arg[] argsImp, CheckTypeVisitor.Result[] resultArgs, List<CompareVisitor.Equation> equations, List<CompareVisitor.Equation> resultEquations, Abstract.Expression fun) {
    int found = size;
    for (CompareVisitor.Equation equation : equations) {
      for (int i = 0; i < size; ++i) {
        if (resultArgs[i] instanceof CheckTypeVisitor.InferErrorResult && resultArgs[i].expression == equation.hole) {
          if (!(argsImp[i].expression instanceof Abstract.InferHoleExpression)) {
            if (argsImp[i].expression instanceof Expression) {
              Expression expr1 = ((Expression) argsImp[i].expression).normalize(NormalizeVisitor.Mode.NF, myVisitor.getLocalContext());
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
                  if (resultArgs[j] instanceof CheckTypeVisitor.InferErrorResult && resultArgs[j].expression == equation.hole) {
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
                TypeCheckingError error = new InferredArgumentsMismatch(i + 1, options, fun, getNames(myVisitor.getLocalContext()));
                myVisitor.getErrorReporter().report(error);
                return new SolveEquationsResult(-1, error);
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
    return new SolveEquationsResult(found, null);
  }

  private boolean typeCheckArgs(Arg[] argsImp, CheckTypeVisitor.Result[] resultArgs, List<TypeArgument> signature, List<CompareVisitor.Equation> resultEquations, int startIndex, int parametersNumber, Abstract.Expression fun) {
    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] == null) {
        TypeCheckingError error = new ArgInferenceError(i < parametersNumber ? parameter(i + 1) : functionArg(i - parametersNumber + 1), fun, getNames(myVisitor.getLocalContext()), fun);
        resultArgs[i] = new CheckTypeVisitor.InferErrorResult(new InferHoleExpression(error), error, null);
      }
    }

    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] instanceof CheckTypeVisitor.OKResult || argsImp[i].expression instanceof Abstract.InferHoleExpression) continue;

      List<Expression> substExprs = new ArrayList<>(i);
      for (int j = i - 1; j >= 0; --j) {
        substExprs.add(resultArgs[j].expression);
      }
      Expression type = signature.get(i).getType().subst(substExprs, 0);

      CheckTypeVisitor.Result result;
      /* TODO
      if (argsImp[i].expression instanceof Expression) {
        List<Expression> context = new ArrayList<>(myVisitor.getLocalContext().size());
        for (Binding binding : myVisitor.getLocalContext()) {
          context.add(binding.getType());
        }
        Expression actualType = ((Expression) argsImp[i].expression).getType(context);
        result = checkResult(type, new OKResult((Expression) argsImp[i].expression, actualType, null), argsImp[i].expression);
      } else { */
      result = myVisitor.typeCheck(argsImp[i].expression, type);
      // }
      if (result == null) {
        for (int j = i + 1; j < resultArgs.length; ++j) {
          if (!(argsImp[j].expression instanceof Abstract.InferHoleExpression)) {
            myVisitor.typeCheck(argsImp[j].expression, null);
          }
        }
        return false;
      }
      if (result instanceof CheckTypeVisitor.OKResult) {
        resultArgs[i] = result;
        argsImp[i].expression = result.expression;
      } else {
        if (resultArgs[i] != null && resultArgs[i].expression instanceof InferHoleExpression) {
          resultArgs[i] = new CheckTypeVisitor.InferErrorResult((InferHoleExpression) resultArgs[i].expression, ((CheckTypeVisitor.InferErrorResult) result).error, result.equations);
        } else {
          resultArgs[i] = result;
        }
      }

      if (resultArgs[i].equations == null) continue;
      SolveEquationsResult result1 = solveEquations(i, argsImp, resultArgs, resultArgs[i].equations, resultEquations, fun);
      if (result1.index < 0) return false;
      if (result1.index != i) {
        i = result1.index - 1;
      }
    }
    return true;
  }

  private CheckTypeVisitor.Result typeCheckApps(Abstract.Expression fun, int argsSkipped, CheckTypeVisitor.OKResult okFunction, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression expression) {
    List<TypeArgument> signatureArguments = new ArrayList<>();
    int parametersNumber = 0;
    if (okFunction.expression instanceof ConCallExpression) {
      Constructor def = ((ConCallExpression) okFunction.expression).getDefinition();
      if (def.getPatterns() == null) {
        parametersNumber = numberOfVariables(def.getDataType().getParameters());
      } else {
        parametersNumber = expandConstructorParameters(def, myVisitor.getLocalContext()).size();
      }
      if (def.getThisClass() != null) {
        ++parametersNumber;
      }
      parametersNumber -= ((ConCallExpression) okFunction.expression).getParameters().size();
    }

    Expression signatureResultType = splitArguments(okFunction.type, signatureArguments, myVisitor.getLocalContext());
    assert parametersNumber <= signatureArguments.size();
    Arg[] argsImp = new Arg[signatureArguments.size()];
    int i, j;
    for (i = 0; i < parametersNumber; ++i) {
      argsImp[i] = new Arg(false, null, new InferHoleExpression(new ArgInferenceError(parameter(i + 1), fun, getNames(myVisitor.getLocalContext()), fun)));
    }
    for (j = 0; i < signatureArguments.size() && j < args.size(); ++i) {
      if (args.get(j).isExplicit() == signatureArguments.get(i).getExplicit()) {
        argsImp[i] = new Arg(args.get(j).isHidden(), null, args.get(j).getExpression());
        ++j;
      } else
      if (args.get(j).isExplicit()) {
        argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(functionArg(i - parametersNumber + 1), fun, getNames(myVisitor.getLocalContext()), fun)));
      } else {
        TypeCheckingError error = new TypeCheckingError("Unexpected implicit argument", args.get(j).getExpression(), getNames(myVisitor.getLocalContext()));
        args.get(j).getExpression().setWellTyped(myVisitor.getLocalContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        for (Abstract.ArgumentExpression arg : args) {
          myVisitor.typeCheck(arg.getExpression(), null);
        }
        return null;
      }
    }

    if (j < args.size() && signatureArguments.isEmpty()) {
      TypeCheckingError error = new TypeCheckingError("Function expects " + (argsSkipped + i) + " arguments, but is applied to " + (argsSkipped + i + args.size() - j), fun, getNames(myVisitor.getLocalContext()));
      fun.setWellTyped(myVisitor.getLocalContext(), Error(okFunction.expression, error));
      myVisitor.getErrorReporter().report(error);
      for (Abstract.ArgumentExpression arg : args) {
        myVisitor.typeCheck(arg.getExpression(), null);
      }
      return null;
    }

    if (okFunction.expression instanceof DefCallExpression && ((DefCallExpression) okFunction.expression).getDefinition() == Prelude.PATH_CON && args.size() == 1 && j == 1) {
      Expression argExpectedType = null;
      InferHoleExpression holeExpression = null;
      if (expectedType != null) {
        List<Expression> argsExpectedType = new ArrayList<>(3);
        Expression fexpectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF, myVisitor.getLocalContext()).getFunction(argsExpectedType);
        if (fexpectedType instanceof DefCallExpression && ((DefCallExpression) fexpectedType).getDefinition().equals(Prelude.PATH) && argsExpectedType.size() == 3) {
          if (argsExpectedType.get(2) instanceof InferHoleExpression) {
            holeExpression = (InferHoleExpression) argsExpectedType.get(2);
          } else {
            argExpectedType = Pi("i", DataCall(Prelude.INTERVAL), Apps(argsExpectedType.get(2).liftIndex(0, 1), Index(0)));
          }
        }
      }

      InferHoleExpression inferHoleExpr = null;
      if (argExpectedType == null) {
        inferHoleExpr = new InferHoleExpression(new ArgInferenceError(type(), args.get(0).getExpression(), getNames(myVisitor.getLocalContext()), args.get(0).getExpression()));
        argExpectedType = Pi("i", DataCall(Prelude.INTERVAL), inferHoleExpr);
      }

      CheckTypeVisitor.Result argResult = myVisitor.typeCheck(args.get(0).getExpression(), argExpectedType);
      if (!(argResult instanceof CheckTypeVisitor.OKResult)) return argResult;
      if (argResult.equations != null) {
        for (int k = 0; k < argResult.equations.size(); ++k) {
          if (argResult.equations.get(k).hole.equals(inferHoleExpr)) {
            argResult.equations.remove(k--);
          }
        }
      }
      PiExpression piType = (PiExpression) ((CheckTypeVisitor.OKResult) argResult).type;

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
      Expression parameter1 = Lam(lamArgs(Tele(vars("i"), DataCall(Prelude.INTERVAL))), type);
      Expression parameter2 = Apps(argResult.expression, ConCall(Prelude.LEFT));
      Expression parameter3 = Apps(argResult.expression, ConCall(Prelude.RIGHT));
      Expression resultType = Apps(DataCall(Prelude.PATH), parameter1, parameter2, parameter3);
      List<CompareVisitor.Equation> resultEquations = argResult.equations;
      if (holeExpression != null) {
        if (resultEquations == null) {
          resultEquations = new ArrayList<>(1);
        }
        resultEquations.add(new CompareVisitor.Equation(holeExpression, Lam(lamArgs(Tele(vars("i"), DataCall(Prelude.INTERVAL))), type.normalize(NormalizeVisitor.Mode.NF, myVisitor.getLocalContext()))));
      }

      List<Expression> parameters = new ArrayList<>(3);
      parameters.add(parameter1);
      parameters.add(parameter2);
      parameters.add(parameter3);
      Expression resultExpr = Apps(ConCall(Prelude.PATH_CON, parameters), new ArgumentExpression(argResult.expression, true, false));
      return new CheckTypeVisitor.OKResult(resultExpr, resultType, resultEquations);
    }

    if (expectedType != null && j == args.size()) {
      for (; i < signatureArguments.size() - numberOfVariables(expectedType, myVisitor.getLocalContext()); ++i) {
        if (signatureArguments.get(i).getExplicit()) {
          break;
        } else {
          argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(functionArg(i + 1), fun, getNames(myVisitor.getLocalContext()), fun)));
        }
      }
    }

    int argsNumber = i;
    CheckTypeVisitor.Result[] resultArgs = new CheckTypeVisitor.Result[argsNumber];
    List<CompareVisitor.Equation> resultEquations = new ArrayList<>();
    if (!typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, 0, parametersNumber, fun)) {
      expression.setWellTyped(myVisitor.getLocalContext(), Error(null, null)); // TODO
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
      if (!(resultArgs[i] instanceof CheckTypeVisitor.OKResult)) {
        argIndex = i + 1;
        break;
      }
    }

    if (argIndex != 0 && expectedType != null && j == args.size() && expectedType.accept(new FindHoleVisitor()) == null) {
      Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF, myVisitor.getLocalContext());
      Expression actualNorm = resultType.normalize(NormalizeVisitor.Mode.NF, myVisitor.getLocalContext());
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      CompareVisitor.Result result = compare(actualNorm, expectedNorm, equations);

      if (result instanceof CompareVisitor.JustResult && result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
        Expression resultExpr = okFunction.expression;
        for (i = parametersNumber; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }

        TypeCheckingError error = new TypeMismatchError(expectedNorm, actualNorm, expression, getNames(myVisitor.getLocalContext()));
        expression.setWellTyped(myVisitor.getLocalContext(), Error(resultExpr, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }

      SolveEquationsResult result1 = solveEquations(argsNumber, argsImp, resultArgs, equations, resultEquations, fun);
      if (result1.index < 0 || (result1.index != argsNumber && !typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, result1.index, parametersNumber, fun))) {
        Expression resultExpr = okFunction.expression;
        for (i = parametersNumber; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i] == null ? new InferHoleExpression(null) : resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }
        expression.setWellTyped(myVisitor.getLocalContext(), Error(resultExpr, result1.error));
        return null;
      }

      argIndex = 0;
      for (i = argsNumber - 1; i >= 0; --i) {
        if (!(resultArgs[i] instanceof CheckTypeVisitor.OKResult)) {
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
      parameters = ((ConCallExpression) resultExpr).getParameters();
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
        return typeCheckApps(fun, argsSkipped + j, new CheckTypeVisitor.OKResult(resultExpr, resultType, resultEquations), restArgs, expectedType, expression);
      } else {
        return new CheckTypeVisitor.OKResult(resultExpr, resultType, resultEquations);
      }
    } else {
      TypeCheckingError error;
      if (resultArgs[argIndex - 1] instanceof CheckTypeVisitor.InferErrorResult) {
        error = ((CheckTypeVisitor.InferErrorResult) resultArgs[argIndex - 1]).error;
      } else {
        if (argIndex > parametersNumber) {
          error = new ArgInferenceError(functionArg(argIndex - parametersNumber), fun, getNames(myVisitor.getLocalContext()), fun);
        } else {
          error = new ArgInferenceError(parameter(argIndex), fun, null, new ArgInferenceError.StringPrettyPrintable(((Constructor) ((DefCallExpression) okFunction.expression).getDefinition()).getDataType().getName()));
        }
      }
      expression.setWellTyped(myVisitor.getLocalContext(), Error(resultExpr, error));
      return new CheckTypeVisitor.InferErrorResult(new InferHoleExpression(error), error, resultEquations);
    }
  }
}
