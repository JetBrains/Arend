package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.jetbrains.jetpad.vclang.error.ReportableRuntimeException;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.Collection;
import java.util.Set;

public interface Scope {
  Set<String> getNames();
  Referable resolveName(String name);

  Collection<? extends Concrete.ClassViewInstance> getInstances(); // TODO[abstract]: Replace Concrete.ClassViewInstance with something else, idk

  abstract class InvalidScopeException extends ReportableRuntimeException {}
}
