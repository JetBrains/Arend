package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ArgInferenceError extends TypecheckingError {
  public final CoreExpression[] candidates;
  public final CoreExpression expected;
  public final CoreExpression actual;

  protected ArgInferenceError(String message, CoreExpression expected, CoreExpression actual, ConcreteSourceNode cause, CoreExpression[] candidates) {
    super(message, cause);
    this.candidates = candidates;
    this.expected = expected;
    this.actual = actual;
  }

  public ArgInferenceError(String message, ConcreteSourceNode cause, CoreExpression[] candidates) {
    this(message, null, null, cause, candidates);
  }

  public ArgInferenceError(String message, CoreExpression expected, CoreExpression actual, ConcreteSourceNode cause, CoreExpression candidate) {
    this(message, expected, actual, cause, new CoreExpression[1]);
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
        : hang(text(candidates.length == 1 ? "Candidate is:" : "Candidates are:"),
            DocFactory.vList(Arrays.stream(candidates).map(expr -> termDoc(expr, ppConfig)).collect(Collectors.toList()))),
      expected == null && actual == null
        ? nullDoc()
        : vList(text("Since types of the candidates are not less than or equal to the expected type"),
                expected == null ? nullDoc() : hang(text("Expected type:"), termDoc(expected, ppConfig)),
                actual   == null ? nullDoc() : hang(text("  Actual type:"), termDoc(actual, ppConfig))));
  }

  @Override
  public boolean hasExpressions() {
    return candidates.length != 0 || expected != null || actual != null;
  }
}
