package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.parser.BinOpParser;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolveListener;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.*;
import com.jetbrains.jetpad.vclang.naming.error.NoSuchFieldError;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;

import java.util.*;

public class ExpressionResolveNameVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final Scope myParentScope;
  private final List<Abstract.ReferableSourceNode> myContext;
  private final NameResolver myNameResolver;
  private final ResolveListener myResolveListener;
  private final ErrorReporter myErrorReporter;

  public ExpressionResolveNameVisitor(Scope parentScope, List<Abstract.ReferableSourceNode> context, NameResolver nameResolver, ResolveListener resolveListener, ErrorReporter errorReporter) {
    myParentScope = parentScope;
    myContext = context;
    myNameResolver = nameResolver;
    myResolveListener = resolveListener;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitReference(Abstract.ReferenceExpression expr, Void params) {
    Abstract.Expression expression = expr.getExpression();
    if (expression != null) {
      expression.accept(this, null);
    }

    if (expr.getReferent() == null) {
      Abstract.ReferableSourceNode ref = null;
      if (expression == null) {
        for (int i = myContext.size() - 1; i >= 0; i--) {
          if (Objects.equals(myContext.get(i).getName(), expr.getName())) {
            ref = myContext.get(i);
            break;
          }
        }
      }

      if (ref == null) {
        try {
          ref = myNameResolver.resolveReference(myParentScope, expr);
        } catch (Scope.InvalidScopeException e) {
          myErrorReporter.report(e.toError());
          return null;
        }
      }

      if (ref != null) {
        myResolveListener.nameResolved(expr, ref);
      } else if (expression == null
              || expression instanceof Abstract.ModuleCallExpression
              || expression instanceof Abstract.ReferenceExpression &&
                (((Abstract.ReferenceExpression) expression).getReferent() instanceof Abstract.ClassDefinition
                || ((Abstract.ReferenceExpression) expression).getReferent() instanceof Abstract.DataDefinition
                || ((Abstract.ReferenceExpression) expression).getReferent() instanceof Abstract.ClassView)) {
        myErrorReporter.report(new NotInScopeError(expr, expr.getName()));
      }
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Abstract.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    if (expr.getModule() == null) {
      Abstract.Definition ref = myNameResolver.resolveModuleCall(myParentScope, expr);
      if (ref != null) {
        myResolveListener.moduleResolved(expr, ref);
      } else {
        myErrorReporter.report(new NotInScopeError(expr, expr.getPath().toString()));
      }
    }
    return null;
  }

  void visitParameters(List<? extends Abstract.Parameter> parameters) {
    for (Abstract.Parameter parameter : parameters) {
      if (parameter instanceof Abstract.TypeParameter) {
        ((Abstract.TypeParameter) parameter).getType().accept(this, null);
      }
      if (parameter instanceof Abstract.TelescopeParameter) {
        for (Abstract.ReferableSourceNode referable : ((Abstract.TelescopeParameter) parameter).getReferableList()) {
          if (referable != null && referable.getName() != null && !referable.getName().equals("_")) {
            for (Abstract.ReferableSourceNode referable1 : ((Abstract.TelescopeParameter) parameter).getReferableList()) {
              if (referable1 == referable) {
                break;
              }
              if (referable1 != null && referable.getName().equals(referable1.getName())) {
                myErrorReporter.report(new DuplicateName(referable1));
              }
            }
            myContext.add(referable);
          }
        }
      } else
      if (parameter instanceof Abstract.NameParameter) {
        Abstract.ReferableSourceNode referable = (Abstract.NameParameter) parameter;
        if (referable.getName() != null && !referable.getName().equals("_")) {
          myContext.add(referable);
        }
      }
    }
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      expr.getBody().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      expr.getCodomain().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Void params) {
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
      visitParameters(expr.getParameters());
    }
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Void params) {
    if (expr.getReferent() == null) {
      Abstract.Definition ref = myParentScope.resolveName(expr.getName());
      if (ref != null) {
        myResolveListener.nameResolved(expr, ref);
      } else {
        myErrorReporter.report(new NotInScopeError(expr, expr.getName()));
      }
    }
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
      BinOpParser parser = new BinOpParser(expr, myResolveListener, myErrorReporter);
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

  void visitClauses(List<? extends Abstract.FunctionClause> clauses) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.FunctionClause clause : clauses) {
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          Abstract.Constructor constructor = visitPattern(clause.getPatterns().get(i), usedNames);
          if (constructor != null) {
            myResolveListener.replaceWithConstructor(clause, i, constructor);
          }
          resolvePattern(clause.getPatterns().get(i));
        }

        if (clause.getExpression() != null)
          clause.getExpression().accept(this, null);
      }
    }
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void params) {
    for (Abstract.Expression expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    visitClauses(expr.getClauses());
    return null;
  }

  Abstract.Constructor visitPattern(Abstract.Pattern pattern, Set<String> usedNames) {
    if (pattern instanceof Abstract.NamePattern) {
      Abstract.NamePattern namePattern = (Abstract.NamePattern) pattern;
      String name = namePattern.getName();
      if (name == null) return null;
      Abstract.Definition ref = myParentScope.resolveName(name);
      if (ref != null) {
        if (ref instanceof Abstract.Constructor) {
          return (Abstract.Constructor) ref;
        } else {
          myErrorReporter.report(new WrongDefinition("Expected a constructor", ref, pattern));
        }
      }
      if (!name.equals("_")) {
        if (!usedNames.add(name)) {
          myErrorReporter.report(new DuplicateName(namePattern));
        }
        myContext.add(namePattern);
      }
      return null;
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      List<? extends Abstract.Pattern> patterns = ((Abstract.ConstructorPattern) pattern).getArguments();
      for (int i = 0; i < patterns.size(); i++) {
        Abstract.Constructor constructor = visitPattern(patterns.get(i), usedNames);
        if (constructor != null) {
          myResolveListener.replaceWithConstructor(patterns, i, constructor);
        }
      }
      if (((Abstract.ConstructorPattern) pattern).getConstructor() != null) {
        String name = ((Abstract.ConstructorPattern) pattern).getConstructorName();
        Abstract.Definition def = myParentScope.resolveName(name);
        if (def instanceof Abstract.Constructor) {
          return (Abstract.Constructor) def;
        }
        myErrorReporter.report(def == null ? new NotInScopeError(pattern, name) : new WrongDefinition("Expected a constructor", def, pattern));
      }
      return null;
    } else if (pattern instanceof Abstract.EmptyPattern) {
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  void resolvePattern(Abstract.Pattern pattern) {
    if (pattern instanceof Abstract.ConstructorPattern) {
      if (((Abstract.ConstructorPattern) pattern).getConstructor() == null) {
        String name = ((Abstract.ConstructorPattern) pattern).getConstructorName();
        Abstract.Definition definition = myParentScope.resolveName(name);
        if (definition instanceof Abstract.Constructor) {
          myResolveListener.patternResolved((Abstract.ConstructorPattern) pattern, (Abstract.Constructor) definition);
        } else {
          if (definition != null) {
            myErrorReporter.report(new WrongDefinition("Expected a constructor", definition, pattern));
          } else {
            myErrorReporter.report(new UnknownConstructor(name, pattern));
          }
        }
      }
      for (Abstract.Pattern patternArg : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        resolvePattern(patternArg);
      }
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

  void visitClassFieldImpls(Collection<? extends Abstract.ClassFieldImpl> classFieldImpls, Abstract.ClassView classView, Abstract.ClassDefinition classDef) {
    for (Abstract.ClassFieldImpl statement : classFieldImpls) {
      String name = statement.getImplementedFieldName();
      Abstract.ClassField resolvedRef = classView != null ? myNameResolver.resolveClassFieldByView(classView, name) : classDef != null ? myNameResolver.resolveClassField(classDef, name) : null;
      if (resolvedRef != null) {
        myResolveListener.implementResolved(statement, resolvedRef);
      } else {
        myErrorReporter.report(new NoSuchFieldError(statement, name));
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
        try (Utils.ContextSaver ignored1 = new Utils.ContextSaver(myContext)) {
          visitParameters(clause.getParameters());

          if (clause.getResultType() != null) {
            clause.getResultType().accept(this, null);
          }
          clause.getTerm().accept(this, null);
        }
        if (clause.getName() != null && !clause.getName().equals("_")) {
          myContext.add(clause);
        }
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
