package org.arend.ext.error.quickFix;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ErrorQuickFix {
  @NotNull String getMessage();

  /**
   * The target will be replaced with the result of this method.
   * If the result is {@code null}, the target will be removed.
   * The quick fix will use the data of subnodes instead of subnodes themselves if possible.
   * Thus, if the result is constructed with {@link org.arend.ext.concrete.ConcreteFactory},
   * the data of the factory should be set to {@code null}.
   */
  @Nullable ConcreteSourceNode getReplacement();

  /**
   * @return The (data of the) concrete source node to which this quick fix should be applied.
   *         If returns {@code null}, then the cause of the error will be used as the target.
   */
  default @Nullable Object getTarget() {
    return null;
  }
}
