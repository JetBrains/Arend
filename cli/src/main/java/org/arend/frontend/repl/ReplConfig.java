package org.arend.frontend.repl;

import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;

import java.util.List;

public class ReplConfig {
  public NormalizationMode normalizationMode;
  public String prompt;
  public List<PrettyPrinterFlag> prettyPrinterFlags;
}
