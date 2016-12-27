package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.ConCall;

public class DataDefinition extends Definition {
  private List<Constructor> myConstructors;
  private DefParameters myDefParameters;
  private Map<Constructor, Condition> myConditions;
  private SortMax mySorts;
  private boolean myMatchesOnInterval;
  private boolean myTypeHasErrors;
  private TypeCheckingStatus myHasErrors;

  public DataDefinition(Abstract.DataDefinition abstractDef) {
    this(abstractDef, new SortMax(), EmptyDependentLink.getInstance());
  }

  public DataDefinition(Abstract.DataDefinition abstractDef, SortMax sorts, DependentLink parameters) {
    super(abstractDef);
    myConstructors = new ArrayList<>();
    myDefParameters = new DefParameters(parameters);
    mySorts = sorts;
    myMatchesOnInterval = false;
    myTypeHasErrors = parameters != null;
    myHasErrors = myTypeHasErrors ? TypeCheckingStatus.HAS_ERRORS : TypeCheckingStatus.TYPE_CHECKING;
  }

  @Override
  public Abstract.DataDefinition getAbstractDefinition() {
    return (Abstract.DataDefinition) super.getAbstractDefinition();
  }

  public SortMax getSorts() {
    return mySorts;
  }

  public void setSorts(SortMax sorts) {
    mySorts = sorts;
  }

  @Override
  public DependentLink getParameters() {
    assert !typeHasErrors();
    return myDefParameters.getParameters();
  }

  public DefParameters getDefParameters() {
    return myDefParameters;
  }

  public void setDefParameters(DefParameters parameters) {
    assert parameters != null;
    myDefParameters = parameters;
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public List<ConCallExpression> getMatchedConstructors(DataCallExpression dataCall) {
    List<ConCallExpression> result = new ArrayList<>();
    for (Constructor constructor : myConstructors) {
      if (constructor.typeHasErrors())
        continue;
      List<? extends Expression> matchedParameters;
      if (constructor.getPatterns() != null) {
        Pattern.MatchResult matchResult = constructor.getPatterns().match(dataCall.getDefCallArguments());
        if (matchResult instanceof Pattern.MatchMaybeResult) {
          return null;
        } else if (matchResult instanceof Pattern.MatchFailedResult) {
          continue;
        } else if (matchResult instanceof Pattern.MatchOKResult) {
          matchedParameters = ((Pattern.MatchOKResult) matchResult).expressions;
        } else {
          throw new IllegalStateException();
        }
      } else {
        matchedParameters = dataCall.getDefCallArguments();
      }

      result.add(ConCall(constructor, dataCall.getPolyArguments(), new ArrayList<>(matchedParameters), new ArrayList<Expression>()));
    }
    return result;
  }

  public void addCondition(Condition condition) {
    if (myConditions == null) {
      myConditions = new HashMap<>();
    }
    myConditions.put(condition.getConstructor(), condition);
  }

  public Collection<Condition> getConditions() {
    return myConditions == null ? Collections.<Condition>emptyList() : myConditions.values();
  }

  public Condition getCondition(Constructor constructor) {
    return myConditions == null ? null : myConditions.get(constructor);
  }

  public Constructor getConstructor(String name) {
    for (Constructor constructor : myConstructors) {
      if (constructor.getName().equals(name)) {
        return constructor;
      }
    }
    return null;
  }

  public void addConstructor(Constructor constructor) {
    myConstructors.add(constructor);
  }

  public boolean matchesOnInterval() { return myMatchesOnInterval; }

  public void setMatchesOnInterval() { myMatchesOnInterval = true; }

  @Override
  public TypeMax getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    if (typeHasErrors()) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution polySubst = polyArguments.toLevelSubstitution(this);
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myDefParameters.getParameters(), subst, polySubst)));
    return new PiUniverseType(EmptyDependentLink.getInstance(), mySorts).subst(subst, polySubst);
  }

  @Override
  public DataCallExpression getDefCall(LevelArguments polyArguments) {
    return ExpressionFactory.DataCall(this, polyArguments, new ArrayList<Expression>());
  }

  @Override
  public DataCallExpression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    return new DataCallExpression(this, polyArguments, args);
  }

  @Override
  public boolean typeHasErrors() {
    return myTypeHasErrors;
  }

  public void typeHasErrors(boolean has) {
    myTypeHasErrors = has;
    if (has) {
      myHasErrors = TypeCheckingStatus.HAS_ERRORS;
    }
  }

  @Override
  public TypeCheckingStatus hasErrors() {
    return myHasErrors;
  }

  @Override
  public void hasErrors(TypeCheckingStatus status) {
    myHasErrors = status;
  }
}
