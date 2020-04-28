package org.arend.repl.action;

import org.arend.repl.ReplApi;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Scanner;
import java.util.stream.Collectors;

public class ListLoadedModulesAction extends ReplCommand {
  public ListLoadedModulesAction(@NotNull String command) {
    super(command);
  }

  @Override
  protected void doInvoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner) {
    var string = api.getReplLibrary().getLoadedModules().stream().map(String::valueOf).collect(Collectors.joining(" "));
    if (string.isBlank()) api.println("[INFO] No modules loaded.");
    else api.println(string);
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description() {
    return "List all loaded modules";
  }
}
