package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.TCReferable;

import java.util.function.Predicate;

public interface InstanceProvider {
  TCReferable findInstance(Predicate<TCReferable> pred);
}
