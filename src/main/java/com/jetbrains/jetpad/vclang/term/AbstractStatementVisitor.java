package com.jetbrains.jetpad.vclang.term;

public interface AbstractStatementVisitor<P, R> {
  R visitDefine(Abstract.DefineStatement stat, P params);
  R visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, P params);
}
