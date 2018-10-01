package org.arend.typechecking.order.dependency;

import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.typechecking.visitor.CollectDefCallsVisitor;

import java.util.Collection;
import java.util.List;

public class DefinitionGetDependenciesVisitor implements ConcreteDefinitionVisitor<Boolean, Void> {
  private final CollectDefCallsVisitor myVisitor;

  public DefinitionGetDependenciesVisitor(Collection<TCReferable> dependencies) {
    myVisitor = new CollectDefCallsVisitor(dependencies);
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Boolean isHeader) {
    for (Concrete.TelescopeParameter param : def.getParameters()) {
      param.getType().accept(myVisitor, null);
    }

    if (isHeader) {
      Concrete.Expression resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(myVisitor, null);
      }
    } else {
      Concrete.FunctionBody body = def.getBody();
      if (body instanceof Concrete.TermFunctionBody) {
        ((Concrete.TermFunctionBody) body).getTerm().accept(myVisitor, null);
      }
      myVisitor.visitClassFieldImpls(body.getClassFieldImpls());
      visitClauses(body.getClauses());
    }

    return null;
  }

  private void visitClauses(List<Concrete.FunctionClause> clauses) {
    for (Concrete.FunctionClause clause : clauses) {
      for (Concrete.Pattern pattern : clause.getPatterns()) {
        visitPattern(pattern);
      }
      if (clause.getExpression() != null) {
        clause.getExpression().accept(myVisitor, null);
      }
    }
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Boolean isHeader) {
    if (isHeader) {
      for (Concrete.TypeParameter param : def.getParameters()) {
        param.getType().accept(myVisitor, null);
      }

      Concrete.Expression universe = def.getUniverse();
      if (universe != null) {
        universe.accept(myVisitor, null);
      }
    } else {
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        if (clause.getPatterns() != null) {
          for (Concrete.Pattern pattern : clause.getPatterns()) {
            visitPattern(pattern);
          }
        }
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          visitConstructor(constructor);
        }
      }
    }

    return null;
  }

  private void visitPattern(Concrete.Pattern pattern) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
      if (conPattern.getConstructor() instanceof TCReferable) {
        myVisitor.getDependencies().add((TCReferable) conPattern.getConstructor());
      }
      for (Concrete.Pattern patternArg : conPattern.getPatterns()) {
        visitPattern(patternArg);
      }
    } else if (pattern instanceof Concrete.TuplePattern) {
      for (Concrete.Pattern patternArg : ((Concrete.TuplePattern) pattern).getPatterns()) {
        visitPattern(patternArg);
      }
    }
  }

  private void visitConstructor(Concrete.Constructor def) {
    for (Concrete.TypeParameter param : def.getParameters()) {
      param.getType().accept(myVisitor, null);
    }
    if (def.getResultType() != null) {
      def.getResultType().accept(myVisitor, null);
    }
    if (!def.getEliminatedReferences().isEmpty()) {
      visitClauses(def.getClauses());
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Boolean params) {
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      myVisitor.visitReference(superClass, null);
    }

    for (Concrete.ClassField field : def.getFields()) {
      field.getResultType().accept(myVisitor, null);
    }

    myVisitor.visitClassFieldImpls(def.getImplementations());
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Boolean params) {
    for (Concrete.Parameter param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).getType().accept(myVisitor, null);
      }
    }

    def.getResultType().accept(myVisitor, null);
    myVisitor.visitClassFieldImpls(def.getClassFieldImpls());
    return null;
  }
}
