package org.arend.core.elimtree;

import org.arend.core.constructor.IdpConstructor;
import org.arend.core.context.LinkList;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.Pattern;
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
    computeRefinedPatterns(myElimTree, DependentLink.Helper.toList((DependentLink) parameters), new ArrayList<>(), result, linkList);
    return result;
  }

  private static DependentLink copyDependentLink(DependentLink link, LinkList linkList) {
    TypedDependentLink result = new TypedDependentLink(link.isExplicit(), link.getName(), link.getType(), link.isHidden(), EmptyDependentLink.getInstance());
    linkList.append(result);
    return result;
  }

  private static void computeRefinedPatterns(ElimTree elimTree, List<DependentLink> params, List<Util.ClauseElem> clauseElems, List<List<CorePattern>> result, LinkList linkList) {
    if (elimTree.getSkip() - 1 >= params.size()) {
      throw new IllegalArgumentException();
    }
    int originalSize = clauseElems.size();
    for (int i = 0; i < elimTree.getSkip(); i++) {
      clauseElems.add(new Util.PatternClauseElem(new BindingPattern(copyDependentLink(params.get(i), linkList))));
    }

    if (elimTree instanceof LeafElimTree) {
      List<CorePattern> patterns = new ArrayList<>();
      Util.unflattenClauses(new ArrayList<>(clauseElems), patterns);
      result.add(patterns);
    } else {
      if (elimTree.getSkip() >= params.size()) {
        throw new IllegalArgumentException();
      }

      int originalSize1 = clauseElems.size();
      BranchElimTree branchElimTree = (BranchElimTree) elimTree;
      for (BranchKey key : branchElimTree.getKeys()) {
        List<DependentLink> newParams = new ArrayList<>();
        if (key instanceof Constructor) {
          clauseElems.add(new Util.ConstructorClauseElem((Constructor) key));
          newParams.addAll(DependentLink.Helper.toList(key.getParameters()));
        } else if (key instanceof IdpConstructor) {
          clauseElems.add(new Util.PatternClauseElem(ConstructorPattern.make(Prelude.IDP, Collections.emptyList()).toExpressionPattern(params.get(elimTree.getSkip()).getTypeExpr().normalize(NormalizationMode.WHNF))));
        } else {
          clauseElems.add(new Util.PatternClauseElem(new BindingPattern(copyDependentLink(params.get(elimTree.getSkip()), linkList))));
        }
        newParams.addAll(params.subList(elimTree.getSkip() + 1, params.size()));
        computeRefinedPatterns(branchElimTree.getChild(key), newParams, clauseElems, result, linkList);
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
