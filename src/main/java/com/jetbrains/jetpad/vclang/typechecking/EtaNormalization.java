package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public class EtaNormalization {
  public static Expression normalize(Expression expression) {
    if (expression.isInstance(LamExpression.class)) {
      return normalizeLam(expression.cast(LamExpression.class));
    }
    if (expression.isInstance(ConCallExpression.class)) {
      return normalizePath(expression.cast(ConCallExpression.class));
    }
    if (expression.isInstance(TupleExpression.class)) {
      return normalizeTuple(expression.cast(TupleExpression.class));
    }
    return expression;
  }

  public static Expression normalizeLam(LamExpression expression) {
    List<DependentLink> params = DependentLink.Helper.toList(expression.getParameters());
    Expression body = normalize(expression.getBody());
    int index = params.size() - 1;
    for (; index >= 0; index--) {
      if (body.isInstance(AppExpression.class)) {
        AppExpression appBody = body.cast(AppExpression.class);
        ReferenceExpression argRef = normalize(appBody.getArgument()).checkedCast(ReferenceExpression.class);
        if (argRef != null && argRef.getBinding() == params.get(index) && !appBody.getFunction().findBinding(argRef.getBinding())) {
          body = appBody.getFunction();
          continue;
        }
      }

      if (index == params.size() - 1) {
        return new LamExpression(expression.getResultSort(), expression.getParameters(), body);
      } else {
        ExprSubstitution substitution = new ExprSubstitution();
        SingleDependentLink newParams = expression.getParameters().subst(substitution, LevelSubstitution.EMPTY, index + 1);
        return new LamExpression(expression.getResultSort(), newParams, body.subst(substitution));
      }
    }

    return normalize(body);
  }

  public static Expression normalizePath(ConCallExpression expr) {
    if (expr.getDefinition() == Prelude.PATH_CON) {
      Expression arg = normalize(expr.getDefCallArguments().get(0));
      LamExpression lamArg = arg.checkedCast(LamExpression.class);
      if (lamArg != null && !lamArg.getParameters().getNext().hasNext()) {
        FunCallExpression funCall = lamArg.getBody().checkedCast(FunCallExpression.class);
        if (funCall != null && funCall.getDefinition() == Prelude.AT) {
          List<? extends Expression> args = funCall.getDefCallArguments();
          if (args.get(4).isInstance(ReferenceExpression.class) && args.get(4).cast(ReferenceExpression.class).getBinding() == lamArg.getParameters()) {
            for (int i = 0; i < 4; i++) {
              if (args.get(i).findBinding(lamArg.getParameters())) {
                return expr;
              }
            }
            return normalize(args.get(3));
          }
        }
      }
    }
    return expr;
  }

  public static Expression normalizeTuple(TupleExpression tuple) {
    if (tuple.getFields().isEmpty()) {
      return tuple;
    }

    int index = 0;
    Expression[] fields = new Expression[tuple.getFields().size()];
    for (Expression field : tuple.getFields()) {
      ProjExpression projField = normalize(field).checkedCast(ProjExpression.class);
      if (!(projField != null && projField.getField() == index)) {
        return tuple;
      }
      fields[index++] = projField.getExpression();
    }
    for (int i = 1; i < fields.length; i++) {
      if (!CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, fields[0], fields[i], null)) {
        return tuple;
      }
    }

    return normalize(fields[0]);
  }
}
