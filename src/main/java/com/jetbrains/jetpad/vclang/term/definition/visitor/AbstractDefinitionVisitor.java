package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface AbstractDefinitionVisitor<P, R> {
  R visitFunction(Abstract.FunctionDefinition def, P params);
  R visitAbstract(Abstract.AbstractDefinition def, P params);
  R visitData(Abstract.DataDefinition def, P params);
  R visitConstructor(Abstract.Constructor def, P params);
  R visitClass(Abstract.ClassDefinition def, P params);
  R visitImplement(Abstract.ImplementDefinition def, P params);
}
