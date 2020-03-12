package org.arend.ext.core.body;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CorePattern {
  /**
   * If the pattern is a variable pattern, return the binding corresponding to the variable.
   * Otherwise, returns null.
   *
   * @return the variable bound in the pattern or null.
   */
  @Nullable CoreBinding getBinding();

  /**
   * If the pattern is a constructor pattern, returns either a {@link CoreConstructor}
   * or a {@link CoreFunctionDefinition} (for defined constructors).
   * Otherwise, returns null.
   *
   * @return the head constructor of the pattern or null.
   */
  @Nullable CoreDefinition getDefinition();

  /**
   * If the pattern is a constructor pattern or a tuple pattern, returns the list of subpatterns.
   * Otherwise, returns the empty list.
   *
   * @return the list of subpatterns.
   */
  @NotNull List<? extends CorePattern> getSubPatterns();

  /**
   * @return true if the pattern is the absurd pattern, false otherwise.
   */
  boolean isAbsurd();
}
