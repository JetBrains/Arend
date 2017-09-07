package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.BinOpParser;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.error.NoSuchFieldError;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.LocalScope;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.Precedence;

import java.util.*;

public class ExpressionResolveNameVisitor<T> implements ConcreteExpressionVisitor<T, Void, Void> {
  private final Scope myParentScope;
  private final Scope myScope;
  private final List<Referable> myContext;
  private final NameResolver myNameResolver;
  private final ErrorReporter<T> myErrorReporter;

  public ExpressionResolveNameVisitor(Scope parentScope, List<Referable> context, NameResolver nameResolver, ErrorReporter<T> errorReporter) {
    myParentScope = parentScope;
    myScope = context == null ? parentScope : new MergeScope(new LocalScope(context), parentScope);
    myContext = context;
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitApp(Concrete.AppExpression<T> expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression<T> expr, Void params) {
    Concrete.Expression<T> left = expr.getExpression();
    if (left != null) {
      left.accept(this, null);
      Referable referable = expr.getReferent();
      if (referable instanceof UnresolvedReference) {
        GlobalReferable globalRef = null;

        if (left instanceof Concrete.ModuleCallExpression) {
          globalRef = ((Concrete.ModuleCallExpression) left).getModule();
        } else if (left instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) left).getExpression() == null && ((Concrete.ReferenceExpression) left).getReferent() instanceof GlobalReferable) {
          globalRef = (GlobalReferable) ((Concrete.ReferenceExpression) left).getReferent();
        } else {
          // myErrorReporter.report(new NotInScopeError<>(name, expr));
        }

        if (globalRef != null) {
          Referable newRef = myNameResolver.nsProviders.statics.forReferable(globalRef).resolveName(referable.textRepresentation()); // TODO[abstract]: Resolve correctly
          if (newRef != null) {
            expr.setExpression(null);
            expr.setReferent(newRef);
          } else {
            myErrorReporter.report(new NotInScopeError<>(referable.textRepresentation(), expr)); // TODO[abstract]: report another error
          }
        }
      }
    } else {
      Referable referable = resolveReferable(expr.getReferent(), expr);
      if (referable != null) {
        expr.setReferent(referable);
      }
    }
    return null;
  }

  private Referable resolveReferable(Referable referable, Concrete.SourceNode<T> sourceNode) {
    if (!(referable instanceof UnresolvedReference)) {
      return null;
    }

    referable = ((UnresolvedReference) referable).resolve(myScope, myNameResolver);

    if (referable instanceof UnresolvedReference) {
      myErrorReporter.report(new NotInScopeError<>(referable.textRepresentation(), sourceNode));
      return null;
    } else {
      return referable;
    }
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitModuleCall(Concrete.ModuleCallExpression<T> expr, Void params) {
    if (expr.getModule() == null) {
      GlobalReferable ref = myNameResolver.resolveModuleCall(myParentScope, expr);
      if (ref != null) {
        expr.setModule(ref);
      } else {
        myErrorReporter.report(new NotInScopeError<>(expr.getPath().toString(), expr));
      }
    }
    return null;
  }

  void visitParameters(List<? extends Concrete.Parameter<T>> parameters) {
    for (Concrete.Parameter<T> parameter : parameters) {
      if (parameter instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter<T>) parameter).getType().accept(this, null);
      }
      if (parameter instanceof Concrete.TelescopeParameter) {
        List<? extends Referable> referableList = ((Concrete.TelescopeParameter<T>) parameter).getReferableList();
        for (int i = 0; i < referableList.size(); i++) {
          Referable referable = referableList.get(i);
          if (referable != null && !referable.textRepresentation().equals("_")) {
            for (int j = 0; j < i; j++) {
              Referable referable1 = referableList.get(j);
              if (referable1 != null && referable.textRepresentation().equals(referable1.textRepresentation())) {
                myErrorReporter.report(new DuplicateNameError<>(Error.Level.WARNING, referable1, referable));
              }
            }
            myContext.add(referable);
          }
        }
      } else
      if (parameter instanceof Concrete.NameParameter) {
        Referable referable = ((Concrete.NameParameter) parameter).getReferable();
        if (referable != null && !referable.textRepresentation().equals("_")) {
          myContext.add(referable);
        }
      }
    }
  }

  @Override
  public Void visitLam(Concrete.LamExpression<T> expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      expr.getBody().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitPi(Concrete.PiExpression<T> expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      expr.getCodomain().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitInferHole(Concrete.InferHoleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression<T> expr, Void params) {
    Concrete.Expression<T> expression = expr.getExpression();
    if (expression != null) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression<T> expr, Void params) {
    for (Concrete.Expression<T> expression : expr.getFields()) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression<T> expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
    }
    return null;
  }

  @Override
  public Void visitBinOp(Concrete.BinOpExpression<T> expr, Void params) {
    expr.getLeft().accept(this, null);
    if (expr.getRight() != null) {
      expr.getRight().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitBinOpSequence(Concrete.BinOpSequenceExpression<T> expr, Void params) {
    if (expr.getSequence().isEmpty()) {
      Concrete.Expression<T> left = expr.getLeft();
      left.accept(this, null);
      expr.replace(left);
    } else {
      BinOpParser<T> parser = new BinOpParser<>(expr, myErrorReporter);
      List<Concrete.BinOpSequenceElem<T>> sequence = expr.getSequence();

      expr.getLeft().accept(this, null);
      for (Concrete.BinOpSequenceElem<T> elem : sequence) {
        if (elem.argument != null) {
          elem.argument.accept(this, null);
        }
      }

      T errorCause = null;
      Concrete.Expression<T> expression = expr.getLeft();
      List<BinOpParser.StackElem<T>> stack = new ArrayList<>(sequence.size());
      for (Concrete.BinOpSequenceElem<T> elem : expr.getSequence()) {
        visitReference(elem.binOp, null);
        Referable ref = elem.binOp.getReferent();
        if (ref instanceof UnresolvedReference) {
          errorCause = elem.binOp.getData();
        } else {
          parser.pushOnStack(stack, expression, ref, ref instanceof GlobalReferable ? ((GlobalReferable) ref).getPrecedence() : Precedence.DEFAULT, elem.binOp, elem.argument == null);
          expression = elem.argument;
        }
      }

      if (errorCause == null) {
        expr.replace(parser.rollUpStack(stack, expression));
      } else {
        expr.replace(new Concrete.InferHoleExpression<>(errorCause));
      }
    }
    return null;
  }

  static <T> void replaceWithConstructor(Concrete.PatternContainer<T> container, int index, Referable constructor) {
    Concrete.Pattern<T> old = container.getPatterns().get(index);
    Concrete.Pattern<T> newPattern = new Concrete.ConstructorPattern<>(old.getData(), constructor, Collections.emptyList());
    newPattern.setExplicit(old.isExplicit());
    container.getPatterns().set(index, newPattern);
  }

  void visitClauses(List<? extends Concrete.FunctionClause<T>> clauses) {
    if (clauses.isEmpty()) {
      return;
    }
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Concrete.FunctionClause<T> clause : clauses) {
        Map<String, Concrete.NamePattern> usedNames = new HashMap<>();
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          Referable constructor = visitPattern(clause.getPatterns().get(i), usedNames);
          if (constructor != null) {
            replaceWithConstructor(clause, i, constructor);
          }
          resolvePattern(clause.getPatterns().get(i));
        }

        if (clause.getExpression() != null)
          clause.getExpression().accept(this, null);
      }
    }
  }

  @Override
  public Void visitCase(Concrete.CaseExpression<T> expr, Void params) {
    for (Concrete.Expression<T> expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    visitClauses(expr.getClauses());
    return null;
  }

  GlobalReferable visitPattern(Concrete.Pattern<T> pattern, Map<String, Concrete.NamePattern> usedNames) {
    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      Referable referable = namePattern.getReferable();
      String name = referable == null ? null : referable.textRepresentation();
      if (name == null) return null;
      Referable ref = myParentScope.resolveName(name);
      if (ref instanceof GlobalReferable) {
        return (GlobalReferable) ref;
      }
      if (!name.equals("_")) {
        Concrete.NamePattern prev = usedNames.put(name, namePattern);
        if (prev != null) {
          myErrorReporter.report(new DuplicateNameError<>(Error.Level.WARNING, referable, prev.getReferable()));
        }
        myContext.add(referable);
      }
      return null;
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      List<? extends Concrete.Pattern<T>> patterns = ((Concrete.ConstructorPattern<T>) pattern).getPatterns();
      for (int i = 0; i < patterns.size(); i++) {
        Referable constructor = visitPattern(patterns.get(i), usedNames);
        if (constructor != null) {
          replaceWithConstructor((Concrete.ConstructorPattern<T>) pattern, i, constructor);
        }
      }
      return null;
    } else if (pattern instanceof Concrete.EmptyPattern) {
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  void resolvePattern(Concrete.Pattern<T> pattern) {
    if (!(pattern instanceof Concrete.ConstructorPattern)) {
      return;
    }

    Referable referable = ((Concrete.ConstructorPattern<T>) pattern).getConstructor();
    if (referable instanceof UnresolvedReference) {
      Referable newRef = ((UnresolvedReference) referable).resolve(myParentScope, myNameResolver);
      if (newRef instanceof UnresolvedReference) {
        myErrorReporter.report(new NotInScopeError<>(referable.textRepresentation(), pattern));
      } else {
        ((Concrete.ConstructorPattern<T>) pattern).setConstructor(newRef);
      }
    }

    for (Concrete.Pattern<T> patternArg : ((Concrete.ConstructorPattern<T>) pattern).getPatterns()) {
      resolvePattern(patternArg);
    }
  }

  @Override
  public Void visitProj(Concrete.ProjExpression<T> expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression<T> expr, Void params) {
    expr.getBaseClassExpression().accept(this, null);
    GlobalReferable classDef = Concrete.getUnderlyingClassDef(expr);
    if (classDef != null) {
      visitClassFieldImpls(expr.getStatements(), classDef);
    }
    return null;
  }

  void visitClassFieldImpls(Collection<? extends Concrete.ClassFieldImpl<T>> classFieldImpls, GlobalReferable classDef) {
    for (Concrete.ClassFieldImpl<T> impl : classFieldImpls) {
      Referable field = impl.getImplementedField();
      if (field instanceof UnresolvedReference) { // TODO[abstract]: Rewrite this using resolve method of UnresolvedReference
        GlobalReferable resolvedRef = myNameResolver.nsProviders.dynamics.forReferable(classDef).resolveName(field.textRepresentation());
        if (resolvedRef != null) {
          impl.setImplementedField(resolvedRef);
        } else {
          myErrorReporter.report(new NoSuchFieldError<>(field.textRepresentation(), impl));
        }
      }
      impl.getImplementation().accept(this, null);
    }
  }

  @Override
  public Void visitNew(Concrete.NewExpression<T> expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Concrete.LetExpression<T> expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Concrete.LetClause<T> clause : expr.getClauses()) {
        try (Utils.ContextSaver ignored1 = new Utils.ContextSaver(myContext)) {
          visitParameters(clause.getParameters());

          if (clause.getResultType() != null) {
            clause.getResultType().accept(this, null);
          }
          clause.getTerm().accept(this, null);
        }
        myContext.add(clause.getReferable());
      }

      expr.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return null;
  }
}
