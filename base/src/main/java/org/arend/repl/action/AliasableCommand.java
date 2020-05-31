package org.arend.repl.action;

import java.util.List;

public abstract class AliasableCommand implements ReplCommand {
  public final List<String> aliases;

  protected AliasableCommand(List<String> aliases) {
    this.aliases = aliases;
  }
}
