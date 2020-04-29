package org.arend.ext.ui;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a query to the user.
 */
public interface ArendQuery<T> {
  /**
   * @return the result chosen by the user.
   *         May be {@code null} if the user did not choose anything (yet).
   */
  @Nullable T getResult();
}
