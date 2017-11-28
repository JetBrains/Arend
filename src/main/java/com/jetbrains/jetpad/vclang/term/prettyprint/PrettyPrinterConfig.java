package com.jetbrains.jetpad.vclang.term.prettyprint;

import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;

import java.util.EnumSet;

public interface PrettyPrinterConfig {
  default boolean isSingleLine() {
    return false;
  }

  default EnumSet<ToAbstractVisitor.Flag> getExpressionFlags() {
    return EnumSet.of(
      ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS,
      ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM,
      ToAbstractVisitor.Flag.SHOW_CON_PARAMS);
  }

  default NormalizeVisitor.Mode getNormalizationMode() {
    return NormalizeVisitor.Mode.RNF;
  }

  PrettyPrinterConfig DEFAULT = new PrettyPrinterConfig() {};
}
