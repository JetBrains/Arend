package org.arend.module.error;

import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.module.ModulePath;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.ModuleReferable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

public class DeserializationError extends GeneralError {
  public final Exception exception;
  public final ModulePath modulePath;

  public DeserializationError(ModulePath modulePath, Exception exception) {
    super(Level.WARNING, "Cannot deserialize module '" + modulePath + "'");
    this.exception = exception;
    this.modulePath = modulePath;
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(new ModuleReferable(modulePath));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.text(exception.getLocalizedMessage());
  }
}
