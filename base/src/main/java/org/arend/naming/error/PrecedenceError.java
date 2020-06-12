package org.arend.naming.error;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class PrecedenceError extends NamingError {
  public final GlobalReferable ref1;
  public final GlobalReferable ref2;

  public PrecedenceError(GlobalReferable ref1, GlobalReferable ref2, Object cause) {
    super("", cause);
    this.ref1 = ref1;
    this.ref2 = ref2;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Precedence parsing error: cannot mix "), refDoc(ref1), text(" [" + ref1.getPrecedence() + "] and "), refDoc(ref2), text(" [" + ref2.getPrecedence() + "] in the same infix expression"));
  }
}
