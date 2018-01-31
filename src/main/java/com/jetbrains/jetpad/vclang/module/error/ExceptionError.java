package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

public class ExceptionError extends GeneralError {
  public final Exception exception;
  public final ModulePath modulePath;

  public ExceptionError(Exception exception, ModulePath modulePath) {
    super(Level.ERROR, "An exception happened while loading module: " + modulePath);
    this.exception = exception;
    this.modulePath = modulePath;
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(new ModuleReferable(modulePath));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return DocFactory.text(exception.getLocalizedMessage());
  }
}
