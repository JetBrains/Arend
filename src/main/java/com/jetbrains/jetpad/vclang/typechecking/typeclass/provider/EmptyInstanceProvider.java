package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.Collections;

public class EmptyInstanceProvider implements InstanceProvider {
  private static final EmptyInstanceProvider INSTANCE = new EmptyInstanceProvider();

  private EmptyInstanceProvider() {}

  public static EmptyInstanceProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public Collection<? extends Concrete.ClassViewInstance> getInstances(Concrete.ClassView classView) {
    return Collections.emptyList();
  }
}
