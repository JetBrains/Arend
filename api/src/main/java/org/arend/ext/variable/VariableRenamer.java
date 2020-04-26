package org.arend.ext.variable;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Can be used to generate fresh names.
 */
public interface VariableRenamer {
  /**
   * Generates a fresh name distinct from names of variables in {@code variables}.
   */
  @NotNull String generateFreshName(@NotNull Variable var, @NotNull Collection<? extends Variable> variables);
}
