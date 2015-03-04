package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.AppExpression;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static jetbrains.jetpad.cell.util.CellFactory.indent;
import static jetbrains.jetpad.cell.util.CellFactory.space;

public class AppExpressionMapper extends Mapper<AppExpression, AppExpressionMapper.Cell> {
  public AppExpressionMapper(AppExpression source) {
    super(source, new AppExpressionMapper.Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    ProjectionalRoleSynchronizer<Node, Expression> functionSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().function, getTarget().function, ExpressionMapperFactory.getInstance());
    functionSynchronizer.setPlaceholderText("<fun>");
    functionSynchronizer.setCompletion(ExpressionCompletion.getAppFunInstance());
    conf.add(functionSynchronizer);

    ProjectionalRoleSynchronizer<Node, Expression> argumentSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().argument, getTarget().argument, ExpressionMapperFactory.getInstance());
    argumentSynchronizer.setPlaceholderText("<arg>");
    argumentSynchronizer.setCompletion(ExpressionCompletion.getAppArgInstance());
    conf.add(argumentSynchronizer);
  }

  public static class Cell extends IndentCell {
    public jetbrains.jetpad.cell.Cell function = indent();
    public jetbrains.jetpad.cell.Cell argument = indent();

    public Cell() {
      CellFactory.to(this,
          function,
          space(),
          argument);

      focusable().set(true);
    }
  }
}
