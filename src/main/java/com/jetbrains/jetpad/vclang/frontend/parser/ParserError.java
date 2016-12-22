package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.frontend.Concrete;

public class ParserError extends ModuleLoadingError {
  public final Concrete.Position position;

  public ParserError(Concrete.Position position, String message) {
    super(position.module, message);
    this.position = position;
  }
}
