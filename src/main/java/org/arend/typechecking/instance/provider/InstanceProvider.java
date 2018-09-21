package org.arend.typechecking.instance.provider;

import org.arend.term.concrete.Concrete;

import java.util.List;

public interface InstanceProvider {
  List<? extends Concrete.Instance> getInstances();
}
