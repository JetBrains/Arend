package com.jetbrains.jetpad.vclang.term;

public interface ConcreteDefinitionVisitor<T, P, R> {
  R visitFunction(Concrete.FunctionDefinition<T> def, P params);
  R visitData(Concrete.DataDefinition<T> def, P params);
  R visitClass(Concrete.ClassDefinition<T> def, P params);
  R visitClassView(Concrete.ClassView<T> def, P params);
  R visitClassViewField(Concrete.ClassViewField<T> def, P params);
  R visitInstance(Concrete.Instance<T> def, P params);
}
