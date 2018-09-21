package org.arend.typechecking.instance.provider;

import org.arend.term.concrete.Concrete;

import java.util.Collections;
import java.util.List;

public class EmptyInstanceProvider implements InstanceProvider {
  private static final EmptyInstanceProvider INSTANCE = new EmptyInstanceProvider();

  private EmptyInstanceProvider() {}

  public static EmptyInstanceProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public List<? extends Concrete.Instance> getInstances() {
    return Collections.emptyList();
  }
}
