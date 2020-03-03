package org.arend.typechecking.subexpr;

import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.Expression;
import org.arend.ext.core.elimtree.CoreBranchKey;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.util.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class CorrespondedSubDefVisitor implements
    ConcreteDefinitionVisitor<Definition, Pair<Expression, Concrete.Expression>> {
  private final CorrespondedSubExprVisitor visitor;

  public CorrespondedSubDefVisitor(CorrespondedSubExprVisitor visitor) {
    this.visitor = visitor;
  }

  public CorrespondedSubDefVisitor(Concrete.Expression subExpr) {
    this(new CorrespondedSubExprVisitor(subExpr));
  }

  private Pair<Expression, Concrete.Expression> visitBody(Concrete.FunctionBody body, Body coreBody) {
    if (body instanceof Concrete.TermFunctionBody && coreBody instanceof Expression) {
      Concrete.Expression term = body.getTerm();
      if (term != null) return term.accept(visitor, (Expression) coreBody);
    } else if (body instanceof Concrete.ElimFunctionBody && coreBody instanceof BranchElimTree) {
      // We don't deal with complicated pattern matching until patterns are stored in core.
      if (body.getEliminatedReferences().size() > 1) return null;
      Collection<Map.Entry<CoreBranchKey, ElimTree>> caseTree = ((BranchElimTree) coreBody).getChildren();
      for (Concrete.FunctionClause clause : body.getClauses()) {
        // We know there's only one constructor.
        Concrete.Pattern pattern = clause.getPatterns().get(0);
        Concrete.Expression expression = clause.getExpression();
        if (expression == null) return null;
        if (pattern instanceof Concrete.ConstructorPattern) {
          Referable constructor = ((Concrete.ConstructorPattern) pattern).getConstructor();
          Predicate<Map.Entry<CoreBranchKey, ElimTree>> findElim = entry -> {
            CoreBranchKey key = entry.getKey();
            if (key instanceof Constructor) {
              return ((Constructor) key).getReferable() == constructor;
            } else return false;
          };
          Optional<Pair<Expression, Concrete.Expression>> result = caseTree.stream()
              .filter(entry -> entry.getValue() instanceof LeafElimTree)
              .filter(findElim)
              .map(Map.Entry::getValue)
              .map(elim -> (LeafElimTree) elim)
              .map(LeafElimTree::getExpression)
              .findFirst()
              .map(elim -> expression.accept(visitor, elim));
          if (result.isPresent()) return result.get();
        }
      }
    } else if (body instanceof Concrete.CoelimFunctionBody && coreBody instanceof BranchElimTree) {
    }
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitFunction(Concrete.BaseFunctionDefinition def, Definition params) {
    FunctionDefinition coreDef;
    if (params instanceof FunctionDefinition)
      coreDef = (FunctionDefinition) params;
    else return null;
    Concrete.Expression resultType = def.getResultType();
    Expression coreResultType = coreDef.getResultType();
    if (resultType != null && coreResultType != null) {
      Pair<Expression, Concrete.Expression> typeResult = resultType.accept(visitor, coreResultType);
      if (typeResult != null) return typeResult;
    }
    Pair<Expression, Concrete.Expression> parametersResult = visitor
        .visitSigmaParameters(def.getParameters(), coreDef.getParameters());
    if (parametersResult != null) return parametersResult;
    return visitBody(def.getBody(), coreDef.getActualBody());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitData(Concrete.DataDefinition def, Definition params) {
    DataDefinition coreDef;
    if (params instanceof DataDefinition) coreDef = (DataDefinition) params;
    else return null;
    return def.getConstructorClauses()
        .stream()
        .flatMap(clause -> clause.getConstructors().stream())
        .map(cons -> {
          Constructor coreC = coreDef.getConstructor(cons.getData());
          if (coreC == null) return null;
          return visitor.visitSigmaParameters(cons.getParameters(), coreC.getParameters());
        })
        .filter(Objects::nonNull)
        .findFirst()
        .orElseGet(() -> visitor
            .visitSigmaParameters(def.getParameters(), coreDef.getParameters()));
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitClass(Concrete.ClassDefinition def, Definition params) {
    // TODO :)
    return null;
  }
}
