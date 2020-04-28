package org.arend.repl.action;

import org.arend.library.Library;
import org.arend.repl.ReplApi;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.Scanner;

public class LoadLibraryCommand extends ReplCommand {
  public LoadLibraryCommand(@NotNull String command) {
    super(command);
  }

  @Override
  protected void doInvoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner) {
    Library library = api.createLibrary(Paths.get(line));
    if (library == null || api.checkErrors()) {
      api.eprintln("[ERROR] Cannot find a library at '" + line + "'.");
      return;
    }
    if (!api.loadLibrary(library)) {
      api.checkErrors();
      api.eprintln("[ERROR] No library loaded.");
    }
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description() {
    return "Load a library of given directory or arend.yaml file";
  }
}
