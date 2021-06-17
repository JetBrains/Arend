package org.arend.naming.resolving.visitor;

import org.arend.core.context.Utils;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.error.CountingErrorReporter;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.ExpressionResolver;
import org.arend.ext.typechecking.MetaResolver;
import org.arend.extImpl.ContextDataImpl;
import org.arend.naming.MetaBinOpParser;
import org.arend.naming.error.DuplicateNameError;
import org.arend.ext.error.NameResolverError;
import org.arend.naming.error.NotInScopeError;
import org.arend.naming.error.ReferenceError;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.resolving.ResolverListener;
import org.arend.naming.scope.*;
import org.arend.naming.scope.local.ElimScope;
import org.arend.term.Fixity;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteLevelExpressionVisitor;
import org.arend.typechecking.error.local.ExpectedConstructorError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ExpressionResolveNameVisitor extends BaseConcreteExpressionVisitor<Void> implements ExpressionResolver, ConcreteLevelExpressionVisitor<LevelVariable, Concrete.LevelExpression> {
  private final ReferableConverter myReferableConverter;
  private final Scope myParentScope;
  private final Scope myScope;
  private final List<Referable> myContext;
  private final CountingErrorReporter myErrorReporter;
  private final ResolverListener myResolverListener;
  private final Map<String, Referable> myPLevelVars;
  private final Map<String, Referable> myHLevelVars;

  private ExpressionResolveNameVisitor(ReferableConverter referableConverter, Scope parentScope, Scope scope, List<Referable> context, ErrorReporter errorReporter, ResolverListener resolverListener, Map<String, Referable> pLevelVars, Map<String, Referable> hLevelVars) {
    myReferableConverter = referableConverter;
    myParentScope = parentScope;
    myScope = scope;
    myContext = context;
    myErrorReporter = new CountingErrorReporter(GeneralError.Level.ERROR, errorReporter);
    myResolverListener = resolverListener;
    myPLevelVars = pLevelVars;
    myHLevelVars = hLevelVars;
  }

  public ExpressionResolveNameVisitor(ReferableConverter referableConverter, Scope parentScope, List<Referable> context, ErrorReporter errorReporter, ResolverListener resolverListener, Map<String, Referable> pLevelVars, Map<String, Referable> hLevelVars) {
    this(referableConverter, parentScope, context == null ? parentScope : new MergeScope(new ListScope(context), parentScope), context, errorReporter, resolverListener, pLevelVars, hLevelVars);
  }

  public ExpressionResolveNameVisitor(ReferableConverter referableConverter, Scope parentScope, List<Referable> context, ErrorReporter errorReporter, ResolverListener resolverListener) {
    this(referableConverter, parentScope, context, errorReporter, resolverListener, Collections.emptyMap(), Collections.emptyMap());
  }

  @Override
  public @NotNull CountingErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public @NotNull ConcreteExpression resolve(@NotNull ConcreteExpression expression) {
    if (!(expression instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    return ((Concrete.Expression) expression).accept(this, null);
  }

  @Override
  public @NotNull ConcreteExpression resolve(@Nullable Object data, @NotNull List<? extends ConcreteArgument> arguments) {
    if (arguments.isEmpty()) {
      throw new IllegalArgumentException();
    }
    if (arguments.size() == 1) {
      if (!(arguments.get(0).getExpression() instanceof Concrete.Expression)) {
        throw new IllegalArgumentException();
      }
      return ((Concrete.Expression) arguments.get(0).getExpression()).accept(this, null);
    }

    List<Concrete.BinOpSequenceElem> elems = new ArrayList<>(arguments.size());
    boolean first = true;
    for (ConcreteArgument argument : arguments) {
      if (!(argument instanceof Concrete.Argument)) {
        throw new IllegalArgumentException();
      }
      if (first) {
        elems.add(new Concrete.BinOpSequenceElem(((Concrete.Argument) argument).expression));
        first = false;
      } else {
        elems.add(new Concrete.BinOpSequenceElem(((Concrete.Argument) argument).expression, Fixity.UNKNOWN, argument.isExplicit()));
      }
    }

    return visitBinOpSequence(new Concrete.BinOpSequenceExpression(data, elems, null), null);
  }

  @Override
  public @NotNull ExpressionResolver hideRefs(@NotNull Set<? extends ArendRef> refs) {
    return new ExpressionResolveNameVisitor(myReferableConverter, myParentScope, new ElimScope(myScope, refs), myContext, myErrorReporter, myResolverListener, myPLevelVars, myHLevelVars);
  }

  @Override
  public @NotNull ExpressionResolver useRefs(@NotNull List<? extends ArendRef> refs, boolean allowContext) {
    return new ExpressionResolveNameVisitor(myReferableConverter, myParentScope, allowContext ? new org.arend.naming.scope.local.ListScope(myScope, refs) : new ListScope(refs), myContext, myErrorReporter, myResolverListener, myPLevelVars, myHLevelVars);
  }

  @Override
  public void registerDeclaration(@NotNull ArendRef ref) {
    if (!(ref instanceof Referable)) {
      throw new IllegalArgumentException();
    }
    if (myResolverListener != null) {
      myResolverListener.bindingResolved((Referable) ref);
    }
  }

  @Override
  public boolean isLongUnresolvedReference(@NotNull ArendRef ref) {
    return ref instanceof LongUnresolvedReference && ((LongUnresolvedReference) ref).getPath().size() > 1;
  }

  Scope getScope() {
    return myScope;
  }

  public static Referable resolve(Referable referable, Scope scope, boolean withArg, List<Referable> resolvedRefs) {
    referable = RedirectingReferable.getOriginalReferable(referable);
    if (referable instanceof UnresolvedReference) {
      if (withArg) {
        ((UnresolvedReference) referable).resolveArgument(scope, resolvedRefs);
      }
      referable = RedirectingReferable.getOriginalReferable(((UnresolvedReference) referable).resolve(scope, withArg ? null : resolvedRefs));
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
      convertExpr(expr);
      if (expr.getReferent() instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) expr.getReferent()).getError());
      }
      if (myResolverListener != null) {
        myResolverListener.referenceResolved(null, origRef, expr, resolvedList, myScope);
      }
    }
  }

  private Referable convertChecked(Referable origRef, Object data) {
    if (origRef instanceof ErrorReference) {
      return origRef;
    }
    Referable ref = myReferableConverter.convert(origRef);
    if (ref != null) {
      return ref;
    }
    NameResolverError error = new NameResolverError("Invalid reference", data);
    myErrorReporter.report(error);
    return new ErrorReference(error, origRef.getRefName());
  }

  Referable convertReferable(Referable referable, Object data) {
    Referable origRef = referable instanceof RedirectingReferable ? ((RedirectingReferable) referable).getOriginalReferable() : referable;
    while (origRef instanceof RedirectingReferable) {
      origRef = ((RedirectingReferable) origRef).getOriginalReferable();
    }

    if (origRef instanceof ModuleReferable) {
      return origRef;
    }
    if (!(origRef instanceof GlobalReferable)) {
      return referable;
    }

    origRef = convertChecked(origRef, data);
    if (referable instanceof RedirectingReferable) {
      return new RedirectingReferableImpl(origRef, ((RedirectingReferable) referable).getPrecedence(), referable.textRepresentation());
    } else {
      return origRef;
    }
  }

  private void convertExpr(Concrete.ReferenceExpression expr) {
    if (expr.getReferent() instanceof GlobalReferable && !(expr.getReferent() instanceof TCDefReferable)) {
      expr.setReferent(convertReferable(expr.getReferent(), expr.getData()));
    }
  }

  private void convertArgument(Concrete.Expression arg) {
    for (; arg instanceof Concrete.AppExpression; arg = ((Concrete.AppExpression) arg).getArguments().get(0).expression) {
      convertExpr((Concrete.ReferenceExpression) ((Concrete.AppExpression) arg).getFunction());
    }
    if (arg instanceof Concrete.ReferenceExpression) {
      convertExpr((Concrete.ReferenceExpression) arg);
    }
  }

  public static MetaResolver getMetaResolver(Referable ref) {
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    return ref instanceof MetaReferable ? ((MetaReferable) ref).getResolver() : null;
  }

  public Concrete.Expression invokeMetaWithoutArguments(Concrete.ReferenceExpression expr, Concrete.Expression argument, boolean invokeMeta) {
    if (invokeMeta) {
      MetaResolver metaDef = getMetaResolver(expr.getReferent());
      if (metaDef != null) {
        myErrorReporter.resetErrorsNumber();
        return convertMetaResult(metaDef.resolvePrefix(this, new ContextDataImpl(expr, argument == null ? Collections.emptyList() : Collections.singletonList(new Concrete.Argument(argument, false)), null, null, null, null)), expr, Collections.emptyList(), null, null);
      }
    }

    return argument == null ? expr : Concrete.AppExpression.make(expr.getData(), expr, argument, false);
  }

  private Concrete.Expression visitReference(Concrete.ReferenceExpression expr, boolean invokeMeta) {
    if (expr instanceof Concrete.FixityReferenceExpression) {
      Fixity fixity = ((Concrete.FixityReferenceExpression) expr).fixity;
      if (fixity == Fixity.INFIX || fixity == Fixity.POSTFIX) {
        myErrorReporter.report(new NameResolverError((fixity == Fixity.INFIX ? "Infix" : "Postfix") + " notation is not allowed here", expr));
      }
    }

    Referable origRef = expr.getReferent();
    while (origRef instanceof RedirectingReferable) {
      origRef = ((RedirectingReferable) origRef).getOriginalReferable();
    }

    Concrete.Expression argument;
    if (origRef instanceof UnresolvedReference) {
      expr.setReferent(origRef);
      List<Referable> resolvedList = myResolverListener == null ? null : new ArrayList<>();
      argument = resolve(expr, myScope, false, resolvedList);
      if (expr.getReferent() instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) expr.getReferent()).getError());
      }
      convertExpr(expr);
      convertArgument(argument);
      if (myResolverListener != null) {
        myResolverListener.referenceResolved(argument, origRef, expr, resolvedList, myScope);
      }
    } else {
      argument = null;
    }

    if (expr.getPLevel() != null) {
      expr.getPLevel().accept(this, LevelVariable.PVAR);
    }
    if (expr.getHLevel() != null) {
      expr.getHLevel().accept(this, LevelVariable.HVAR);
    }

    return invokeMetaWithoutArguments(expr, argument, invokeMeta);
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    return visitReference(expr, true);
  }

  public Concrete.Expression convertMetaResult(ConcreteExpression expr, Concrete.ReferenceExpression refExpr, List<Concrete.Argument> args, Concrete.Coclauses coclauses, Concrete.FunctionClauses clauses) {
    if (!(expr == null || expr instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    if (expr == null) {
      if (myErrorReporter.getErrorsNumber() == 0) {
        myErrorReporter.report(new NameResolverError("Meta '" + refExpr.getReferent().getRefName() + "' failed", refExpr));
      }
      return new Concrete.ErrorHoleExpression(refExpr.getData(), null);
    }
    if (myResolverListener != null) {
      myResolverListener.metaResolved(refExpr, args, (Concrete.Expression) expr, coclauses, clauses);
    }
    return (Concrete.Expression) expr;
  }

  private Concrete.Expression visitMeta(Concrete.Expression function, List<Concrete.Argument> arguments, Concrete.Coclauses coclauses) {
    Concrete.ReferenceExpression refExpr;
    if (function instanceof Concrete.AppExpression && ((Concrete.AppExpression) function).getFunction() instanceof Concrete.ReferenceExpression) {
      refExpr = (Concrete.ReferenceExpression) ((Concrete.AppExpression) function).getFunction();
      List<Concrete.Argument> newArgs = new ArrayList<>(((Concrete.AppExpression) function).getArguments());
      newArgs.addAll(arguments);
      arguments = newArgs;
    } else if (function instanceof Concrete.ReferenceExpression) {
      refExpr = (Concrete.ReferenceExpression) function;
    } else {
      refExpr = null;
    }

    MetaResolver metaDef = refExpr == null ? null : getMetaResolver(refExpr.getReferent());
    if (metaDef == null) {
      return null;
    }
    myErrorReporter.resetErrorsNumber();
    return convertMetaResult(metaDef.resolvePrefix(this, new ContextDataImpl(refExpr, arguments, coclauses, null, null, null)), refExpr, arguments, coclauses, null);
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    if (expr.getFunction() instanceof Concrete.ReferenceExpression) {
      Concrete.Expression function = visitReference((Concrete.ReferenceExpression) expr.getFunction(), false);
      Concrete.Expression metaResult = visitMeta(function, expr.getArguments(), null);
      return metaResult != null ? metaResult : visitArguments(function, expr.getArguments());
    } else {
      return visitArguments(expr.getFunction().accept(this, null), expr.getArguments());
    }
  }

  private Concrete.Expression visitArguments(Concrete.Expression function, List<Concrete.Argument> arguments) {
    for (Concrete.Argument argument : arguments) {
      function = Concrete.AppExpression.make(function.getData(), function, argument.expression.accept(this, null), argument.isExplicit());
    }
    return function;
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
      return visitBinOpSequence(expr.getData(), expr, null);
  }

  private Concrete.Expression visitBinOpSequence(Object data, Concrete.BinOpSequenceExpression expr, Concrete.Coclauses coclauses) {
    if (expr.getSequence().isEmpty() && expr.getClauses() == null) {
      return visitClassExt(data, expr, coclauses);
    }
    if (expr.getSequence().size() == 1 && expr.getClauses() == null) {
      return visitClassExt(data, expr.getSequence().get(0).expression.accept(this, null), coclauses);
    }

    boolean hasMeta = false;
    List<MetaBinOpParser.ResolvedReference> resolvedRefs = new ArrayList<>();
    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      if (elem.expression instanceof Concrete.ReferenceExpression) {
        Concrete.ReferenceExpression refExpr = (Concrete.ReferenceExpression) elem.expression;
        Referable ref = refExpr.getReferent();
        while (ref instanceof RedirectingReferable) {
          ref = ((RedirectingReferable) ref).getOriginalReferable();
        }
        if (ref instanceof UnresolvedReference) {
          List<Referable> resolvedList = myResolverListener == null ? null : new ArrayList<>();
          Concrete.Expression argument = resolve(refExpr, myScope, false, resolvedList);
          convertExpr(refExpr);
          convertArgument(argument);
          elem.expression = argument == null ? refExpr : Concrete.AppExpression.make(refExpr.getData(), refExpr, argument, false);
          resolvedRefs.add(new MetaBinOpParser.ResolvedReference(refExpr, (UnresolvedReference) ref, resolvedList));
        } else {
          resolvedRefs.add(new MetaBinOpParser.ResolvedReference(refExpr, null, null));
        }

        if (!hasMeta && getMetaResolver(refExpr.getReferent()) != null) {
          hasMeta = true;
        }
      } else {
        resolvedRefs.add(null);
      }
    }

    if (!hasMeta) {
      if (expr.getClauses() != null) {
        myErrorReporter.report(new NameResolverError("Clauses are not allowed here", expr.getClauses()));
      }
      for (int i = 0; i < resolvedRefs.size(); i++) {
        finalizeReference(expr.getSequence().get(i), resolvedRefs.get(i));
      }
      return visitClassExt(data, expr, coclauses);
    }

    return new MetaBinOpParser(this, expr, resolvedRefs, coclauses).parse(data);
  }

  public void finalizeReference(Concrete.BinOpSequenceElem elem, MetaBinOpParser.ResolvedReference resolvedReference) {
    if (resolvedReference == null) {
      elem.expression = elem.expression.accept(this, null);
      return;
    }
    if (resolvedReference.originalReference == null) {
      return;
    }

    if (resolvedReference.refExpr.getReferent() instanceof ErrorReference) {
      myErrorReporter.report(((ErrorReference) resolvedReference.refExpr.getReferent()).getError());
    }
    if (resolvedReference.resolvedList != null && myResolverListener != null) {
      Concrete.Expression argument = elem.expression instanceof Concrete.AppExpression ? ((Concrete.AppExpression) elem.expression).getArguments().get(0).expression : null;
      myResolverListener.referenceResolved(argument, resolvedReference.originalReference, resolvedReference.refExpr, resolvedReference.resolvedList, myScope);
    }
  }

  void updateScope(Collection<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      for (Referable referable : parameter.getReferableList()) {
        if (referable != null && !referable.textRepresentation().equals("_")) {
          addLocalRef(referable, null);
        }
      }
    }
  }

  static boolean checkName(Referable ref, ErrorReporter errorReporter) {
    if (ref == null) {
      return true;
    }
    String name = ref.getRefName();
    for (int i = 0; i < name.length(); i++) {
      if (!Character.UnicodeBlock.BASIC_LATIN.equals(Character.UnicodeBlock.of(name.codePointAt(i)))) {
        errorReporter.report(new ReferenceError(GeneralError.Stage.RESOLVER, "Invalid name", ref));
        return false;
      }
    }
    return true;
  }

  private void addLocalRef(Referable ref, ClassReferable classRef) {
    if (checkName(ref, myErrorReporter)) {
      myContext.add(classRef == null ? ref : new TypedRedirectingReferable(ref, classRef));
      if (myResolverListener != null) {
        myResolverListener.bindingResolved(ref);
      }
    }
  }

  @Override
  protected void visitParameter(Concrete.Parameter parameter, Void params) {
    if (parameter instanceof Concrete.TypeParameter) {
      ((Concrete.TypeParameter) parameter).type = ((Concrete.TypeParameter) parameter).type.accept(this, null);
    }

    ClassReferable classRef = new TypeClassReferenceExtractVisitor().getTypeClassReference(Collections.emptyList(), parameter.getType());
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
        addLocalRef(referable, classRef);
      }
    }
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      if (expr instanceof Concrete.PatternLamExpression) {
        visitPatterns(((Concrete.PatternLamExpression) expr).getPatterns(), expr.getParameters(), new HashMap<>(), true);
      } else {
        visitParameters(expr.getParameters(), null);
      }
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
          myErrorReporter.report(new NameResolverError("Parameters in sigma types must be explicit", parameter));
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
        if (caseArg.isElim && caseArg.expression instanceof Concrete.ReferenceExpression) {
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

    addLocalRef(referable, type == null ? null : new TypeClassReferenceExtractVisitor().getTypeClassReference(Collections.emptyList(), type));
  }

  private GlobalReferable visitPattern(Concrete.Pattern pattern, Map<String, Referable> usedNames) {
    if (pattern.getAsReferable() != null && pattern.getAsReferable().type != null) {
      pattern.getAsReferable().type = pattern.getAsReferable().type.accept(this, null);
    }

    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      if (namePattern.type != null) {
        namePattern.type = namePattern.type.accept(this, null);
      }

      Referable referable = namePattern.getReferable();
      if (referable == null || referable instanceof GlobalReferable) {
        return null;
      }

      if (namePattern.type == null) {
        Referable ref = myReferableConverter.convert(RedirectingReferable.getOriginalReferable(myParentScope.resolveName(referable.getRefName())));
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

    if (pattern.getAsReferable() != null) {
      addReferable(pattern.getAsReferable().referable, pattern.getAsReferable().type, usedNames);
    }

    return null;
  }

  private void visitPatterns(List<Concrete.Pattern> patterns, List<Concrete.Parameter> parameters, Map<String, Referable> usedNames, boolean resolvePatterns) {
    int j = 0;
    for (int i = 0; i < patterns.size(); i++) {
      Concrete.Pattern pattern = patterns.get(i);
      if (pattern == null) {
        visitParameter(parameters.get(j++), null);
        continue;
      }
      Referable ref = pattern instanceof Concrete.NamePattern ? ((Concrete.NamePattern) pattern).getReferable() : null;
      Referable constructor = visitPattern(pattern, usedNames);
      if (constructor != null) {
        Concrete.ConstructorPattern newPattern = new Concrete.ConstructorPattern(pattern.getData(), pattern.isExplicit(), constructor, Collections.emptyList(), null);
        patterns.set(i, newPattern);
        if (myResolverListener != null) {
          myResolverListener.patternResolved(ref, newPattern, Collections.singletonList(constructor));
        }
      } else if (pattern instanceof Concrete.NamePattern && myResolverListener != null) {
        myResolverListener.patternResolved((Concrete.NamePattern) pattern);
      }
      if (resolvePatterns) {
        resolvePattern(patterns.get(i));
      }
    }
  }

  public void visitPatterns(List<Concrete.Pattern> patterns, Map<String, Referable> usedNames, boolean resolvePatterns) {
    visitPatterns(patterns, null, usedNames, resolvePatterns);
  }

  public void resolvePattern(Concrete.Pattern pattern) {
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
      Referable referable = convertChecked(resolve(origReferable, myParentScope, false, resolvedList), ((UnresolvedReference) origReferable).getData());
      if (referable instanceof ErrorReference) {
        myErrorReporter.report(((ErrorReference) referable).getError());
      } else if (referable instanceof GlobalReferable && !((GlobalReferable) referable).getKind().isConstructor()) {
        myErrorReporter.report(new ExpectedConstructorError((GlobalReferable) referable, null, null, pattern, null, EmptyDependentLink.getInstance(), null));
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

  public Concrete.Expression visitClassExt(Object data, Concrete.Expression baseExpr, Concrete.Coclauses coclauses) {
    if (coclauses == null) {
      return baseExpr;
    }
    if (coclauses.getCoclauseList().isEmpty()) {
      return Concrete.ClassExtExpression.make(data, baseExpr, coclauses);
    }

    TypeClassReferenceExtractVisitor visitor = new TypeClassReferenceExtractVisitor();
    Referable ref = visitor.getTypeReference(Collections.emptyList(), baseExpr, true);
    ClassReferable classRef = visitor.findClassReference(ref);
    if (classRef == null && ref != null && !(ref instanceof TypedReferable)) {
      ref = ref.getUnderlyingReferable();
      if (!(ref instanceof TypedReferable)) {
        ref = baseExpr.getUnderlyingReferable();
        if (ref != null && !(ref instanceof TypedReferable)) {
          ref = ref.getUnderlyingReferable();
        }
      }
    }
    if (classRef == null && ref instanceof TypedReferable) {
      classRef = ((TypedReferable) ref).getTypeClassReference();
    }

    if (classRef != null) {
      visitClassFieldImpls(coclauses.getCoclauseList(), classRef);
    } else {
      LocalError error = new NameResolverError("Expected a class or a class instance", baseExpr);
      myErrorReporter.report(error);
      return new Concrete.ErrorHoleExpression(data, error);
    }
    return Concrete.ClassExtExpression.make(data, baseExpr, coclauses);
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    Concrete.Expression baseExpr = expr.getBaseClassExpression();
    if (baseExpr instanceof Concrete.ReferenceExpression) {
      baseExpr = visitReference((Concrete.ReferenceExpression) baseExpr, false);
      Concrete.Expression metaResult = visitMeta(baseExpr, Collections.emptyList(), expr.getCoclauses());
      if (metaResult != null) {
        return metaResult;
      }
    } else if (baseExpr instanceof Concrete.AppExpression) {
      Concrete.Expression function = ((Concrete.AppExpression) baseExpr).getFunction();
      function = function instanceof Concrete.ReferenceExpression ? visitReference((Concrete.ReferenceExpression) function, false) : function.accept(this, null);
      Concrete.Expression metaResult = visitMeta(function, ((Concrete.AppExpression) baseExpr).getArguments(), expr.getCoclauses());
      if (metaResult != null) {
        return metaResult;
      }
      baseExpr = visitArguments(function, ((Concrete.AppExpression) baseExpr).getArguments());
    } else if (baseExpr instanceof Concrete.BinOpSequenceExpression) {
      return visitBinOpSequence(expr.getData(), (Concrete.BinOpSequenceExpression) baseExpr, expr.getCoclauses());
    } else {
      baseExpr = expr.getBaseClassExpression().accept(this, null);
    }

    expr.setBaseClassExpression(baseExpr);
    return visitClassExt(expr.getData(), baseExpr, expr.getCoclauses());
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
      } else if (field instanceof GlobalReferable && !(field instanceof TCDefReferable)) {
        field = convertChecked(field, ((UnresolvedReference) oldField).getData());
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
      Referable ref = impl.getImplementedField();
      if (!(ref instanceof ClassReferable || ref instanceof TypedReferable)) {
        ref = ref.getUnderlyingReferable();
      }
      if (!(ref instanceof ClassReferable) && ref instanceof TypedReferable) {
        ref = ((TypedReferable) ref).getTypeClassReference();
      }
      if (ref instanceof ClassReferable) {
        impl.classRef = (TCDefReferable) myReferableConverter.toDataLocatedReferable((ClassReferable) ref);
        visitClassFieldImpls(impl.getSubCoclauseList(), (ClassReferable) ref);
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

        Concrete.Pattern pattern = clause.getPattern();
        if (pattern instanceof Concrete.NamePattern && ((Concrete.NamePattern) pattern).getRef() != null) {
          ClassReferable classRef = clause.resultType != null
            ? new TypeClassReferenceExtractVisitor().getTypeClassReference(clause.getParameters(), clause.resultType)
            : clause.term instanceof Concrete.NewExpression
              ? new TypeClassReferenceExtractVisitor().getTypeClassReference(clause.getParameters(), ((Concrete.NewExpression) clause.term).expression)
              : null;
          addLocalRef(((Concrete.NamePattern) pattern).getRef(), classRef);
        } else {
          visitPatterns(Collections.singletonList(pattern), null, new HashMap<>(), true);
        }
      }

      expr.expression = expr.expression.accept(this, null);
      return expr;
    }
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Void params) {
    Concrete.LevelExpression pLevel = expr.getPLevel();
    if (pLevel != null) {
      pLevel = pLevel.accept(this, null);
    }
    Concrete.LevelExpression hLevel = expr.getHLevel();
    if (hLevel != null) {
      hLevel = hLevel.accept(this, null);
    }
    return new Concrete.UniverseExpression(expr.getData(), pLevel, hLevel);
  }

  @Override
  public Concrete.LevelExpression visitInf(Concrete.InfLevelExpression expr, LevelVariable param) {
    return expr;
  }

  @Override
  public Concrete.LevelExpression visitLP(Concrete.PLevelExpression expr, LevelVariable param) {
    return expr;
  }

  @Override
  public Concrete.LevelExpression visitLH(Concrete.HLevelExpression expr, LevelVariable param) {
    return expr;
  }

  @Override
  public Concrete.LevelExpression visitNumber(Concrete.NumberLevelExpression expr, LevelVariable param) {
    return expr;
  }

  @Override
  public Concrete.LevelExpression visitId(Concrete.IdLevelExpression expr, LevelVariable type) {
    Referable ref = (type == LevelVariable.HVAR ? myHLevelVars : myPLevelVars).get(expr.getReferent().getRefName());
    if (ref == null) {
      myErrorReporter.report(new NotInScopeError(expr.getData(), null, -1, expr.getReferent().getRefName()));
      return type == LevelVariable.HVAR ? new Concrete.HLevelExpression(expr.getData()) : new Concrete.PLevelExpression(expr.getData());
    }
    return new Concrete.IdLevelExpression(expr.getData(), ref);
  }

  @Override
  public Concrete.LevelExpression visitSuc(Concrete.SucLevelExpression expr, LevelVariable type) {
    return new Concrete.SucLevelExpression(expr.getData(), expr.getExpression().accept(this, type));
  }

  @Override
  public Concrete.LevelExpression visitMax(Concrete.MaxLevelExpression expr, LevelVariable type) {
    return new Concrete.MaxLevelExpression(expr.getData(), expr.getLeft().accept(this, type), expr.getRight().accept(this, type));
  }

  @Override
  public Concrete.LevelExpression visitVar(Concrete.InferVarLevelExpression expr, LevelVariable param) {
    return expr;
  }
}
