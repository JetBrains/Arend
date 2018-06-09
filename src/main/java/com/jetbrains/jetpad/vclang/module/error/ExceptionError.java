package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;

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
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(affectedReferable);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return DocFactory.text(stringWriter.toString());
  }
}
