package org.arend.repl.action;

import org.arend.ext.module.ModulePath;
import org.arend.naming.scope.Scope;
import org.arend.repl.ReplApi;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Scanner;

public final class UnloadFileCommand extends ReplCommand {
  public UnloadFileCommand(@NotNull String command) {
    super(command);
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Unload an Arend module loaded before.";
  }

  @Override
  protected void doInvoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner) {
    var modulePath = ModulePath.fromString(line);
    if (!api.getReplLibrary().containsModule(modulePath)) {
      api.eprintln("[ERROR] Module " + modulePath + " is not loaded.");
      return;
    }
    Scope scope = api.loadModule(modulePath);
    assert scope == null || api.removeScope(scope);
  }
}
