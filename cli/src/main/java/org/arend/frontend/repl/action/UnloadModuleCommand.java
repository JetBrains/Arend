package org.arend.frontend.repl.action;

import org.arend.ext.module.ModulePath;
import org.arend.frontend.repl.CommmonCliRepl;
import org.arend.naming.scope.Scope;
import org.arend.repl.Repl;
import org.arend.repl.action.ReplCommand;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class UnloadModuleCommand implements ReplCommand {
  public static final @NotNull UnloadModuleCommand INSTANCE = new UnloadModuleCommand();

  private UnloadModuleCommand() {
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Unload an Arend module loaded before.";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
    var modulePath = ModulePath.fromString(line);
    assert api instanceof CommmonCliRepl;
    var cliApi = (CommmonCliRepl) api;
    if (!cliApi.getReplLibrary().containsModule(modulePath)) {
      cliApi.eprintln("[ERROR] Module " + modulePath + " is not loaded.");
      return;
    }
    Scope scope = cliApi.getAvailableModuleScopeProvider().forModule(modulePath);
    if (scope != null) cliApi.removeScope(scope);
    boolean isUnloaded = cliApi.unloadModule(modulePath);
    assert isUnloaded;
    cliApi.checkErrors();
  }
}
