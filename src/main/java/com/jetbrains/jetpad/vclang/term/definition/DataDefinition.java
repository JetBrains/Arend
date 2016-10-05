package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ConCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.DataCall;

public class DataDefinition extends Definition {
  private List<Constructor> myConstructors;
  private DependentLink myParameters;
  private Map<Constructor, Condition> myConditions;
  private SortMax mySorts;
  private boolean myMatchesOnInterval;

  public DataDefinition(Abstract.DataDefinition abstractDef) {
    this(abstractDef, new SortMax(), EmptyDependentLink.getInstance());
  }

  public DataDefinition(Abstract.DataDefinition abstractDef, SortMax sorts, DependentLink parameters) {
    super(abstractDef);
    hasErrors(false);
    myConstructors = new ArrayList<>();
    myParameters = parameters;
    mySorts = sorts;
    myMatchesOnInterval = false;
  }

  public SortMax getSorts() {
    return mySorts;
  }

  public void setSorts(SortMax sorts) {
    mySorts = sorts;
  }

  @Override
  public DependentLink getParameters() {
    assert !hasErrors();
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public List<ConCallExpression> getMatchedConstructors(DataCallExpression dataCall) {
    List<ConCallExpression> result = new ArrayList<>();
    for (Constructor constructor : myConstructors) {
      if (constructor.hasErrors())
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

      result.add(ConCall(constructor, dataCall.getPolyParamsSubst(), new ArrayList<>(matchedParameters), new ArrayList<Expression>()));
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
  public Type getType(LevelSubstitution polyParams) {
    if (hasErrors()) {
      return null;
    }

    return new PiUniverseType(myParameters, mySorts).subst(new ExprSubstitution(), polyParams);
  }

  @Override
  public DataCallExpression getDefCall(LevelSubstitution polyParams) {
    return DataCall(this, polyParams, new ArrayList<Expression>());
  }

  @Override
  public DataCallExpression getDefCall(LevelSubstitution polyParams, List<Expression> args) {
    return new DataCallExpression(this, polyParams, args);
  }

  @Override
  public int getNumberOfParameters() {
    return DependentLink.Helper.size(myParameters);
  }
}
