package org.arend.repl.action;

import org.arend.repl.ReplState;
import org.jetbrains.annotations.NotNull;

public abstract class ReplCommand implements ReplAction {
  private final @NotNull String myCommandWithColon;

  protected ReplCommand(@NotNull String command) {
    myCommandWithColon = ":" + command;
  }

  @Override
  public boolean isApplicable(@NotNull String line) {
    return line.startsWith(myCommandWithColon);
  }

  @Override
  public void invoke(@NotNull String line, @NotNull ReplState state) {
    var content = line.substring(myCommandWithColon.length()).trim();
    doInvoke(content, state);
  }

  /**
   * @param line  the command is already removed.
   * @param state repl context
   */
  protected abstract void doInvoke(@NotNull String line, @NotNull ReplState state);
}
