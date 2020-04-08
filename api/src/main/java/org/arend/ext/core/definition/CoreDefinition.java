package org.arend.ext.core.definition;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a core definition.
 */
public interface CoreDefinition {
  /**
   * Returns the underlying reference of this definition.
   */
  @NotNull ArendRef getRef();

  /**
   * Returns the linked list of parameters of this definition.
   * This list is always empty for class definitions and consists of a single parameter for fields.
   */
  @NotNull CoreParameter getParameters();
}
