package org.arend.module.error;

import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.naming.reference.ModuleReferable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ModuleNotFoundError extends GeneralError {
  public final ModulePath notFoundModule;
  public final ModulePath currentModule;

  public ModuleNotFoundError(ModulePath notFoundModule, ModulePath currentModule) {
    super(Level.ERROR, "Module not found: " + notFoundModule);
    this.notFoundModule = notFoundModule;
    this.currentModule = currentModule;
  }

  public ModuleNotFoundError(ModulePath notFoundModule) {
    this(notFoundModule, null);
  }

  @Override
  public ModuleReferable getCause() {
    return currentModule == null ? null : new ModuleReferable(currentModule);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return currentModule == null ? nullDoc() : hList(text("While processing: "), refDoc(new ModuleReferable(currentModule)));
  }
}
