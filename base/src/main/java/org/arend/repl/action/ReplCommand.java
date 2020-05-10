package org.arend.repl.action;

import org.arend.repl.QuitReplException;
import org.arend.repl.Repl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface ReplCommand {
  /**
   * Displayed in the help message.
   * Do <strong>not</strong> insert a dot at the end of the sentence.
   */
  @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description();

  /**
   * @param line    the command prefix is already removed.
   * @param api     repl context
   * @param scanner user input reader
   */
  void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) throws QuitReplException;
}
