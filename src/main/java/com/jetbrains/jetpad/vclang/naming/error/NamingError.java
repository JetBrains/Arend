package com.jetbrains.jetpad.vclang.naming.error;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalError;

public class NamingError extends LocalError {
  public final Object cause;

  public NamingError(String message, Object cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  public NamingError(Level level, String message, Object cause) {
    super(level, message);
    this.cause = cause;
  }

  @Override
  public Object getCause() {
    return cause instanceof Concrete.SourceNode ? ((Concrete.SourceNode) cause).getData() : cause;
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return cause instanceof Concrete.SourceNode ? DocFactory.ppDoc((PrettyPrintable) cause, src) :
           cause instanceof PrettyPrintable ? ((PrettyPrintable) cause).prettyPrint(src) :
           null;
  }
}
