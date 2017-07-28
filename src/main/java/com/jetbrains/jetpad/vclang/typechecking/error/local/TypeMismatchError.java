package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.Abstract;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class TypeMismatchError extends LocalTypeCheckingError {
  public final Doc expected;
  public final Doc actual;

  public TypeMismatchError(Doc expected, Doc actual, Abstract.SourceNode sourceNode) {
    super("Type mismatch", sourceNode);
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  public Doc getBodyDoc() {
    return vList(
      hang(text("Expected type:"), expected),
      hang(text("  Actual type:"), actual));
  }
}
