package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.FunctionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedSingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class StdImplicitArgsInference extends BaseImplicitArgsInference {
  public StdImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  private static Concrete.ClassView getClassViewFromDefCall(Concrete.Definition definition, int paramIndex) {
    Collection<? extends Concrete.Parameter> parameters = Concrete.getParameters(definition);
    if (parameters == null) {
      return null;
    }

    int i = 0;
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.NameParameter) {
        i++;
      } else
      if (parameter instanceof Concrete.TypeParameter) {
        if (parameter instanceof Concrete.TelescopeParameter) {
          i += ((Concrete.TelescopeParameter) parameter).getReferableList().size();
        } else {
          i++;
        }
        if (i > paramIndex) {
          return Concrete.getUnderlyingClassView(((Concrete.TypeParameter) parameter).getType());
        }
      } else {
        throw new IllegalStateException();
      }
    }
    return null;
  }

  protected CheckTypeVisitor.TResult fixImplicitArgs(CheckTypeVisitor.TResult result, List<? extends DependentLink> implicitParameters, Concrete.Expression expr) {
    ExprSubstitution substitution = new ExprSubstitution();
    int i = 0;
    for (DependentLink parameter : implicitParameters) {
      Expression type = parameter.getTypeExpr().subst(substitution, LevelSubstitution.EMPTY);
      InferenceVariable infVar = null;
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        /* TODO[abstract]: Fix class view inference
        Concrete.ClassView classView = getClassViewFromDefCall(defCallResult.getDefinition().getConcreteDefinition(), i);
        if (classView != null) {
          infVar = new TypeClassInferenceVariable<>(parameter.getName(), type, classView, false, defCallResult.getDefCall(), myVisitor.getAllBindings());
        }
        */
      }
      if (infVar == null) {
        if (result instanceof CheckTypeVisitor.DefCallResult) {
          infVar = new FunctionInferenceVariable(parameter.getName(), type, ((CheckTypeVisitor.DefCallResult) result).getArguments().size() + 1, ((CheckTypeVisitor.DefCallResult) result).getDefinition(), expr, myVisitor.getAllBindings());
        } else {
          infVar = new FunctionInferenceVariable(parameter.getName(), type, i + 1, null, expr, myVisitor.getAllBindings());
        }
      }
      Expression binding = new InferenceReferenceExpression(infVar, myVisitor.getEquations());
      result = result.applyExpression(binding);
      substitution.add(parameter, binding);
      i++;
    }
    return result;
  }

  protected CheckTypeVisitor.TResult inferArg(CheckTypeVisitor.TResult result, Concrete.Expression arg, boolean isExplicit, Concrete.Expression fun) {
    if (result == null || arg == null || result instanceof CheckTypeVisitor.Result && ((CheckTypeVisitor.Result) result).expression.isInstance(ErrorExpression.class)) {
      return result;
    }

    if (isExplicit) {
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        if (defCallResult.getDefinition() == Prelude.PATH_CON && defCallResult.getArguments().isEmpty()) {
          SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", Interval());
          UniverseExpression type = new UniverseExpression(new Sort(defCallResult.getSortArgument().getPLevel(), defCallResult.getSortArgument().getHLevel().add(1)));
          Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable("A", type, 1, Prelude.PATH_CON, fun, myVisitor.getAllBindings()), myVisitor.getEquations());
          Sort sort = type.getSort().succ();
          result = result.applyExpression(new LamExpression(sort, lamParam, binding));

          CheckTypeVisitor.Result argResult = myVisitor.checkExpr(arg, new PiExpression(sort, lamParam, binding));
          if (argResult == null) {
            return null;
          }

          Expression expr1 = new AppExpression(argResult.expression, Left());
          Expression expr2 = new AppExpression(argResult.expression, Right());
          return ((CheckTypeVisitor.DefCallResult) result).applyExpressions(Arrays.asList(expr1, expr2, argResult.expression));
        }
      }

      result = fixImplicitArgs(result, result.getImplicitParameters(), fun);
    }

    DependentLink param = result.getParameter();
    if (!param.hasNext()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations());
      if (!result1.type.isInstance(ErrorExpression.class)) {
        myVisitor.getErrorReporter().report(new TypeMismatchError(text("A pi type"), termDoc(result1.type), fun));
      }
      return null;
    }

    CheckTypeVisitor.Result argResult = myVisitor.checkExpr(arg, param.getTypeExpr());
    if (argResult == null) {
      return null;
    }

    if (param.isExplicit() != isExplicit) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError("Expected an " + (param.isExplicit() ? "explicit" : "implicit") + " argument", arg));
      return null;
    }

    return result.applyExpression(argResult.expression);
  }

  protected CheckTypeVisitor.TResult inferArg(Concrete.Expression fun, Concrete.Expression arg, boolean isExplicit, ExpectedType expectedType) {
    CheckTypeVisitor.TResult result;
    if (fun instanceof Concrete.AppExpression) {
      Concrete.Argument argument = ((Concrete.AppExpression) fun).getArgument();
      result = checkBinOpInferArg(((Concrete.AppExpression) fun).getFunction(), argument.getExpression(), argument.isExplicit(), expectedType);
    } else {
      if (fun instanceof Concrete.ReferenceExpression) {
        Concrete.ReferenceExpression defCall = (Concrete.ReferenceExpression) fun;
        result = defCall.getExpression() == null && !(defCall.getReferent() instanceof GlobalReferable) ? myVisitor.getLocalVar(defCall) : myVisitor.getTypeCheckingDefCall().typeCheckDefCall(defCall);
      } else {
        result = myVisitor.checkExpr(fun, null);
      }

      if (result instanceof CheckTypeVisitor.DefCallResult && isExplicit && expectedType != null) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        if (defCallResult.getDefinition() instanceof Constructor && defCallResult.getArguments().size() < DependentLink.Helper.size(((Constructor) defCallResult.getDefinition()).getDataTypeParameters())) {
          DataCallExpression dataCall = expectedType instanceof Expression ? ((Expression) expectedType).normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DataCallExpression.class) : null;
          if (dataCall != null) {
            if (((Constructor) defCallResult.getDefinition()).getDataType() != dataCall.getDefinition()) {
              myVisitor.getErrorReporter().report(new TypeMismatchError(termDoc(dataCall), refDoc(((Constructor) defCallResult.getDefinition()).getDataType().getReferable()), fun));
              return null;
            }

            List<? extends Expression> args = dataCall.getDefCallArguments();
            List<Expression> args1 = new ArrayList<>(args.size());
            args1.addAll(defCallResult.getArguments());
            args1.addAll(args.subList(defCallResult.getArguments().size(), args.size()));
            args1 = ((Constructor) defCallResult.getDefinition()).matchDataTypeArguments(args1);
            if (args1 != null) {
              result = CheckTypeVisitor.DefCallResult.makeTResult(defCallResult.getDefCall(), defCallResult.getDefinition(), defCallResult.getSortArgument(), null);
              if (!args1.isEmpty()) {
                result = ((CheckTypeVisitor.DefCallResult) result).applyExpressions(args1);
              }
            }
          }
        }
      }
    }

    if (result == null) {
      myVisitor.checkExpr(arg, null);
      return null;
    }
    return inferArg(result, arg, isExplicit, fun);
  }

  private CheckTypeVisitor.TResult checkBinOpInferArg(Concrete.Expression fun, Concrete.Expression arg, boolean isExplicit, ExpectedType expectedType) {
    if (fun instanceof Concrete.BinOpExpression) {
      return inferArg(inferArg(inferArg(fun, ((Concrete.BinOpExpression) fun).getLeft(), true, null), ((Concrete.BinOpExpression) fun).getRight(), true, fun), arg, isExplicit, fun);
    } else {
      return inferArg(fun, arg, isExplicit, expectedType);
    }
  }

  @Override
  public CheckTypeVisitor.TResult infer(Concrete.AppExpression expr, ExpectedType expectedType) {
    Concrete.Argument arg = expr.getArgument();
    return checkBinOpInferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit(), expectedType);
  }

  @Override
  public CheckTypeVisitor.TResult infer(Concrete.BinOpExpression expr, ExpectedType expectedType) {
    return inferArg(inferArg(expr, expr.getLeft(), true, null), expr.getRight(), true, expr);
  }

  @Override
  public CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult result, ExpectedType expectedType, Concrete.Expression expr) {
    List<? extends DependentLink> actualParams = result.getImplicitParameters();
    List<SingleDependentLink> expectedParams = new ArrayList<>(actualParams.size());
    expectedType.getPiParameters(expectedParams, true);
    if (expectedParams.size() > actualParams.size()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations());
      if (!result1.type.isInstance(ErrorExpression.class)) {
        myVisitor.getErrorReporter().report(new TypeMismatchError(typeDoc(expectedType), termDoc(result1.type), expr));
      }
      return null;
    }

    if (expectedParams.size() != actualParams.size()) {
      result = fixImplicitArgs(result, actualParams.subList(0, actualParams.size() - expectedParams.size()), expr);
    }

    return result;
  }
}
