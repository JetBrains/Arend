package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Set;

public interface ClassViewInstanceProvider {
  Set<Abstract.ClassViewInstance> getInstances(Abstract.Definition definition, Abstract.ClassView classView);
}
