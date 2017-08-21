package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;

public interface InstanceProvider {
  Collection<? extends Concrete.ClassViewInstance> getInstances(Concrete.ClassView classView);
}
