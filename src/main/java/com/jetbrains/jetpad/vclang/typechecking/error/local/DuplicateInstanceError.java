package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class DuplicateInstanceError<T> extends LocalTypeCheckingError<T> {
  public final Expression oldInstance;
  public final Expression newInstance;

  public DuplicateInstanceError(Expression oldInstance, Expression newInstance, Concrete.SourceNode<T> cause) {
    super(Level.WARNING, "Duplicate instance", cause);
    this.oldInstance = oldInstance;
    this.newInstance = newInstance;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    return vList(
      hang(text("Old instance:"), termDoc(oldInstance)),
      hang(text("New instance:"), termDoc(newInstance)));
  }
}
