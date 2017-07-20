package com.jetbrains.jetpad.vclang.term;

public class BaseAbstractVisitor<P, R> implements AbstractDefinitionVisitor<P, R> {
  @Override
  public R visitFunction(Abstract.FunctionDefinition def, P params) {
    return null;
  }

  @Override
  public R visitClassField(Abstract.ClassField def, P params) {
    return null;
  }

  @Override
  public R visitData(Abstract.DataDefinition def, P params) {
    return null;
  }

  @Override
  public R visitConstructor(Abstract.Constructor def, P params) {
    return null;
  }

  @Override
  public R visitClass(Abstract.ClassDefinition def, P params) {
    return null;
  }

  @Override
  public R visitImplement(Abstract.Implementation def, P params) {
    return null;
  }

  @Override
  public R visitClassView(Abstract.ClassView def, P params) {
    return null;
  }

  @Override
  public R visitClassViewField(Abstract.ClassViewField def, P params) {
    return null;
  }

  @Override
  public R visitClassViewInstance(Abstract.ClassViewInstance def, P params) {
    return null;
  }
}
