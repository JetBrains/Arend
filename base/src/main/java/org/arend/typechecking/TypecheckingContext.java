package org.arend.typechecking;

import org.arend.core.context.binding.Binding;
import org.arend.ext.ArendExtension;
import org.arend.extImpl.userData.UserDataHolderImpl;
import org.arend.naming.reference.Referable;
import org.arend.typechecking.instance.pool.GlobalInstancePool;

import java.util.Map;

public class TypecheckingContext {
  public final Map<Referable, Binding> localContext;
  public final GlobalInstancePool instancePool;
  public final ArendExtension arendExtension;
  public final UserDataHolderImpl userDataHolder;
  public final LevelContext levelContext;

  public TypecheckingContext(Map<Referable, Binding> localContext, GlobalInstancePool instancePool, ArendExtension arendExtension, UserDataHolderImpl userDataHolder, LevelContext levelContext) {
    this.localContext = localContext;
    this.instancePool = instancePool;
    this.arendExtension = arendExtension;
    this.userDataHolder = userDataHolder;
    this.levelContext = levelContext;
  }
}
