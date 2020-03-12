package org.arend.ext.core.body;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreElimBody extends CoreBody {
  @NotNull List<? extends CoreElimClause> getClauses();
}
