package org.arend.frontend.repl.action;

import org.arend.frontend.repl.CommonCliRepl;
import org.arend.library.Library;
import org.arend.repl.Repl;
import org.arend.repl.action.ReplCommand;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class LoadLibraryCommand implements ReplCommand {
  public static final @NotNull LoadLibraryCommand INSTANCE = new LoadLibraryCommand();

  private LoadLibraryCommand() {
  }

  @Override
  public final void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
    assert api instanceof CommonCliRepl;
    var cliApi = (CommonCliRepl) api;
    Library library = cliApi.createLibrary(line);
    if (library == null || cliApi.checkErrors()) {
      cliApi.eprintln("[ERROR] Cannot find a library at '" + line + "'.");
      return;
    }
    if (!cliApi.loadLibrary(library)) {
      cliApi.checkErrors();
      cliApi.eprintln("[ERROR] No library loaded.");
    }
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description() {
    return "Load a library of given directory or arend.yaml file";
  }
}
