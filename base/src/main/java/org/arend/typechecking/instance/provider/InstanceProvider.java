package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.TCDefReferable;

import java.util.List;
import java.util.function.Predicate;

public interface InstanceProvider {
  TCDefReferable findInstance(Predicate<TCDefReferable> pred);
  List<? extends TCDefReferable> geTInstances();

  static boolean compare(InstanceProvider provider1, InstanceProvider provider2) {
    return provider1 == null ? provider2 == null : provider2 != null && provider1.geTInstances().equals(provider2.geTInstances());
  }
}
