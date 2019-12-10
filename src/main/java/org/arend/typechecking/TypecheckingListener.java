package org.arend.typechecking;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.Variable;
import org.arend.core.definition.Definition;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TypecheckingListener {
  default void referableTypechecked(@Nonnull Referable referable, @Nonnull Binding binding) { }
  default void referenceTypechecked(@Nonnull Referable referable, @Nonnull Variable variable) { }
  default void typecheckingHeaderStarted(@Nonnull TCReferable definition) { }
  default void typecheckingBodyStarted(@Nonnull TCReferable definition) { }
  default void typecheckingUnitStarted(@Nonnull TCReferable definition) { }
  default void typecheckingHeaderFinished(@Nonnull TCReferable referable, @Nonnull Definition definition) { }
  default void typecheckingBodyFinished(@Nonnull TCReferable referable, @Nonnull Definition definition) { }
  default void typecheckingUnitFinished(@Nonnull TCReferable referable, @Nonnull Definition definition) { }
  default void typecheckingInterrupted(@Nonnull TCReferable definition, @Nullable Definition typechecked) { }

  TypecheckingListener DEFAULT = new TypecheckingListener() {};
}
