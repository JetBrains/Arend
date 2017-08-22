package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;

public interface ParserInfoProvider {
  Precedence precedenceOf(GlobalReferable referable);
}
