package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ParserInfoProvider {
  Abstract.Precedence precedenceOf(Abstract.GlobalReferableSourceNode referable);
}
