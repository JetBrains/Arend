package com.jetbrains.jetpad.vclang.naming.oneshot;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

public interface ResolveListener {
  void nameResolved(Abstract.DefCallExpression defCallExpression, Referable definition);
  void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Referable definition);
  void nsCmdResolved(Abstract.NamespaceCommandStatement nsCmdStatement, Referable definition);
  void superClassResolved(Abstract.SuperClass superClass, Referable definition);
  void idPairFirstResolved(Abstract.IdPair idPair, Referable definition);
  void identifierResolved(Abstract.Identifier identifier, Referable definition);

  Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Referable binOp, Abstract.DefCallExpression var, Abstract.Expression right);
  Abstract.Expression makeError(Abstract.BinOpSequenceExpression binOpExpr, Abstract.SourceNode node);
  void replaceBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression expression);
  void replaceWithConstructor(Abstract.PatternArgument patternArg);
  void replaceWithConstructor(Abstract.PatternContainer container, int index);
}
