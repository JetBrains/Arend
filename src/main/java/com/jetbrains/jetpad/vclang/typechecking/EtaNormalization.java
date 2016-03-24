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
    if (expression instanceof LamExpression) {
      return normalizeLam((LamExpression) expression);
    }
    if (expression instanceof AppExpression) {
      return normalizePath((AppExpression) expression);
    }
    if (expression instanceof TupleExpression) {
      return normalizeTuple((TupleExpression) expression);
    }
    if (expression instanceof OfTypeExpression) {
      return normalize(((OfTypeExpression) expression).getExpression());
    }

    return expression;
  }

  public static Expression normalizeLam(LamExpression expression) {
    List<DependentLink> params = DependentLink.Helper.toList(expression.getParameters());
    Expression body = normalize(expression.getBody());
    List<? extends Expression> args = body.getArguments();

    int index = 0;
    for (; index < params.size() && index < args.size(); index++) {
      Expression arg = normalize(args.get(args.size() - 1 - index));
      DependentLink param = params.get(params.size() - 1 - index);
      if (!(arg instanceof ReferenceExpression && ((ReferenceExpression) arg).getBinding() == param && !body.getFunction().findBinding(param))) {
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

    body = Apps(body.getFunction(), args.subList(0, args.size() - index), ((AppExpression) body).getFlags().subList(0, args.size() - index));
    if (index == params.size()) {
      return body;
    }

    Substitution substitution = new Substitution();
    DependentLink newParams = expression.getParameters().subst(substitution, params.size() - index);
    return new LamExpression(newParams, body.subst(substitution));
  }

  public static Expression normalizePath(AppExpression expr) {
    List<? extends Expression> args = expr.getArguments();
    Expression fun = expr.getFunction();
    if (fun instanceof ConCallExpression && Prelude.isPathCon(((ConCallExpression) fun).getDefinition())) {
      Expression arg = normalize(args.get(args.size() - 1));
      List<? extends Expression> argArgs = arg.getArguments();
      arg = arg.getFunction();
      if (argArgs.size() > 0 && arg instanceof FunCallExpression && Prelude.isAt(((FunCallExpression) arg).getDefinition())) {
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

    return normalize(fields[0]);
  }
}
