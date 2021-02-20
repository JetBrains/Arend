package org.arend.naming.error;

import org.arend.ext.error.NameResolverError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class PrecedenceError extends NameResolverError {
  public final Referable ref1;
  public final Precedence prec1;
  public final Referable ref2;
  public final Precedence prec2;

  public PrecedenceError(Referable ref1, Precedence prec1, Referable ref2, Precedence prec2, Object cause) {
    super("", cause);
    this.ref1 = ref1;
    this.prec1 = prec1;
    this.ref2 = ref2;
    this.prec2 = prec2;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    Precedence prec1 = this.prec1 != null ? this.prec1 : ref1 instanceof GlobalReferable ? ((GlobalReferable) ref1).getPrecedence() : null;
    Precedence prec2 = this.prec2 != null ? this.prec2 : ref2 instanceof GlobalReferable ? ((GlobalReferable) ref2).getPrecedence() : null;
    return hList(text("Precedence parsing error: cannot mix "), refDoc(ref1), text((prec1 != null ? " [" + prec1 + "]" : "") + " and "), refDoc(ref2), text((prec2 != null ? " [" + prec2 + "]" : "") + " in the same infix expression"));
  }
}
