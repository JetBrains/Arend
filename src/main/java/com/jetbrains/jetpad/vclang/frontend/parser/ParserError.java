package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class ParserError extends GeneralError {
  public Position position;

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
    return Collections.singletonList(new SourceIdReference(position.module));
  }

  public void fixup(Function<SourceId, SourceId> fix) {
    position = new Position(fix.apply(position.module), position.line, position.column);
  }
}
