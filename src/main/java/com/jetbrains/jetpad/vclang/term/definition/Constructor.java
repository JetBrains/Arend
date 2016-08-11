package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Constructor extends Definition implements Function {
  private DataDefinition myDataType;
  private DependentLink myParameters;
  private Patterns myPatterns;

  public Constructor(Abstract.Constructor abstractDef, DataDefinition dataType) {
    super(abstractDef);
    myDataType = dataType;
    myParameters = EmptyDependentLink.getInstance();
    if (dataType != null) {
      setPolyParams(dataType.getPolyParams());
    }
  }

  public Constructor(Abstract.Constructor abstractDef, DependentLink parameters, DataDefinition dataType, Patterns patterns) {
    super(abstractDef);
    hasErrors(false);
    myDataType = dataType;
    myParameters = parameters;
    myPatterns = patterns;
    if (dataType != null) {
      setPolyParams(dataType.getPolyParams());
    }
  }

  public Constructor(Abstract.Constructor abstractDef, DependentLink parameters, DataDefinition dataType) {
    this(abstractDef, parameters, dataType, null);
  }

  public Patterns getPatterns() {
    assert !hasErrors() && !myDataType.hasErrors();
    return myPatterns;
  }

  public void setPatterns(Patterns patterns) {
    myPatterns = patterns;
  }

  @Override
  public ElimTreeNode getElimTree() {
    Condition condition = myDataType.getCondition(this);
    return condition == null ? EmptyElimTreeNode.getInstance() : condition.getElimTree();
  }

  public DependentLink getParameters() {
    assert !hasErrors() && !myDataType.hasErrors();
    return myParameters;
  }

  @Override
  public Expression getResultType() {
    return getDataTypeExpression();
  }

  @Override
  public int getNumberOfRequiredArguments() {
    return DependentLink.Helper.size(myParameters);
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
    setPolyParams(dataType.getPolyParams());
  }

  public DependentLink getDataTypeParameters() {
    assert !hasErrors() && !myDataType.hasErrors();
    return myPatterns == null ? myDataType.getParameters() : myPatterns.getParameters();
  }

  public List<Expression> matchDataTypeArguments(List<Expression> arguments) {
    assert !hasErrors() && !myDataType.hasErrors();
    if (myPatterns == null) {
      return arguments;
    } else {
      Pattern.MatchResult result = myPatterns.match(arguments);
      if (result instanceof Pattern.MatchOKResult) {
        return ((Pattern.MatchOKResult) result).expressions;
      } else {
        return null;
      }
    }
  }

  public Expression getDataTypeExpression() {
    return getDataTypeExpression(null);
  }

  public Expression getDataTypeExpression(ExprSubstitution substitution) {
    assert !hasErrors() && !myDataType.hasErrors();

    Expression resultType = DataCall(myDataType);
    if (myPatterns == null) {
      List<Expression> arguments = new ArrayList<>();
      List<EnumSet<AppExpression.Flag>> flags = new ArrayList<>();
      for (DependentLink link = myDataType.getParameters(); link.hasNext(); link = link.getNext()) {
        arguments.add(Reference(link));
        flags.add(link.isExplicit() ? EnumSet.of(AppExpression.Flag.EXPLICIT, AppExpression.Flag.VISIBLE) : EnumSet.noneOf(AppExpression.Flag.class));
      }
      resultType = Apps(resultType, arguments, flags);
    } else {
      ExprSubstitution subst = new ExprSubstitution();

      DependentLink dataTypeParams = myDataType.getParameters();
      List<Expression> arguments = new ArrayList<>(myPatterns.getPatterns().size());
      for (PatternArgument patternArg : myPatterns.getPatterns()) {
        ExprSubstitution innerSubst = new ExprSubstitution();

        if (patternArg.getPattern() instanceof ConstructorPattern) {
          List<? extends Expression> argDataTypeParams = dataTypeParams.getType().subst(subst).normalize(NormalizeVisitor.Mode.WHNF).getArguments();
          Collections.reverse(argDataTypeParams);
          innerSubst = ((ConstructorPattern) patternArg.getPattern()).getMatchedArguments(new ArrayList<>(argDataTypeParams));
        }

        if (substitution != null) {
          innerSubst.add(substitution);
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
    if (hasErrors()) {
      return null;
    }

    Expression resultType = getDataTypeExpression();
    if (myParameters.hasNext()) {
      resultType = Pi(myParameters, resultType);
    }

    DependentLink parameters = getDataTypeParameters();
    if (parameters.hasNext()) {
      ExprSubstitution substitution = new ExprSubstitution();
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