package com.jetbrains.jetpad.vclang.term.legacy;

public interface LegacyAbstractStatementVisitor<P, R> {
  R visitDefine(LegacyAbstract.DefineStatement stat, P params);
  R visitNamespaceCommand(LegacyAbstract.NamespaceCommandStatement stat, P params);
}
