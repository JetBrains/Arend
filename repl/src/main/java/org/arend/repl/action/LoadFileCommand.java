package org.arend.repl.action;

import org.arend.ext.module.ModulePath;
import org.arend.naming.scope.Scope;
import org.arend.repl.ReplApi;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Scanner;

public final class LoadFileCommand extends ReplCommand {
  public LoadFileCommand(@NotNull String command) {
    super(command);
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Load an Arend module from working directory.";
  }

  @Override
  protected void doInvoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner) {
    Scope scope = api.loadModule(ModulePath.fromString(line));
    if (scope != null) api.addScope(scope);
    else api.println("[INFO] No module loaded.");
    api.checkErrors();
  }
}
