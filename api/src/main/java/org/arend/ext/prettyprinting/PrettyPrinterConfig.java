package org.arend.ext.prettyprinting;

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
      PrettyPrinterFlag.SHOW_FIELD_INSTANCE,
      PrettyPrinterFlag.SHOW_TYPES_IN_LAM,
      PrettyPrinterFlag.SHOW_CON_PARAMS);
  }

  @Nullable
  default NormalizationMode getNormalizationMode() {
    return getExpressionFlags().contains(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS) ? NormalizationMode.RNF : NormalizationMode.RNF_EXP;
  }

  PrettyPrinterConfig DEFAULT = new PrettyPrinterConfig() {};
}
