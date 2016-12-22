package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolveListener;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.frontend.parser.BinOpParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

public class ExpressionResolveNameVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final Scope myParentScope;
  private final List<String> myContext;
  private final NameResolver myNameResolver;
  private final ResolveListener myResolveListener;
  private final ErrorReporter myErrorReporter;

  public ExpressionResolveNameVisitor(Scope parentScope, List<String> context, NameResolver nameResolver, ErrorReporter errorReporter, ResolveListener resolveListener) {
    myParentScope = parentScope;
    myContext = context;
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Void params) {
    Abstract.Expression expression = expr.getExpression();
    if (expression != null) {
      expression.accept(this, null);
    }

    if (expr.getReferent() == null) {
      if (expression != null || !myContext.contains(expr.getName())) {
        Abstract.Definition ref = myNameResolver.resolveDefCall(myParentScope, expr);
        if (ref != null) {
          myResolveListener.nameResolved(expr, ref);
        } else
        if (expression == null
            || expression instanceof Abstract.ModuleCallExpression
            || expression instanceof Abstract.DefCallExpression &&
              (((Abstract.DefCallExpression) expression).getReferent() instanceof Abstract.ClassDefinition
              || ((Abstract.DefCallExpression) expression).getReferent() instanceof Abstract.DataDefinition
              || ((Abstract.DefCallExpression) expression).getReferent() instanceof Abstract.ClassView)) {
          myErrorReporter.report(new NotInScopeError(expr, expr.getName()));
        }
      }
    }
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    if (expr.getModule() == null) {
      Abstract.Definition ref = myNameResolver.resolveModuleCall(myParentScope, expr);
      if (ref != null) {
        myResolveListener.moduleResolved(expr, ref);
      }
    }
    return null;
  }

  private void visitArguments(List<? extends Abstract.Argument> arguments) {
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) argument).getType().accept(this, null);
      }
      if (argument instanceof Abstract.TelescopeArgument) {
        myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
      } else
      if (argument instanceof Abstract.NameArgument) {
        myContext.add(((Abstract.NameArgument) argument).getName());
      }
    }
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitArguments(expr.getArguments());
      expr.getBody().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitArguments(expr.getArguments());
      expr.getCodomain().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitPolyUniverse(Abstract.PolyUniverseExpression expr, Void params) {
    if (expr.getPLevel() != null) {
      expr.getPLevel().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Void params) {
    Abstract.Expression expression = expr.getExpr();
    if (expression != null) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Void params) {
    for (Abstract.Expression expression : expr.getFields()) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitArguments(expr.getArguments());
    }
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Void params) {
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void params) {
    if (expr.getSequence().isEmpty()) {
      Abstract.Expression left = expr.getLeft();
      left.accept(this, null);
      myResolveListener.replaceBinOp(expr, left);
    } else {
      BinOpParser parser = new BinOpParser(myErrorReporter, expr, myResolveListener);
      List<Abstract.BinOpSequenceElem> sequence = expr.getSequence();
      List<BinOpParser.StackElem> stack = new ArrayList<>(sequence.size());
      Abstract.Expression expression = expr.getLeft();
      expression.accept(this, null);
      NotInScopeError error = null;
      for (Abstract.BinOpSequenceElem elem : sequence) {
        String name = elem.binOp.getName();
        Abstract.Definition ref = myParentScope.resolveName(name);
        if (ref != null) {
          parser.pushOnStack(stack, expression, ref, ref.getPrecedence(), elem.binOp);
          expression = elem.argument;
          expression.accept(this, null);
        } else {
          error = new NotInScopeError(elem.binOp, name);
          myErrorReporter.report(error);
        }
      }
      if (error == null) {
        myResolveListener.replaceBinOp(expr, parser.rollUpStack(stack, expression));
      } else {
        myResolveListener.replaceBinOp(expr, myResolveListener.makeError(expr, error.getCause()));
      }
    }
    return null;
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Void params) {
    visitElimCase(expr);
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void params) {
    visitElimCase(expr);
    return null;
  }

  private void visitElimCase(Abstract.ElimCaseExpression expr) {
    for (Abstract.Expression expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Clause clause : expr.getClauses()) {
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          if (visitPattern(clause.getPatterns().get(i))) {
            myResolveListener.replaceWithConstructor(clause, i);
          }
        }

        if (clause.getExpression() != null)
          clause.getExpression().accept(this, null);
      }
    }
  }

  public boolean visitPattern(Abstract.Pattern pattern) {
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      if (name == null) return false;
      Abstract.Definition ref = myParentScope.resolveName(name);
      if (ref != null && (ref instanceof Constructor || ref instanceof Abstract.Constructor)) {
        return true;
      } else {
        myContext.add(name);
        return false;
      }
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      for (Abstract.PatternArgument patternArg : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        if (visitPattern(patternArg.getPattern())) {
          myResolveListener.replaceWithConstructor(patternArg);
        }
      }
      return false;
    } else if (pattern instanceof Abstract.AnyConstructorPattern) {
      return false;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Void params) {
    expr.getBaseClassExpression().accept(this, null);

    Abstract.ClassView classView = Abstract.getUnderlyingClassView(expr);
    Abstract.ClassDefinition classDef = Abstract.getUnderlyingClassDef(expr);
    for (Abstract.ClassFieldImpl statement : expr.getStatements()) {
      Abstract.Definition resolvedRef = classView != null ? myNameResolver.resolveClassFieldByView(classView, statement.getImplementedFieldName(), myErrorReporter, statement) : classDef != null ? myNameResolver.resolveClassField(classDef, statement.getImplementedFieldName(), myErrorReporter, statement) : null;
      if (resolvedRef != null) {
        myResolveListener.implementResolved(statement, resolvedRef);
      }
      statement.getImplementation().accept(this, null);
    }

    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Abstract.LetExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.LetClause clause : expr.getClauses()) {
        visitArguments(clause.getArguments());

        if (clause.getResultType() != null) {
          clause.getResultType().accept(this, null);
        }
        clause.getTerm().accept(this, null);
        myContext.add(clause.getName());
      }

      expr.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitNumericLiteral(Abstract.NumericLiteral expr, Void params) {
    return null;
  }
}
