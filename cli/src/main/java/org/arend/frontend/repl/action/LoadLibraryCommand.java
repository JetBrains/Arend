package org.arend.frontend.repl.action;

import org.arend.frontend.repl.CommonCliRepl;
import org.arend.library.Library;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class LoadLibraryCommand implements CliReplCommand {
  public static final @NotNull LoadLibraryCommand INSTANCE = new LoadLibraryCommand();

  private LoadLibraryCommand() {
  }

  @Override
  public final void invoke(@NotNull String line, @NotNull CommonCliRepl api, @NotNull Supplier<@NotNull String> scanner) {
    Library library = api.createLibrary(line);
    if (library == null || api.checkErrors()) {
      api.eprintln("[ERROR] Cannot find a library at '" + line + "'.");
      // check again in case `library == null`
      api.checkErrors();
      return;
    }
    api.println("[INFO] Starts loading library " + library.getName() + "...");
    long startTime = System.currentTimeMillis();
    if (!api.loadLibrary(library)) {
      api.checkErrors();
      api.eprintln("[ERROR] No library loaded.");
    } else {
      api.println("[INFO] Library " + library.getName() + " loaded in " + (System.currentTimeMillis() - startTime) + "ms.");
    }
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @Nullable String description() {
    return "Load a library of given directory or arend.yaml file";
  }
}
