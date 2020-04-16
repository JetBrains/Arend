package org.arend.term.prettyprint;

import org.arend.core.expr.visitor.DefCallRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterConfigImpl;

public class PrettyPrinterConfigWithRenamer extends PrettyPrinterConfigImpl {
  public PrettyPrinterConfigWithRenamer(PrettyPrinterConfig config) {
    super(config);
    definitionRenamer = new DefCallRenamer(null);
  }

  public PrettyPrinterConfigWithRenamer() {
    definitionRenamer = new DefCallRenamer(null);
  }
}
