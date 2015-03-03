package com.jetbrains.jetpad.vclang.editor.definition;

import com.jetbrains.jetpad.vclang.editor.expr.ExpressionMapperFactory;
import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.mapper.Mapper;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class FunctionDefinitionMapper extends Mapper<FunctionDefinition, FunctionDefinitionCell> {
  public FunctionDefinitionMapper(FunctionDefinition source) {
    super(source, new FunctionDefinitionCell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().name, getTarget().name.text()));

    ProjectionalRoleSynchronizer<Node, Expression> typeSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().resultType, getTarget().type, new ExpressionMapperFactory());
    typeSynchronizer.setPlaceholderText("<type>");
    conf.add(typeSynchronizer);

    ProjectionalRoleSynchronizer<Node, Expression> termSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().term, getTarget().term, new ExpressionMapperFactory());
    termSynchronizer.setPlaceholderText("<term>");
    conf.add(termSynchronizer);
  }
}
