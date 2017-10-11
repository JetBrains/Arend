package com.jetbrains.jetpad.vclang.term;

import javax.annotation.Nullable;

public interface ChildNamespaceCommand extends NamespaceCommand {
  @Nullable ChildGroup getParentGroup();
}
