package org.arend.module.error;

import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.naming.reference.ModuleReferable;

public class DeserializationError extends GeneralError {
  public final Exception exception;
  public final ModulePath modulePath;

  public DeserializationError(ModulePath modulePath, Exception exception) {
    super(Level.WARNING, "Cannot deserialize module '" + modulePath + "'");
    this.exception = exception;
    this.modulePath = modulePath;
  }

  @Override
  public ModuleReferable getCause() {
    return new ModuleReferable(modulePath);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.text(exception.getLocalizedMessage());
  }
}
