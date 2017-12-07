package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

/**
 * A proxy class for local errors
 */
public class ProxyError extends GeneralError {
  public final GlobalReferable definition;
  public final LocalError localError;

  public ProxyError(@Nonnull GlobalReferable definition, LocalError localError) {
    super(localError.level, localError.message);
    this.definition = definition;
    this.localError = localError;
  }

  @Override
  public Object getCause() {
    return localError.getCause();
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return localError.getCauseDoc(src);
  }

  @Override
  public Doc getDoc(PrettyPrinterConfig src) {
    return vHang(localError.getDoc(src), hList(text("While processing: "), refDoc(definition)));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(definition);
  }
}
