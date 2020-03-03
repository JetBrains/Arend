package org.arend.typechecking.error.local;

import org.arend.core.sort.Sort;
import org.arend.ext.error.TypecheckingError;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

public class DataUniverseError extends TypecheckingError {
  public final Sort actualSort;
  public final Sort specifiedSort;

  public DataUniverseError(Sort actualSort, Sort specifiedSort, @NotNull Concrete.SourceNode cause) {
    super("Actual universe " + actualSort + " is not compatible with specified universe " + specifiedSort, cause);
    this.actualSort = actualSort;
    this.specifiedSort = specifiedSort;
  }
}
