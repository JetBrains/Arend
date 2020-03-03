package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.DataCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.definition.CoreConstructor;
import org.arend.core.elimtree.BranchKey;
import org.arend.naming.reference.TCReferable;
import org.arend.util.Decision;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Constructor extends Definition implements Function, BranchKey, CoreConstructor {
  private final DataDefinition myDataType;
  private DependentLink myParameters;
  private List<ExpressionPattern> myPatterns;
  private Body myConditions;
  private List<Integer> myParametersTypecheckingOrder;
  private List<Boolean> myGoodThisParameters = Collections.emptyList();
  private List<TypeClassParameterKind> myTypeClassParameters = Collections.emptyList();

  public Constructor(TCReferable referable, DataDefinition dataType) {
    super(referable, TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
    myDataType = dataType;
    myParameters = EmptyDependentLink.getInstance();
  }

  public void setBody(Body conditions) {
    myConditions = conditions;
  }

  public List<ExpressionPattern> getPatterns() {
    return myPatterns;
  }

  public void setPatterns(List<ExpressionPattern> patterns) {
    myPatterns = patterns;
  }

  @Override
  public Body getBody() {
    return myConditions;
  }

  @Nonnull
  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  @Override
  public int getNumberOfParameters() {
    return DependentLink.Helper.size(myParameters);
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  @Nonnull
  @Override
  public DataDefinition getDataType() {
    return myDataType;
  }

  public DependentLink getDataTypeParameters() {
    return myDataType.status().headerIsOK() ? (myPatterns == null ? myDataType.getParameters() : Pattern.getFirstBinding(myPatterns)) : EmptyDependentLink.getInstance();
  }

  public List<Expression> matchDataTypeArguments(List<Expression> arguments) {
    assert myDataType.status().headerIsOK();
    if (myPatterns == null) {
      return arguments;
    } else {
      List<Expression> result = new ArrayList<>();
      return ExpressionPattern.match(myPatterns, arguments, result) == Decision.YES ? result : null;
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
        arguments = new ArrayList<>(myPatterns.size());
        for (ExpressionPattern pattern : myPatterns) {
          arguments.add(pattern.toExpression());
        }
      } else {
        ExprSubstitution substitution = new ExprSubstitution();
        DependentLink link = Pattern.getFirstBinding(myPatterns);
        for (Expression argument : dataTypeArguments) {
          substitution.add(link, argument);
          link = link.getNext();
        }

        arguments = new ArrayList<>(myPatterns.size());
        for (ExpressionPattern pattern : myPatterns) {
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
  public void setTypeClassParameters(List<TypeClassParameterKind> typeClassParameters) {
    myTypeClassParameters = typeClassParameters;
  }

  @Override
  public DataCallExpression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
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

  @Override
  public void fill() {
    if (myParameters == null) {
      myParameters = EmptyDependentLink.getInstance();
    }
  }
}