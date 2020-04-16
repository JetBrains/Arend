package org.arend.ext.prettyprinting;

import org.arend.ext.core.ops.NormalizationMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PrettyPrinterConfigImpl implements PrettyPrinterConfig {
  public boolean isSingleLine;
  public EnumSet<PrettyPrinterFlag> expressionFlags;
  public NormalizationMode normalizationMode;
  public DefinitionRenamer definitionRenamer;

  public PrettyPrinterConfigImpl(PrettyPrinterConfig config) {
    isSingleLine = config.isSingleLine();
    expressionFlags = config.getExpressionFlags();
    normalizationMode = config.getNormalizationMode();
    definitionRenamer = config.getDefinitionRenamer();
  }

  public PrettyPrinterConfigImpl() {
    this(PrettyPrinterConfig.DEFAULT);
  }

  @Override
  public boolean isSingleLine() {
    return isSingleLine;
  }

  @Override
  public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
    return expressionFlags;
  }

  @Override
  public @Nullable NormalizationMode getNormalizationMode() {
    return normalizationMode;
  }

  @Override
  public DefinitionRenamer getDefinitionRenamer() {
    return definitionRenamer;
  }
}
