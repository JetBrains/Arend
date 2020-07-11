package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.TCReferable;

import java.util.function.Predicate;

public class EmptyInstanceProvider implements InstanceProvider {
  private static final EmptyInstanceProvider INSTANCE = new EmptyInstanceProvider();

  private EmptyInstanceProvider() {}

  public static EmptyInstanceProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public TCReferable findInstance(Predicate<TCReferable> pred) {
    return null;
  }
}
