package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.frontend.parser.BinOpParser;
import com.jetbrains.jetpad.vclang.frontend.resolving.NamespaceProviders;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolveListener;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExpressionResolveNameVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final NamespaceProviders myNsProviders;
  private final Scope myParentScope;
  private final List<String> myContext;
  private final NameResolver myNameResolver;
  private final ResolveListener myResolveListener;

  public ExpressionResolveNameVisitor(NamespaceProviders namespaceProviders, Scope parentScope, List<String> context, NameResolver nameResolver, ResolveListener resolveListener) {
    myNsProviders = namespaceProviders;
    myParentScope = parentScope;
    myContext = context;
    myNameResolver = nameResolver;
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
        Abstract.Definition ref = myNameResolver.resolveDefCall(myParentScope, expr, myNsProviders.modules, myNsProviders.statics);
        if (ref != null) {
          myResolveListener.nameResolved(expr, ref);
        } else
        if (expression == null
            || expression instanceof Abstract.ModuleCallExpression
            || expression instanceof Abstract.DefCallExpression &&
              (((Abstract.DefCallExpression) expression).getReferent() instanceof Abstract.ClassDefinition
              || ((Abstract.DefCallExpression) expression).getReferent() instanceof Abstract.DataDefinition
              || ((Abstract.DefCallExpression) expression).getReferent() instanceof Abstract.ClassView)) {
          myResolveListener.report(new NotInScopeError(expr, expr.getName()));
        }
      }
    }
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    if (expr.getModule() == null) {
      Abstract.Definition ref = myNameResolver.resolveModuleCall(myParentScope, expr, myNsProviders.modules);
      if (ref != null) {
        myResolveListener.moduleResolved(expr, ref);
      }
    }
    return null;
  }

  public void visitArguments(List<? extends Abstract.Argument> arguments) {
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
  public Void visitLvl(Abstract.LvlExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitLP(Abstract.LPExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitLH(Abstract.LHExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitPolyUniverse(Abstract.PolyUniverseExpression expr, Void params) {
    if (expr.getPLevel() != null) {
      for (Abstract.Expression maxArg : expr.getPLevel()) {
        maxArg.accept(this, null);
      }
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
      BinOpParser parser = new BinOpParser(myResolveListener, expr, myResolveListener);
      List<Abstract.BinOpSequenceElem> sequence = expr.getSequence();

      expr.getLeft().accept(this, null);
      for (Abstract.BinOpSequenceElem elem : sequence) {
        elem.argument.accept(this, null);
      }

      NotInScopeError error = null;
      Abstract.Expression expression = expr.getLeft();
      List<BinOpParser.StackElem> stack = new ArrayList<>(sequence.size());
      for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
        String name = elem.binOp.getName();
        Abstract.Definition ref = myParentScope.resolveName(name);
        if (ref != null) {
          parser.pushOnStack(stack, expression, ref, ref.getPrecedence(), elem.binOp);
          expression = elem.argument;
        } else {
          error = new NotInScopeError(elem.binOp, name);
          myResolveListener.report(error);
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
    Abstract.ClassDefinition classDef = classView == null ? Abstract.getUnderlyingClassDef(expr) : null;
    visitClassFieldImpls(expr.getStatements(), classView, classDef);
    return null;
  }

  public void visitClassFieldImpls(Collection<? extends Abstract.ClassFieldImpl> classFieldImpls, Abstract.ClassView classView, Abstract.ClassDefinition classDef) {
    for (Abstract.ClassFieldImpl statement : classFieldImpls) {
      Abstract.ClassField resolvedRef = classView != null ? myNameResolver.resolveClassFieldByView(classView, statement.getImplementedFieldName(), myResolveListener, statement) : classDef != null ? myNameResolver.resolveClassField(classDef, statement.getImplementedFieldName(), myNsProviders.dynamics, myResolveListener, statement) : null;
      if (resolvedRef != null) {
        myResolveListener.implementResolved(statement, resolvedRef);
      }
      statement.getImplementation().accept(this, null);
    }
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
