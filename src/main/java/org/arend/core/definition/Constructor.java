package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ClauseBase;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.DataCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.pattern.Pattern;
import org.arend.core.pattern.Patterns;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.naming.reference.TCReferable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Constructor extends Definition implements Function {
  private final DataDefinition myDataType;
  private DependentLink myParameters;
  private Patterns myPatterns;
  private Body myConditions;
  private List<ClauseBase> myClauses;
  private int myNumberOfIntervalParameters;
  private List<Integer> myParametersTypecheckingOrder;
  private List<Boolean> myGoodThisParameters = Collections.emptyList();
  private List<TypeClassParameterKind> myTypeClassParameters = Collections.emptyList();

  public Constructor(TCReferable referable, DataDefinition dataType) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myDataType = dataType;
    myParameters = null;
    myClauses = Collections.emptyList();
  }

  public void setBody(Body conditions) {
    myConditions = conditions;
  }

  public Patterns getPatterns() {
    return myPatterns;
  }

  public void setPatterns(Patterns patterns) {
    myPatterns = patterns;
  }

  public List<? extends ClauseBase> getClauses() {
    return myClauses;
  }

  public void setClauses(List<? extends ClauseBase> clauses) {
    myClauses = new ArrayList<>(clauses);
  }

  @Override
  public Body getBody() {
    return myConditions;
  }

  @Override
  public DependentLink getParameters() {
    assert myParameters != null;
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  public int getNumberOfIntervalParameters() {
    return myNumberOfIntervalParameters;
  }

  public void setNumberOfIntervalParameters(int numberOfIntervalParameters) {
    myNumberOfIntervalParameters = numberOfIntervalParameters;
  }

  public DataDefinition getDataType() {
    return myDataType;
  }

  public DependentLink getDataTypeParameters() {
    return myDataType.status().headerIsOK() ? (myPatterns == null ? myDataType.getParameters() : myPatterns.getFirstBinding()) : EmptyDependentLink.getInstance();
  }

  public List<Expression> matchDataTypeArguments(List<Expression> arguments) {
    assert myDataType.status().headerIsOK();
    if (myPatterns == null) {
      return arguments;
    } else {
      List<Expression> result = new ArrayList<>();
      return myPatterns.match(arguments, result) == Pattern.MatchResult.OK ? result : null;
    }
  }

  public DataCallExpression getDataTypeExpression(Sort sortArgument) {
    return getDataTypeExpression(sortArgument, null);
  }

  public DataCallExpression getDataTypeExpression(Sort sortArgument, List<? extends Expression> dataTypeArguments) {
    assert myDataType.status().headerIsOK();

    List<Expression> arguments;
    if (myPatterns == null) {
      if (dataTypeArguments == null) {
        arguments = new ArrayList<>();
        for (DependentLink link = getDataTypeParameters(); link.hasNext(); link = link.getNext()) {
          arguments.add(new ReferenceExpression(link));
        }
      } else {
        arguments = new ArrayList<>(dataTypeArguments);
      }
    } else {
      if (dataTypeArguments == null) {
        arguments = new ArrayList<>(myPatterns.getPatternList().size());
        for (Pattern pattern : myPatterns.getPatternList()) {
          arguments.add(pattern.toExpression());
        }
      } else {
        ExprSubstitution substitution = new ExprSubstitution();
        DependentLink link = myPatterns.getFirstBinding();
        for (Expression argument : dataTypeArguments) {
          substitution.add(link, argument);
          link = link.getNext();
        }

        arguments = new ArrayList<>(myPatterns.getPatternList().size());
        for (Pattern pattern : myPatterns.getPatternList()) {
          arguments.add(pattern.toExpression().subst(substitution));
        }
      }
    }

    return myDataType.getDefCall(sortArgument, arguments);
  }

  @Override
  public List<Integer> getParametersTypecheckingOrder() {
    return myParametersTypecheckingOrder;
  }

  @Override
  public void setParametersTypecheckingOrder(List<Integer> order) {
    myParametersTypecheckingOrder = order;
  }

  @Override
  public List<Boolean> getGoodThisParameters() {
    return myGoodThisParameters;
  }

  @Override
  public boolean isGoodParameter(int index) {
    if (myPatterns != null) {
      return super.isGoodParameter(index);
    }

    int dataTypeParams = DependentLink.Helper.size(myDataType.getParameters());
    return index < dataTypeParams ? myDataType.isGoodParameter(index) : super.isGoodParameter(index - dataTypeParams);
  }

  @Override
  public void setGoodThisParameters(List<Boolean> goodThisParameters) {
    myGoodThisParameters = goodThisParameters;
  }

  @Override
  public List<TypeClassParameterKind> getTypeClassParameters() {
    return myTypeClassParameters;
  }

  @Override
  public TypeClassParameterKind getTypeClassParameterKind(int index) {
    int dataTypeParams = DependentLink.Helper.size(myDataType.getParameters());
    return index < dataTypeParams ? (myPatterns == null ? myDataType.getTypeClassParameterKind(index) : TypeClassParameterKind.NO) : super.getTypeClassParameterKind(index - dataTypeParams);
  }

  @Override
  public void setTypeClassParameters(List<TypeClassParameterKind> typeClassParameters) {
    myTypeClassParameters = typeClassParameters;
  }

  @Override
  public DataCallExpression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (myParameters == null) {
      return null;
    }

    LevelSubstitution polySubst = sortArgument.toLevelSubstitution();
    DataCallExpression resultType = getDataTypeExpression(sortArgument);
    DependentLink parameters = getDataTypeParameters();
    ExprSubstitution substitution = new ExprSubstitution();
    List<DependentLink> paramList = null;
    if (parameters.hasNext()) {
      parameters = DependentLink.Helper.subst(parameters, substitution, polySubst);
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        link.setExplicit(false);
      }
      paramList = DependentLink.Helper.toList(parameters);
      params.addAll(paramList);
    }
    DependentLink conParams = DependentLink.Helper.subst(myParameters, substitution, polySubst);
    if (paramList != null && !paramList.isEmpty()) {
      paramList.get(paramList.size() - 1).setNext(conParams);
    }
    params.addAll(DependentLink.Helper.toList(conParams));
    resultType = resultType.subst(new SubstVisitor(substitution, polySubst));
    return resultType;
  }

  @Override
  public Expression getDefCall(Sort sortArgument, List<Expression> args) {
    int dataTypeArgsNumber = DependentLink.Helper.size(getDataTypeParameters());
    List<Expression> dataTypeArgs = new ArrayList<>(dataTypeArgsNumber);
    dataTypeArgs.addAll(args.subList(0, dataTypeArgsNumber));
    return ConCallExpression.make(this, sortArgument, dataTypeArgs, args.subList(dataTypeArgsNumber, args.size()));
  }
}