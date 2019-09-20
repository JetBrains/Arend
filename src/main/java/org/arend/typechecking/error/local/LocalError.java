package org.arend.typechecking.error.local;

import org.arend.error.GeneralError;
import org.arend.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

public class LocalError extends GeneralError {
  public GlobalReferable definition;

  public LocalError(@Nonnull Level level, String message) {
    super(level, message);
  }

  @Override
  public Object getCause() {
    Object cause = super.getCause();
    return cause != null ? cause : definition;
  }

  @Override
  public void forAffectedDefinitions(BiConsumer<GlobalReferable, GeneralError> consumer) {
    if (definition != null) {
      consumer.accept(definition, this);
    }
  }

  public LocalError withDefinition(GlobalReferable definition) {
    this.definition = definition;
    return this;
  }
}
