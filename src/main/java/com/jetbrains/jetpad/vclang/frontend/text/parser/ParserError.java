package com.jetbrains.jetpad.vclang.frontend.text.parser;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.frontend.text.Position;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;

public class ParserError extends ModuleLoadingError {
  public final Position position;

  public ParserError(Position position, String message) {
    super(position.module, message);
    this.position = position;
  }

  @Override
  public LineDoc getPositionDoc(SourceInfoProvider src) {
    return text(position.toString());
  }
}
