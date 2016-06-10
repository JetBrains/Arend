package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.ReportableRuntimeException;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

public interface Namespace extends Scope {
  abstract class InvalidNamespaceException extends ReportableRuntimeException {}
}
