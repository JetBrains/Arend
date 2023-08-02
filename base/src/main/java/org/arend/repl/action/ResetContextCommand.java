package org.arend.repl.action;

import org.arend.repl.QuitReplException;
import org.arend.repl.Repl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ResetContextCommand implements ReplCommand {
  public static final @NotNull ResetContextCommand INSTANCE = new ResetContextCommand();
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @Override
  public @NotNull String description() {
    return "Reset Repl context";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) throws QuitReplException {
    api.resetReplContext();
    api.println("Repl context has been cleared");
  }
}
