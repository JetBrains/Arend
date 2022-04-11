package org.arend.ext.prettyprinting;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.NormalizationMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public interface PrettyPrinterConfig {
  default boolean isSingleLine() {
    return false;
  }

  @NotNull
  default EnumSet<PrettyPrinterFlag> getExpressionFlags() {
    return EnumSet.of(
      PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS,
      PrettyPrinterFlag.SHOW_IMPLICIT_ARGS,
      PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE,
      PrettyPrinterFlag.SHOW_TYPES_IN_LAM,
      PrettyPrinterFlag.SHOW_CON_PARAMS);
  }

  @Nullable
  default NormalizationMode getNormalizationMode() {
    return getExpressionFlags().contains(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS) ? NormalizationMode.RNF : NormalizationMode.RNF_EXP;
  }

  default @Nullable DefinitionRenamer getDefinitionRenamer() {
    return null;
  }

  /**
   * If an expression has a positive verbose level, then it is printed with additional information.
   * The kind of the information depends on runtime class of the expression.
   * For example, if a {@link org.arend.ext.core.expr.CoreAppExpression} has verbose level = 2,
   * then it will be printed with two additional implicit arguments.
   */
  default int getVerboseLevel(@NotNull CoreExpression expression) {
        return 0;
    }

  PrettyPrinterConfig DEFAULT = new PrettyPrinterConfig() {};
}
