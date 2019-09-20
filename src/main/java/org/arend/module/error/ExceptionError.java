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
import java.util.function.BiConsumer;

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
  public void forAffectedDefinitions(BiConsumer<GlobalReferable, GeneralError> consumer) {
    if (affectedReferable != null) {
      consumer.accept(affectedReferable, this);
    }
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return DocFactory.text(stringWriter.toString());
  }

  @Override
  public boolean isShort() {
    return false;
  }
}
