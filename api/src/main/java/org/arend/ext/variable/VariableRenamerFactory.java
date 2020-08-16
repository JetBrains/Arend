package org.arend.ext.variable;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.UncheckedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface VariableRenamerFactory {
  @NotNull VariableRenamer variableRenamer();

  /**
   * Computes a name for a variable based on its type.
   *
   * @param type  the type of the variable
   * @param def   the default name
   * @return the computed name or {@code def} if the name cannot be computed
   */
  @NotNull String getNameFromType(@NotNull UncheckedExpression type, @Nullable String def);

  default @NotNull String getNameFromBinding(@NotNull CoreBinding binding, @Nullable String def) {
    String name = binding.getName();
    return name != null ? name : getNameFromType(binding.getTypeExpr(), null);
  }
}
