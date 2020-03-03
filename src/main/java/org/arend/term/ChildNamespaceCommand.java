package org.arend.term;

import org.arend.term.group.ChildGroup;
import org.jetbrains.annotations.Nullable;

public interface ChildNamespaceCommand extends NamespaceCommand {
  @Nullable
  ChildGroup getParentGroup();
}
