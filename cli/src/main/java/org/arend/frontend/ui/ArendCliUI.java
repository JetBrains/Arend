package org.arend.frontend.ui;

import org.arend.ext.ui.ArendSession;
import org.arend.ext.ui.ArendUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArendCliUI implements ArendUI {
  @Override
  public @NotNull ArendSession newSession() {
    return new CliSession();
  }

  @Override
  public void showMessage(@Nullable String title, @NotNull String message) {
    System.out.println((title == null ? "" : title + ": ") + message);
  }

  @Override
  public void showErrorMessage(@Nullable String title, @NotNull String message) {
    System.err.println((title == null ? "" : title + ": ") + message);
  }
}
