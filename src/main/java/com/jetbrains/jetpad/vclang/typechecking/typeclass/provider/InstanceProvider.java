package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;

public interface InstanceProvider {
  Collection<? extends Abstract.ClassViewInstance> getInstances(Abstract.ClassView classView);
}
