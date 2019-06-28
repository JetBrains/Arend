package org.arend.error;

import org.arend.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Errors that can be readily reported to an ErrorReporter.
 */
public abstract class GeneralError extends Error {
  public GeneralError(@Nonnull Level level, String message) {
    super(level, message);
  }

  public void forAffectedDefinitions(BiConsumer<GlobalReferable, GeneralError> consumer) {
    Object cause = getCause();
    if (cause instanceof GlobalReferable) {
      consumer.accept((GlobalReferable) cause, this);
    } else if (cause instanceof Collection) {
      for (Object elem : ((Collection) cause)) {
        if (elem instanceof GlobalReferable) {
          consumer.accept((GlobalReferable) elem, this);
        }
      }
    }
  }

  public boolean isSevere() {
    return getCause() == null;
  }
}
