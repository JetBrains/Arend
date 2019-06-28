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

  public ExceptionError(Exception exception, GlobalReferable affectedReferable, boolean isLoading) {
    super(Level.ERROR, "An exception happened while " + (isLoading ? "loading" : "persisting") + " module: " + affectedReferable.textRepresentation());
    this.exception = exception;
    this.affectedReferable = affectedReferable;
  }

  public ExceptionError(Exception exception, ModulePath modulePath, boolean isLoading) {
    this(exception, new ModuleReferable(modulePath), isLoading);
  }

  @Override
  public void forAffectedDefinitions(BiConsumer<GlobalReferable, GeneralError> consumer) {
    consumer.accept(affectedReferable, this);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return DocFactory.text(stringWriter.toString());
  }
}
