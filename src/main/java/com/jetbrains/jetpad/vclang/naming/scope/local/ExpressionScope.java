package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.ClassFieldImplScope;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.abs.Abstract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExpressionScope {
  public static Scope localScope(Scope parentScope, Abstract.SourceNode sourceNode) {
    if (sourceNode == null) {
      return parentScope;
    }

    // We cannot use any references in level expressions
    if (sourceNode instanceof Abstract.LevelExpression) {
      return EmptyScope.INSTANCE;
    }

    // We can use only global definitions in patterns
    if (sourceNode instanceof Abstract.Pattern) {
      return parentScope.getGlobalSubscope();
    }

    Abstract.SourceNode parentSourceNode = sourceNode.getParentSourceNode();

    // We cannot use any references in the universe of a data type
    if (parentSourceNode instanceof Abstract.DataDefinition && sourceNode instanceof Abstract.Expression) {
      return EmptyScope.INSTANCE;
    }

    if (parentSourceNode instanceof Abstract.EliminatedExpressionsHolder) {
      // We can use only parameters in eliminated expressions
      if (sourceNode instanceof Abstract.Reference) {
        Collection<? extends Abstract.Reference> elimExprs = ((Abstract.EliminatedExpressionsHolder) parentSourceNode).getEliminatedExpressions();
        if (elimExprs != null) {
          for (Abstract.Reference elimExpr : elimExprs) {
            if (sourceNode.equals(elimExpr)) {
              return new TelescopeScope(EmptyScope.INSTANCE, ((Abstract.FunctionDefinition) parentSourceNode).getParameters());
            }
          }
        }
      }

      // Remove eliminated expressions from the scope in clauses
      if (sourceNode instanceof Abstract.Clause) {
        Collection<? extends Abstract.Reference> elimExprs = ((Abstract.EliminatedExpressionsHolder) parentSourceNode).getEliminatedExpressions();
        if (elimExprs != null) {
          if (elimExprs.isEmpty()) {
            return parentScope;
          } else {
            List<Referable> excluded = new ArrayList<>(elimExprs.size());
            Scope parametersScope = new TelescopeScope(EmptyScope.INSTANCE, ((Abstract.EliminatedExpressionsHolder) parentSourceNode).getParameters());
            for (Abstract.Reference elimExpr : elimExprs) {
              Referable referable = elimExpr.getReferent();
              if (referable instanceof UnresolvedReference) {
                referable = ((UnresolvedReference) referable).resolve(parametersScope, null);
              }
              excluded.add(referable);
            }
            return new TelescopeScope(parentScope, ((Abstract.EliminatedExpressionsHolder) parentSourceNode).getParameters(), excluded);
          }
        }
      }
    }

    // Extend the scope with parameters
    if (parentSourceNode instanceof Abstract.ParametersHolder) {
      List<? extends Abstract.Parameter> parameters = ((Abstract.ParametersHolder) parentSourceNode).getParameters();
      if (sourceNode instanceof Abstract.Parameter && !(sourceNode instanceof Abstract.Expression)) {
        List<Abstract.Parameter> parameters1 = new ArrayList<>(parameters.size());
        for (Abstract.Parameter parameter : parameters) {
          if (sourceNode.equals(parameter)) {
            break;
          }
          parameters1.add(parameter);
        }
        return new TelescopeScope(parentScope, parameters1);
      } else {
        return new TelescopeScope(parentScope, parameters);
      }
    }

    // Extend the scope with let clauses
    if (parentSourceNode instanceof Abstract.LetClausesHolder) {
      Collection<? extends Abstract.LetClause> clauses = ((Abstract.LetClausesHolder) parentSourceNode).getLetClauses();
      List<Abstract.LetClause> clauses1;
      if (sourceNode instanceof Abstract.LetClause) {
        clauses1 = new ArrayList<>(clauses.size());
        for (Abstract.LetClause clause : clauses) {
          if (sourceNode.equals(clause)) {
            break;
          }
          clauses1.add(clause);
        }
      } else {
        clauses1 = new ArrayList<>(clauses);
      }
      return new LetScope(parentScope, clauses1);
    }

    // Replace the scope with class fields in class extensions
    if (parentSourceNode instanceof Abstract.ClassFieldImpl && !(sourceNode instanceof Abstract.Expression)) {
      Abstract.SourceNode parentParent = parentSourceNode.getParentSourceNode();
      if (parentParent instanceof Abstract.ClassReferenceHolder) {
        return new ClassFieldImplScope(((Abstract.ClassReferenceHolder) parentParent).getClassReference());
      }
    }

    // Extend the scope with patterns
    if (parentSourceNode instanceof Abstract.Clause && sourceNode instanceof Abstract.Expression) {
      return new PatternScope(parentScope, ((Abstract.Clause) parentSourceNode).getPatterns());
    }

    return parentScope;
  }
}
