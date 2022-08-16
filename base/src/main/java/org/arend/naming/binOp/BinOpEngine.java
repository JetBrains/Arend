package org.arend.naming.binOp;

import org.arend.ext.util.Pair;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Customize parsing abstract bin op sequences
 */
interface BinOpEngine<T extends Concrete.SourceNode> {

  /**
   * Extracts a referable from a component. Extracted referable is a subject of being a root of newly parsed application tree
   */
  @Nullable Referable getReferable(@NotNull T elem);

  @NotNull T wrapSequence(Object data, @NotNull T base, List<@NotNull Pair<? extends T, Boolean>> explicitComponents);

  /**
   * leftRef is a custom unnamed referable that may be appended to incomplete parsed bin op trees
   */
  @NotNull T augmentWithLeftReferable(Object data, @NotNull Referable leftRef, @NotNull T mid, T right);

  @NotNull @Nls String getPresentableComponentName();
}
