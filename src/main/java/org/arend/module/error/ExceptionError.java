package org.arend.module.error;

import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.ModuleReferable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.BiConsumer;

public class ExceptionError extends GeneralError {
  public final Exception exception;
  public final GlobalReferable affectedReferable;

  public ExceptionError(Exception exception, String action, GlobalReferable affectedReferable) {
    super(Level.ERROR, "An exception happened during " + action + (affectedReferable == null ? "" : " of module: " + affectedReferable.textRepresentation()));
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
  public void forAffectedDefinitions(BiConsumer<ArendRef, GeneralError> consumer) {
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
