package com.jetbrains.jetpad.vclang.naming.resolving.visitor;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.frontend.reference.TypeClassReferenceExtractVisitor;
import com.jetbrains.jetpad.vclang.naming.BinOpParser;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateNameError;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.scope.ClassFieldImplScope;
import com.jetbrains.jetpad.vclang.naming.scope.ListScope;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class ExpressionResolveNameVisitor implements ConcreteExpressionVisitor<Void, Concrete.Expression> {
  private final TypeClassReferenceExtractVisitor myTypeClassReferenceExtractVisitor;
  private final Scope myParentScope;
  private final Scope myScope;
  private final List<Referable> myContext;
  private final LocalErrorReporter myErrorReporter;

  public ExpressionResolveNameVisitor(ConcreteProvider concreteProvider, Scope parentScope, List<Referable> context, LocalErrorReporter errorReporter) {
    myTypeClassReferenceExtractVisitor = new TypeClassReferenceExtractVisitor(concreteProvider);
    myParentScope = parentScope;
    myScope = context == null ? parentScope : new MergeScope(new ListScope(context), parentScope);
    myContext = context;
    myErrorReporter = errorReporter;
  }

  Scope getScope() {
    return myScope;
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    expr.function = expr.function.accept(this, null);
    expr.argument.expression = expr.argument.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable referable = expr.getReferent();
    while (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }

    Concrete.Expression argument = null;
    if (referable instanceof UnresolvedReference) {
      argument = ((UnresolvedReference) referable).resolveArgument(myScope);
      referable = ((UnresolvedReference) referable).resolve(myScope);
      if (referable instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) referable).getError());
      }
      while (referable instanceof RedirectingReferable) {
        referable = ((RedirectingReferable) referable).getOriginalReferable();
      }
    }

    expr.setReferent(referable);
    return argument == null ? expr : new Concrete.AppExpression(expr.getData(), expr, new Concrete.Argument(argument, false));
  }

  @Override
  public Concrete.Expression visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return expr;
  }

  void visitParameters(List<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      visitParameter(parameter);
    }
  }

  void updateScope(Collection<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TelescopeParameter) {
        ClassReferable classRef = getTypeClassReference(((Concrete.TelescopeParameter) parameter).getType());
        for (Referable referable : ((Concrete.TelescopeParameter) parameter).getReferableList()) {
          if (referable != null && !referable.textRepresentation().equals("_")) {
            myContext.add(classRef == null ? referable : new TypedRedirectingReferable(referable, classRef));
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

  private ClassReferable getTypeClassReference(Concrete.Expression type) {
    return myTypeClassReferenceExtractVisitor.getTypeClassReference(Collections.emptyList(), type);
  }

  void visitParameter(Concrete.Parameter parameter) {
    if (parameter instanceof Concrete.TypeParameter) {
      ((Concrete.TypeParameter) parameter).type = ((Concrete.TypeParameter) parameter).type.accept(this, null);
    }

    if (parameter instanceof Concrete.TelescopeParameter) {
      ClassReferable classRef = getTypeClassReference(((Concrete.TelescopeParameter) parameter).getType());
      List<? extends Referable> referableList = ((Concrete.TelescopeParameter) parameter).getReferableList();
      for (int i = 0; i < referableList.size(); i++) {
        Referable referable = referableList.get(i);
        if (referable != null && !referable.textRepresentation().equals("_")) {
          for (int j = 0; j < i; j++) {
            Referable referable1 = referableList.get(j);
            if (referable1 != null && referable.textRepresentation().equals(referable1.textRepresentation())) {
              myErrorReporter.report(new DuplicateNameError(Error.Level.WARNING, referable1, referable));
            }
          }
          myContext.add(classRef == null ? referable : new TypedRedirectingReferable(referable, classRef));
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

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      expr.body = expr.body.accept(this, null);
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      expr.codomain = expr.codomain.accept(this, null);
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Void params) {
    if (expr.expression != null) {
      expr.expression = expr.expression.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void params) {
    for (int i = 0; i < expr.getFields().size(); i++) {
      expr.getFields().set(i, expr.getFields().get(i).accept(this, null));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters());
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    if (expr.getSequence().size() == 1) {
      return expr.getSequence().get(0).expression.accept(this, null);
    }

    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      elem.expression = elem.expression.accept(this, null);
    }
    return new BinOpParser(myErrorReporter).parse(expr);
  }

  static void replaceWithConstructor(Concrete.PatternContainer container, int index, Referable constructor) {
    Concrete.Pattern old = container.getPatterns().get(index);
    Concrete.Pattern newPattern = new Concrete.ConstructorPattern(old.getData(), constructor, Collections.emptyList());
    newPattern.setExplicit(old.isExplicit());
    container.getPatterns().set(index, newPattern);
  }

  void visitClauses(List<Concrete.FunctionClause> clauses) {
    for (Concrete.FunctionClause clause : clauses) {
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        Map<String, Concrete.NamePattern> usedNames = new HashMap<>();
        for (int j = 0; j < clause.getPatterns().size(); j++) {
          Referable constructor = visitPattern(clause.getPatterns().get(j), usedNames);
          if (constructor != null) {
            replaceWithConstructor(clause, j, constructor);
          }
          resolvePattern(clause.getPatterns().get(j));
        }

        if (clause.expression != null) {
          clause.expression = clause.expression.accept(this, null);
        }
      }
    }
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      expr.getExpressions().set(i, expr.getExpressions().get(i).accept(this, null));
    }
    visitClauses(expr.getClauses());
    return expr;
  }

  GlobalReferable visitPattern(Concrete.Pattern pattern, Map<String, Concrete.NamePattern> usedNames) {
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
          myErrorReporter.report(new DuplicateNameError(Error.Level.WARNING, referable, prev.getReferable()));
        }
        myContext.add(referable);
      }
      return null;
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      List<? extends Concrete.Pattern> patterns = ((Concrete.ConstructorPattern) pattern).getPatterns();
      for (int i = 0; i < patterns.size(); i++) {
        Referable constructor = visitPattern(patterns.get(i), usedNames);
        if (constructor != null) {
          replaceWithConstructor((Concrete.ConstructorPattern) pattern, i, constructor);
        }
      }
      return null;
    } else if (pattern instanceof Concrete.EmptyPattern) {
      return null;
    } else {
      throw new IllegalStateException();
    }
  }

  void resolvePattern(Concrete.Pattern pattern) {
    if (!(pattern instanceof Concrete.ConstructorPattern)) {
      return;
    }

    Referable referable = ((Concrete.ConstructorPattern) pattern).getConstructor();
    if (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }
    if (referable instanceof UnresolvedReference) {
      Referable newRef = ((UnresolvedReference) referable).resolve(myParentScope);
      if (newRef instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) newRef).getError());
      } else {
        ((Concrete.ConstructorPattern) pattern).setConstructor(newRef);
      }
    }

    for (Concrete.Pattern patternArg : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
      resolvePattern(patternArg);
    }
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void params) {
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    expr.baseClassExpression = expr.baseClassExpression.accept(this, null);
    GlobalReferable classDef = Concrete.getUnderlyingClassDef(expr.baseClassExpression);
    if (classDef instanceof ClassReferable) {
      visitClassFieldImpls(expr.getStatements(), (ClassReferable) classDef);
    }
    return expr;
  }

  void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls, ClassReferable classDef) {
    for (Concrete.ClassFieldImpl impl : classFieldImpls) {
      Referable field = impl.getImplementedField();
      if (field instanceof RedirectingReferable) {
        field = ((RedirectingReferable) field).getOriginalReferable();
      }
      if (field instanceof UnresolvedReference) {
        Referable newField = ((UnresolvedReference) field).resolve(new ClassFieldImplScope(classDef, true));
        if (newField instanceof ErrorReference) {
          myErrorReporter.report(((ErrorReference) newField).getError());
        }
        impl.setImplementedField(newField);
      }

      impl.implementation = impl.implementation.accept(this, null);
    }
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void params) {
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Concrete.LetClause clause : expr.getClauses()) {
        try (Utils.ContextSaver ignored1 = new Utils.ContextSaver(myContext)) {
          visitParameters(clause.getParameters());
          if (clause.resultType != null) {
            clause.resultType = clause.resultType.accept(this, null);
          }
          clause.term = clause.term.accept(this, null);
        }

        ClassReferable classRef = myTypeClassReferenceExtractVisitor.getTypeClassReference(clause.getParameters(), clause.getResultType());
        myContext.add(classRef == null ? clause.getData() : new TypedRedirectingReferable(clause.getData(), classRef));
      }

      expr.expression = expr.expression.accept(this, null);
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return expr;
  }
}
