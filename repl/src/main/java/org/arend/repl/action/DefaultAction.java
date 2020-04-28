package org.arend.repl.action;

import org.arend.repl.ReplApi;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * The default action. No action command.
 * Added to {@link org.arend.repl.ReplState} by default.
 */
public final class DefaultAction implements ReplAction {
  public static final @NotNull DefaultAction INSTANCE = new DefaultAction();
  public static final @NotNull List<String> definitionEvidence = Arrays.asList(
      "\\import", "\\open", "\\use", "\\func", "\\sfunc", "\\lemma",
      "\\data", "\\module", "\\meta", "\\instance", "\\class");

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description() {
    return null;
  }

  private DefaultAction() {
  }

  @Override
  public boolean isApplicable(@NotNull String line) {
    return !line.isBlank() && !line.startsWith(":");
  }

  @Override
  public void invoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner) {
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
