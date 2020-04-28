package org.arend.repl;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface ReplHandler {
  boolean isApplicable(@NotNull String line);

  void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> lineSupplier);
}
