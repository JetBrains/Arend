package org.arend.repl.action;

import org.arend.repl.ReplApi;
import org.jetbrains.annotations.NotNull;

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

  private DefaultAction() {
  }

  @Override
  public boolean isApplicable(@NotNull String line) {
    return !line.isBlank() && !line.startsWith(":");
  }

  @Override
  public void invoke(@NotNull String line, @NotNull ReplApi state, @NotNull Scanner scanner) {
    if (definitionEvidence.stream().anyMatch(line::contains)) {
      state.checkStatements(line);
      return;
    }

    var result = state.checkExpr(line, null);
    if (result == null) return;
    state.println(state.prettyExpr(new StringBuilder(), result.expression));
  }
}
