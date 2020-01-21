package org.arend.ext.error;

import org.arend.ext.reference.ArendRef;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

public class LocalError extends GeneralError {
  public ArendRef definition;

  public LocalError(@Nonnull Level level, String message) {
    super(level, message);
  }

  @Override
  public Object getCause() {
    Object cause = super.getCause();
    return cause != null ? cause : definition;
  }

  @Override
  public void forAffectedDefinitions(BiConsumer<ArendRef, GeneralError> consumer) {
    if (definition != null) {
      consumer.accept(definition, this);
    }
  }

  public LocalError withDefinition(ArendRef definition) {
    this.definition = definition;
    return this;
  }
}
