package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;
import java.util.Collections;

public class EmptyClassViewInstanceProvider implements ClassViewInstanceProvider {
  private static final EmptyClassViewInstanceProvider INSTANCE = new EmptyClassViewInstanceProvider();

  private EmptyClassViewInstanceProvider() {}

  public static EmptyClassViewInstanceProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances(Abstract.DefCallExpression defCall, int paramIndex) {
    return Collections.emptyList();
  }
}
