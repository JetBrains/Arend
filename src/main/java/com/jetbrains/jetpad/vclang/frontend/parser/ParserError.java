package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;

public class ParserError extends ModuleLoadingError {
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
