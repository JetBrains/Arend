package org.arend.ext.typechecking;

import org.arend.ext.concrete.pattern.ConcretePattern;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the context information of the current position of a pattern.
 *
 * @see ContextData
 */
public interface PatternContextData extends BaseContextData {
  @NotNull ConcretePattern getMarker();
}
