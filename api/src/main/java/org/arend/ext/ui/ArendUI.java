package org.arend.ext.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a way for the extension to interact with the user.
 */
public interface ArendUI {
  @NotNull ArendSession newSession();

  void showMessage(@Nullable String title, @NotNull String message);

  void showErrorMessage(@Nullable String title, @NotNull String message);

  @NotNull ArendConsole getConsole(@Nullable Object marker);
}
