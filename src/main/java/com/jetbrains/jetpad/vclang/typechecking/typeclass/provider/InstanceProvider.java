package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.Collection;

public interface InstanceProvider {
  Collection<? extends Concrete.Instance> getInstances(Concrete.ClassView classView);
}
