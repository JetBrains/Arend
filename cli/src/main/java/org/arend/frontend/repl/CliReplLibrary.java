package org.arend.frontend.repl;

import org.arend.ext.module.ModulePath;
import org.arend.frontend.source.FileRawSource;
import org.arend.repl.ReplLibrary;
import org.arend.typechecking.TypecheckerState;
import org.jetbrains.annotations.NotNull;

public class CliReplLibrary extends ReplLibrary {
  public CliReplLibrary(@NotNull TypecheckerState typecheckerState) {
    super(typecheckerState);
  }

  @Override
  public @NotNull FileRawSource getRawSource(ModulePath modulePath) {
    return new FileRawSource(currentDir, modulePath, false);
  }
}
