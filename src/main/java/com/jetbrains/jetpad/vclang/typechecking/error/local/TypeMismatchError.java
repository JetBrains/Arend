package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class TypeMismatchError extends TypecheckingError {
  public final Doc expected;
  public final Doc actual;

  public TypeMismatchError(Doc expected, Doc actual, Concrete.SourceNode sourceNode) {
    super("Type mismatch", sourceNode);
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    return vList(
      hang(text("Expected type:"), expected),
      hang(text("  Actual type:"), actual));
  }
}
