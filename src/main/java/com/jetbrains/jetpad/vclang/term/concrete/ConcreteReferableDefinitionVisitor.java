package com.jetbrains.jetpad.vclang.term.concrete;

public interface ConcreteReferableDefinitionVisitor<P, R> extends ConcreteDefinitionVisitor<P, R> {
  R visitConstructor(Concrete.Constructor def, P params);
  R visitClassField(Concrete.ClassField def, P params);
}
