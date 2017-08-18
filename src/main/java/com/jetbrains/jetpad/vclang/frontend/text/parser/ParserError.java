package com.jetbrains.jetpad.vclang.frontend.text.parser;

import com.jetbrains.jetpad.vclang.frontend.text.Position;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;

public class ParserError extends ModuleLoadingError<Position> {
  public final Position position;

  public ParserError(Position position, String message) {
    super(position.module, message);
    this.position = position;
  }

  @Override
  public Position getCause() {
    return position;
  }
}
