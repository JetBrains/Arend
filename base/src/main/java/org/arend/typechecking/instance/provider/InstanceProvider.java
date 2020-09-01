package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.TCDefReferable;

import java.util.function.Predicate;

public interface InstanceProvider {
  TCDefReferable findInstance(Predicate<TCDefReferable> pred);
}
