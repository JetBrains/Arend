package org.arend.core.expr;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.subst.UnfoldVisitor;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.expr.*;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.variable.Variable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.IncorrectExpressionException;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.ExpressionMapper;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.reference.Precedence;
import org.arend.extImpl.UncheckedExpressionImpl;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.term.prettyprint.ToAbstractVisitor;
import org.arend.typechecking.error.local.TypeComputationError;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.patternmatching.ExpressionMatcher;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.FindSubexpressionVisitor;
import org.arend.typechecking.visitor.FixLevelParameters;
import org.arend.util.Decision;
import org.arend.util.GraphClosure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

public abstract class Expression implements Body, CoreExpression {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  public abstract <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    Concrete.Expression expr = ToAbstractVisitor.convert(this, new PrettyPrinterConfig() {
      @Override
      public NormalizationMode getNormalizationMode() {
        return null;
      }
    });
    expr.accept(new PrettyPrintVisitor(builder, 0), new Precedence(Concrete.Expression.PREC));
    return builder.toString();
  }

  @TestOnly
  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Expression && compare(this, (Expression) obj, null, CMP.EQ);
  }

  @Override
  public boolean isError() {
    ErrorExpression errorExpr = cast(ErrorExpression.class);
    return errorExpr != null && errorExpr.isError();
  }

  @Override
  public boolean reportIfError(@NotNull ErrorReporter errorReporter, @Nullable ConcreteSourceNode marker) {
    ErrorExpression errorExpr = cast(ErrorExpression.class);
    return errorExpr != null && errorExpr.reportIfError(errorReporter, marker);
  }

  @Override
  public void prettyPrint(StringBuilder builder, PrettyPrinterConfig config) {
    if (config.getNormalizationMode() != null) {
      FixLevelParameters.fix(this); // Expressions created in errors might have not fixed levels, so we fix them here
    }
    ToAbstractVisitor.convert(this, config).accept(new PrettyPrintVisitor(builder, 0, !config.isSingleLine()), new Precedence(Concrete.Expression.PREC));
  }

  @Override
  public Doc prettyPrint(PrettyPrinterConfig ppConfig) {
    return DocFactory.termDoc(this, ppConfig);
  }

  public boolean isLessOrEquals(Expression type, Equations equations, Concrete.SourceNode sourceNode) {
    return CompareVisitor.compare(equations, CMP.LE, this, type, Type.OMEGA, sourceNode);
  }

  @Override
  public int getNumberOfAbstractedBindings() {
    return 0;
  }

  public Expression pred() {
    return null;
  }

  public Sort toSort() {
    UniverseExpression universe = normalize(NormalizationMode.WHNF).cast(UniverseExpression.class);
    return universe == null ? null : universe.getSort();
  }

  public Sort getSortOfType() {
    Expression type = getType();
    return type == null ? null : type.toSort();
  }

  public Expression getType(boolean normalizing) {
    if (normalizing) {
      try {
        return accept(GetTypeVisitor.INSTANCE, null);
      } catch (IncorrectExpressionException e) {
        return new ErrorExpression(new TypeComputationError(null, this, null));
      }
    } else {
      return accept(GetTypeVisitor.NN_INSTANCE, null);
    }
  }

  public Expression getType() {
    return getType(true);
  }

  @Override
  public @NotNull Expression computeType() {
    Expression type = getType(true);
    return type != null ? type : new ErrorExpression(new TypeComputationError(null, this, null));
  }

  @Override
  public @NotNull TypecheckingResult computeTyped() {
    return new TypecheckingResult(this, computeType());
  }

  public boolean findBinding(Variable binding) {
    return accept(new FindBindingVisitor(Collections.singleton(binding)), null);
  }

  @Override
  public boolean findFreeBinding(@NotNull CoreBinding binding) {
    if (!(binding instanceof Binding)) {
      return false;
    }
    return accept(new FindBindingVisitor(Collections.singleton((Binding) binding)), null);
  }

  @Override
  public @Nullable CoreBinding findFreeBinding(@NotNull Set<? extends CoreBinding> bindings) {
    if (bindings.isEmpty()) return null;
    FindBindingVisitor visitor = new FindBindingVisitor(bindings);
    return accept(visitor, null) ? (CoreBinding) visitor.getResult() : null;
  }

  public Variable findBinding(Set<? extends Variable> bindings) {
    FindBindingVisitor visitor = new FindBindingVisitor(bindings);
    return accept(visitor, null) ? visitor.getResult() : null;
  }

  @Override
  public @Nullable CoreBinding findFreeBindings(@NotNull Set<? extends CoreBinding> bindings) {
    if (bindings.isEmpty()) {
      return null;
    }
    FindBindingVisitor visitor = new FindBindingVisitor(bindings);
    return accept(visitor, null) ? (CoreBinding) visitor.getResult() : null;
  }

  @Override
  public @NotNull Set<? extends CoreBinding> findFreeBindings() {
    return FreeVariablesCollector.getFreeVariables(this);
  }

  public Expression copy() {
    return accept(new SubstVisitor(new ExprSubstitution(), LevelSubstitution.EMPTY), null);
  }

  public final Expression subst(Binding binding, Expression substExpr) {
    if (substExpr instanceof ReferenceExpression && ((ReferenceExpression) substExpr).getBinding() == binding) {
      return this;
    }
    return accept(new SubstVisitor(new ExprSubstitution(binding, substExpr), LevelSubstitution.EMPTY), null);
  }

  public final Expression subst(ExprSubstitution subst) {
     return subst.isEmpty() ? this : subst(subst, LevelSubstitution.EMPTY);
  }

  public Expression subst(LevelSubstitution subst) {
    return subst.isEmpty() ? this : subst(new ExprSubstitution(), subst);
  }

  public Expression subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    return exprSubst.isEmpty() && levelSubst.isEmpty() ? this : accept(new SubstVisitor(exprSubst, levelSubst), null);
  }

  public Type subst(SubstVisitor substVisitor) {
    Expression result = substVisitor.isEmpty() ? this : accept(substVisitor, null);
    if (result instanceof Type) {
      return (Type) result;
    }

    Sort sort = result.getSortOfType();
    if (sort == null) {
      throw new SubstVisitor.SubstException();
    }
    return new TypeExpression(result, sort);
  }

  @NotNull
  @Override
  public Expression normalize(@NotNull NormalizationMode mode) {
    return accept(NormalizeVisitor.INSTANCE, mode);
  }

  @Override
  public @NotNull CoreExpression unfold(@NotNull Set<? extends Variable> functions, @Nullable Set<Variable> unfolded, boolean unfoldLet, boolean unfoldFields) {
    return functions.isEmpty() && !unfoldLet && !unfoldFields ? this : accept(new UnfoldVisitor(functions, unfolded, unfoldLet, unfoldFields ? UnfoldVisitor.UnfoldFields.ALL_FIELDS : UnfoldVisitor.UnfoldFields.ONLY_SPECIFIED), null);
  }

  @Nullable
  @Override
  public UncheckedExpressionImpl replaceSubexpressions(@NotNull ExpressionMapper mapper) {
    try {
      return new UncheckedExpressionImpl(accept(new RecreateExpressionVisitor(mapper), null));
    } catch (SubstVisitor.SubstException e) {
      return null;
    }
  }

  @Override
  public @NotNull UncheckedExpression substitute(@NotNull Map<? extends CoreBinding, ? extends UncheckedExpression> map) {
    if (map.isEmpty()) {
      return this;
    }
    ExprSubstitution substitution = new ExprSubstitution();
    for (Map.Entry<? extends CoreBinding, ? extends UncheckedExpression> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof Binding)) {
        throw new IllegalArgumentException();
      }
      substitution.add((Binding) entry.getKey(), UncheckedExpressionImpl.extract(entry.getValue()));
    }
    return new UncheckedExpressionImpl(accept(new SubstVisitor(substitution, LevelSubstitution.EMPTY), null));
  }

  @Override
  public @Nullable Expression lambdaToPi() {
    Expression expr = getUnderlyingExpression();
    if (expr instanceof LamExpression) {
      Expression cod = ((LamExpression) expr).getBody().lambdaToPi();
      return cod == null ? null : new PiExpression(((LamExpression) expr).getResultSort(), ((LamExpression) expr).getParameters(), cod);
    } else {
      return expr.getSortOfType() == null ? null : expr;
    }
  }

  @Override
  public boolean processSubexpression(@NotNull Function<CoreExpression, FindAction> function) {
    return accept(new FindSubexpressionVisitor(function), null);
  }

  public static boolean compare(Expression expr1, Expression expr2, Expression type, CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, expr1, expr2, type, null);
  }

  @Override
  public boolean compare(@NotNull UncheckedExpression expr2, @NotNull CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, this, UncheckedExpressionImpl.extract(expr2), null, null);
  }

  @Override
  public @Nullable Expression removeUnusedBinding(@NotNull CoreBinding binding) {
    if (!(binding instanceof Binding)) {
      throw new IllegalArgumentException();
    }
    return ElimBindingVisitor.elimBinding(this, (Binding) binding);
  }

  @Nullable
  @Override
  public Expression removeConstLam() {
    return ElimBindingVisitor.elimLamBinding(normalize(NormalizationMode.WHNF).cast(LamExpression.class));
  }

  @Nullable
  @Override
  public FunCallExpression toEquality() {
    Expression expr = getUnderlyingExpression();
    if (expr instanceof FunCallExpression && ((FunCallExpression) expr).getDefinition() == Prelude.PATH_INFIX) {
      return (FunCallExpression) expr;
    }
    DataCallExpression dataCall = expr instanceof DataCallExpression ? (DataCallExpression) expr : expr.normalize(NormalizationMode.WHNF).cast(DataCallExpression.class);
    if (dataCall != null && dataCall.getDefinition() == Prelude.PATH) {
      List<Expression> args = dataCall.getDefCallArguments();
      Expression type = args.get(0).removeConstLam();
      if (type != null) {
        return FunCallExpression.makeFunCall(Prelude.PATH_INFIX, dataCall.getLevels(), Arrays.asList(type, args.get(1), args.get(2)));
      }
    }

    return null;
  }

  private static boolean addConstructor(Expression expr, Constructor constructor, GraphClosure<Constructor> closure) {
    if (expr == null) {
      return true;
    }
    if (expr instanceof ConCallExpression) {
      closure.addSymmetric(((ConCallExpression) expr).getDefinition(), constructor);
      return true;
    } else {
      return false;
    }
  }

  private static boolean checkInteger(BigInteger n, UncheckedExpression expr) {
    if (expr instanceof IntegerExpression) {
      return !n.equals(((IntegerExpression) expr).getBigInteger());
    }
    if (!(expr instanceof ConCallExpression)) {
      return false;
    }

    ConCallExpression conCall = (ConCallExpression) expr;
    if (conCall.getDefinition() == Prelude.ZERO) {
      return !n.equals(BigInteger.ZERO);
    }
    if (conCall.getDefinition() == Prelude.SUC) {
      return n.equals(BigInteger.ZERO) || checkInteger(n.subtract(BigInteger.ONE), conCall.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF));
    }
    return false;
  }

  @Override
  public boolean areDisjointConstructors(@NotNull UncheckedExpression expr2) {
    Expression expr1 = normalize(NormalizationMode.WHNF);
    expr2 = UncheckedExpressionImpl.extract(expr2).normalize(NormalizationMode.WHNF);
    if (expr1 instanceof IntegerExpression) {
      return checkInteger(((IntegerExpression) expr1).getBigInteger(), expr2);
    }
    if (expr2 instanceof IntegerExpression) {
      return checkInteger(((IntegerExpression) expr2).getBigInteger(), expr1);
    }
    if (expr1 instanceof ArrayExpression && expr2 instanceof ArrayExpression) {
      ArrayExpression array1 = (ArrayExpression) expr1;
      ArrayExpression array2 = (ArrayExpression) expr2;
      if (array1.getTail() == null && array1.getElements().size() < array2.getElements().size() || array2.getTail() == null && array2.getElements().size() < array1.getElements().size()) {
        return true;
      }
      for (int i = 0; i < array1.getElements().size() && i < array2.getElements().size(); i++) {
        if (array1.getElements().get(i).areDisjointConstructors(array2.getElements().get(i))) {
          return true;
        }
      }
      return array1.getTail() != null && Boolean.TRUE.equals(ConstructorExpressionPattern.isArrayEmpty(array1.getTail().getType())) || array2.getTail() != null && Boolean.TRUE.equals(ConstructorExpressionPattern.isArrayEmpty(array2.getTail().getType()));
    }
    if (!(expr1 instanceof ConCallExpression) || !(expr2 instanceof ConCallExpression)) {
      return false;
    }

    ConCallExpression conCall1 = (ConCallExpression) expr1;
    ConCallExpression conCall2 = (ConCallExpression) expr2;
    Constructor con1 = conCall1.getDefinition();
    Constructor con2 = conCall2.getDefinition();
    if (con1.getDataType() != con2.getDataType() || con1.getDataType() == Prelude.INTERVAL) {
      return false;
    }

    if (con1.getDataType().isHIT()) {
      GraphClosure<Constructor> closure = new GraphClosure<>();
      for (Constructor constructor : con1.getDataType().getConstructors()) {
        Body body = constructor.getBody();
        if (body instanceof IntervalElim) {
          for (IntervalElim.CasePair pair : ((IntervalElim) body).getCases()) {
            if (!addConstructor(pair.proj1, constructor, closure) || !addConstructor(pair.proj2, constructor, closure)) {
              return false;
            }
          }
          body = ((IntervalElim) body).getOtherwise();
        }
        if (body instanceof ElimBody) {
          for (ElimClause<Pattern> clause : ((ElimBody) body).getClauses()) {
            if (!addConstructor(clause.getExpression(), constructor, closure)) {
              return false;
            }
          }
        } else if (body != null) {
          throw new IllegalStateException();
        }
      }

      return !closure.areEquivalent(con1, con2);
    } else {
      List<ConCallExpression> conCalls1 = new ArrayList<>();
      conCalls1.add(conCall1);
      List<ConCallExpression> conCalls2 = new ArrayList<>();
      conCalls2.add(conCall2);
      if (!computeClosure(conCalls1) || !computeClosure(conCalls2)) {
        return false;
      }
      for (ConCallExpression cc1 : conCalls1) {
        for (ConCallExpression cc2 : conCalls2) {
          if (cc1.getDefinition() == cc2.getDefinition()) {
            boolean ok = false;
            for (int i = 0; i < cc1.getDefCallArguments().size(); i++) {
              if (cc1.getDefCallArguments().get(i).areDisjointConstructors(cc2.getDefCallArguments().get(i))) {
                ok = true;
                break;
              }
            }
            if (!ok) {
              return false;
            }
          }
        }
      }
      return true;
    }
  }

  private boolean computeClosure(List<ConCallExpression> conCalls) {
    for (int i = 0; i < conCalls.size(); i++) {
      ConCallExpression conCall = conCalls.get(i);
      Body body = conCall.getDefinition().getBody();
      List<Expression> rhsList;
      if (body instanceof Expression) {
        rhsList = Collections.singletonList(((Expression) body).subst(new ExprSubstitution().add(conCall.getDefinition().getParameters(), conCall.getDefCallArguments())).normalize(NormalizationMode.WHNF));
      } else if (body instanceof ElimBody) {
        rhsList = new ArrayList<>();
        loop:
        for (ElimClause<Pattern> clause : ((ElimBody) body).getClauses()) {
          if (clause.getExpression() == null) continue;
          List<ExpressionMatcher.MatchResult> matchResults = new ArrayList<>();
          if (ExpressionMatcher.matchExpressions(conCall.getDefCallArguments(), clause.getPatterns(), false, matchResults) != null) {
            ExprSubstitution substitution = new ExprSubstitution();
            for (ExpressionMatcher.MatchResult matchResult : matchResults) {
              if (matchResult.expression instanceof ConCallExpression && matchResult.pattern.getDefinition() != null && ((ConCallExpression) matchResult.expression).getDefinition() != matchResult.pattern.getDefinition()) {
                continue loop;
              }
              if (matchResult.expression instanceof IntegerExpression && (matchResult.pattern.getDefinition() == Prelude.ZERO) != ((IntegerExpression) matchResult.expression).isZero()) {
                continue loop;
              }
              substitution.add(matchResult.binding, matchResult.expression);
            }
            rhsList.add(clause.getExpression().subst(substitution).normalize(NormalizationMode.WHNF));
          }
        }
      } else {
        continue;
      }

      for (Expression rhs : rhsList) {
        if (!(rhs instanceof ConCallExpression)) {
          return false;
        }
        conCalls.add((ConCallExpression) rhs);
      }
    }

    return true;
  }

  public Expression dropPiParameter(int n) {
    if (n == 0) {
      return this;
    }

    Expression cod = normalize(NormalizationMode.WHNF);
    for (PiExpression piCod = cod.cast(PiExpression.class); piCod != null; piCod = cod.cast(PiExpression.class)) {
      SingleDependentLink link = piCod.getParameters();
      while (n > 0 && link.hasNext()) {
        link = link.getNext();
        n--;
      }
      if (n == 0) {
        return link.hasNext() ? new PiExpression(piCod.getResultSort(), link, piCod.getCodomain()) : piCod.getCodomain();
      }
      cod = piCod.getCodomain().normalize(NormalizationMode.WHNF);
    }

    return null;
  }

  @Override
  public @NotNull CoreExpression getPiParameters(@Nullable List<? super CoreParameter> parameters) {
    return getPiParameters(parameters, false);
  }

  public Expression getPiParameters(List<? super SingleDependentLink> params, boolean implicitOnly) {
    Expression cod = normalize(NormalizationMode.WHNF);
    return cod instanceof PiExpression ? cod.getPiParameters(params, implicitOnly) : cod;
  }

  public Expression normalizePi(List<? super SingleDependentLink> parameters) {
    Expression expr = normalize(NormalizationMode.WHNF);
    if (!(expr instanceof PiExpression)) {
      return expr;
    }

    List<PiExpression> piExprs = new ArrayList<>();
    while (expr instanceof PiExpression) {
      PiExpression piExpr = (PiExpression) expr;
      piExprs.add(piExpr);
      if (parameters != null) {
        for (SingleDependentLink link = piExpr.getParameters(); link.hasNext(); link = link.getNext()) {
          parameters.add(link);
        }
      }
      expr = piExpr.getCodomain();
    }

    for (int i = piExprs.size() - 1; i >= 0; i--) {
      expr = new PiExpression(piExprs.get(i).getResultSort(), piExprs.get(i).getParameters(), expr);
    }
    return expr;
  }

  public Expression getLamParameters(List<DependentLink> params) {
    Expression body = this;
    for (LamExpression lamBody = body.cast(LamExpression.class); lamBody != null; lamBody = body.cast(LamExpression.class)) {
      if (params != null) {
        for (DependentLink link = lamBody.getParameters(); link.hasNext(); link = link.getNext()) {
          params.add(link);
        }
      }
      body = lamBody.getBody();
    }
    return body;
  }

  public Expression applyExpression(Expression expression) {
    return applyExpression(expression, true);
  }

  public Expression applyExpression(Expression expression, boolean normalizing) {
    Expression normExpr = (normalizing ? normalize(NormalizationMode.WHNF) : this).getUnderlyingExpression();
    return normExpr instanceof ErrorExpression ? normExpr : normExpr instanceof PiExpression ? normExpr.applyExpression(expression, normalizing) : null;
  }

  public boolean canBeConstructor() {
    return true;
  }

  public boolean isBoxed() {
    return false;
  }

  @NotNull
  @Override
  public Expression getUnderlyingExpression() {
    return this;
  }

  public <T extends Expression> boolean isInstance(Class<T> clazz) {
    return clazz.isInstance(this);
  }

  public <T extends Expression> T cast(Class<T> clazz) {
    return clazz.isInstance(this) ? clazz.cast(this) : null;
  }

  public Expression getFunction() {
    AppExpression app = cast(AppExpression.class);
    return app != null ? app.getFunction() : this;
  }

  public Expression getArguments(List<Expression> args) {
    return this;
  }

  @Override
  public Decision isWHNF(List<? extends Expression> arguments) {
    return Decision.NO;
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    return null;
  }

  public abstract Decision isWHNF();

  // This function assumes that the expression is in a WHNF.
  // If the expression is a constructor, then the function returns null.
  @Override
  public abstract Expression getStuckExpression();

  public InferenceVariable getStuckInferenceVariable() {
    Expression stuck = getStuckExpression();
    return stuck instanceof InferenceReferenceExpression && ((InferenceReferenceExpression) stuck).getVariable() != null ? ((InferenceReferenceExpression) stuck).getVariable() : null;
  }

  public InferenceVariable getInferenceVariable() {
    Expression expr = this;
    while (true) {
      expr = expr.cast(InferenceReferenceExpression.class);
      if (expr == null) {
        return null;
      }
      InferenceVariable var = ((InferenceReferenceExpression) expr).getVariable();
      if (var != null) {
        return var;
      }
      expr = ((InferenceReferenceExpression) expr).getSubstExpression();
      if (expr == null) {
        return null;
      }
    }
  }

  @Override
  public boolean computeMatchedConstructorsWithDataArguments(List<? super ConstructorWithDataArguments> result) {
    return false;
  }

  @Override
  public @Nullable List<ConstructorWithDataArguments> computeMatchedConstructorsWithDataArguments() {
    return null;
  }
}
