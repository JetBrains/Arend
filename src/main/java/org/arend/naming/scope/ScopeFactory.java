package org.arend.naming.scope;

import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.ErrorReference;
import org.arend.naming.reference.LongUnresolvedReference;
import org.arend.naming.reference.Referable;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.local.LetScope;
import org.arend.naming.scope.local.ListScope;
import org.arend.naming.scope.local.PatternScope;
import org.arend.naming.scope.local.TelescopeScope;
import org.arend.prelude.Prelude;
import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScopeFactory {
  public static @Nonnull Scope forGroup(@Nullable Group group, @Nonnull ModuleScopeProvider moduleScopeProvider) {
    return forGroup(group, moduleScopeProvider, true);
  }

  public static @Nonnull Scope parentScopeForGroup(@Nullable Group group, @Nonnull ModuleScopeProvider moduleScopeProvider, boolean prelude) {
    ChildGroup parentGroup = group instanceof ChildGroup ? ((ChildGroup) group).getParentGroup() : null;
    Scope parentScope;
    if (parentGroup == null) {
      if (prelude && group != null) {
        for (NamespaceCommand command : group.getNamespaceCommands()) {
          if (command.getKind() == NamespaceCommand.Kind.IMPORT && command.getPath().equals(Prelude.MODULE_PATH.toList())) {
            prelude = false;
          }
        }
      }
      Scope preludeScope = prelude ? moduleScopeProvider.forModule(Prelude.MODULE_PATH) : null;
      if (group == null) {
        return preludeScope == null ? EmptyScope.INSTANCE : preludeScope;
      }

      ImportedScope importedScope = new ImportedScope(group, moduleScopeProvider);
      parentScope = preludeScope == null ? importedScope : new MergeScope(preludeScope, importedScope);
    } else {
      parentScope = forGroup(parentGroup, moduleScopeProvider, prelude);
    }
    return parentScope;
  }

  public static @Nonnull Scope forGroup(@Nullable Group group, @Nonnull ModuleScopeProvider moduleScopeProvider, boolean prelude) {
    return LexicalScope.insideOf(group, parentScopeForGroup(group, moduleScopeProvider, prelude));
  }

  public static boolean isGlobalScopeVisible(Abstract.SourceNode sourceNode) {
    while (sourceNode != null && !(sourceNode instanceof Abstract.Definition || sourceNode instanceof Abstract.NamespaceCommandHolder)) {
      // We cannot use any references in level expressions
      if (sourceNode instanceof Abstract.LevelExpression) {
        return false;
      }

      // We can use only constructors in patterns
      if (sourceNode instanceof Abstract.Pattern) {
        return false;
      }

      Abstract.SourceNode parentSourceNode = sourceNode.getParentSourceNode();
      if (parentSourceNode instanceof Abstract.Expression && sourceNode instanceof Abstract.Reference) {
        sourceNode = parentSourceNode;
        parentSourceNode = sourceNode.getParentSourceNode();
      }

      // After namespace command
      if (parentSourceNode instanceof Abstract.NamespaceCommandHolder && sourceNode instanceof Abstract.Reference) {
        if (((Abstract.NamespaceCommandHolder) parentSourceNode).getKind() == NamespaceCommand.Kind.IMPORT) {
          return false;
        }
        return sourceNode.equals(((Abstract.NamespaceCommandHolder) parentSourceNode).getOpenedReference());
      }

      // After a dot
      if (parentSourceNode instanceof Abstract.LongReference && sourceNode instanceof Abstract.Reference) {
        Abstract.Reference headRef = ((Abstract.LongReference) parentSourceNode).getHeadReference();
        if (headRef != null && !sourceNode.equals(headRef)) {
          return false;
        }
      } else

      // We cannot use any references in the universe of a data type
      if (parentSourceNode instanceof Abstract.DataDefinition && sourceNode instanceof Abstract.Expression) {
        return false;
      } else

      if (parentSourceNode instanceof Abstract.EliminatedExpressionsHolder) {
        // We can use only parameters in eliminated expressions
        if (sourceNode instanceof Abstract.Reference) {
          Collection<? extends Abstract.Reference> elimExprs = ((Abstract.EliminatedExpressionsHolder) parentSourceNode).getEliminatedExpressions();
          if (elimExprs != null) {
            for (Abstract.Reference elimExpr : elimExprs) {
              if (sourceNode.equals(elimExpr)) {
                return false;
              }
            }
          }
        }
      } else

      // Class extensions
      if ((parentSourceNode instanceof Abstract.ClassFieldImpl && !(sourceNode instanceof Abstract.Expression) || parentSourceNode instanceof Abstract.ClassFieldSynonym && sourceNode instanceof Abstract.Reference) && parentSourceNode.getParentSourceNode() instanceof Abstract.ClassReferenceHolder) {
        return false;
      }

      sourceNode = parentSourceNode;
    }

    return true;
  }

  public static Scope forSourceNode(Scope parentScope, Abstract.SourceNode sourceNode, Scope importElements) {
    if (sourceNode == null) {
      return parentScope;
    }

    // We cannot use any references in level expressions
    if (sourceNode instanceof Abstract.LevelExpression) {
      return EmptyScope.INSTANCE;
    }

    // We can use only constructors in patterns
    if (sourceNode instanceof Abstract.Pattern) {
      return new ConstructorFilteredScope(parentScope.getGlobalSubscope());
    }

    Abstract.SourceNode parentSourceNode = sourceNode.getParentSourceNode();
    if (parentSourceNode instanceof Abstract.Expression && sourceNode instanceof Abstract.Reference) {
      sourceNode = parentSourceNode;
      parentSourceNode = sourceNode.getParentSourceNode();
    }

    // After namespace command
    if (parentSourceNode instanceof Abstract.NamespaceCommandHolder && sourceNode instanceof Abstract.Reference) {
      Scope scope;
      if (((Abstract.NamespaceCommandHolder) parentSourceNode).getKind() == NamespaceCommand.Kind.IMPORT) {
        ImportedScope importedScope = parentScope.getImportedSubscope();
        if (importedScope != null && importElements != null) {
          importedScope = new ImportedScope(importedScope, importElements);
        }
        scope = importedScope == null ? (importElements == null ? EmptyScope.INSTANCE : importElements) : importedScope;
      } else {
        scope = parentScope.getGlobalSubscopeWithoutOpens();
      }
      if (sourceNode.equals(((Abstract.NamespaceCommandHolder) parentSourceNode).getOpenedReference())) {
        return scope;
      } else {
        scope = scope.resolveNamespace(((Abstract.NamespaceCommandHolder) parentSourceNode).getPath());
        return scope == null ? EmptyScope.INSTANCE : scope;
      }
    }

    // After a dot
    if (parentSourceNode instanceof Abstract.LongReference && sourceNode instanceof Abstract.Reference) {
      Abstract.Reference headRef = ((Abstract.LongReference) parentSourceNode).getHeadReference();
      if (headRef == null || sourceNode.equals(headRef)) {
        return parentScope;
      }

      List<String> path = new ArrayList<>();
      path.add(headRef.getReferent().textRepresentation());
      for (Abstract.Reference reference : ((Abstract.LongReference) parentSourceNode).getTailReferences()) {
        if (reference == null) {
          return EmptyScope.INSTANCE;
        }
        if (sourceNode.equals(reference)) {
          return new LongUnresolvedReference(sourceNode, path).resolveNamespaceWithArgument(parentScope);
        }
        path.add(reference.getReferent().textRepresentation());
      }

      return EmptyScope.INSTANCE;
    }

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
              return new TelescopeScope(EmptyScope.INSTANCE, ((Abstract.EliminatedExpressionsHolder) parentSourceNode).getParameters());
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
              Referable ref = ExpressionResolveNameVisitor.resolve(elimExpr.getReferent(), parametersScope);
              if (!(ref == null || ref instanceof ErrorReference)) {
                excluded.add(ref);
              }
            }
            return new TelescopeScope(parentScope, ((Abstract.EliminatedExpressionsHolder) parentSourceNode).getParameters(), excluded);
          }
        }
      }
    }

    // Replace the scope with class fields in class extensions and class field synonyms
    if (parentSourceNode instanceof Abstract.ClassFieldImpl && sourceNode instanceof Abstract.Reference || parentSourceNode instanceof Abstract.ClassFieldSynonym && sourceNode instanceof Abstract.Reference) {
      Abstract.SourceNode parentParent = parentSourceNode.getParentSourceNode();
      if (parentParent instanceof Abstract.ClassReferenceHolder) {
        ClassReferable classRef = ((Abstract.ClassReferenceHolder) parentParent).getClassReference();
        return classRef == null ? EmptyScope.INSTANCE : new ClassFieldImplScope(classRef, true);
      }
    }

    // Extend the scope with parameters
    if (parentSourceNode instanceof Abstract.ParametersHolder) {
      if (sourceNode instanceof Abstract.Reference && parentSourceNode instanceof Abstract.InstanceDefinition) {
        return parentScope;
      }

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

    // Extend the scope with case arguments
    if (sourceNode instanceof Abstract.Expression && (parentSourceNode instanceof Abstract.CaseArgumentsHolder || parentSourceNode instanceof Abstract.CaseArgument && sourceNode.equals(((Abstract.CaseArgument) parentSourceNode).getType()))) {
      Abstract.CaseArgumentsHolder caseArgumentsHolder;
      if (parentSourceNode instanceof Abstract.CaseArgumentsHolder) {
        caseArgumentsHolder = (Abstract.CaseArgumentsHolder) parentSourceNode;
      } else {
        Abstract.SourceNode parentParent = parentSourceNode.getParentSourceNode();
        if (!(parentParent instanceof Abstract.CaseArgumentsHolder)) {
          return parentScope;
        }
        caseArgumentsHolder = (Abstract.CaseArgumentsHolder) parentParent;
      }

      List<? extends Abstract.CaseArgument> caseArguments = caseArgumentsHolder.getCaseArguments();
      List<Referable> referables = new ArrayList<>(caseArguments.size());
      for (Abstract.CaseArgument caseArgument : caseArguments) {
        if (parentSourceNode instanceof Abstract.CaseArgument && parentSourceNode.equals(caseArgument)) {
          break;
        }
        Referable ref = caseArgument.getReferable();
        if (ref != null) {
          referables.add(ref);
        }
      }
      return referables.isEmpty() ? parentScope : new ListScope(parentScope, referables);
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

    // Extend the scope with patterns
    if (parentSourceNode instanceof Abstract.Clause) {
      return new PatternScope(parentScope, ((Abstract.Clause) parentSourceNode).getPatterns());
    }

    return parentScope;
  }
}
