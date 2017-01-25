package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.error.ReportableRuntimeException;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collection;
import java.util.Set;

public interface Scope {
  Set<String> getNames();
  Abstract.Definition resolveName(String name);

  Collection<? extends Abstract.ClassViewInstance> getInstances();
  Abstract.ClassViewInstance resolveInstance(Abstract.ClassView classView, Abstract.Definition classifyingDefinition);
  Abstract.ClassViewInstance resolveInstance(Abstract.ClassDefinition classDefinition, Abstract.Definition classifyingDefinition);

  abstract class InvalidScopeException extends ReportableRuntimeException {}
}
