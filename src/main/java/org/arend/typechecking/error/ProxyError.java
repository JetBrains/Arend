package org.arend.typechecking.error;

import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

import static org.arend.error.doc.DocFactory.*;

/**
 * A proxy class for local errors
 */
public class ProxyError extends GeneralError {
  public final GlobalReferable definition;
  public final LocalError localError;

  public ProxyError(@Nonnull GlobalReferable definition, LocalError localError) {
    super(localError.level, localError.message);
    this.definition = definition;
    this.localError = localError;
  }

  @Override
  public Object getCause() {
    return localError.getCause();
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return localError.getCauseDoc(src);
  }

  @Override
  public LineDoc getPositionDoc(PrettyPrinterConfig ppConfig) {
    return localError.getPositionDoc(ppConfig);
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return localError.getShortHeaderDoc(ppConfig);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return localError.getBodyDoc(ppConfig);
  }

  @Override
  public Doc getShortDoc() {
    return localError.getShortDoc();
  }

  @Override
  public String getShortMessage() {
    return localError.getShortMessage();
  }

  @Override
  public Doc getDoc(PrettyPrinterConfig src) {
    return vHang(localError.getDoc(src), hList(text("While processing: "), refDoc(definition)));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(definition);
  }

  @Override
  public boolean isTypecheckingError() {
    return localError.isTypecheckingError();
  }
}
