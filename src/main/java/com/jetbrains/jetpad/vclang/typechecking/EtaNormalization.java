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
    LamExpression lam = expression.toLam();
    if (lam != null) {
      return normalizeLam(lam);
    }
    ConCallExpression conCall = expression.toConCall();
    if (conCall != null) {
      return normalizePath(conCall);
    }
    TupleExpression tuple = expression.toTuple();
    if (tuple != null) {
      return normalizeTuple(tuple);
    }
    return expression;
  }

  public static Expression normalizeLam(LamExpression expression) {
    List<DependentLink> params = DependentLink.Helper.toList(expression.getParameters());
    Expression body = normalize(expression.getBody());
    int index = params.size() - 1;
    for (; index >= 0; index--) {
      if (body.toApp() != null) {
        ReferenceExpression argRef = normalize(body.toApp().getArgument()).toReference();
        if (argRef != null && argRef.getBinding() == params.get(index) && !body.toApp().getFunction().findBinding(argRef.getBinding())) {
          body = body.toApp().getFunction();
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
      if (arg.toLam() != null && !arg.toLam().getParameters().getNext().hasNext() && arg.toLam().getBody().toFunCall() != null && arg.toLam().getBody().toFunCall().getDefinition() == Prelude.AT) {
        List<? extends Expression> args = arg.toLam().getBody().toFunCall().getDefCallArguments();
        if (args.get(4).toReference() != null && args.get(4).toReference().getBinding() == arg.toLam().getParameters()) {
          for (int i = 0; i < 4; i++) {
            if (args.get(i).findBinding(arg.toLam().getParameters())) {
              return expr;
            }
          }
          return normalize(args.get(3));
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
      ProjExpression projField = normalize(field).toProj();
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
