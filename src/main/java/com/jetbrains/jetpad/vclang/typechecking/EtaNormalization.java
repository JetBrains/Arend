package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EtaNormalization {
  public static Expression normalize(Expression expression) {
    if (expression instanceof LamExpression) {
      return normalizeLam((LamExpression) expression);
    }
    if (expression instanceof AppExpression) {
      return normalizePath((AppExpression) expression);
    }
    if (expression instanceof TupleExpression) {
      return normalizeTuple((TupleExpression) expression);
    }

    return expression;
  }

  public static Expression normalizeLam(LamExpression expression) {
    Expression body = expression.getBody();
    List<DependentLink> params = DependentLink.Helper.toList(expression.getParameters());

    int index = params.size();
    for (; index != 0; index--) {
      body = normalize(body);
      if (!(body instanceof AppExpression)) {
        break;
      }

      AppExpression appBody = (AppExpression) body;
      Expression arg = normalize(appBody.getArgument().getExpression());
      if (arg instanceof ReferenceExpression && ((ReferenceExpression) arg).getBinding() == params.get(index - 1) && !appBody.getFunction().findBinding(params.get(index - 1))) {
        body = appBody.getFunction();
      } else {
        break;
      }
    }

    if (index == 0) {
      return normalize(body);
    }
    if (index == params.size()) {
      return expression;
    }

    Substitution substitution = new Substitution();
    DependentLink newParams = expression.getParameters().subst(substitution, index);
    return new LamExpression(newParams, body.subst(substitution));
  }

  public static Expression normalizePath(AppExpression expr) {
    List<ArgumentExpression> args = new ArrayList<>();
    Expression fun = expr.getFunctionArgs(args);
    Collections.reverse(args);
    if (fun instanceof ConCallExpression && Prelude.isPathCon(((ConCallExpression) fun).getDefinition())) {
      ArgumentExpression lastArg = args.get(args.size() - 1);
      Expression arg = normalize(lastArg.getExpression());
      List<Expression> argArgs = new ArrayList<>();
      arg = arg.getFunction(argArgs);
      if (argArgs.size() > 0 && arg instanceof FunCallExpression && Prelude.isAt(((FunCallExpression) arg).getDefinition())) {
        return argArgs.get(0);
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
      field = normalize(field);
      if (!(field instanceof ProjExpression && ((ProjExpression) field).getField() == index)) {
        return tuple;
      }
      fields[index++] = ((ProjExpression) field).getExpression();
    }
    for (int i = 1; i < fields.length; i++) {
      if (!CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, fields[0], fields[i], null)) {
        return tuple;
      }
    }

    return fields[0];
  }
}
