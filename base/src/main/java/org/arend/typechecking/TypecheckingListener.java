package org.arend.typechecking;

import org.arend.core.context.binding.Binding;
import org.arend.naming.reference.Referable;

public interface TypecheckingListener {
  default void referableTypechecked(Referable referable, Binding binding) {

  }

  TypecheckingListener DEFAULT = new TypecheckingListener() {};
}
