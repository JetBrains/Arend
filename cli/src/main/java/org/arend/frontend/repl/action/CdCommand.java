package org.arend.frontend.repl.action;

import org.arend.frontend.repl.CommonCliRepl;
import org.arend.repl.action.DirectoryArgumentCommand;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class CdCommand implements CliReplCommand, DirectoryArgumentCommand {
  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Modify current working directory";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull CommonCliRepl api, @NotNull Supplier<@NotNull String> scanner) {
    api.pwd = api.pwd.resolve(line.trim()).normalize();
  }
}
