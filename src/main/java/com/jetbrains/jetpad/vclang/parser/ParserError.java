package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class ParserError extends ModuleLoadingError {
  public final Concrete.Position position;

  public ParserError(ModuleID module, Concrete.Position position, String message) {
    super(module, message);
    this.position = position;
  }

  public ParserError(Concrete.Position position, String message) {
    this(null, position, message);
  }
}
