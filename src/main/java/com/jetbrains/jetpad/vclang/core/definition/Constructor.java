package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.StdLevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

public class Constructor extends Definition implements Function {
  private final DataDefinition myDataType;
  private DependentLink myParameters;
  private Patterns myPatterns;

  public Constructor(Abstract.Constructor abstractDef, DataDefinition dataType) {
    super(abstractDef, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myDataType = dataType;
    myParameters = null;
  }

  public Patterns getPatterns() {
    assert myParameters != null;
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

  @Override
  public DependentLink getParameters() {
    assert myParameters != null;
    return myParameters;
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

  public DependentLink getDataTypeParameters() {
    assert myParameters != null && myDataType.status().headerIsOK();
    return myPatterns == null ? myDataType.getParameters() : myPatterns.getParameters();
  }

  public List<Expression> matchDataTypeArguments(List<Expression> arguments) {
    assert myParameters != null && myDataType.status().headerIsOK();
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

  public Expression getDataTypeExpression(LevelArguments polyParams) {
    return getDataTypeExpression(null, polyParams);
  }

  public Expression getDataTypeExpression(ExprSubstitution substitution, LevelArguments polyArgs) {
    assert myParameters != null && myDataType.status().headerIsOK();

    List<Expression> arguments;
    if (myPatterns == null) {
      // TODO: Why substitution is not applied?
      arguments = new ArrayList<>();
      for (DependentLink link = myDataType.getParameters(); link.hasNext(); link = link.getNext()) {
        arguments.add(ExpressionFactory.Reference(link));
      }
    } else {
      ExprSubstitution subst = new ExprSubstitution();

      DependentLink dataTypeParams = myDataType.getParameters();
      LevelSubstitution levelSubst = polyArgs == null ? LevelSubstitution.EMPTY : polyArgs.toLevelSubstitution();
      arguments = new ArrayList<>(myPatterns.getPatterns().size());
      for (PatternArgument patternArg : myPatterns.getPatterns()) {
        ExprSubstitution innerSubst = new ExprSubstitution();
        LevelSubstitution innerLevelSubst;

        if (patternArg.getPattern() instanceof ConstructorPattern) {
          DataCallExpression dataCall = dataTypeParams.getType().toExpression().subst(subst).normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
          List<? extends Expression> argDataTypeParams = dataCall.getDefCallArguments();
          innerSubst = ((ConstructorPattern) patternArg.getPattern()).getMatchedArguments(new ArrayList<>(argDataTypeParams));
          innerLevelSubst = new StdLevelSubstitution(dataCall.getLevelArguments().getPLevel(), dataCall.getLevelArguments().getHLevel());
        } else {
          innerLevelSubst = LevelSubstitution.EMPTY;
        }

        if (substitution != null) {
          innerSubst.addAll(substitution);
        }
        Expression expr = patternArg.getPattern().toExpression(innerSubst).subst(innerLevelSubst).subst(levelSubst);

        subst.add(dataTypeParams, expr);
        arguments.add(expr);
        dataTypeParams = dataTypeParams.getNext();
      }
    }

    if (polyArgs == null) {
      polyArgs = LevelArguments.STD;
    }

    return myDataType.getDefCall(polyArgs, arguments);
  }

  @Override
  public Expression getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    if (myParameters == null) {
      return null;
    }

    LevelSubstitution polySubst = polyArguments.toLevelSubstitution();
    Expression resultType = getDataTypeExpression(polyArguments);
    DependentLink parameters = getDataTypeParameters();
    ExprSubstitution substitution = new ExprSubstitution();
    if (parameters.hasNext()) {
      parameters = DependentLink.Helper.subst(parameters, substitution, polySubst);
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        link.setExplicit(false);
      }
      params.addAll(DependentLink.Helper.toList(parameters));
    }
    DependentLink conParams = DependentLink.Helper.subst(myParameters, substitution, polySubst);
    if (!params.isEmpty()) {
      params.get(params.size() - 1).setNext(conParams);
    }
    params.addAll(DependentLink.Helper.toList(conParams));
    resultType = resultType.subst(substitution, polySubst);
    return resultType;
  }

  @Override
  public ConCallExpression getDefCall(LevelArguments polyArguments) {
    return new ConCallExpression(this, polyArguments, new ArrayList<Expression>(), new ArrayList<Expression>());
  }

  @Override
  public ConCallExpression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    throw new IllegalStateException();
  }
}