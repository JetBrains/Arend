package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class DataDefinition extends Definition {
  private List<Constructor> myConstructors;
  private DependentLink myParameters;
  private Map<Constructor, Condition> myConditions;

  public DataDefinition(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence) {
    super(parentNamespace, name, precedence);
    myConstructors = new ArrayList<>();
    myParameters = EmptyDependentLink.getInstance();
  }

  public DataDefinition(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, Universe universe, DependentLink parameters) {
    super(parentNamespace, name, precedence);
    assert parameters != null;
    setUniverse(universe);
    hasErrors(false);
    myParameters = parameters;
    myConstructors = new ArrayList<>();
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public int getNumberOfAllParameters() {
    int s = 0;
    for (DependentLink link = myParameters; link.hasNext(); link = link.getNext()) {
      s++;
    }
    return s + (getThisClass() == null ? 0 : 1);
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public List<ConCallExpression> getMatchedConstructors(List<Expression> parameters) {
    List<ConCallExpression> result = new ArrayList<>();
    for (Constructor constructor : myConstructors) {
      if (constructor.hasErrors())
        continue;
      List<Expression> matchedParameters = null;
      if (constructor.getPatterns() != null) {
        Pattern.MatchResult matchResult = constructor.getPatterns().match(parameters);
        if (matchResult instanceof Pattern.MatchMaybeResult) {
          return null;
        } else if (matchResult instanceof Pattern.MatchFailedResult) {
          continue;
        } else if (matchResult instanceof Pattern.MatchOKResult) {
          matchedParameters = ((Pattern.MatchOKResult) matchResult).expressions;
        }
      } else {
        matchedParameters = parameters;
      }

      result.add(ConCall(constructor, matchedParameters));
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
    return myConditions == null ? null : myConditions.values();
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
    constructor.getParentNamespace().addDefinition(constructor);
  }

  @Override
  public Expression getBaseType() {
    Expression resultType = new UniverseExpression(getUniverse());
    return myParameters == null ? resultType : Pi(myParameters, resultType);
  }

  @Override
  public DataCallExpression getDefCall() {
    return DataCall(this);
  }
}
