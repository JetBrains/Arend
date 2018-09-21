package org.arend.term;

import org.arend.term.group.ChildGroup;

import javax.annotation.Nullable;

public interface ChildNamespaceCommand extends NamespaceCommand {
  @Nullable
  ChildGroup getParentGroup();
}
