package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.ArrayList;

public class ArgInferenceError extends TypeCheckingError {
  private final PrettyPrintable myWhere;
  private final Expression[] myCandidates;

  public ArgInferenceError(ResolvedName resolvedName, String message, Abstract.SourceNode expression, PrettyPrintable where, Expression... candidates) {
    super(resolvedName, message, expression);
    myWhere = where;
    myCandidates = candidates;
  }

  public ArgInferenceError(String message, Abstract.SourceNode expression, PrettyPrintable where, Expression... candidates) {
    super(message, expression);
    myWhere = where;
    myCandidates = candidates;
  }

  public static String functionArg(int index) {
    return "Cannot infer " + ordinal(index) + " argument to function";
  }

  public static String typeOfFunctionArg(int index) {
    return "Cannot infer type of " + ordinal(index) + " argument of function";
  }

  public static String lambdaArg(int index) {
    return "Cannot infer type of " + ordinal(index) + " parameter of lambda";
  }

  public static String parameter(int index) {
    return "Cannot infer " + ordinal(index) + " parameter to constructor";
  }

  public static String expression() {
    return "Cannot infer an expression";
  }

  public static String type() {
    return "Cannot infer type of expression";
  }

  public static String suffix(int n) {
    if (n >= 10 && n < 20) {
      return "th";
    }
    switch (n % 10) {
      case 1:
        return "st";
      case 2:
        return "nd";
      case 3:
        return "rd";
      default:
        return "th";
    }
  }

  public static String ordinal(int n) {
    return n + suffix(n);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(printHeader()).append(getMessage());
    if (getCause() != null) {
      if (myWhere != null) {
        builder.append(' ');
        myWhere.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
      } else {
        builder.append(' ');
        new PrettyPrintVisitor(builder, new ArrayList<String>(), 0).prettyPrint(getCause(), Abstract.Expression.PREC);
      }
    }

    if (myCandidates.length > 0) {
      builder.append("\nCandidates are:");
      for (Expression candidate : myCandidates) {
        builder.append("\n\t");
        candidate.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
      }
    }

    return builder.toString();
  }
}
