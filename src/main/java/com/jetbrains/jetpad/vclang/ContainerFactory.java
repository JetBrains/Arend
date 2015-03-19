package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.editor.ModuleMapper;
import com.jetbrains.jetpad.vclang.editor.error.ErrorListMapper;
import com.jetbrains.jetpad.vclang.model.Module;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.projectional.util.RootController;

import static com.jetbrains.jetpad.vclang.model.expr.Model.*;

public class ContainerFactory {
  private static CellContainer MAIN_CONTAINER;
  private static CellContainer ERRORS_CONTAINER;
  private static ModuleMapper MAIN_ROOT_MAPPER;
  private static ErrorListMapper ERRORS_ROOT_MAPPER;

  static {
    Module m = createModel();
    MAIN_ROOT_MAPPER = new ModuleMapper(m);
    MAIN_ROOT_MAPPER.attachRoot();

    MAIN_CONTAINER = new CellContainer();
    MAIN_CONTAINER.root.children().add(MAIN_ROOT_MAPPER.getTarget());
    RootController.install(MAIN_CONTAINER);
  }

  static {
    ERRORS_ROOT_MAPPER = new ErrorListMapper();
    ERRORS_ROOT_MAPPER.attachRoot();

    ERRORS_CONTAINER = new CellContainer();
    ERRORS_CONTAINER.root.children().add(ERRORS_ROOT_MAPPER.getTarget());
    RootController.install(ERRORS_CONTAINER);
  }

  public static CellContainer getMainContainer() {
    return MAIN_CONTAINER;
  }

  public static CellContainer getErrorsContainer() {
    return ERRORS_CONTAINER;
  }

  public static ModuleMapper getMainRootMapper() {
    return MAIN_ROOT_MAPPER;
  }

  public static ErrorListMapper getErrorsRootMapper() {
    return ERRORS_ROOT_MAPPER;
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
