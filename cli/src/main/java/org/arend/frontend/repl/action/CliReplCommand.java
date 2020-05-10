package org.arend.frontend.repl.action;

import org.arend.frontend.repl.CommonCliRepl;
import org.arend.repl.Repl;
import org.arend.repl.action.ReplCommand;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface CliReplCommand extends ReplCommand {
  @Override
  default void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
    assert api instanceof CommonCliRepl;
    invoke(line, (CommonCliRepl) api, scanner);
  }

  /**
   * @param line    the command prefix is already removed.
   * @param api     repl context
   * @param scanner user input reader
   */
  void invoke(@NotNull String line, @NotNull CommonCliRepl api, @NotNull Supplier<@NotNull String> scanner);
}
