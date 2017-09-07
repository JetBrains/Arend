package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

public interface GlobalReferable extends Referable {
  Precedence getPrecedence();
}
