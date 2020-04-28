package org.arend.repl.action;

import org.arend.repl.ReplApi;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Scanner;

public interface ReplAction {
  boolean isApplicable(@NotNull String line);

  void invoke(@NotNull String line, @NotNull ReplApi state, @NotNull Scanner scanner);

  /**
   * Displayed in the help message.
   */
  @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description();
}
