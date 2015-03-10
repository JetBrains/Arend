package com.jetbrains.jetpad.vclang.editor.util;

import jetbrains.jetpad.completion.CompletionItem;

import static com.jetbrains.jetpad.vclang.editor.util.Validators.identifier;

public abstract class IdCompletionItem implements CompletionItem {
  @Override
  public String visibleText(String text) {
    return "ID";
  }

  @Override
  public boolean isStrictMatchPrefix(String text) {
    return identifier().apply(text);
  }

  @Override
  public boolean isMatchPrefix(String text) {
    return identifier().apply(text);
  }

  @Override
  public boolean isMatch(String text) {
    return identifier().apply(text);
  }

  @Override
  public boolean isLowPriority() {
    return true;
  }
}
