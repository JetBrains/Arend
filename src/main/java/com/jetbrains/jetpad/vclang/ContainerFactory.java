package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.editor.ModuleMapper;
import com.jetbrains.jetpad.vclang.model.Module;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.util.RootController;

import static com.jetbrains.jetpad.vclang.model.expr.Model.*;

public class ContainerFactory {
  public static CellContainer getContainer() {
    Module m = createModel();
    Mapper<Module, ModuleMapper.Cell> rootMapper = new ModuleMapper(m);
    rootMapper.attachRoot();

    CellContainer cellContainer = new CellContainer();
    cellContainer.root.children().add(rootMapper.getTarget());
    RootController.install(cellContainer);

    return cellContainer;
  }

  private static Module createModel() {
    Module result = new Module();
    FunctionDefinition def = new FunctionDefinition();
    result.definitions.add(def);
    LamExpression expr1 = new LamExpression();
    def.term().set(expr1);
    expr1.variable().set("x");
    VarExpression expr2 = new VarExpression();
    expr1.body().set(expr2);
    expr2.name().set("x");
    PiExpression expr3 = new PiExpression();
    def.resultType().set(expr3);
    expr3.domain().set(new NatExpression());
    expr3.codomain().set(new NatExpression());
    return result;
  }
}
