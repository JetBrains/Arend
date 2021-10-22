package org.arend.core.definition;

public interface DefinitionVisitor<P, R> {
  R visitFunction(FunctionDefinition def, P params);
  R visitData(DataDefinition def, P params);
  R visitClass(ClassDefinition def, P params);
  R visitConstructor(Constructor constructor, P params);
  R visitField(ClassField field, P params);
}
