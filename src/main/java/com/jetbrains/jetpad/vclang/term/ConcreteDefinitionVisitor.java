package com.jetbrains.jetpad.vclang.term;

public interface ConcreteDefinitionVisitor<T, P, R> {
  R visitFunction(Concrete.FunctionDefinition<T> def, P params);
  R visitClassField(Concrete.ClassField<T> def, P params);
  R visitData(Concrete.DataDefinition<T> def, P params);
  R visitConstructor(Concrete.Constructor<T> def, P params);
  R visitClass(Concrete.ClassDefinition<T> def, P params);
  R visitImplement(Concrete.Implementation<T> def, P params);
  R visitClassView(Concrete.ClassView<T> def, P params);
  R visitClassViewField(Concrete.ClassViewField<T> def, P params);
  R visitClassViewInstance(Concrete.ClassViewInstance<T> def, P params);
}
