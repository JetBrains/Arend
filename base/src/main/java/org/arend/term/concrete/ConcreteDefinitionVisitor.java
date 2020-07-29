package org.arend.term.concrete;

public interface ConcreteDefinitionVisitor<P, R> {
  R visitFunction(Concrete.BaseFunctionDefinition def, P params);
  R visitData(Concrete.DataDefinition def, P params);
  R visitClass(Concrete.ClassDefinition def, P params);
  R visitMeta(Concrete.MetaDefinition def, P params);
}
