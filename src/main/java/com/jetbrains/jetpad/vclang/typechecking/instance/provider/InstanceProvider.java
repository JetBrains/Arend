package com.jetbrains.jetpad.vclang.typechecking.instance.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import java.util.List;

public interface InstanceProvider {
  List<? extends Concrete.Instance> getInstances(ClassReferable classRef);
}
