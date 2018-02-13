package com.jetbrains.jetpad.vclang.source.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

public class LocationError extends GeneralError {
  public final GlobalReferable referable;

  private LocationError(GlobalReferable referable, String message) {
    super(Level.ERROR, message);
    this.referable = referable;
  }

  public static LocationError definition(GlobalReferable referable) {
    return new LocationError(referable, "Cannot locate definition: ");
  }

  public static LocationError module(ModulePath modulePath) {
    return new LocationError(new ModuleReferable(modulePath), "Cannot locate module: ");
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    return DocFactory.hList(super.getHeaderDoc(src), DocFactory.refDoc(referable));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(referable);
  }
}
