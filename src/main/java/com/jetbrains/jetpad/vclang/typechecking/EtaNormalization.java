package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;

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
    List<? extends Expression> args = body.getArguments();

    int index = 0;
    for (; index < params.size() && index < args.size(); index++) {
      DependentLink param = params.get(params.size() - 1 - index);
      ReferenceExpression argRef = normalize(args.get(args.size() - 1 - index)).toReference();
      if (!(argRef != null && argRef.getBinding() == param && !body.getFunction().findBinding(param))) {
        break;
      }
      for (int i = 0; i < args.size() - 1 - index; i++) {
        if (args.get(i).findBinding(param)) {
          break;
        }
      }
    }

    if (index == 0) {
      return new LamExpression(expression.getParameters(), body);
    }

    body = Apps(body.getFunction(), args.subList(0, args.size() - index));
    if (index == params.size()) {
      return body;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink newParams = expression.getParameters().subst(substitution, new LevelSubstitution(), params.size() - index);
    return new LamExpression(newParams, body.subst(substitution));
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
