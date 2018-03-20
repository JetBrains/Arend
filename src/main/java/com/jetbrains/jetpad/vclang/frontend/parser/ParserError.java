package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;

import java.util.Collection;
import java.util.Collections;

public class ParserError extends GeneralError {
  public final Position position;

  public ParserError(Position position, String message) {
    super(Level.ERROR, message);
    this.position = position;
  }

  @Override
  public Position getCause() {
    return position;
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(new ModuleReferable(position.module));
  }
}
