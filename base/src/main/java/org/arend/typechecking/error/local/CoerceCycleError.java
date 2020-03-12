package org.arend.typechecking.error.local;

import org.arend.ext.error.GeneralError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;

import java.util.List;
import java.util.function.BiConsumer;

public class CoerceCycleError extends TypecheckingError {
  public final List<? extends Concrete.UseDefinition> cycle;
  private final GlobalReferable myCauseReferable;

  private CoerceCycleError(List<? extends Concrete.UseDefinition> cycle, GlobalReferable causeReferable) {
    super("Dependency cycle between \\coerce definitions", cycle.get(0));
    this.cycle = cycle;
    myCauseReferable = causeReferable;
  }

  public CoerceCycleError(List<? extends Concrete.UseDefinition> cycle) {
    this(cycle, null);
  }

  @Override
  public Object getCause() {
    return myCauseReferable != null ? myCauseReferable : cycle;
  }

  @Override
  public void forAffectedDefinitions(BiConsumer<ArendRef, GeneralError> consumer) {
    for (Concrete.UseDefinition def : cycle) {
      consumer.accept(def.getData(), new CoerceCycleError(cycle, def.getData()));
    }
  }
}
