package org.arend.typechecking.visitor;

import org.arend.ext.error.ErrorReporter;
import org.arend.naming.reference.Parameter;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.Reference;
import org.arend.term.abs.Abstract;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.CertainTypecheckingError;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.error.local.NotEnoughPatternsError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DumbTypechecker extends VoidConcreteVisitor<Void, Void> {
  private final BaseDefinitionTypechecker myTypechecker;
  private Concrete.Definition myDefinition;

  public DumbTypechecker(ErrorReporter errorReporter) {
    myTypechecker = new BaseDefinitionTypechecker(errorReporter);
  }

  @Override
  public void visitFunctionHeader(Concrete.BaseFunctionDefinition def, Void params) {
    myDefinition = def;
    super.visitFunctionHeader(def, null);
    myTypechecker.checkFunctionLevel(def, def.getKind());
  }

  @Override
  public Void visitFunctionBody(Concrete.BaseFunctionDefinition def, Void params) {
    checkClauses(def.getBody().getClauses(), def.getBody().getEliminatedReferences(), def.getParameters());
    super.visitFunctionBody(def, null);
    myTypechecker.checkElimBody(def);
    return null;
  }

  @Override
  public void visitDataHeader(Concrete.DataDefinition def, Void params) {
    myDefinition = def;
    super.visitDataHeader(def, null);
  }

  @Override
  public Void visitDataBody(Concrete.DataDefinition def, Void params) {
    checkClauses(def.getConstructorClauses(), def.getEliminatedReferences(), def.getParameters());
    super.visitDataBody(def, null);
    return null;
  }

  @Override
  protected void visitConstructor(Concrete.Constructor def, Void params) {
    super.visitConstructor(def, params);
    checkClauses(def.getClauses(), def.getEliminatedReferences(), def.getParameters());
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    myDefinition = def;
    super.visitClass(def, null);
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (expr.getReferent().equals(myDefinition.getData())) {
      myDefinition.setRecursive(true);
    }

    super.visitReference(expr, null);
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Void params) {
    myTypechecker.errorReporter.report(new GoalError(null, null, null, Collections.emptyList(), null, expr));
    return null;
  }

  @Override
  public Void visitCase(Concrete.CaseExpression expr, Void params) {
    checkClauses(expr.getClauses(), null, expr.getArguments().size());
    super.visitCase(expr, params);
    return null;
  }

  @Override
  protected void visitPattern(Concrete.Pattern pattern, Void params) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
      Referable ref = conPattern.getConstructor().getUnderlyingReferable();
      if (ref instanceof Abstract.ParametersHolder) {
        checkClauses(Collections.singletonList(conPattern), Collections.emptyList(), ((Abstract.ParametersHolder) ref).getParameters());
      }
    }

    super.visitPattern(pattern, params);
  }

  public static void findImplicitPatterns(List<? extends Concrete.PatternHolder> clauses, ErrorReporter errorReporter) {
    for (Concrete.PatternHolder clause : clauses) {
      if (clause.getPatterns() == null) {
        continue;
      }
      for (Concrete.Pattern pattern : clause.getPatterns()) {
        if (!pattern.isExplicit()) {
          errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.IMPLICIT_PATTERN, pattern));
        }
      }
    }
  }

  private void checkClauses(List<? extends Concrete.PatternHolder> clauses, List<Boolean> arguments, int numberOfArguments) {
    if (arguments == null) {
      findImplicitPatterns(clauses, myTypechecker.errorReporter);
    }

    loop:
    for (Concrete.PatternHolder clause : clauses) {
      List<Concrete.Pattern> patterns = clause.getPatterns();
      if (patterns == null) {
        continue;
      }

      if (arguments == null) {
        if (patterns.size() > numberOfArguments) {
          myTypechecker.errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.TOO_MANY_PATTERNS, patterns.get(numberOfArguments)));
        } else if (patterns.size() < numberOfArguments) {
          myTypechecker.errorReporter.report(new NotEnoughPatternsError(numberOfArguments - patterns.size(), clause.getSourceNode()));
        }
      } else {
        int i = 0, j = 0;
        while (i < arguments.size() && j < patterns.size()) {
          if (arguments.get(i) == patterns.get(j).isExplicit()) {
            i++;
            j++;
          } else if (arguments.get(i)) {
            myTypechecker.errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.EXPECTED_EXPLICIT_PATTERN, patterns.get(j)));
            continue loop;
          } else {
            i++;
          }
        }

        while (i < arguments.size() && !arguments.get(i)) {
          i++;
        }

        if (i < arguments.size()) {
          myTypechecker.errorReporter.report(new NotEnoughPatternsError(arguments.size() - i, clause.getSourceNode()));
        }
        if (j < patterns.size()) {
          myTypechecker.errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.TOO_MANY_PATTERNS, patterns.get(j)));
        }
      }
    }
  }

  private void checkClauses(List<? extends Concrete.PatternHolder> clauses, Collection<? extends Reference> eliminatedReferences, Collection<? extends Parameter> parameters) {
    if (clauses.isEmpty() || eliminatedReferences == null) {
      return;
    }

    List<Boolean> arguments;
    if (eliminatedReferences.isEmpty()) {
      arguments = new ArrayList<>();
      for (Parameter parameter : parameters) {
        for (Referable ignored : parameter.getReferableList()) {
          arguments.add(parameter.isExplicit());
        }
      }
    } else {
      arguments = null;
    }

    checkClauses(clauses, arguments, arguments == null ? eliminatedReferences.size() : arguments.size());
  }
}
