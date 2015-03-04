package com.jetbrains.jetpad.vclang.editor.definition;

import com.jetbrains.jetpad.vclang.editor.expr.ExpressionCompletion;
import com.jetbrains.jetpad.vclang.editor.expr.ExpressionMapperFactory;
import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.mapper.Mapper;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.cell.Utils.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.*;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class FunctionDefinitionMapper extends Mapper<FunctionDefinition, FunctionDefinitionMapper.Cell> {
  public FunctionDefinitionMapper(FunctionDefinition source) {
    super(source, new FunctionDefinitionMapper.Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().name, getTarget().name.text()));

    ProjectionalRoleSynchronizer<Node, Expression> typeSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().resultType, getTarget().type, ExpressionMapperFactory.getInstance());
    typeSynchronizer.setPlaceholderText("<type>");
    typeSynchronizer.setCompletion(ExpressionCompletion.getGlobalInstance());
    conf.add(typeSynchronizer);

    ProjectionalRoleSynchronizer<Node, Expression> termSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().term, getTarget().term, ExpressionMapperFactory.getInstance());
    termSynchronizer.setPlaceholderText("<term>");
    termSynchronizer.setCompletion(ExpressionCompletion.getGlobalInstance());
    conf.add(termSynchronizer);
  }

  public static class Cell extends IndentCell {
    final TextCell name = noDelete(new TextCell());
    final jetbrains.jetpad.cell.Cell type = noDelete(indent());
    final jetbrains.jetpad.cell.Cell term = noDelete(indent());

    Cell() {
      to(this,
          noDelete(keyword("function")),
          newLine(),
          name,
          placeHolder(name, "<no name>"),
          space(),
          text(":"),
          space(),
          type,
          space(),
          text("=>"),
          space(),
          term);

      focusable().set(true);
      name.addTrait(TextEditing.validTextEditing(Validators.identifier()));
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(name));
    }
  }
}
