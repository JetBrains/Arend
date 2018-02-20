package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.group.ChildGroup;

import javax.annotation.Nullable;

public interface ChildNamespaceCommand extends NamespaceCommand {
  @Nullable
  ChildGroup getParentGroup();
}
