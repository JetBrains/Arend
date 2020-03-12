package org.arend.term.abs;

public interface AbstractDefinitionVisitor<R> {
  R visitFunction(Abstract.FunctionDefinition def);
  R visitData(Abstract.DataDefinition def);
  R visitClass(Abstract.ClassDefinition def);
}
