package org.arend.core.elimtree;

import org.arend.core.constructor.IdpConstructor;
import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.body.CoreElimBody;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.patternmatching.Util;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElimBody implements Body, CoreElimBody {
  private final List<ElimClause<Pattern>> myClauses;
  private final ElimTree myElimTree;

  public ElimBody(List<ElimClause<Pattern>> clauses, ElimTree elimTree) {
    myElimTree = elimTree;
    myClauses = clauses;
  }

  @Override
  @NotNull
  public final List<? extends ElimClause<Pattern>> getClauses() {
    return myClauses;
  }

  @Override
  public @NotNull List<List<CorePattern>> computeRefinedPatterns(@NotNull CoreParameter parameters) {
    if (!(parameters instanceof DependentLink)) {
      throw new IllegalArgumentException();
    }
    List<List<CorePattern>> result = new ArrayList<>();
    LinkList linkList = new LinkList();
    computeRefinedPatterns(myElimTree, DependentLink.Helper.toList((DependentLink) parameters), new ExprSubstitution(), new ArrayList<>(), result, linkList);
    return result;
  }

  private static DependentLink copyDependentLink(DependentLink link, ExprSubstitution substitution, LinkList linkList) {
    TypedDependentLink result = new TypedDependentLink(link.isExplicit(), link.getName(), link.getType().subst(new SubstVisitor(substitution, LevelSubstitution.EMPTY)), link.isHidden(), EmptyDependentLink.getInstance());
    linkList.append(result);
    substitution.add(link, new ReferenceExpression(result));
    return result;
  }

  private static void computeRefinedPatterns(ElimTree elimTree, List<DependentLink> params, ExprSubstitution substitution, List<Util.ClauseElem> clauseElems, List<List<CorePattern>> result, LinkList linkList) {
    if (elimTree.getSkip() - 1 >= params.size()) {
      throw new IllegalArgumentException();
    }
    int originalSize = clauseElems.size();
    for (int i = 0; i < elimTree.getSkip(); i++) {
      clauseElems.add(new Util.PatternClauseElem(new BindingPattern(copyDependentLink(params.get(i), substitution, linkList))));
    }

    if (elimTree instanceof LeafElimTree) {
      List<CorePattern> patterns = new ArrayList<>();
      Util.unflattenClauses(new ArrayList<>(clauseElems), patterns);
      result.add(patterns);
    } else {
      if (elimTree.getSkip() >= params.size()) {
        throw new IllegalArgumentException();
      }

      DependentLink param = params.get(elimTree.getSkip());
      int originalSize1 = clauseElems.size();
      BranchElimTree branchElimTree = (BranchElimTree) elimTree;
      for (BranchKey key : branchElimTree.getKeys()) {
        Expression type = param.getTypeExpr().subst(substitution).normalize(NormalizationMode.WHNF);
        List<DependentLink> newParams = new ArrayList<>();

        ConstructorExpressionPattern conPattern;
        if (key instanceof Constructor) {
          if (!(type instanceof DataCallExpression)) {
            throw new IllegalArgumentException();
          }
          boolean isFin = ((DataCallExpression) type).getDefinition() == Prelude.FIN;
          Constructor constructor = isFin && key == Prelude.ZERO ? Prelude.FIN_ZERO : isFin && key == Prelude.SUC ? Prelude.FIN_SUC : (Constructor) key;
          List<ConCallExpression> conCalls = new ArrayList<>(1);
          ((DataCallExpression) type).getMatchedConCall(constructor, conCalls);
          if (conCalls.isEmpty()) {
            if (constructor.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
              throw new IllegalArgumentException();
            }
            continue;
          }
          DataCallExpression dataCall = (DataCallExpression) type;
          conPattern = new ConstructorExpressionPattern(new ConCallExpression(constructor, dataCall.getSortArgument(), dataCall.getDefCallArguments(), Collections.emptyList()), Collections.emptyList());
          clauseElems.add(new Util.ConstructorClauseElem(constructor, dataCall.getDefCallArguments()));
          newParams.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(constructor.getParameters(), new ExprSubstitution().add(constructor.getDataTypeParameters(), conCalls.get(0).getDataTypeArguments()), conCalls.get(0).getSortArgument().toLevelSubstitution())));
        } else if (key instanceof IdpConstructor) {
          conPattern = ConstructorPattern.make(Prelude.IDP, Collections.emptyList()).toExpressionPattern(type);
          clauseElems.add(new Util.PatternClauseElem(conPattern));
        } else {
          if (type instanceof SigmaExpression) {
            conPattern = new ConstructorExpressionPattern((SigmaExpression) type.subst(substitution), Collections.emptyList());
            clauseElems.add(Util.makeDataClauseElem(key, conPattern));
            newParams.addAll(DependentLink.Helper.toList(((SigmaExpression) type).getParameters()));
          } else if (type instanceof ClassCallExpression) {
            conPattern = new ConstructorExpressionPattern((ClassCallExpression) type.subst(substitution), Collections.emptyList());
            clauseElems.add(Util.makeDataClauseElem(key, conPattern));
            newParams.addAll(DependentLink.Helper.toList(((ClassCallExpression) type).getClassFieldParameters()));
          } else {
            throw new IllegalArgumentException();
          }
        }

        List<Expression> args = new ArrayList<>(newParams.size());
        for (Binding binding : newParams) {
          args.add(new ReferenceExpression(binding));
        }
        substitution.add(param, conPattern.toExpression(args));
        newParams.addAll(params.subList(elimTree.getSkip() + 1, params.size()));
        computeRefinedPatterns(branchElimTree.getChild(key), newParams, substitution, clauseElems, result, linkList);
        clauseElems.subList(originalSize1, clauseElems.size()).clear();
      }
    }

    clauseElems.subList(originalSize, clauseElems.size()).clear();
  }

  public final ElimTree getElimTree() {
    return myElimTree;
  }

  @Override
  public Decision isWHNF(List<? extends Expression> arguments) {
    return myElimTree.isWHNF(arguments);
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    return myElimTree.getStuckExpression(arguments, expression);
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof ElimBody && CompareVisitor.compare(DummyEquations.getInstance(), this, (ElimBody) other, null);
  }
}
