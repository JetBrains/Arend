package com.jetbrains.jetpad.vclang.term;

public class BaseAbstractVisitor<T, P, R> implements ConcreteDefinitionVisitor<T, P, R> {
  @Override
  public R visitFunction(Concrete.FunctionDefinition<T> def, P params) {
    return null;
  }

  @Override
  public R visitData(Concrete.DataDefinition<T> def, P params) {
    return null;
  }

  @Override
  public R visitClass(Concrete.ClassDefinition<T> def, P params) {
    return null;
  }

  @Override
  public R visitClassView(Concrete.ClassView<T> def, P params) {
    return null;
  }

  @Override
  public R visitClassViewField(Concrete.ClassViewField<T> def, P params) {
    return null;
  }

  @Override
  public R visitInstance(Concrete.Instance<T> def, P params) {
    return null;
  }
}
