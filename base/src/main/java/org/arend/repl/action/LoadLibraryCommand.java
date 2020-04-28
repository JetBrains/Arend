package org.arend.repl.action;

import org.arend.library.Library;
import org.arend.repl.Repl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class LoadLibraryCommand implements ReplCommand {
  @Override
  public final void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
    Library library = createLibrary(line);
    if (library == null || api.checkErrors()) {
      api.eprintln("[ERROR] Cannot find a library at '" + line + "'.");
      return;
    }
    if (!api.loadLibrary(library)) {
      api.checkErrors();
      api.eprintln("[ERROR] No library loaded.");
    }
  }

  protected abstract @Nullable Library createLibrary(@NotNull String path);

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description() {
    return "Load a library of given directory or arend.yaml file";
  }
}
