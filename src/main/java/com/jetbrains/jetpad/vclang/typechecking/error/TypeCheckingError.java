package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import javax.annotation.Nonnull;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

/**
 * If you would like to add a new type checking error, please, extend {@link LocalTypeCheckingError} instead.
 */
public class TypeCheckingError<T> extends GeneralError<T> {
  public final GlobalReferable definition;
  public final LocalTypeCheckingError<T> localError;

  public TypeCheckingError(@Nonnull GlobalReferable definition, LocalTypeCheckingError<T> localError) {
    super(localError.level, localError.message);
    this.definition = definition;
    this.localError = localError;
  }

  @Override
  public T getCause() {
    return localError.getCause();
  }

  @Override
  public PrettyPrintable getCausePP() {
    return localError.getCausePP();
  }

  @Override
  public Doc getDoc(PrettyPrinterInfoProvider src) {
    return vHang(localError.getDoc(src), hList(text("While typechecking: "), refDoc(definition)));
  }
}
