package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ParserInfoProvider {
  Abstract.Precedence precedenceOf(GlobalReferable referable);
}
