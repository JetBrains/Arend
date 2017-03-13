package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.ConCall;

public class DataDefinition extends Definition {
  private List<Constructor> myConstructors;
  private DependentLink myParameters;
  private Map<Constructor, Condition> myConditions;
  private Sort mySort;
  private boolean myMatchesOnInterval;
  private boolean myIsTruncated;

  public DataDefinition(Abstract.DataDefinition abstractDef) {
    super(abstractDef, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myConstructors = new ArrayList<>();
  }

  @Override
  public Abstract.DataDefinition getAbstractDefinition() {
    return (Abstract.DataDefinition) super.getAbstractDefinition();
  }

  public Sort getSort() {
    return mySort;
  }

  public void setSort(Sort sort) {
    mySort = sort;
  }

  public boolean isTruncated() {
    return myIsTruncated;
  }

  public void setIsTruncated(boolean value) {
    myIsTruncated = value;
  }

  @Override
  public DependentLink getParameters() {
    assert status().headerIsOK();
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    myParameters = parameters;
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public List<ConCallExpression> getMatchedConstructors(DataCallExpression dataCall) {
    List<ConCallExpression> result = new ArrayList<>();
    for (Constructor constructor : myConstructors) {
      if (!constructor.status().headerIsOK())
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

      result.add(ConCall(constructor, dataCall.getLevelArguments(), new ArrayList<>(matchedParameters), new ArrayList<>()));
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
  public Expression getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    if (!status().headerIsOK()) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution polySubst = polyArguments.toLevelSubstitution();
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, subst, polySubst)));
    return new UniverseExpression(mySort).subst(subst, polySubst);
  }

  @Override
  public DataCallExpression getDefCall(LevelArguments polyArguments, Expression thisExpr, List<Expression> arguments) {
    if (thisExpr == null) {
      return ExpressionFactory.DataCall(this, polyArguments, arguments);
    } else {
      List<Expression> args = new ArrayList<>(arguments.size() + 1);
      args.add(thisExpr);
      args.addAll(arguments);
      return ExpressionFactory.DataCall(this, polyArguments, args);
    }
  }

  @Override
  public DataCallExpression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    return new DataCallExpression(this, polyArguments, args);
  }
}
