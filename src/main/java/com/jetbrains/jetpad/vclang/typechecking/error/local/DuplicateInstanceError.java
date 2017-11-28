package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class DuplicateInstanceError extends TypecheckingError {
  public final Expression oldInstance;
  public final Expression newInstance;

  public DuplicateInstanceError(Expression oldInstance, Expression newInstance, Concrete.SourceNode cause) {
    super(Level.WARNING, "Duplicate instance", cause);
    this.oldInstance = oldInstance;
    this.newInstance = newInstance;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Old instance:"), termDoc(oldInstance, ppConfig)),
      hang(text("New instance:"), termDoc(newInstance, ppConfig)));
  }
}
