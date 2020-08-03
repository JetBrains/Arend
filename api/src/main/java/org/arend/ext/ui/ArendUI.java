package org.arend.ext.ui;

import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a way for the extension to interact with the user.
 */
public interface ArendUI {
  @NotNull ArendSession newSession();

  void showMessage(@Nullable String title, @NotNull String message);

  void showErrorMessage(@Nullable String title, @NotNull String message);

  void println(@NotNull Doc doc);

  default void println(@NotNull String text) {
    println(DocFactory.text(text));
  }
}
