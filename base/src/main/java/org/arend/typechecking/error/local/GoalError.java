package org.arend.typechecking.error.local;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.typechecking.GoalSolver;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckingContext;
import org.arend.typechecking.patternmatching.Condition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class GoalError extends GoalDataHolder {
  public final Concrete.Expression result;
  public final List<GeneralError> errors;
  public final GoalSolver goalSolver;

  private List<Condition> myConditions = Collections.emptyList();

  public GoalError(TypecheckingContext typecheckingContext,
                   Map<Binding, Expression> bindingTypes,
                   Expression expectedType,
                   Concrete.Expression result,
                   List<GeneralError> errors,
                   GoalSolver goalSolver,
                   Concrete.GoalExpression expression) {
    super(Level.GOAL, "Goal" + (expression.getName() == null ? "" : " " + expression.getName()), expression,
            typecheckingContext, bindingTypes, expectedType);
    this.result = result;
    this.errors = errors;
    this.goalSolver = goalSolver;
  }

  @Override
  public Concrete.GoalExpression getCauseSourceNode() {
    return (Concrete.GoalExpression) super.getCauseSourceNode();
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    Doc expectedDoc = getExpectedDoc(ppConfig);
    Doc contextDoc = getContextDoc(ppConfig);
    Doc conditionsDoc = getConditionsDoc(ppConfig);
    Doc errorsDoc = getErrorsDoc(ppConfig);
    return vList(expectedDoc, contextDoc, conditionsDoc, errorsDoc);
  }

  @NotNull
  private Doc getConditionsDoc(PrettyPrinterConfig ppConfig) {
    if (!myConditions.isEmpty()) {
      List<Doc> conditionsDocs = new ArrayList<>(myConditions.size());
      for (Condition condition : myConditions) {
        conditionsDocs.add(condition.toDoc(ppConfig));
      }
      return hang(text("Conditions:"), vList(conditionsDocs));
    }
    return nullDoc();
  }

  @NotNull
  private Doc getErrorsDoc(PrettyPrinterConfig ppConfig) {
    if (!errors.isEmpty()) {
      List<Doc> errorsDocs = new ArrayList<>(errors.size());
      for (GeneralError error : errors) {
        errorsDocs.add(hang(error.getHeaderDoc(ppConfig), error.getBodyDoc(ppConfig)));
      }
      return hang(text("Errors:"), vList(errorsDocs));
    }
    return nullDoc();
  }

  public void addCondition(Condition condition) {
    if (myConditions.isEmpty()) {
      myConditions = new ArrayList<>();
    }
    myConditions.add(condition);
  }

  public List<? extends Condition> getConditions() {
    return myConditions;
  }
}
