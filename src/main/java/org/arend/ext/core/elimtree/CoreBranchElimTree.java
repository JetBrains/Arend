package org.arend.ext.core.elimtree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public interface CoreBranchElimTree extends CoreElimTree {
  @Nonnull Collection<? extends Map.Entry<? extends CoreBranchKey, ? extends CoreElimTree>> getChildren();

  @Nullable CoreElimTree getChild(@Nullable CoreBranchKey key);
  @Nullable CoreBranchKey getSingleConstructorKey();
  @Nullable CoreElimTree getSingleConstructorChild();
}
