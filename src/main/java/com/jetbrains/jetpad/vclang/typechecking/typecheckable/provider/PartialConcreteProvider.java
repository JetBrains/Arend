package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Reference;
import com.jetbrains.jetpad.vclang.util.Pair;

import javax.annotation.Nullable;
import java.util.Collection;

public interface PartialConcreteProvider {
  @Nullable Reference getInstanceClassReference(GlobalReferable instance);
  boolean isRecord(ClassReferable classRef);
  boolean isInstance(GlobalReferable ref);
  @Nullable Collection<InstanceParameter> getInstanceParameterReferences(GlobalReferable ref);

  class InstanceParameter {
    public boolean isExplicit;
    public GlobalReferable referable;
    public Object data;

    public InstanceParameter(boolean isExplicit, GlobalReferable referable, Object data) {
      this.isExplicit = isExplicit;
      this.referable = referable;
      this.data = data;
    }
  }
}
