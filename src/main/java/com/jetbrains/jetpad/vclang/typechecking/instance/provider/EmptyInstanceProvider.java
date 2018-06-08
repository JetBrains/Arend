package com.jetbrains.jetpad.vclang.typechecking.instance.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.Collection;
import java.util.Collections;

public class EmptyInstanceProvider implements InstanceProvider {
  private static final EmptyInstanceProvider INSTANCE = new EmptyInstanceProvider();

  private EmptyInstanceProvider() {}

  public static EmptyInstanceProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public Collection<? extends Concrete.Instance> getInstances(ClassReferable classRef) {
    return Collections.emptyList();
  }
}
