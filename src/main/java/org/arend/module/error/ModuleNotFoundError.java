package org.arend.module.error;

import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.module.ModulePath;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.ModuleReferable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

import static org.arend.error.doc.DocFactory.*;

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
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return currentModule == null ? Collections.emptyList() : Collections.singletonList(new ModuleReferable(currentModule));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return currentModule == null ? nullDoc() : hList(text("While processing: "), refDoc(new ModuleReferable(currentModule)));
  }
}
