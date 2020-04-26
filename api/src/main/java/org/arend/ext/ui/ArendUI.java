package org.arend.ext.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Provides a way for the extension to interact with the user.
 */
public interface ArendUI {
  /**
   * Lets the user to chose one option from a list.
   * This method can be invoked only once.
   *
   * @param title     a message that will be shown to the user.
   * @param message   an additional message that will be shown to the user.
   * @param options   a list of options.
   * @param callback  a callback for the result.
   *                  Invoked with the chosen option or with {@code null} if no option was chosen.
   */
  <T> void singleQuery(@Nullable String title, @Nullable String message, @NotNull List<T> options, @NotNull Consumer<T> callback);
}
