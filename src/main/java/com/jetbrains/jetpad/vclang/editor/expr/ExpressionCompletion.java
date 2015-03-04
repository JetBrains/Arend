package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import jetbrains.jetpad.completion.CompletionItem;
import jetbrains.jetpad.completion.CompletionParameters;
import jetbrains.jetpad.completion.SimpleCompletionItem;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.generic.Role;
import jetbrains.jetpad.projectional.generic.RoleCompletion;

import java.util.ArrayList;
import java.util.List;

public class ExpressionCompletion implements RoleCompletion<Node, Expression> {
  private final int myPrec;
  private static ExpressionCompletion GLOBAL_INSTANCE = new ExpressionCompletion(0);
  private static ExpressionCompletion APP_FUN_INSTANCE = new ExpressionCompletion(Abstract.AppExpression.PREC);
  private static ExpressionCompletion APP_ARG_INSTANCE = new ExpressionCompletion(Abstract.AppExpression.PREC + 1);

  private ExpressionCompletion(int prec) {
    myPrec = prec;
  }

  @Override
  public List<CompletionItem> createRoleCompletion(CompletionParameters completionParameters, Mapper<?, ?> mapper, Node node, final Role<Expression> target) {
    List<CompletionItem> result = new ArrayList<>();
    result.add(new SimpleCompletionItem("lam ", "lambda") {
      @Override
      public Runnable complete(String text) {
        return target.set(new LamExpression(myPrec > Abstract.LamExpression.PREC));
      }
    });
    result.add(new SimpleCompletionItem("app ", "application") {
      @Override
      public Runnable complete(String text) {
        return target.set(new AppExpression(myPrec > Abstract.AppExpression.PREC));
      }
    });
    result.add(new SimpleCompletionItem("var ", "variable") {
      @Override
      public Runnable complete(String text) {
        return target.set(new VarExpression());
      }
    });
    result.add(new SimpleCompletionItem("0") {
      @Override
      public Runnable complete(String text) {
        return target.set(new ZeroExpression());
      }
    });
    result.add(new SimpleCompletionItem("N ", "nat") {
      @Override
      public Runnable complete(String s) {
        return target.set(new NatExpression());
      }
    });
    result.add(new SimpleCompletionItem("N-elim ", "nat-elim") {
      @Override
      public Runnable complete(String s) {
        return target.set(new NelimExpression());
      }
    });
    result.add(new SimpleCompletionItem("S ", "suc") {
      @Override
      public Runnable complete(String s) {
        return target.set(new SucExpression());
      }
    });
    result.add(new SimpleCompletionItem("Type ", "Type") {
      @Override
      public Runnable complete(String s) {
        return target.set(new UniverseExpression());
      }
    });
    result.add(new SimpleCompletionItem("pi ", "pi") {
      @Override
      public Runnable complete(String s) {
        return target.set(new PiExpression(myPrec > Abstract.PiExpression.PREC));
      }
    });
    return result;
  }

  public static ExpressionCompletion getAppFunInstance() {
    return APP_FUN_INSTANCE;
  }

  public static ExpressionCompletion getAppArgInstance() {
    return APP_ARG_INSTANCE;
  }

  public static ExpressionCompletion getGlobalInstance() {
    return GLOBAL_INSTANCE;
  }
}
