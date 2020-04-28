package org.arend.repl.action;

import org.arend.repl.ReplApi;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Scanner;

public abstract class ReplCommand implements ReplAction {
  public final @NotNull String commandWithColon;

  protected ReplCommand(@NotNull @NonNls String command) {
    commandWithColon = ":" + command;
  }

  @Override
  public boolean isApplicable(@NotNull String line) {
    return line.startsWith(commandWithColon);
  }

  @Override
  public final void invoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner) {
    var content = line.substring(commandWithColon.length()).trim();
    doInvoke(content, api, scanner);
  }

  /**
   * @param line    the command prefix is already removed.
   * @param api     repl context
   * @param scanner user input reader
   */
  protected abstract void doInvoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner);
}
