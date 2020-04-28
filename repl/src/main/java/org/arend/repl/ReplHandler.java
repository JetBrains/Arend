package org.arend.repl;

import org.jetbrains.annotations.NotNull;

import java.util.Scanner;

public interface ReplHandler {
  boolean isApplicable(@NotNull String line);

  void invoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner);
}
