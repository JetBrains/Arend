package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public interface ResolveListener {
  void nameResolved(Abstract.ReferenceExpression referenceExpression, Abstract.ReferableSourceNode referable);
  void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Abstract.Definition definition);
  void openCmdResolved(OpenCommand openCmd, Abstract.Definition definition);
  void implementResolved(Abstract.Implementation identifier, Abstract.ClassField definition);
  void implementResolved(Abstract.ClassFieldImpl identifier, Abstract.ClassField definition);
  void classViewResolved(Abstract.ClassView classView, Abstract.ClassField classifyingField);
  void classViewFieldResolved(Abstract.ClassViewField field, Abstract.ClassField definition);
  void classViewInstanceResolved(Abstract.ClassViewInstance instance, Abstract.Definition classifyingDefinition);

  Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Abstract.Definition binOp, Abstract.ReferenceExpression var, Abstract.Expression right);
  Abstract.Expression makeError(Abstract.BinOpSequenceExpression binOpExpr, Abstract.SourceNode node);
  void replaceBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression expression);
  void replaceWithConstructor(List<? extends Abstract.Pattern> patterns, int index, Abstract.Constructor constructor);
  void replaceWithConstructor(Abstract.FunctionClause clause, int index, Abstract.Constructor constructor);
  void patternResolved(Abstract.ConstructorPattern pattern, Abstract.Constructor definition);
}
