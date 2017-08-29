package com.jetbrains.jetpad.vclang.typechecking.typeclass.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.HashMap;
import java.util.Map;

public class InstanceProviderSet {
  private final Map<GlobalReferable, InstanceProvider> myProviders = new HashMap<>();

  public void setProvider(GlobalReferable referable, InstanceProvider provider) {
    myProviders.put(referable, provider);
  }

  public InstanceProvider getInstanceProvider(GlobalReferable referable) {
    return myProviders.get(referable);
  }
}
