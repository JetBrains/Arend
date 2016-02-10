package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Constructor extends Definition {
  private DataDefinition myDataType;
  private DependentLink myParameters;
  private Patterns myPatterns;

  public Constructor(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, DataDefinition dataType) {
    super(parentNamespace, name, precedence);
    myDataType = dataType;
    myParameters = EmptyDependentLink.getInstance();
  }

  public Constructor(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, Universe universe, DependentLink parameters, DataDefinition dataType, Patterns patterns) {
    super(parentNamespace, name, precedence);
    assert parameters != null;
    setUniverse(universe);
    hasErrors(false);
    myDataType = dataType;
    myParameters = parameters;
    myPatterns = patterns;
  }

  public Constructor(Namespace parentNamespace, Name name, Abstract.Definition.Precedence precedence, Universe universe, DependentLink parameters, DataDefinition dataType) {
    this(parentNamespace, name, precedence, universe, parameters, dataType, null);
  }

  public Patterns getPatterns() {
    return myPatterns;
  }

  public void setPatterns(Patterns patterns) {
    myPatterns = patterns;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  public DataDefinition getDataType() {
    return myDataType;
  }

  public void setDataType(DataDefinition dataType) {
    myDataType = dataType;
  }

  public DependentLink getDataTypeParameters() {
    return myPatterns == null ? myDataType.getParameters() : myPatterns.getParameters();
  }

  public List<Expression> matchDataTypeArguments(List<Expression> arguments) {
    return myPatterns == null ? arguments : ((Pattern.MatchOKResult) myPatterns.match(arguments)).expressions;
  }

  @Override
  public Expression getType() {
    Expression resultType = DataCall(myDataType);
    if (myPatterns == null) {
      for (DependentLink link = myDataType.getParameters(); link.hasNext(); link = link.getNext()) {
        resultType = Apps(resultType, Reference(link), link.isExplicit(), !link.isExplicit());
      }
    } else {
      Substitution subst = new Substitution();
      DependentLink dataTypeParams = myDataType.getParameters();
      for (PatternArgument patternArg : myPatterns.getPatterns()) {
        Substitution innerSubst = new Substitution();

        if (patternArg.getPattern() instanceof ConstructorPattern) {
          List<Expression> argDataTypeParams = new ArrayList<>();
          dataTypeParams.getType().subst(subst).getFunction(argDataTypeParams);
          Collections.reverse(argDataTypeParams);
          innerSubst = ((ConstructorPattern) patternArg.getPattern()).getMatchedArguments(argDataTypeParams);
        }

        Expression expr = patternArg.getPattern().toExpression(innerSubst);
        resultType = Apps(resultType, expr);
        subst.add(dataTypeParams, expr);
        dataTypeParams = dataTypeParams.getNext();
      }
    }
    return myParameters.hasNext() ? Pi(myParameters, resultType) : resultType;
  }

  @Override
  public ConCallExpression getDefCall() {
    return ConCall(this);
  }
}