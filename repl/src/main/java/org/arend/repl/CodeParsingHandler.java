package org.arend.repl;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * The default action. No action command.
 * Added to {@link org.arend.repl.ReplState} by default.
 */
public final class CodeParsingHandler implements ReplHandler {
  public static final @NotNull CodeParsingHandler INSTANCE = new CodeParsingHandler();
  public static final @NotNull List<String> definitionEvidence = Arrays.asList(
      "\\import", "\\open", "\\use", "\\func", "\\sfunc", "\\lemma",
      "\\data", "\\module", "\\meta", "\\instance", "\\class");

  private CodeParsingHandler() {
  }

  @Override
  public boolean isApplicable(@NotNull String line) {
    return !line.isBlank() && !line.startsWith(":");
  }

  @Override
  public void invoke(@NotNull String line, @NotNull ReplApi api, @NotNull Supplier<@NotNull String> lineSupplier) {
    if (definitionEvidence.stream().anyMatch(line::contains)) {
      api.checkStatements(line);
      return;
    }

    var expr = api.preprocessExpr(line);
    if (api.checkErrors() || expr == null) return;
    var result = api.checkExpr(expr, null);
    if (result == null) return;
    api.println(api.prettyExpr(new StringBuilder(), result.expression));
  }
}
