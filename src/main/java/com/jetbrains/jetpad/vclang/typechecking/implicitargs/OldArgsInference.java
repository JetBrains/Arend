package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.List;

public class OldArgsInference extends RowImplicitArgsInference {
  public OldArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  @Override
  public CheckTypeVisitor.Result inferRow(CheckTypeVisitor.Result fun, List<Abstract.ArgumentExpression> args, Abstract.Expression funExpr, Abstract.Expression expr) {
    return null; // typeCheckApps(funExpr, 0, fun, args, expectedType, expr);
  }

  /*
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
              Expression expr1 = ((Expression) argsImp[i].expression).normalize(NormalizeVisitor.Mode.NF, myVisitor.getContext());
              List<CompareVisitor.Equation> equations1 = new ArrayList<>();
              CompareVisitor.CMP cmp = oldCompare(expr1, equation.expression, equations1).isOK();
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
                      cmp = oldCompare(option, equation.expression, equations1).isOK();
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
                TypeCheckingError error = new InferredArgumentsMismatch(i + 1, options, fun, getNames(myVisitor.getContext()));
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

  private boolean typeCheckArgs(Arg[] argsImp, CheckTypeVisitor.Result[] resultArgs, List<TypeArgument> signature, List<CompareVisitor.Equation> resultEquations, int startIndex, Abstract.Expression fun) {
    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] == null) {
        TypeCheckingError error = new ArgInferenceError(functionArg(i + 1), fun, getNames(myVisitor.getContext()), fun);
        resultArgs[i] = new CheckTypeVisitor.InferErrorResult(new InferHoleExpression(error), error, null);
      }
    }

    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] instanceof CheckTypeVisitor.Result || argsImp[i].expression instanceof Abstract.InferHoleExpression) continue;

      List<Expression> substExprs = new ArrayList<>(i);
      for (int j = i - 1; j >= 0; --j) {
        substExprs.add(resultArgs[j].expression);
      }
      Expression type = signature.get(i).getType().subst(substExprs, 0);

      CheckTypeVisitor.Result result;
      if (argsImp[i].expression instanceof Expression) {
        Expression actualType = ((Expression) argsImp[i].expression).getType(myVisitor.getContext());
        result = myVisitor.checkResult(type, new CheckTypeVisitor.OKResult((Expression) argsImp[i].expression, actualType, null), argsImp[i].expression);
      } else {
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
      if (result instanceof CheckTypeVisitor.Result) {
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

  private CheckTypeVisitor.Result typeCheckApps(Abstract.Expression fun, int argsSkipped, CheckTypeVisitor.Result okFunction, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression expression) {
    List<TypeArgument> signatureArguments = new ArrayList<>();
    Expression signatureResultType = splitArguments(okFunction.type, signatureArguments, myVisitor.getContext());
    Arg[] argsImp = new Arg[signatureArguments.size()];
    int i, j;
    for (i = 0, j = 0; i < signatureArguments.size() && j < args.size(); ++i) {
      if (args.get(j).isExplicit() == signatureArguments.get(i).getExplicit()) {
        argsImp[i] = new Arg(args.get(j).isHidden(), null, args.get(j).getExpression());
        ++j;
      } else
      if (args.get(j).isExplicit()) {
        argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(functionArg(i + 1), fun, getNames(myVisitor.getContext()), fun)));
      } else {
        TypeCheckingError error = new TypeCheckingError("Unexpected implicit argument", args.get(j).getExpression(), getNames(myVisitor.getContext()));
        args.get(j).getExpression().setWellTyped(myVisitor.getContext(), Error(null, error));
        myVisitor.getErrorReporter().report(error);
        for (Abstract.ArgumentExpression arg : args) {
          myVisitor.typeCheck(arg.getExpression(), null);
        }
        return null;
      }
    }

    if (j < args.size() && signatureArguments.isEmpty()) {
      TypeCheckingError error = new TypeCheckingError("Function expects " + (argsSkipped + i) + " arguments, but is applied to " + (argsSkipped + i + args.size() - j), fun, getNames(myVisitor.getContext()));
      fun.setWellTyped(myVisitor.getContext(), Error(okFunction.expression, error));
      myVisitor.getErrorReporter().report(error);
      for (Abstract.ArgumentExpression arg : args) {
        myVisitor.typeCheck(arg.getExpression(), null);
      }
      return null;
    }

    if (okFunction.expression instanceof DefCallExpression && Prelude.isPathCon(((DefCallExpression) okFunction.expression).getDefinition()) && args.size() == 1 && j == 1) {
      Constructor pathCon = ((ConCallExpression) okFunction.expression).getDefinition();
      Expression argExpectedType = null;
      InferHoleExpression holeExpression = null;
      if (expectedType != null) {
        List<Expression> argsExpectedType = new ArrayList<>(3);
        Expression fexpectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF, myVisitor.getContext()).getFunction(argsExpectedType);
        if (fexpectedType instanceof DefCallExpression && ((DefCallExpression) fexpectedType).getDefinition() == pathCon.getDataType() && argsExpectedType.size() == 3) {
          if (argsExpectedType.get(2) instanceof InferHoleExpression) {
            holeExpression = (InferHoleExpression) argsExpectedType.get(2);
          } else {
            argExpectedType = Pi("i", DataCall(Prelude.INTERVAL), Apps(argsExpectedType.get(2).liftIndex(0, 1), Index(0)));
          }
        }
      }

      InferHoleExpression inferHoleExpr = null;
      if (argExpectedType == null) {
        inferHoleExpr = new InferHoleExpression(new ArgInferenceError(type(), args.get(0).getExpression(), getNames(myVisitor.getContext()), args.get(0).getExpression()));
        argExpectedType = Pi("i", DataCall(Prelude.INTERVAL), inferHoleExpr);
      }

      CheckTypeVisitor.Result argResult = myVisitor.typeCheck(args.get(0).getExpression(), argExpectedType);
      if (!(argResult instanceof CheckTypeVisitor.Result)) return argResult;
      if (argResult.equations != null) {
        for (int k = 0; k < argResult.equations.size(); ++k) {
          if (argResult.equations.get(k).hole.equals(inferHoleExpr)) {
            argResult.equations.remove(k--);
          }
        }
      }
      PiExpression piType = (PiExpression) ((CheckTypeVisitor.Result) argResult).type;

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
      Expression parameter1 = Lam(teleArgs(Tele(vars("i"), DataCall(Prelude.INTERVAL))), type);
      Expression parameter2 = Apps(argResult.expression, ConCall(Prelude.LEFT));
      Expression parameter3 = Apps(argResult.expression, ConCall(Prelude.RIGHT));
      Expression resultType = Apps(DataCall(pathCon.getDataType()), parameter1, parameter2, parameter3);
      Equations resultEquations = argResult.equations;
      if (holeExpression != null) {
        resultEquations.add(holeExpression, Lam(teleArgs(Tele(vars("i"), DataCall(Prelude.INTERVAL))), type.normalize(NormalizeVisitor.Mode.NF, myVisitor.getContext())), Equations.CMP.EQ);
      }

      List<Expression> parameters = new ArrayList<>(3);
      parameters.add(parameter1);
      parameters.add(parameter2);
      parameters.add(parameter3);
      Expression resultExpr = Apps(ConCall(pathCon, parameters), new ArgumentExpression(argResult.expression, true, false));
      return new CheckTypeVisitor.Result(resultExpr, resultType, resultEquations);
    }

    if (expectedType != null && j == args.size()) {
      for (; i < signatureArguments.size() - numberOfVariables(expectedType, myVisitor.getContext()); ++i) {
        if (signatureArguments.get(i).getExplicit()) {
          break;
        } else {
          argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(functionArg(i + 1), fun, getNames(myVisitor.getContext()), fun)));
        }
      }
    }

    int argsNumber = i;
    CheckTypeVisitor.Result[] resultArgs = new CheckTypeVisitor.Result[argsNumber];
    Equations resultEquations = new ListEquations();
    if (!typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, 0, fun)) {
      expression.setWellTyped(myVisitor.getContext(), Error(null, null));
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
      if (!(resultArgs[i] instanceof CheckTypeVisitor.Result)) {
        argIndex = i + 1;
        break;
      }
    }

    if (argIndex != 0 && expectedType != null && j == args.size() && expectedType.accept(new FindHoleVisitor(), null) == null) {
      Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF, myVisitor.getContext());
      Expression actualNorm = resultType.normalize(NormalizeVisitor.Mode.NF, myVisitor.getContext());
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      CompareVisitor.Result result = oldCompare(actualNorm, expectedNorm, equations);

      if (result instanceof CompareVisitor.JustResult && result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
        Expression resultExpr = okFunction.expression;
        for (i = 0; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }

        TypeCheckingError error = new TypeMismatchError(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF, myVisitor.getContext()), resultType.normalize(NormalizeVisitor.Mode.HUMAN_NF, myVisitor.getContext()), expression, getNames(myVisitor.getContext()));
        expression.setWellTyped(myVisitor.getContext(), Error(resultExpr, error));
        myVisitor.getErrorReporter().report(error);
        return null;
      }

      SolveEquationsResult result1 = solveEquations(argsNumber, argsImp, resultArgs, equations, resultEquations, fun);
      if (result1.index < 0 || (result1.index != argsNumber && !typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, result1.index, fun))) {
        Expression resultExpr = okFunction.expression;
        for (i = 0; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i] == null ? new InferHoleExpression(null) : resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }
        expression.setWellTyped(myVisitor.getContext(), Error(resultExpr, result1.error));
        return null;
      }

      argIndex = 0;
      for (i = argsNumber - 1; i >= 0; --i) {
        if (!(resultArgs[i] instanceof CheckTypeVisitor.Result)) {
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
    for (i = 0; i < argsNumber; ++i) {
      resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
    }

    if (argIndex == 0) {
      if (j < args.size()) {
        List<Abstract.ArgumentExpression> restArgs = new ArrayList<>(args.size() - j);
        for (int k = j; k < args.size(); ++k) {
          restArgs.add(args.get(k));
        }
        return typeCheckApps(fun, argsSkipped + j, new CheckTypeVisitor.Result(resultExpr, resultType, resultEquations), restArgs, expectedType, expression);
      } else {
        return new CheckTypeVisitor.Result(resultExpr, resultType, resultEquations);
      }
    } else {
      TypeCheckingError error;
      if (resultArgs[argIndex - 1] instanceof CheckTypeVisitor.InferErrorResult) {
        error = ((CheckTypeVisitor.InferErrorResult) resultArgs[argIndex - 1]).error;
      } else {
        error = new ArgInferenceError(functionArg(argIndex), fun, getNames(myVisitor.getContext()), fun);
      }
      expression.setWellTyped(myVisitor.getContext(), Error(resultExpr, error));
      return new CheckTypeVisitor.InferErrorResult(new InferHoleExpression(error), error, resultEquations);
    }
  }
  */
}
