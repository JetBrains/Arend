package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
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
    AppExpression app = expression.toApp();
    if (app != null) {
      return normalizePath(app);
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
      return expression;
    }

    body = Apps(body.getFunction(), args.subList(0, args.size() - index), body.toApp().getFlags().subList(0, args.size() - index));
    if (index == params.size()) {
      return body;
    }

    Substitution substitution = new Substitution();
    DependentLink newParams = expression.getParameters().subst(substitution, params.size() - index);
    return new LamExpression(newParams, body.subst(substitution));
  }

  public static Expression normalizePath(AppExpression expr) {
    List<? extends Expression> args = expr.getArguments();
    ConCallExpression fun = expr.getFunction().toConCall();
    if (fun != null && Prelude.isPathCon(fun.getDefinition())) {
      Expression arg = normalize(args.get(args.size() - 1));
      List<? extends Expression> argArgs = arg.getArguments();
      FunCallExpression argFun = arg.getFunction().toFunCall();
      if (argArgs.size() > 0 && argFun != null && Prelude.isAt(argFun.getDefinition())) {
        return normalize(argArgs.get(argArgs.size() - 1));
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
