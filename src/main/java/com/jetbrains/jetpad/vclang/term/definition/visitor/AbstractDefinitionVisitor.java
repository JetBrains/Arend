package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface AbstractDefinitionVisitor<P, R> {
  R visitFunction(Abstract.FunctionDefinition def, P params);
  // R visitData(Abstract.DataDefinition def, P params);
}
