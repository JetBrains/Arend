package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface AbstractStatementVisitor<P, R> {
  R visitDefine(Abstract.DefineStatement stat, P params);
  R visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, P params);
}
