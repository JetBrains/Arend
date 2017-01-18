package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.Set;

public class EmptyClassViewInstanceProvider implements ClassViewInstanceProvider {
  private static final EmptyClassViewInstanceProvider INSTANCE = new EmptyClassViewInstanceProvider();

  private EmptyClassViewInstanceProvider() {}

  public static EmptyClassViewInstanceProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public Set<Abstract.ClassViewInstance> getInstances(Abstract.Definition definition, Abstract.ClassView classView) {
    return Collections.emptySet();
  }
}
