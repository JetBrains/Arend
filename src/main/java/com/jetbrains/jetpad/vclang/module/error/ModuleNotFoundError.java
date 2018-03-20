package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ModuleNotFoundError extends GeneralError {
  public final ModulePath notFoundModule;
  public final ModulePath currentModule;

  public ModuleNotFoundError(ModulePath notFoundModule) {
    super(Level.ERROR, "Module not found: " + notFoundModule);
    this.notFoundModule = notFoundModule;
    this.currentModule = null;
  }

  public ModuleNotFoundError(ModulePath notFoundModule, ModulePath currentModule) {
    super(Level.ERROR, "Module not found: " + notFoundModule);
    this.notFoundModule = notFoundModule;
    this.currentModule = currentModule;
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
