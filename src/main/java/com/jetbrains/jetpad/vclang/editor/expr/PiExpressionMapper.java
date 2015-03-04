package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.editor.util.Validators;
import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.PiExpression;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.*;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class PiExpressionMapper extends Mapper<PiExpression, PiExpressionMapper.Cell> {
  public PiExpressionMapper(PiExpression source) {
    super(source, new PiExpressionMapper.Cell(source.parens));
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().variable, getTarget().variable.text()));

    ProjectionalRoleSynchronizer<Node, Expression> domainSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().domain, getTarget().domain, ExpressionMapperFactory.getInstance());
    domainSynchronizer.setPlaceholderText("<dom>");
    domainSynchronizer.setCompletion(ExpressionCompletion.getGlobalInstance());
    conf.add(domainSynchronizer);

    ProjectionalRoleSynchronizer<Node, Expression> codomainSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().codomain, getTarget().codomain, ExpressionMapperFactory.getInstance());
    codomainSynchronizer.setPlaceholderText("<cod>");
    codomainSynchronizer.setCompletion(ExpressionCompletion.getGlobalInstance());
    conf.add(codomainSynchronizer);
  }

  public static class Cell extends IndentCell {
    public final TextCell variable = noDelete(new TextCell());
    public final jetbrains.jetpad.cell.Cell domain = noDelete(indent());
    public final jetbrains.jetpad.cell.Cell codomain = noDelete(indent());

    public Cell(boolean parens) {
      if (parens) children().add(text("("));
      CellFactory.to(this,
          text("("),
          variable,
          placeHolder(variable, "<no name>"),
          space(),
          text(":"),
          space(),
          domain,
          text(")"),
          space(),
          text("->"),
          space(),
          codomain);
      if (parens) children().add(text(")"));

      focusable().set(true);
      variable.addTrait(TextEditing.validTextEditing(Validators.identifier()));
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(variable));
    }
  }
}
