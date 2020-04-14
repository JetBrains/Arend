package org.arend.naming.resolving.visitor;

import org.arend.core.context.Utils;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.naming.error.DuplicateNameError;
import org.arend.naming.error.NamingError;
import org.arend.naming.reference.*;
import org.arend.naming.resolving.ResolverListener;
import org.arend.naming.scope.*;
import org.arend.term.Fixity;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.ExpectedConstructorError;
import org.arend.typechecking.provider.ConcreteProvider;

import java.util.*;

public class ExpressionResolveNameVisitor extends BaseConcreteExpressionVisitor<Void> {
  final TypeClassReferenceExtractVisitor typeClassReferenceExtractVisitor;
  private final Scope myParentScope;
  private final Scope myScope;
  private final List<Referable> myContext;
  private final ErrorReporter myErrorReporter;
  private final ResolverListener myResolverListener;

  public ExpressionResolveNameVisitor(ConcreteProvider concreteProvider, Scope parentScope, List<Referable> context, ErrorReporter errorReporter, ResolverListener resolverListener) {
    typeClassReferenceExtractVisitor = new TypeClassReferenceExtractVisitor(concreteProvider);
    myParentScope = parentScope;
    myScope = context == null ? parentScope : new MergeScope(new ListScope(context), parentScope);
    myContext = context;
    myErrorReporter = errorReporter;
    myResolverListener = resolverListener;
  }

  Scope getScope() {
    return myScope;
  }

  public static Referable resolve(Referable referable, Scope scope, boolean withArg, List<Referable> resolvedRefs) {
    while (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }
    if (referable instanceof UnresolvedReference) {
      if (withArg) {
        ((UnresolvedReference) referable).resolveArgument(scope, resolvedRefs);
      }
      referable = ((UnresolvedReference) referable).resolve(scope, withArg ? null : resolvedRefs);
      while (referable instanceof RedirectingReferable) {
        referable = ((RedirectingReferable) referable).getOriginalReferable();
      }
    }
    return referable;
  }

  public static Referable resolve(Referable referable, Scope scope) {
    return resolve(referable, scope, false, null);
  }

  public static Concrete.Expression resolve(Concrete.ReferenceExpression refExpr, Scope scope, boolean removeRedirection, List<Referable> resolvedRefs) {
    Referable referable = refExpr.getReferent();
    while (referable instanceof RedirectingReferable) {
      referable = ((RedirectingReferable) referable).getOriginalReferable();
    }

    Concrete.Expression arg = null;
    if (referable instanceof UnresolvedReference) {
      arg = ((UnresolvedReference) referable).resolveArgument(scope, resolvedRefs);
      referable = ((UnresolvedReference) referable).resolve(scope, null);
      if (removeRedirection) {
        while (referable instanceof RedirectingReferable) {
          referable = ((RedirectingReferable) referable).getOriginalReferable();
        }
      }
    }

    refExpr.setReferent(referable);
    return arg;
  }

  void resolveLocal(Concrete.ReferenceExpression expr) {
    Referable origRef = expr.getReferent();
    if (origRef instanceof UnresolvedReference) {
      List<Referable> resolvedList = myResolverListener == null ? null : new ArrayList<>();
      resolve(expr, myContext == null ? EmptyScope.INSTANCE : new ListScope(myContext), true, resolvedList);
      if (expr.getReferent() instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) expr.getReferent()).getError());
      }
      if (myResolverListener != null) {
        myResolverListener.referenceResolved(null, origRef, expr, resolvedList);
      }
    }
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (expr instanceof Concrete.FixityReferenceExpression) {
      Fixity fixity = ((Concrete.FixityReferenceExpression) expr).fixity;
      if (fixity == Fixity.INFIX || fixity == Fixity.POSTFIX) {
        myErrorReporter.report(new NamingError((fixity == Fixity.INFIX ? "Infix" : "Postfix") + " notation is not allowed here", expr));
      }
    }

    Referable origRef = expr.getReferent();
    while (origRef instanceof RedirectingReferable) {
      origRef = ((RedirectingReferable) origRef).getOriginalReferable();
    }
    expr.setReferent(origRef);
    if (!(origRef instanceof UnresolvedReference)) {
      return expr;
    }

    List<Referable> resolvedList = myResolverListener == null ? null : new ArrayList<>();
    Concrete.Expression argument = resolve(expr, myScope, false, resolvedList);
    if (expr.getReferent() instanceof ErrorReference) {
      myErrorReporter.report(((ErrorReference) expr.getReferent()).getError());
    }
    if (myResolverListener != null) {
      myResolverListener.referenceResolved(argument, origRef, expr, resolvedList);
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

  @Override
  protected void visitParameter(Concrete.Parameter parameter, Void params) {
    if (parameter instanceof Concrete.TypeParameter) {
      ((Concrete.TypeParameter) parameter).type = ((Concrete.TypeParameter) parameter).type.accept(this, null);
    }

    ClassReferable classRef = typeClassReferenceExtractVisitor.getTypeClassReference(Collections.emptyList(), parameter.getType());
    List<? extends Referable> referableList = parameter.getReferableList();
    for (int i = 0; i < referableList.size(); i++) {
      Referable referable = referableList.get(i);
      if (referable != null && !referable.textRepresentation().equals("_")) {
        for (int j = 0; j < i; j++) {
          Referable referable1 = referableList.get(j);
          if (referable1 != null && referable.textRepresentation().equals(referable1.textRepresentation())) {
            myErrorReporter.report(new DuplicateNameError(GeneralError.Level.WARNING, referable, referable1));
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
    Set<Referable> eliminatedRefs = new HashSet<>();
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Concrete.CaseArgument caseArg : expr.getArguments()) {
        caseArg.expression = caseArg.expression.accept(this, null);
        if (caseArg.isElim && !(caseArg.expression instanceof Concrete.ReferenceExpression)) {
          myErrorReporter.report(new NamingError("Expected a variable", caseArg.expression));
          caseArg.isElim = false;
        }
        if (caseArg.isElim) {
          eliminatedRefs.add(((Concrete.ReferenceExpression) caseArg.expression).getReferent());
        }
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

    List<Referable> origContext = eliminatedRefs.isEmpty() ? null : new ArrayList<>(myContext);
    if (!eliminatedRefs.isEmpty()) {
      myContext.removeAll(eliminatedRefs);
    }
    visitClauses(expr.getClauses(), null);
    if (origContext != null) {
      myContext.clear();
      myContext.addAll(origContext);
    }

    return expr;
  }

  @Override
  public void visitClause(Concrete.Clause clause, Void params) {
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
      myErrorReporter.report(new DuplicateNameError(GeneralError.Level.WARNING, referable, prev));
    }

    ClassReferable classRef = type == null ? null : typeClassReferenceExtractVisitor.getTypeClassReference(Collections.emptyList(), type);
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
        if (ref instanceof GlobalReferable && ((GlobalReferable) ref).getKind().isConstructor()) {
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

  public void visitPatterns(List<Concrete.Pattern> patterns) {
    visitPatterns(patterns, new HashMap<>(), true);
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
    if (origReferable instanceof UnresolvedReference) {
      List<Referable> resolvedList = myResolverListener == null ? null : new ArrayList<>();
      Referable referable = resolve(origReferable, myParentScope, false, resolvedList);
      if (referable instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) referable).getError());
      } else if (referable instanceof GlobalReferable && !((GlobalReferable) referable).getKind().isConstructor()) {
        myErrorReporter.report(new ExpectedConstructorError((GlobalReferable) referable, null, pattern));
      }

      ((Concrete.ConstructorPattern) pattern).setConstructor(referable);
      if (myResolverListener != null) {
        myResolverListener.patternResolved(origReferable, (Concrete.ConstructorPattern) pattern, resolvedList);
      }
    }

    for (Concrete.Pattern patternArg : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
      resolvePattern(patternArg);
    }
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    expr.setBaseClassExpression(expr.getBaseClassExpression().accept(this, null));
    if (expr.getStatements().isEmpty()) {
      return expr;
    }

    Referable ref = typeClassReferenceExtractVisitor.getTypeReference(Collections.emptyList(), expr.getBaseClassExpression(), true);
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
      LocalError error = new NamingError("Expected a class or a class instance", expr.getBaseClassExpression());
      myErrorReporter.report(error);
      return new Concrete.ErrorHoleExpression(expr.getData(), error);
    }
    return expr;
  }

  Referable visitClassFieldReference(Concrete.ClassElement element, Referable oldField, ClassReferable classDef) {
    if (oldField instanceof UnresolvedReference) {
      List<Referable> resolvedRefs = myResolverListener == null ? null : new ArrayList<>();
      Referable field = resolve(oldField, new ClassFieldImplScope(classDef, true), false, resolvedRefs);
      if (myResolverListener != null) {
        if (element instanceof Concrete.CoClauseElement) {
          myResolverListener.coPatternResolved((Concrete.CoClauseElement) element, oldField, field, resolvedRefs);
        } else if (element instanceof Concrete.OverriddenField) {
          myResolverListener.overriddenFieldResolved((Concrete.OverriddenField) element, oldField, field, resolvedRefs);
        }
      }
      if (field instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) field).getError());
      }
      if (element instanceof Concrete.CoClauseElement) {
        ((Concrete.CoClauseElement) element).setImplementedField(field);
      } else if (element instanceof Concrete.OverriddenField) {
        ((Concrete.OverriddenField) element).setOverriddenField(field);
      }
      return field;
    }
    return oldField;
  }

  void visitClassFieldImpl(Concrete.ClassFieldImpl impl, ClassReferable classDef) {
    visitClassFieldReference(impl, impl.getImplementedField(), classDef);

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

  void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls, ClassReferable classDef) {
    for (Concrete.ClassFieldImpl impl : classFieldImpls) {
      visitClassFieldImpl(impl, classDef);
    }
  }

  private void visitLetClausePattern(Concrete.LetClausePattern pattern) {
    if (pattern.type != null) {
      pattern.type = pattern.type.accept(this, null);
    }
    addReferable(pattern.getReferable(), pattern.type, null);

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
            ? typeClassReferenceExtractVisitor.getTypeClassReference(clause.getParameters(), clause.resultType)
            : clause.term instanceof Concrete.NewExpression
              ? typeClassReferenceExtractVisitor.getTypeClassReference(clause.getParameters(), ((Concrete.NewExpression) clause.term).expression)
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
