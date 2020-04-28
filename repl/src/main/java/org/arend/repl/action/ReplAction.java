package org.arend.repl.action;

import org.arend.repl.ReplApi;
import org.jetbrains.annotations.NotNull;

public interface ReplAction {
  boolean isApplicable(@NotNull String line);

  void invoke(@NotNull String line, @NotNull ReplApi state);
}
