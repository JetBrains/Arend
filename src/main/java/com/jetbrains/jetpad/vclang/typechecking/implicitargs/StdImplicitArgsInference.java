package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.FunctionInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
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
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
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

public class StdImplicitArgsInference<T> extends BaseImplicitArgsInference<T> {
  public StdImplicitArgsInference(CheckTypeVisitor<T> visitor) {
    super(visitor);
  }

  private static Abstract.ClassView getClassViewFromDefCall(Concrete.Definition<?> definition, int paramIndex) {
    Collection<? extends Abstract.Parameter> parameters = Abstract.getParameters(definition);
    if (parameters == null) {
      return null;
    }

    int i = 0;
    for (Abstract.Parameter parameter : parameters) {
      if (parameter instanceof Abstract.NameParameter) {
        i++;
      } else
      if (parameter instanceof Abstract.TypeParameter) {
        if (parameter instanceof Abstract.TelescopeParameter) {
          i += ((Abstract.TelescopeParameter) parameter).getReferableList().size();
        } else {
          i++;
        }
        if (i > paramIndex) {
          return Abstract.getUnderlyingClassView(((Abstract.TypeParameter) parameter).getType());
        }
      } else {
        throw new IllegalStateException();
      }
    }
    return null;
  }

  protected CheckTypeVisitor.TResult<T> fixImplicitArgs(CheckTypeVisitor.TResult<T> result, List<? extends DependentLink> implicitParameters, Concrete.Expression<T> expr) {
    ExprSubstitution substitution = new ExprSubstitution();
    int i = 0;
    for (DependentLink parameter : implicitParameters) {
      Expression type = parameter.getTypeExpr().subst(substitution, LevelSubstitution.EMPTY);
      InferenceVariable<T> infVar = null;
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult<T> defCallResult = (CheckTypeVisitor.DefCallResult<T>) result;
        Abstract.ClassView classView = getClassViewFromDefCall(defCallResult.getDefinition().getConcreteDefinition(), i);
        if (classView != null) {
          infVar = new TypeClassInferenceVariable<>(parameter.getName(), type, classView, false, defCallResult.getDefCall(), myVisitor.getAllBindings());
        }
      }
      if (infVar == null) {
        infVar = new FunctionInferenceVariable<>(parameter.getName(), type, i + 1, result instanceof CheckTypeVisitor.DefCallResult ? ((CheckTypeVisitor.DefCallResult) result).getDefinition() : null, expr, myVisitor.getAllBindings());
      }
      Expression binding = new InferenceReferenceExpression(infVar, myVisitor.getEquations());
      result = result.applyExpression(binding);
      substitution.add(parameter, binding);
      i++;
    }
    return result;
  }

  protected CheckTypeVisitor.TResult<T> inferArg(CheckTypeVisitor.TResult<T> result, Concrete.Expression<T> arg, boolean isExplicit, Concrete.Expression<T> fun) {
    if (result == null || arg == null || result instanceof CheckTypeVisitor.Result && ((CheckTypeVisitor.Result) result).expression.isInstance(ErrorExpression.class)) {
      return result;
    }

    if (isExplicit) {
      if (result instanceof CheckTypeVisitor.DefCallResult) {
        CheckTypeVisitor.DefCallResult defCallResult = (CheckTypeVisitor.DefCallResult) result;
        if (defCallResult.getDefinition() == Prelude.PATH_CON && defCallResult.getArguments().isEmpty()) {
          SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", Interval());
          UniverseExpression type = new UniverseExpression(new Sort(defCallResult.getSortArgument().getPLevel(), defCallResult.getSortArgument().getHLevel().add(1)));
          Expression binding = new InferenceReferenceExpression(new FunctionInferenceVariable<>("A", type, 1, Prelude.PATH_CON, fun, myVisitor.getAllBindings()), myVisitor.getEquations());
          Sort sort = type.getSort().succ();
          result = result.applyExpression(new LamExpression(sort, lamParam, binding));

          CheckTypeVisitor.Result argResult = myVisitor.checkExpr(arg, new PiExpression(sort, lamParam, binding));
          if (argResult == null) {
            return null;
          }

          Expression expr1 = new AppExpression(argResult.expression, Left());
          Expression expr2 = new AppExpression(argResult.expression, Right());
          return ((CheckTypeVisitor.DefCallResult<T>) result).applyExpressions(Arrays.asList(expr1, expr2, argResult.expression));
        }
      }

      result = fixImplicitArgs(result, result.getImplicitParameters(), fun);
    }

    DependentLink param = result.getParameter();
    if (!param.hasNext()) {
      CheckTypeVisitor.Result result1 = result.toResult(myVisitor.getEquations());
      myVisitor.getErrorReporter().report(new TypeMismatchError<>(text("A pi type"), termDoc(result1.type), fun));
      return null;
    }

    CheckTypeVisitor.Result argResult = myVisitor.checkExpr(arg, param.getTypeExpr());
    if (argResult == null) {
      return null;
    }

    if (param.isExplicit() != isExplicit) {
      myVisitor.getErrorReporter().report(new LocalTypeCheckingError<>("Expected an " + (param.isExplicit() ? "explicit" : "implicit") + " argument", arg));
      return null;
    }

    return result.applyExpression(argResult.expression);
  }

  protected CheckTypeVisitor.TResult<T> inferArg(Concrete.Expression<T> fun, Concrete.Expression<T> arg, boolean isExplicit, ExpectedType expectedType) {
    CheckTypeVisitor.TResult<T> result;
    if (fun instanceof Concrete.AppExpression) {
      Concrete.Argument<T> argument = ((Concrete.AppExpression<T>) fun).getArgument();
      result = checkBinOpInferArg(((Concrete.AppExpression<T>) fun).getFunction(), argument.getExpression(), argument.isExplicit(), expectedType);
    } else {
      if (fun instanceof Concrete.ReferenceExpression) {
        Concrete.ReferenceExpression<T> defCall = (Concrete.ReferenceExpression<T>) fun;
        result = defCall.getExpression() == null && !(defCall.getReferent() instanceof Abstract.GlobalReferableSourceNode) ? myVisitor.getLocalVar(defCall) : myVisitor.getTypeCheckingDefCall().typeCheckDefCall(defCall);
      } else {
        //noinspection unchecked
        result = myVisitor.checkExpr(fun, null);
      }

      if (result instanceof CheckTypeVisitor.DefCallResult && isExplicit && expectedType != null) {
        CheckTypeVisitor.DefCallResult<T> defCallResult = (CheckTypeVisitor.DefCallResult<T>) result;
        if (defCallResult.getDefinition() instanceof Constructor && defCallResult.getArguments().size() < DependentLink.Helper.size(((Constructor) defCallResult.getDefinition()).getDataTypeParameters())) {
          DataCallExpression dataCall = expectedType instanceof Expression ? ((Expression) expectedType).normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DataCallExpression.class) : null;
          if (dataCall != null) {
            if (((Constructor) defCallResult.getDefinition()).getDataType() != dataCall.getDefinition()) {
              myVisitor.getErrorReporter().report(new TypeMismatchError<>(termDoc(dataCall), refDoc(((Constructor) defCallResult.getDefinition()).getDataType().getConcreteDefinition()), fun));
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
                result = ((CheckTypeVisitor.DefCallResult<T>) result).applyExpressions(args1);
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

  private CheckTypeVisitor.TResult<T> checkBinOpInferArg(Concrete.Expression<T> fun, Concrete.Expression<T> arg, boolean isExplicit, ExpectedType expectedType) {
    if (fun instanceof Concrete.BinOpExpression) {
      return inferArg(inferArg(inferArg(fun, ((Concrete.BinOpExpression<T>) fun).getLeft(), true, null), ((Concrete.BinOpExpression<T>) fun).getRight(), true, fun), arg, isExplicit, fun);
    } else {
      return inferArg(fun, arg, isExplicit, expectedType);
    }
  }

  @Override
  public CheckTypeVisitor.TResult<T> infer(Concrete.AppExpression<T> expr, ExpectedType expectedType) {
    Concrete.Argument<T> arg = expr.getArgument();
    return checkBinOpInferArg(expr.getFunction(), arg.getExpression(), arg.isExplicit(), expectedType);
  }

  @Override
  public CheckTypeVisitor.TResult<T> infer(Concrete.BinOpExpression<T> expr, ExpectedType expectedType) {
    return inferArg(inferArg(expr, expr.getLeft(), true, null), expr.getRight(), true, expr);
  }

  @Override
  public CheckTypeVisitor.TResult<T> inferTail(CheckTypeVisitor.TResult<T> result, ExpectedType expectedType, Concrete.Expression<T> expr) {
    List<? extends DependentLink> actualParams = result.getImplicitParameters();
    List<SingleDependentLink> expectedParams = new ArrayList<>(actualParams.size());
    expectedType.getPiParameters(expectedParams, true);
    if (expectedParams.size() > actualParams.size()) {
      myVisitor.getErrorReporter().report(new TypeMismatchError<>(typeDoc(expectedType), termDoc(result.toResult(myVisitor.getEquations()).type), expr));
      return null;
    }

    if (expectedParams.size() != actualParams.size()) {
      result = fixImplicitArgs(result, actualParams.subList(0, actualParams.size() - expectedParams.size()), expr);
    }

    return result;
  }
}
