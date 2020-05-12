package org.arend.repl.action;

import org.arend.repl.QuitReplException;
import org.arend.repl.Repl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface ReplCommand {
  /**
   * Displayed in <code>:help</code>.
   * Do <strong>not</strong> insert a dot at the end of the sentence.
   */
  @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description();

  /**
   * Displayed in <code>:help [name of command]</code>.
   */
  default @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String help() {
    return description() + ".";
  }

  /**
   * @param line    the command prefix is already removed.
   * @param api     repl context
   * @param scanner user input reader
   */
  void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) throws QuitReplException;
}
