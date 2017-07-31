package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;

public class ParserError extends ModuleLoadingError {
  public final Concrete.Position position;

  public ParserError(Concrete.Position position, String message) {
    super(position.module, message);
    this.position = position;
  }

  @Override
  public LineDoc getPositionDoc(SourceInfoProvider src) {
    return text(position.toString());
  }
}
