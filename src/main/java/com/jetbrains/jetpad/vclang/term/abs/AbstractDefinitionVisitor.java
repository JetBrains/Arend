package com.jetbrains.jetpad.vclang.term.abs;

public interface AbstractDefinitionVisitor<R> {
  R visitFunction(Abstract.FunctionDefinition def);
  R visitData(Abstract.DataDefinition def);
  R visitClass(Abstract.ClassDefinition def);
  // R visitClassView(Abstract.ClassView def); TODO[classes]
  // R visitInstance(Abstract.Instance def); TODO[classes]
}
