package org.arend.frontend.parser;

import org.arend.ext.error.GeneralError;
import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.ModuleReferable;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

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
  public void forAffectedDefinitions(BiConsumer<ArendRef, GeneralError> consumer) {
    consumer.accept(new ModuleReferable(position.module), this);
  }

  @Nonnull
  @Override
  public Stage getStage() {
    return Stage.PARSER;
  }
}
