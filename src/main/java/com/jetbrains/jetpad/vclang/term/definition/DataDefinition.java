package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.param.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Utils;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.param.Utils.numberOfVariables;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.patternMatchAll;

public class DataDefinition extends Definition {
  private List<Constructor> myConstructors;
  private DependentLink myParameters;
  private Map<Constructor, Condition> myConditions;

  public DataDefinition(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence) {
    super(parentNamespace, name, precedence);
    myConstructors = new ArrayList<>();
  }

  public DataDefinition(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, Universe universe, DependentLink parameters) {
    super(parentNamespace, name, precedence);
    setUniverse(universe);
    hasErrors(false);
    myParameters = parameters;
    myConstructors = new ArrayList<>();
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public int getNumberOfAllParameters() {
    return numberOfVariables(myParameters) + (getThisClass() == null ? 0 : 1);
  }

  public void setParameters(List<TypeArgument> arguments) {
    myParameters = arguments;
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public List<ConCallExpression> getConstructors(List<Expression> parameters, List<Binding> context) {
    List<ConCallExpression> result = new ArrayList<>();
    for (Constructor constructor : myConstructors) {
      if (constructor.hasErrors())
        continue;
      List<Expression> matchedParameters = null;
      if (constructor.getPatterns() != null) {
        Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), parameters, context);
        if (matchResult instanceof Utils.PatternMatchMaybeResult) {
          return null;
        } else if (matchResult instanceof Utils.PatternMatchFailedResult) {
          continue;
        } else if (matchResult instanceof Utils.PatternMatchOKResult) {
          matchedParameters = ((Utils.PatternMatchOKResult) matchResult).expressions;
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
      if (constructor.getName().name.equals(name)) {
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
    return myParameters.isEmpty() ? resultType : Pi(myParameters, resultType);
  }

  @Override
  public DataCallExpression getDefCall() {
    return DataCall(this);
  }
}
