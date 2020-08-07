package org.arend.term.concrete;

public interface ConcreteResolvableDefinitionVisitor<P, R> extends ConcreteDefinitionVisitor<P, R> {
  R visitMeta(DefinableMetaDefinition def, P params);
}
