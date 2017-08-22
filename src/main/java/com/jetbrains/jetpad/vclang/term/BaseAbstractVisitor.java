package com.jetbrains.jetpad.vclang.term;

public class BaseAbstractVisitor<T, P, R> implements ConcreteDefinitionVisitor<T, P, R> {
  @Override
  public R visitFunction(Concrete.FunctionDefinition<T> def, P params) {
    return null;
  }

  @Override
  public R visitClassField(Concrete.ClassField<T> def, P params) {
    return null;
  }

  @Override
  public R visitData(Concrete.DataDefinition<T> def, P params) {
    return null;
  }

  @Override
  public R visitConstructor(Concrete.Constructor<T> def, P params) {
    return null;
  }

  @Override
  public R visitClass(Concrete.ClassDefinition<T> def, P params) {
    return null;
  }

  @Override
  public R visitImplement(Concrete.Implementation<T> def, P params) {
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
  public R visitClassViewInstance(Concrete.ClassViewInstance<T> def, P params) {
    return null;
  }
}
