package com.jetbrains.jetpad.vclang.naming.oneshot;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ResolveListener {
  void nameResolved(Abstract.DefCallExpression defCallExpression, Abstract.Definition definition);
  void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Abstract.Definition definition);
  void nsCmdResolved(Abstract.NamespaceCommandStatement nsCmdStatement, Abstract.Definition definition);
  void implementResolved(Abstract.ImplementDefinition identifier, Abstract.Definition definition);
  void implementResolved(Abstract.ImplementStatement identifier, Abstract.Definition definition);
  void classViewResolved(Abstract.ClassView classView, Abstract.ClassField classifyingField);
  void classViewFieldResolved(Abstract.ClassViewField field, Abstract.ClassField definition);

  Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Abstract.Definition binOp, Abstract.DefCallExpression var, Abstract.Expression right);
  Abstract.Expression makeError(Abstract.BinOpSequenceExpression binOpExpr, Abstract.SourceNode node);
  void replaceBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression expression);
  void replaceWithConstructor(Abstract.PatternArgument patternArg);
  void replaceWithConstructor(Abstract.PatternContainer container, int index);
}
