package org.arend.naming.resolving.visitor;

import org.arend.core.context.Utils;
import org.arend.error.Error;
import org.arend.naming.BinOpParser;
import org.arend.naming.error.DuplicateNameError;
import org.arend.naming.error.NamingError;
import org.arend.naming.reference.*;
import org.arend.naming.resolving.ResolverListener;
import org.arend.naming.scope.*;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.ExpectedConstructor;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class ExpressionResolveNameVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final TypeClassReferenceExtractVisitor myTypeClassReferenceExtractVisitor;
  private final Scope myParentScope;
  private final Scope myScope;
  private final List<Referable> myContext;
  private final LocalErrorReporter myErrorReporter;
  private final ResolverListener myResolverListener;

  public ExpressionResolveNameVisitor(ConcreteProvider concreteProvider, Scope parentScope, List<Referable> context, LocalErrorReporter errorReporter, ResolverListener resolverListener) {
    myTypeClassReferenceExtractVisitor = new TypeClassReferenceExtractVisitor(concreteProvider);
    myParentScope = parentScope;
    myScope = context == null ? parentScope : new MergeScope(new ListScope(context), parentScope);
    myContext = context;
    myErrorReporter = errorReporter;
    myResolverListener = resolverListener;
  }

  Scope getScope() {
    return myScope;
  }

  public static Referable resolve(Referable referable, Scope scope, boolean withArg) {
    while (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }
    if (referable instanceof UnresolvedReference) {
      if (withArg) {
        ((UnresolvedReference) referable).resolveArgument(scope);
      }
      referable = ((UnresolvedReference) referable).resolve(scope);
      while (referable instanceof RedirectingReferable) {
        referable = ((RedirectingReferable) referable).getOriginalReferable();
      }
    }
    return referable;
  }

  public static Referable resolve(Referable referable, Scope scope) {
    return resolve(referable, scope, false);
  }

  public static Concrete.Expression resolve(Concrete.ReferenceExpression refExpr, Scope scope) {
    Referable referable = refExpr.getReferent();
    while (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }

    Concrete.Expression arg = null;
    if (referable instanceof UnresolvedReference) {
      arg = ((UnresolvedReference) referable).resolveArgument(scope);
      referable = ((UnresolvedReference) referable).resolve(scope);
      while (referable instanceof RedirectingReferable) {
        referable = ((RedirectingReferable) referable).getOriginalReferable();
      }
    }

    refExpr.setReferent(referable);
    return arg;
  }

  void resolveLocal(Concrete.ReferenceExpression expr) {
    Referable origRef = expr.getReferent();
    resolve(expr, myContext == null ? EmptyScope.INSTANCE : new ListScope(myContext));
    if (expr.getReferent() instanceof ErrorReference) {
      myErrorReporter.report(((ErrorReference) expr.getReferent()).getError());
    }
    if (myResolverListener != null) {
      myResolverListener.referenceResolved(null, origRef, expr);
    }
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable origRef = expr.getReferent();
    Concrete.Expression argument = resolve(expr, myScope);
    if (expr.getReferent() instanceof ErrorReference) {
      myErrorReporter.report(((ErrorReference) expr.getReferent()).getError());
    }
    if (myResolverListener != null) {
      myResolverListener.referenceResolved(argument, origRef, expr);
    }
    return argument == null ? expr : Concrete.AppExpression.make(expr.getData(), expr, argument, false);
  }

  void updateScope(Collection<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      for (Referable referable : parameter.getReferableList()) {
        if (referable != null && !referable.textRepresentation().equals("_")) {
          myContext.add(referable);
        }
      }
    }
  }

  protected void visitParameter(Concrete.Parameter parameter, Void params) {
    if (parameter instanceof Concrete.TypeParameter) {
      ((Concrete.TypeParameter) parameter).type = ((Concrete.TypeParameter) parameter).type.accept(this, null);
    }

    ClassReferable classRef = myTypeClassReferenceExtractVisitor.getTypeClassReference(Collections.emptyList(), parameter.getType());
    List<? extends Referable> referableList = parameter.getReferableList();
    for (int i = 0; i < referableList.size(); i++) {
      Referable referable = referableList.get(i);
      if (referable != null && !referable.textRepresentation().equals("_")) {
        for (int j = 0; j < i; j++) {
          Referable referable1 = referableList.get(j);
          if (referable1 != null && referable.textRepresentation().equals(referable1.textRepresentation())) {
            myErrorReporter.report(new DuplicateNameError(Error.Level.WARNING, referable, referable1));
          }
        }
        myContext.add(classRef == null ? referable : new TypedRedirectingReferable(referable, classRef));
      }
    }
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters(), null);
      expr.body = expr.body.accept(this, null);
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters(), null);
      expr.codomain = expr.codomain.accept(this, null);
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitParameters(expr.getParameters(), null);
      for (Concrete.TypeParameter parameter : expr.getParameters()) {
        if (!parameter.isExplicit()) {
          myErrorReporter.report(new NamingError("Parameters in sigma types must be explicit", parameter));
          parameter.setExplicit(true);
        }
      }
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Concrete.CaseArgument caseArg : expr.getArguments()) {
        caseArg.expression = caseArg.expression.accept(this, null);
        if (caseArg.type != null) {
          caseArg.type = caseArg.type.accept(this, null);
        }
        addReferable(caseArg.referable, caseArg.type, null);
      }
      if (expr.getResultType() != null) {
        expr.setResultType(expr.getResultType().accept(this, null));
      }
      if (expr.getResultTypeLevel() != null) {
        expr.setResultTypeLevel(expr.getResultTypeLevel().accept(this, null));
      }
    }
    visitClauses(expr.getClauses(), null);
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    Concrete.Expression result = super.visitBinOpSequence(expr, null);
    return result instanceof Concrete.BinOpSequenceExpression ? new BinOpParser(myErrorReporter).parse((Concrete.BinOpSequenceExpression) result) : result;
  }

  @Override
  protected void visitClause(Concrete.Clause clause, Void params) {
    if (clause instanceof Concrete.FunctionClause) {
      Concrete.FunctionClause functionClause = (Concrete.FunctionClause) clause;
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        visitPatterns(clause.getPatterns(), new HashMap<>(), true);
        if (functionClause.expression != null) {
          functionClause.expression = functionClause.expression.accept(this, null);
        }
      }
    }
  }

  private void addReferable(Referable referable, Concrete.Expression type, Map<String, Referable> usedNames) {
    if (referable == null) {
      return;
    }

    String name = referable.textRepresentation();
    if (name.equals("_")) {
      return;
    }

    Referable prev = usedNames == null ? null : usedNames.put(name, referable);
    if (prev != null) {
      myErrorReporter.report(new DuplicateNameError(Error.Level.WARNING, referable, prev));
    }

    ClassReferable classRef = type == null ? null : myTypeClassReferenceExtractVisitor.getTypeClassReference(Collections.emptyList(), type);
    myContext.add(classRef == null ? referable : new TypedRedirectingReferable(referable, classRef));
  }

  private GlobalReferable visitPattern(Concrete.Pattern pattern, Map<String, Referable> usedNames) {
    for (Concrete.TypedReferable typedReferable : pattern.getAsReferables()) {
      if (typedReferable.type != null) {
        typedReferable.type = typedReferable.type.accept(this, null);
      }
    }

    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      if (namePattern.type != null) {
        namePattern.type = namePattern.type.accept(this, null);
      }

      Referable referable = namePattern.getReferable();
      String name = referable == null ? null : referable.textRepresentation();
      if (name == null) {
        return null;
      }

      if (namePattern.type == null) {
        Referable ref = myParentScope.resolveName(name);
        if (ref instanceof GlobalReferable && ((GlobalReferable) ref).getKind() == GlobalReferable.Kind.CONSTRUCTOR) {
          return (GlobalReferable) ref;
        }
      }

      addReferable(referable, namePattern.type, usedNames);
      return null;
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      visitPatterns(((Concrete.ConstructorPattern) pattern).getPatterns(), usedNames, false);
    } else if (pattern instanceof Concrete.TuplePattern) {
      visitPatterns(((Concrete.TuplePattern) pattern).getPatterns(), usedNames, false);
    } else if (!(pattern instanceof Concrete.NumberPattern)) {
      throw new IllegalStateException();
    }

    for (Concrete.TypedReferable typedReferable : pattern.getAsReferables()) {
      addReferable(typedReferable.referable, typedReferable.type, usedNames);
    }

    return null;
  }

  void visitPatterns(List<Concrete.Pattern> patterns, Map<String, Referable> usedNames, boolean resolvePatterns) {
    for (int i = 0; i < patterns.size(); i++) {
      Referable constructor = visitPattern(patterns.get(i), usedNames);
      if (constructor != null) {
        patterns.set(i, new Concrete.ConstructorPattern(patterns.get(i).getData(), patterns.get(i).isExplicit(), constructor, Collections.emptyList(), Collections.emptyList()));
      }
      if (resolvePatterns) {
        resolvePattern(patterns.get(i));
      }
    }
  }

  private void resolvePattern(Concrete.Pattern pattern) {
    if (pattern instanceof Concrete.TuplePattern) {
      for (Concrete.Pattern patternArg : ((Concrete.TuplePattern) pattern).getPatterns()) {
        resolvePattern(patternArg);
      }
      return;
    }
    if (!(pattern instanceof Concrete.ConstructorPattern)) {
      return;
    }

    Referable origReferable = ((Concrete.ConstructorPattern) pattern).getConstructor();
    Referable referable = resolve(origReferable, myParentScope);
    if (referable instanceof ErrorReference) {
      myErrorReporter.report(((ErrorReference) referable).getError());
    } else if (referable instanceof GlobalReferable && ((GlobalReferable) referable).getKind() != GlobalReferable.Kind.CONSTRUCTOR) {
      myErrorReporter.report(new ExpectedConstructor(referable, null, pattern));
    }

    ((Concrete.ConstructorPattern) pattern).setConstructor(referable);
    if (myResolverListener != null) {
      myResolverListener.patternResolved(origReferable, (Concrete.ConstructorPattern) pattern);
    }

    for (Concrete.Pattern patternArg : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
      resolvePattern(patternArg);
    }
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    Referable ref = null;
    Concrete.Expression baseExpr = expr.getBaseClassExpression();
    if (baseExpr instanceof Concrete.AppExpression) {
      baseExpr = ((Concrete.AppExpression) expr.getBaseClassExpression()).getFunction();
    }
    if (baseExpr instanceof Concrete.ReferenceExpression) {
      Concrete.ReferenceExpression refExpr = (Concrete.ReferenceExpression) baseExpr;
      if (refExpr.getReferent() instanceof NamedUnresolvedReference) {
        ref = ((NamedUnresolvedReference) refExpr.getReferent()).resolve(myScope);
        refExpr.setReferent(ref);
      }
    }

    expr.setBaseClassExpression(expr.getBaseClassExpression().accept(this, null));
    if (expr.getStatements().isEmpty()) {
      return expr;
    }

    if (!(ref instanceof TypedReferable)) {
      ref = expr.getBaseClassExpression().getUnderlyingReferable();
    }
    ClassReferable classRef = null;
    if (ref instanceof ClassReferable) {
      classRef = (ClassReferable) ref;
    } else if (ref instanceof TypedReferable) {
      classRef = ((TypedReferable) ref).getTypeClassReference();
    }

    if (classRef != null) {
      visitClassFieldImpls(expr.getStatements(), classRef);
    } else {
      LocalError error = new NamingError("Expected a class or a class instance", expr.getBaseClassExpression().getData());
      myErrorReporter.report(error);
      return new Concrete.ErrorHoleExpression(expr.getData(), error);
    }
    return expr;
  }

  void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls, ClassReferable classDef) {
    for (Concrete.ClassFieldImpl impl : classFieldImpls) {
      Referable field = impl.getImplementedField();
      while (field instanceof RedirectingReferable) {
        field = ((RedirectingReferable) field).getOriginalReferable();
      }
      if (field instanceof UnresolvedReference) {
        field = ((UnresolvedReference) field).resolve(new ClassFieldImplScope(classDef, true));
        while (field instanceof RedirectingReferable) {
          field = ((RedirectingReferable) field).getOriginalReferable();
        }
        if (field instanceof ErrorReference) {
          myErrorReporter.report(((ErrorReference) field).getError());
        }
        impl.setImplementedField(field);
      }

      if (impl.implementation == null) {
        ClassReferable classRef = impl.getImplementedField() instanceof ClassReferable
          ? (ClassReferable) impl.getImplementedField()
          : impl.getImplementedField() instanceof TypedReferable
            ? ((TypedReferable) impl.getImplementedField()).getTypeClassReference()
            : null;
        if (classRef != null) {
          visitClassFieldImpls(impl.subClassFieldImpls, classRef);
        }
      } else {
        impl.implementation = impl.implementation.accept(this, null);
      }
    }
  }

  private void visitLetClausePattern(Concrete.LetClausePattern pattern) {
    Referable referable = pattern.getReferable();
    if (pattern.type != null) {
      pattern.type = pattern.type.accept(this, null);
    }
    addReferable(referable, pattern.type, null);

    for (Concrete.LetClausePattern subPattern : pattern.getPatterns()) {
      visitLetClausePattern(subPattern);
    }
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Concrete.LetClause clause : expr.getClauses()) {
        try (Utils.ContextSaver ignored1 = new Utils.ContextSaver(myContext)) {
          visitParameters(clause.getParameters(), null);
          if (clause.resultType != null) {
            clause.resultType = clause.resultType.accept(this, null);
          }
          clause.term = clause.term.accept(this, null);
        }

        Concrete.LetClausePattern pattern = clause.getPattern();
        if (pattern.getReferable() != null) {
          ClassReferable classRef = clause.resultType != null
            ? myTypeClassReferenceExtractVisitor.getTypeClassReference(clause.getParameters(), clause.resultType)
            : clause.term instanceof Concrete.NewExpression
              ? myTypeClassReferenceExtractVisitor.getTypeClassReference(clause.getParameters(), ((Concrete.NewExpression) clause.term).expression)
              : null;
          myContext.add(classRef == null ? pattern.getReferable() : new TypedRedirectingReferable(pattern.getReferable(), classRef));
        } else {
          visitLetClausePattern(pattern);
        }
      }

      expr.expression = expr.expression.accept(this, null);
      return expr;
    }
  }
}
