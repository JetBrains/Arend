package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.LamExpression;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class LamExpressionMapper extends Mapper<LamExpression, LamExpressionCell> {
  public LamExpressionMapper(LamExpression source) {
    super(source, new LamExpressionCell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().variable, getTarget().variable.text()));

    ProjectionalRoleSynchronizer<Node, Expression> termSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().body, getTarget().body, new ExpressionMapperFactory());
    termSynchronizer.setPlaceholderText("<term>");
    conf.add(termSynchronizer);
  }
}
