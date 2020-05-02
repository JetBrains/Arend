package org.arend.frontend.repl.action;

import org.arend.frontend.repl.CommmonCliRepl;
import org.arend.repl.Repl;
import org.arend.repl.action.ReplCommand;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ListLoadedModulesAction implements ReplCommand {
  public static final @NotNull ListLoadedModulesAction INSTANCE = new ListLoadedModulesAction();

  private ListLoadedModulesAction() {
  }

  @Override
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
    assert api instanceof CommmonCliRepl;
    var cliApi = (CommmonCliRepl) api;
    var string = cliApi.getReplLibrary().getLoadedModules().stream().map(String::valueOf).collect(Collectors.joining(" "));
    if (string.isBlank()) cliApi.println("[INFO] No modules loaded.");
    else cliApi.println(string);
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "List all loaded modules";
  }
}
