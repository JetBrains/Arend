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
  private final Expression myExpected;
  private final Expression myActual;

  public ArgInferenceError(ResolvedName resolvedName, String message, Abstract.SourceNode expression, PrettyPrintable where, Expression... candidates) {
    super(resolvedName, message, expression);
    myWhere = where;
    myCandidates = candidates;
    myExpected = null;
    myActual = null;
  }

  public ArgInferenceError(String message, Abstract.SourceNode expression, PrettyPrintable where, Expression... candidates) {
    super(message, expression);
    myWhere = where;
    myCandidates = candidates;
    myExpected = null;
    myActual = null;
  }

  public ArgInferenceError(String message, Expression expected, Expression actual, Abstract.SourceNode expression, PrettyPrintable where, Expression candidate) {
    super(message, expression);
    myWhere = where;
    myCandidates = new Expression[1];
    myCandidates[0] = candidate;
    myExpected = expected;
    myActual = actual;
  }

  public static String functionArg(int index) {
    return "Cannot infer the " + ordinal(index) + " argument to function";
  }

  public static String typeOfFunctionArg(int index) {
    return "Cannot infer type of the " + ordinal(index) + " argument of function";
  }

  public static String lambdaArg(int index) {
    return "Cannot infer type of the " + ordinal(index) + " parameter of lambda";
  }

  public static String levelOfLambdaArg(int index) {
    return "Cannot infer level of the type of the " + ordinal(index) + " parameter of lambda";
  }

  public static String parameter(int index) {
    return "Cannot infer the " + ordinal(index) + " parameter to constructor";
  }

  public static String expression() {
    return "Cannot infer an expression";
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
        myWhere.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 0);
      } else {
        builder.append(' ');
        new PrettyPrintVisitor(builder, new ArrayList<String>(), 0).prettyPrint(getCause(), Abstract.Expression.PREC);
      }
    }

    if (myCandidates.length > 0) {
      builder.append("\nCandidates are:");
      for (Expression candidate : myCandidates) {
        builder.append("\n");
        PrettyPrintVisitor.printIndent(builder, PrettyPrintVisitor.INDENT / 2);
        candidate.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, PrettyPrintVisitor.INDENT / 2);
      }
    }

    if (myExpected != null || myActual != null) {
      builder.append("\nSince types of the candidates are not less or equal to the expected type");
      if (myExpected != null) {
        String msg = "Expected type: ";
        builder.append('\n').append(msg);
        myExpected.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, msg.length());
      }
      if (myActual != null) {
        String msg = "  Actual type: ";
        builder.append('\n').append(msg);
        myActual.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, msg.length());
      }
    }

    return builder.toString();
  }
}
