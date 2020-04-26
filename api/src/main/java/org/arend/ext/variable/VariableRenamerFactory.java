package org.arend.ext.variable;

import org.jetbrains.annotations.NotNull;

public interface VariableRenamerFactory {
  @NotNull VariableRenamer variableRenamer();
}
