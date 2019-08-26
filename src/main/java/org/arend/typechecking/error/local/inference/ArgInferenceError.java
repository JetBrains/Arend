package org.arend.typechecking.error.local.inference;

import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.TypecheckingError;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.*;

public class ArgInferenceError extends TypecheckingError {
  public final Expression[] candidates;
  public final Expression expected;
  public final Expression actual;

  protected ArgInferenceError(String message, Expression expected, Expression actual, Concrete.SourceNode cause, Expression[] candidates) {
    super(message, cause);
    this.candidates = candidates;
    this.expected = expected;
    this.actual = actual;
  }

  public ArgInferenceError(String message, Concrete.SourceNode cause, Expression[] candidates) {
    this(message, null, null, cause, candidates);
  }

  public ArgInferenceError(String message, Expression expected, Expression actual, Concrete.SourceNode cause, Expression candidate) {
    this(message, expected, actual, cause, new Expression[1]);
    candidates[0] = candidate;
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

  public static String number(int n, String r) {
    return n + " " + r + (n == 1 ? "" : "s");
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      candidates.length == 0
        ? nullDoc()
        : hang(text("Candidates are:"),
            vList(Arrays.stream(candidates).map(expr -> termDoc(expr, ppConfig)).collect(Collectors.toList()))),
      expected == null && actual == null
        ? nullDoc()
        : vList(text("Since types of the candidates are not less than or equal to the expected type"),
                expected == null ? nullDoc() : hang(text("Expected type:"), termDoc(expected, ppConfig)),
                actual   == null ? nullDoc() : hang(text("  Actual type:"), termDoc(actual, ppConfig))));
  }
}
