package com.jetbrains.jetpad.vclang.source.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class LocationError extends GeneralError {
  public final GlobalReferable referable;
  public final ModulePath modulePath;

  private LocationError(String message, GlobalReferable referable, ModulePath modulePath) {
    super(Level.ERROR, message);
    this.referable = referable;
    this.modulePath = modulePath;
  }

  public static LocationError definition(GlobalReferable referable, ModulePath modulePath) {
    return new LocationError("Cannot locate definition: ", referable, modulePath);
  }

  public static LocationError module(ModulePath modulePath) {
    return new LocationError("Cannot locate module", null, modulePath);
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return referable == null ? text(message) : DocFactory.hList(text(message), DocFactory.refDoc(referable));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return hList(text("While persisting: "), refDoc(new ModuleReferable(modulePath)));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.emptyList();
  }
}
