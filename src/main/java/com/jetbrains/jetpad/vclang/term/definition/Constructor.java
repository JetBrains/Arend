package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Constructor extends Definition {
  private DataDefinition myDataType;
  private DependentLink myParameters;
  private Patterns myPatterns;

  public Constructor(ResolvedName rn, Abstract.Definition.Precedence precedence, DataDefinition dataType) {
    super(rn, precedence);
    myDataType = dataType;
    myParameters = EmptyDependentLink.getInstance();
  }

  public Constructor(ResolvedName rn, Abstract.Definition.Precedence precedence, Universe universe, DependentLink parameters, DataDefinition dataType, Patterns patterns) {
    super(rn, precedence);
    assert parameters != null;
    setUniverse(universe);
    hasErrors(false);
    myDataType = dataType;
    myParameters = parameters;
    myPatterns = patterns;
  }

  public Constructor(ResolvedName rn, Abstract.Definition.Precedence precedence, Universe universe, DependentLink parameters, DataDefinition dataType) {
    this(rn, precedence, universe, parameters, dataType, null);
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

  public Expression getDataTypeExpression() {
    Expression resultType = DataCall(myDataType);
    if (myPatterns == null) {
      List<Expression> arguments = new ArrayList<>();
      List<EnumSet<AppExpression.Flag>> flags = new ArrayList<>();
      for (DependentLink link = myDataType.getParameters(); link.hasNext(); link = link.getNext()) {
        arguments.add(Reference(link));
        flags.add(link.isExplicit() ? EnumSet.of(AppExpression.Flag.EXPLICIT, AppExpression.Flag.VISIBLE) : EnumSet.noneOf(AppExpression.Flag.class));
      }
      resultType = new AppExpression(resultType, arguments, flags);
    } else {
      Substitution subst = new Substitution();
      DependentLink dataTypeParams = myDataType.getParameters();
      List<Expression> arguments = new ArrayList<>(myPatterns.getPatterns().size());
      for (PatternArgument patternArg : myPatterns.getPatterns()) {
        Substitution innerSubst = new Substitution();

        if (patternArg.getPattern() instanceof ConstructorPattern) {
          List<? extends Expression> argDataTypeParams = dataTypeParams.getType().subst(subst).normalize(NormalizeVisitor.Mode.WHNF).getArguments();
          Collections.reverse(argDataTypeParams);
          innerSubst = ((ConstructorPattern) patternArg.getPattern()).getMatchedArguments(new ArrayList<>(argDataTypeParams));
        }

        Expression expr = patternArg.getPattern().toExpression(innerSubst);

        subst.add(dataTypeParams, expr);
        arguments.add(expr);
        dataTypeParams = dataTypeParams.getNext();
      }
      resultType = Apps(resultType, arguments);
    }

    return resultType;
  }

  @Override
  public Expression getType() {
    Expression resultType = getDataTypeExpression();
    if (myParameters.hasNext()) {
      resultType = Pi(myParameters, resultType);
    }

    DependentLink parameters = getDataTypeParameters();
    if (parameters.hasNext()) {
      Substitution substitution = new Substitution();
      parameters = DependentLink.Helper.subst(parameters, substitution);
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        link.setExplicit(false);
      }
      resultType = Pi(parameters, resultType.subst(substitution));
    }
    return resultType;
  }

  @Override
  public ConCallExpression getDefCall() {
    return ConCall(this);
  }
}