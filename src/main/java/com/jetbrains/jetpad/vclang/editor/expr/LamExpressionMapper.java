package com.jetbrains.jetpad.vclang.editor.expr;

import com.google.common.base.Supplier;
import com.jetbrains.jetpad.vclang.editor.expr.arg.ArgumentMapperFactory;
import com.jetbrains.jetpad.vclang.model.expr.Model;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.cell.util.CellLists;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.model.expr.Model.LamExpression;
import static jetbrains.jetpad.cell.util.CellFactory.*;

public class LamExpressionMapper extends ExpressionMapper<LamExpression, LamExpressionMapper.Cell> {
  public LamExpressionMapper(LamExpression source) {
    super(source, new LamExpressionMapper.Cell(source.prec() > Abstract.LamExpression.PREC));
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    ProjectionalRoleSynchronizer<Model.LamExpression, Model.Argument> synchronizer = ProjectionalSynchronizers.forRole(this, getSource().getArguments(), getTarget(), CellLists.spaced(getTarget().children()), ArgumentMapperFactory.getInstance());
    synchronizer.setItemFactory(new Supplier<Model.Argument>() {
      @Override
      public Model.Argument get() {
        return new Model.NameArgument();
      }
    });
    conf.add(synchronizer);
    conf.add(forExpression(this, getSource().body(), getTarget().body, "<term>"));
  }

  public static class Cell extends IndentCell {
    public final TextCell variable = noDelete(new TextCell());
    public final jetbrains.jetpad.cell.Cell body = noDelete(indent());

    public Cell(boolean parens) {
      if (parens) children().add(label("("));

      CellFactory.to(this,
          label("λ"),
          variable,
          placeHolder(variable, "<no name>"),
          space(),
          label("=>"),
          space(),
          body);

      if (parens) children().add(label(")"));

      focusable().set(true);
      variable.addTrait(TextEditing.validTextEditing(Validators.identifier()));
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(variable));
    }
  }
}
