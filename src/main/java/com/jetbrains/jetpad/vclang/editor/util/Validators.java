package com.jetbrains.jetpad.vclang.editor.util;

import com.google.common.base.Predicate;

public class Validators {
  private static final Predicate<String> IDENTIFIER = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      if (input == null) return false;
      if (input.isEmpty()) return false;

      if (!(Character.isLetter(input.charAt(0)) || input.charAt(0) == '_')) return false;
      for (int i = 1; i < input.length(); i++) {
        char ch = input.charAt(i);
        if (!(Character.isLetter(ch) || Character.isDigit(ch) || ch == '_' || ch == '-' || ch == '\'')) return false;
      }

      return true;
    }
  };

  public static Predicate<String> identifier() {
    return IDENTIFIER;
  }
}
