package com.jetbrains.jetpad.vclang.frontend.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.text.parser.BinOpParser;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.*;
import com.jetbrains.jetpad.vclang.naming.error.NoSuchFieldError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.provider.ParserInfoProvider;

import java.util.*;

public class ExpressionResolveNameVisitor<T> implements ConcreteExpressionVisitor<T, Void, Void> {
  private final Scope myParentScope;
  private final List<Referable> myContext;
  private final NameResolver myNameResolver;
  private final ParserInfoProvider myInfoProvider;
  private final ErrorReporter<T> myErrorReporter;

  public ExpressionResolveNameVisitor(Scope parentScope, List<Referable> context, NameResolver nameResolver, ParserInfoProvider infoProvider, ErrorReporter<T> errorReporter) {
    myParentScope = parentScope;
    myContext = context;
    myNameResolver = nameResolver;
    myInfoProvider = infoProvider;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitApp(Concrete.AppExpression<T> expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  private Referable resolveLocal(String name) {
    for (int i = myContext.size() - 1; i >= 0; i--) {
      if (Objects.equals(myContext.get(i).getName(), name)) {
        return myContext.get(i);
      }
    }
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression<T> expr, Void params) {
    Concrete.Expression<T> expression = expr.getExpression();
    if (expression != null) {
      expression.accept(this, null);
    }

    if (expr.getReferent() == null) {
      Referable ref = null;
      if (expression == null) {
        ref = resolveLocal(expr.getName());
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
        expr.setResolvedReferent(ref);
      } else if (expression == null
              || expression instanceof Concrete.ModuleCallExpression
              || expression instanceof Concrete.ReferenceExpression &&
                (((Concrete.ReferenceExpression) expression).getReferent() instanceof Concrete.ClassDefinition
                || ((Concrete.ReferenceExpression) expression).getReferent() instanceof Concrete.DataDefinition
                || ((Concrete.ReferenceExpression) expression).getReferent() instanceof Concrete.ClassView)) {
        myErrorReporter.report(new NotInScopeError<>(expr.getName(), expr));
      }
    }
    return null;
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
          if (referable != null && referable.getName() != null && !referable.getName().equals("_")) {
            for (int j = 0; j < i; j++) {
              Referable referable1 = referableList.get(j);
              if (referable1 != null && referable.getName().equals(referable1.getName())) {
                myErrorReporter.report(new DuplicateNameError<>(Error.Level.WARNING, referable1, referable, parameter));
              }
            }
            myContext.add(referable);
          }
        }
      } else
      if (parameter instanceof Concrete.NameParameter) {
        Referable referable = (Concrete.NameParameter) parameter;
        if (referable.getName() != null && !referable.getName().equals("_")) {
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

      NotInScopeError<T> error = null;
      Concrete.Expression<T> expression = expr.getLeft();
      List<BinOpParser.StackElem<T>> stack = new ArrayList<>(sequence.size());
      for (Concrete.BinOpSequenceElem<T> elem : expr.getSequence()) {
        String name = elem.binOp.getName();
        Referable ref = resolveLocal(name);
        if (ref == null) {
          ref = myParentScope.resolveName(name);
        }
        if (ref != null) {
          parser.pushOnStack(stack, expression, ref, ref instanceof GlobalReferable ? myInfoProvider.precedenceOf((GlobalReferable) ref) : Abstract.Precedence.DEFAULT, elem.binOp, elem.argument == null);
          expression = elem.argument;
        } else {
          error = new NotInScopeError<>(name, elem.binOp);
          myErrorReporter.report(error);
        }
      }
      if (error == null) {
        expr.replace(parser.rollUpStack(stack, expression));
      } else {
        expr.replace(new Concrete.InferHoleExpression<>(error.getCause()));
      }
    }
    return null;
  }

  static <T> void replaceWithConstructor(Concrete.PatternContainer<T> container, int index, Concrete.Constructor<T> constructor) {
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
          Concrete.Constructor<T> constructor = visitPattern(clause.getPatterns().get(i), usedNames);
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

  Concrete.Constructor<T> visitPattern(Concrete.Pattern<T> pattern, Map<String, Concrete.NamePattern> usedNames) { // TODO[abstract]: return referable, not a constructor
    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      String name = namePattern.getName();
      if (name == null) return null;
      Referable ref = myParentScope.resolveName(name);
      if (ref != null) {
        if (ref instanceof Concrete.Constructor) {
          return (Concrete.Constructor<T>) ref;
        } else {
          myErrorReporter.report(new WrongReferable<>("Expected a constructor", ref, pattern));
        }
      }
      if (!name.equals("_")) {
        Concrete.NamePattern prev = usedNames.put(name, namePattern);
        if (prev != null) {
          myErrorReporter.report(new DuplicateNameError<>(Error.Level.WARNING, namePattern, prev, pattern));
        }
        myContext.add(namePattern);
      }
      return null;
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      List<? extends Concrete.Pattern<T>> patterns = ((Concrete.ConstructorPattern<T>) pattern).getPatterns();
      for (int i = 0; i < patterns.size(); i++) {
        Concrete.Constructor<T> constructor = visitPattern(patterns.get(i), usedNames);
        if (constructor != null) {
          replaceWithConstructor((Concrete.ConstructorPattern<T>) pattern, i, constructor);
        }
      }
      if (((Concrete.ConstructorPattern) pattern).getConstructor() != null) {
        String name = ((Concrete.ConstructorPattern) pattern).getConstructorName();
        Referable def = myParentScope.resolveName(name);
        if (def instanceof Concrete.Constructor) {
          return (Concrete.Constructor<T>) def;
        }
        myErrorReporter.report(def == null ? new NotInScopeError<>(name, pattern) : new WrongReferable<>("Expected a constructor", def, pattern));
      }
      return null;
    } else if (pattern instanceof Concrete.EmptyPattern) {
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  void resolvePattern(Concrete.Pattern<T> pattern) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      if (((Concrete.ConstructorPattern<T>) pattern).getConstructor() == null) {
        String name = ((Concrete.ConstructorPattern<T>) pattern).getConstructorName();
        Referable definition = myParentScope.resolveName(name);
        if (definition instanceof Concrete.Constructor) {
          ((Concrete.ConstructorPattern<T>) pattern).setConstructor((Concrete.Constructor) definition);
        } else {
          if (definition != null) {
            myErrorReporter.report(new WrongReferable<>("Expected a constructor", definition, pattern));
          } else {
            myErrorReporter.report(new UnknownConstructor<>(name, pattern));
          }
        }
      }
      for (Concrete.Pattern<T> patternArg : ((Concrete.ConstructorPattern<T>) pattern).getPatterns()) {
        resolvePattern(patternArg);
      }
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
    Concrete.ClassView classView = Concrete.getUnderlyingClassView(expr);
    Concrete.ClassDefinition classDef = classView == null ? Concrete.getUnderlyingClassDef(expr) : null;
    visitClassFieldImpls(expr.getStatements(), classView, classDef);
    return null;
  }

  void visitClassFieldImpls(Collection<? extends Concrete.ClassFieldImpl<T>> classFieldImpls, Concrete.ClassView classView, GlobalReferable classDef) {
    for (Concrete.ClassFieldImpl<T> statement : classFieldImpls) {
      String name = statement.getImplementedFieldName();
      Concrete.ClassField resolvedRef = classView != null ? myNameResolver.resolveClassFieldByView(classView, name) : classDef != null ? myNameResolver.resolveClassField(classDef, name) : null;
      if (resolvedRef != null) {
        statement.setImplementedField(resolvedRef);
      } else {
        myErrorReporter.report(new NoSuchFieldError<>(name, statement));
      }

      statement.getImplementation().accept(this, null);
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
        if (clause.getName() != null && !clause.getName().equals("_")) {
          myContext.add(clause);
        }
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
