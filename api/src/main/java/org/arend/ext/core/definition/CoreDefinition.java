package org.arend.ext.core.definition;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Represents a core definition.
 */
public interface CoreDefinition extends Variable {
  /**
   * Returns the underlying reference of this definition.
   */
  @NotNull ArendRef getRef();

  /**
   * Returns the linked list of parameters of this definition.
   * This list is always empty for class definitions and consists of a single parameter for fields.
   */
  @NotNull CoreParameter getParameters();

  /**
   * Returns the set of definitions mutually recursive with the given one including itself.
   * If the definition is not recursive, the result will be empty.
   * If the definition is recursive, then the result will be a singleton set consisting of the given definition.
   */
  @NotNull Set<? extends CoreDefinition> getRecursiveDefinitions();
}
