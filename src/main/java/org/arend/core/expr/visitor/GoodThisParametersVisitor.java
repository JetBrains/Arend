package org.arend.core.expr.visitor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;

import java.util.*;

public class GoodThisParametersVisitor extends VoidExpressionVisitor<Void> {
  private final List<Boolean> myGoodParameters;
  private final Map<DependentLink, Integer> myIndexMap;
  private final Set<ClassField> myGoodFields;

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

  public GoodThisParametersVisitor(ElimTree elimTree, int numberOfParameters) {
    myGoodFields = Collections.emptySet();
    myGoodParameters = new ArrayList<>();
    for (int i = 0; i < numberOfParameters; i++) {
      myGoodParameters.add(true);
    }
    myIndexMap = new HashMap<>();
    checkElimTree(elimTree, 0, 0);
  }

  public GoodThisParametersVisitor(Set<ClassField> fields) {
    myGoodFields = fields;
    myGoodParameters = new ArrayList<>(0);
    myIndexMap = Collections.emptyMap();
  }

  private void checkElimTree(ElimTree elimTree, int index, int skip) {
    for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
      if (skip == 0) {
        myIndexMap.put(link, index);
        index++;
      } else {
        skip--;
      }
    }

    if (elimTree instanceof LeafElimTree) {
      ((LeafElimTree) elimTree).getExpression().accept(this, null);
    } else {
      if (skip == 0) {
        myGoodParameters.set(index, false);
        index++;
      } else {
        skip--;
      }

      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        int toSkip = entry.getKey() == null ? 1 :
          entry.getKey() instanceof BranchElimTree.TupleConstructor
            ? ((BranchElimTree.TupleConstructor) entry.getKey()).getLength()
            : DependentLink.Helper.size(entry.getKey().getParameters());
        checkElimTree(entry.getValue(), index, skip + toSkip);
      }
    }
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
    if (expr.getBinding() instanceof DependentLink) {
      Integer index = myIndexMap.get(expr.getBinding());
      if (index != null) {
        myGoodParameters.set(index, false);
      }
    }
    return null;
  }

  private void visitArguments(List<? extends Expression> args, List<Boolean> goodParameters) {
    for (int i = 0; i < args.size(); i++) {
      if (args.get(i) instanceof FieldCallExpression && i < goodParameters.size() && goodParameters.get(i)) {
        visitDefCall((FieldCallExpression) args.get(i), null);
      } else if (!(args.get(i) instanceof ReferenceExpression && i < goodParameters.size() && goodParameters.get(i))) {
        args.get(i).accept(this, null);
      }
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    visitArguments(expr.getDefCallArguments(), expr.getDefinition().getGoodThisParameters());
    return null;
  }

  @Override
  public Void visitConCall(ConCallExpression expr, Void params) {
    if (expr.getDefinition().getPatterns() == null) {
      visitArguments(expr.getDataTypeArguments(), expr.getDefinition().getDataType().getGoodThisParameters());
      visitArguments(expr.getDefCallArguments(), expr.getDefinition().getGoodThisParameters());
    } else {
      List<Expression> args = new ArrayList<>(expr.getDataTypeArguments());
      args.addAll(expr.getDefCallArguments());
      visitArguments(args, expr.getDefinition().getGoodThisParameters());
    }
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      if (entry.getValue() instanceof FieldCallExpression && expr.getDefinition().isGoodField(entry.getKey())) {
        visitDefCall((FieldCallExpression) entry.getValue(), null);
      } else if (!(entry.getValue() instanceof ReferenceExpression && expr.getDefinition().isGoodField(entry.getKey()))) {
        entry.getValue().accept(this, null);
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
}
