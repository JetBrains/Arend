package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.Pattern;

import java.util.*;

public class GoodThisParametersVisitor extends VoidExpressionVisitor<Void> {
  private final List<Boolean> myGoodParameters;
  private final Map<DependentLink, Integer> myIndexMap;
  private final Set<ClassField> myGoodFields;

  public GoodThisParametersVisitor(List<Boolean> goodParameters, DependentLink parameters) {
    myGoodFields = Collections.emptySet();
    myGoodParameters = new ArrayList<>();
    myIndexMap = new HashMap<>();
    int i = 0;
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext(), i++) {
      myGoodParameters.add(i < goodParameters.size() ? goodParameters.get(i) : true);
      myIndexMap.put(link, i);
    }
  }

  public GoodThisParametersVisitor(DependentLink parameters) {
    myGoodFields = Collections.emptySet();
    if (!parameters.hasNext()) {
      myGoodParameters = new ArrayList<>(0);
      myIndexMap = Collections.emptyMap();
      return;
    }

    myGoodParameters = new ArrayList<>();
    myIndexMap = new HashMap<>();
    int i = 0;
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext(), i++) {
      myGoodParameters.add(true);
      myIndexMap.put(link, i);
    }
    visitParameters(parameters, null);
  }

  public GoodThisParametersVisitor(List<Boolean> goodParameters, ElimBody elimBody, int numberOfParameters) {
    myGoodFields = Collections.emptySet();
    myGoodParameters = new ArrayList<>();
    for (int i = 0; i < numberOfParameters; i++) {
      myGoodParameters.add(i < goodParameters.size() ? goodParameters.get(i) : true);
    }
    myIndexMap = new HashMap<>();

    for (ElimClause<Pattern> clause : elimBody.getClauses()) {
      for (int i = 0; i < numberOfParameters; i++) {
        Pattern pattern = clause.getPatterns().get(i);
        if (pattern instanceof BindingPattern) {
          myIndexMap.put(pattern.getFirstBinding(), i);
        } else {
          myGoodParameters.set(i, false);
        }
      }
      if (clause.getExpression() != null) {
        clause.getExpression().accept(this, null);
      }
      myIndexMap.clear();
    }
  }

  public GoodThisParametersVisitor(Set<ClassField> fields) {
    myGoodFields = fields;
    myGoodParameters = new ArrayList<>(0);
    myIndexMap = Collections.emptyMap();
  }

  public List<Boolean> getGoodParameters() {
    for (Boolean good : myGoodParameters) {
      if (good) {
        return myGoodParameters;
      }
    }
    return Collections.emptyList();
  }

  public Set<ClassField> getGoodFields() {
    return myGoodFields.isEmpty() ? Collections.emptySet() : myGoodFields;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    Binding binding = expr.getBinding();
    if (binding instanceof DependentLink) {
      Integer index = myIndexMap.get(binding);
      if (index != null) {
        myGoodParameters.set(index, false);
      }
    }
    return null;
  }

  private void visitArgument(Expression arg, Boolean goodParameter) {
    arg = arg.getUnderlyingExpression();
    if (arg instanceof FieldCallExpression && goodParameter) {
      visitDefCall((FieldCallExpression) arg, null);
    } else if (goodParameter && (arg instanceof ReferenceExpression || arg instanceof NewExpression && ((NewExpression) arg).getRenewExpression() instanceof ReferenceExpression)) {
      if (arg instanceof NewExpression) {
        visitClassCall(((NewExpression) arg).getClassCall(), null);
      }
    } else {
      arg.accept(this, null);
    }
  }

  private void visitArguments(List<? extends Expression> args, List<Boolean> goodParameters) {
    for (int i = 0; i < args.size(); i++) {
      visitArgument(args.get(i), i < goodParameters.size() && goodParameters.get(i));
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    visitArguments(expr.getDefCallArguments(), expr.getDefinition().getGoodThisParameters());
    return null;
  }

  @Override
  public Void visitConCall(ConCallExpression expr, Void params) {
    Expression it = expr;
    boolean goodParam = false;
    do {
      expr = (ConCallExpression) it;

      visitArguments(expr.getDataTypeArguments(), expr.getDefinition().getDataType().getGoodThisParameters());

      int recursiveParam = expr.getDefinition().getRecursiveParameter();
      if (recursiveParam < 0) {
        visitArguments(expr.getDefCallArguments(), expr.getDefinition().getGoodThisParameters());
        return null;
      }

      List<Boolean> goodParams = expr.getDefinition().getGoodThisParameters();
      for (int i = 0; i < expr.getDefCallArguments().size(); i++) {
        if (i != recursiveParam) {
          visitArgument(expr.getDefCallArguments().get(i), i < goodParams.size() && goodParams.get(i));
        } else {
          goodParam = i < goodParams.size() && goodParams.get(i);
        }
      }

      it = expr.getDefCallArguments().get(recursiveParam);
    } while (it instanceof ConCallExpression);

    visitArgument(it, goodParam);
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      Expression arg = entry.getValue().getUnderlyingExpression();
      if (arg instanceof FieldCallExpression && expr.getDefinition().isGoodField(entry.getKey())) {
        visitDefCall((FieldCallExpression) arg, null);
      } else if ((arg instanceof ReferenceExpression || arg instanceof NewExpression && ((NewExpression) arg).getRenewExpression() instanceof ReferenceExpression) && expr.getDefinition().isGoodField(entry.getKey())) {
        if (arg instanceof NewExpression) {
          visitClassCall(((NewExpression) arg).getClassCall(), null);
        }
      } else {
        arg.accept(this, null);
      }
    }
    return null;
  }

  @Override
  public Void visitFieldCall(FieldCallExpression expr, Void params) {
    if (!myGoodFields.isEmpty()) {
      myGoodFields.remove(expr.getDefinition());
    }
    return super.visitFieldCall(expr, params);
  }

  @Override
  public Void visitTypeConstructor(TypeConstructorExpression expr, Void params) {
    visitArguments(expr.getClauseArguments(), expr.getDefinition().getGoodThisParameters());
    expr.getArgument().accept(this, null);
    return null;
  }

  @Override
  public Void visitTypeDestructor(TypeDestructorExpression expr, Void params) {
    expr.getArgument().accept(this, null);
    return null;
  }

}
