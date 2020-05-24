package org.arend.repl.action;

import org.arend.repl.QuitReplException;
import org.arend.repl.Repl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class AliasableCommand implements ReplCommand {
  public @NotNull AliasCommand createAlias() {
    return new AliasCommand();
  }

  private int aliases = 0;

  @Override
  public boolean isAliased() {
    return aliases > 0;
  }

  public final class AliasCommand implements ReplCommand {
    public AliasCommand() {
      aliases++;
    }

    @Override
    public @Nls @NotNull String help(@NotNull Repl api) {
      return AliasableCommand.this.help(api);
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
      return AliasableCommand.this.description();
    }

    @Override
    public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) throws QuitReplException {
      AliasableCommand.this.invoke(line, api, scanner);
    }
  }
}
