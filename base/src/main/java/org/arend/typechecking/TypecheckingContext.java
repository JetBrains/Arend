package org.arend.typechecking;

import org.arend.core.context.binding.Binding;
import org.arend.ext.ArendExtension;
import org.arend.naming.reference.Referable;
import org.arend.typechecking.instance.pool.InstancePool;
import org.arend.typechecking.instance.provider.InstanceProvider;

import java.util.Map;
import java.util.Set;

public class TypecheckingContext {
  public final Map<Referable, Binding> localContext;
  public final InstanceProvider instanceProvider;
  public final InstancePool localInstancePool;
  public final ArendExtension arendExtension;

  public TypecheckingContext(Map<Referable, Binding> localContext, InstanceProvider instanceProvider, InstancePool localInstancePool, ArendExtension arendExtension) {
    this.localContext = localContext;
    this.instanceProvider = instanceProvider;
    this.localInstancePool = localInstancePool;
    this.arendExtension = arendExtension;
  }
}
