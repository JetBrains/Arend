package org.arend.source.error;

import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.ModuleReferable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

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
}
