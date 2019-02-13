package org.arend.term.prettyprint;

import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.ToAbstractVisitor;

import java.util.EnumSet;

public interface PrettyPrinterConfig {
  default boolean isSingleLine() {
    return false;
  }

  default EnumSet<ToAbstractVisitor.Flag> getExpressionFlags() {
    return EnumSet.of(
      ToAbstractVisitor.Flag.SHOW_INFERENCE_LEVEL_VARS,
      ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS,
      ToAbstractVisitor.Flag.SHOW_FIELD_INSTANCE,
      ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM,
      ToAbstractVisitor.Flag.SHOW_CON_PARAMS);
  }

  default NormalizeVisitor.Mode getNormalizationMode() {
    return NormalizeVisitor.Mode.RNF;
  }

  PrettyPrinterConfig DEFAULT = new PrettyPrinterConfig() {};
}
