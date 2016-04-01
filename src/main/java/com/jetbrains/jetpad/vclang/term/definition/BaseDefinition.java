package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface BaseDefinition extends Abstract.Binding {
  Abstract.Definition.Precedence getPrecedence();
}
