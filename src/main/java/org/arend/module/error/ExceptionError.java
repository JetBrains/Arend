package org.arend.module.error;

import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.module.ModulePath;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.ModuleReferable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;

public class ExceptionError extends GeneralError {
  public final Exception exception;
  public final GlobalReferable affectedReferable;

  public ExceptionError(Exception exception, String action, GlobalReferable affectedReferable) {
    super(Level.ERROR, "An exception happened while " + action + (affectedReferable == null ? "" : " module: " + affectedReferable.textRepresentation()));
    this.exception = exception;
    this.affectedReferable = affectedReferable;
  }

  public ExceptionError(Exception exception, String action) {
    this(exception, action, (GlobalReferable) null);
  }

  public ExceptionError(Exception exception, String action, ModulePath modulePath) {
    this(exception, action, new ModuleReferable(modulePath));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return affectedReferable == null ? Collections.emptyList() : Collections.singletonList(affectedReferable);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return DocFactory.text(stringWriter.toString());
  }
}
