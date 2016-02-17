package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.List;

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
      case 1: return "st";
      case 2: return "nd";
      case 3: return "rd";
      default: return "th";
    }
  }

  public static String ordinal(int n) {
    return n + suffix(n);
  }

  @Override
  public String toString() {
    String msg = printHeader() + getMessage();
    if (getCause() != null) {
      if (myWhere != null) {
        msg += " " + prettyPrint(myWhere);
      } else {
        String r = prettyPrint(getCause());
        if (r != null) {
          msg += " " + r;
        }
      }
    }

    if (myCandidates.length > 0) {
      msg += "\nCandidates are:";
      for (Expression candidate : myCandidates) {
        msg += "\n\t" + candidate;
      }
    }

    return msg;
  }

  public static class StringPrettyPrintable implements PrettyPrintable {
    private final String myString;

    public StringPrettyPrintable(String string) {
      myString = string;
    }

    public StringPrettyPrintable(Name name) {
      myString = name.getPrefixName();
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      builder.append(myString);
    }
  }
}
