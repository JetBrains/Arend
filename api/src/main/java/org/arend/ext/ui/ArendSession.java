package org.arend.ext.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface ArendSession {
  /**
   * Sets a description of the session.
   */
  void setDescription(@Nullable String description);

  /**
   * Sets a callback that will be invoked when the session ends.
   * The callback is invoked with {@code true} if the session finished normally and with {@code false} if it was cancelled.
   */
  void setCallback(@Nullable Consumer<Boolean> callback);

  /**
   * Adds a message.
   */
  void message(@NotNull String message);

  /**
   * Adds a query that lets the user to chose one option from a list.
   *
   * @param message         a message that will be shown to the user.
   * @param options         a list of options.
   * @param defaultOption   the default option.
   */
  <T> @NotNull ArendQuery<T> listQuery(@Nullable String message, @NotNull List<T> options, @Nullable T defaultOption);

  /**
   * Adds a query that lets the user choose one of two options yes/no.
   *
   * @param message         a message that will be shown to the user.
   * @param defaultValue    the default answer.
   */
  @NotNull ArendQuery<Boolean> binaryQuery(@Nullable String message, @Nullable Boolean defaultValue);

  /**
   * Adds a query that lets the user input a string.
   *
   * @param message         a message that will be shown to the user.
   * @param defaultValue    the default string.
   */
  @NotNull ArendQuery<String> stringQuery(@Nullable String message, @Nullable String defaultValue);

  /**
   * Adds a query that lets the user input an integer number.
   *
   * @param message         a message that will be shown to the user.
   * @param defaultValue    the default number.
   */
  @NotNull ArendQuery<Integer> intQuery(@Nullable String message, @Nullable Integer defaultValue);

  /**
   * Adds an embedded session.
   */
  void embedded(@NotNull ArendSession session);

  /**
   * Starts a session.
   * This will show all registered queries and invoke the appropriate callback when the session is finished.
   */
  void startSession();
}
