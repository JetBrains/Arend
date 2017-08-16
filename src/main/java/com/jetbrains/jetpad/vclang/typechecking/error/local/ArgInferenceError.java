package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ArgInferenceError extends LocalTypeCheckingError {
  public final Expression[] candidates;
  public final Expression expected;
  public final Expression actual;

  public ArgInferenceError(String message, Abstract.SourceNode expression, Expression[] candidates) {
    super(message, expression);
    this.candidates = candidates;
    this.expected = null;
    this.actual = null;
  }

  public ArgInferenceError(String message, Expression expected, Expression actual, Abstract.SourceNode expression, Expression candidate) {
    super(message, expression);
    this.candidates = new Expression[1];
    this.candidates[0] = candidate;
    this.expected = expected;
    this.actual = actual;
  }

  public static String functionArg(int index, String function) {
    return "Cannot infer the " + ordinal(index) + " argument to function" + (function != null ? " " + function : "");
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

  public static String expression() {
    return "Cannot infer an expression";
  }

  public static String typeClass() {
    return "Cannot infer a type class instance";
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
  public Doc getBodyDoc(SourceInfoProvider src) {
    return vList(
      candidates.length == 0
        ? nullDoc()
        : hang(text("Candidates are:"),
            vList(Arrays.stream(candidates).map(DocFactory::termDoc).collect(Collectors.toList()))),
      expected == null && actual == null
        ? nullDoc()
        : vList(text("Since types of the candidates are not less or equal to the expected type"),
                expected == null ? nullDoc() : hang(text("Expected type:"), termDoc(expected)),
                actual   == null ? nullDoc() : hang(text("  Actual type:"), termDoc(actual))));
  }
}
