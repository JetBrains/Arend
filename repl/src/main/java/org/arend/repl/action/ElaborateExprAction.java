package org.arend.repl.action;

import org.arend.repl.ReplState;
import org.jetbrains.annotations.NotNull;

public final class ElaborateExprAction implements ReplAction {
  @Override
  public boolean isApplicable(@NotNull String line) {
    return !line.isBlank();
  }

  @Override
  public void invoke(@NotNull String line, @NotNull ReplState state) {
    var result = state.checkExpr(line, null);
    if (result == null) return;
    state.println(state.prettyExpr(new StringBuilder(), result.expression));
  }
}
