package com.jetbrains.jetpad.vclang.editor;

import com.jetbrains.jetpad.vclang.model.Module;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.cell.toView.CellToView;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.util.RootController;
import jetbrains.jetpad.projectional.view.ViewContainer;
import org.junit.Before;

public class EditingTestCase {
  protected final CellContainer myCellContainer = new CellContainer();
  protected final ViewContainer myViewContainer = new ViewContainer();
  protected final Module myModule = new Module();
  protected Mapper<Module, ModuleCell> myRootMapper = new ModuleMapper(myModule);

  @Before
  public void initContainers() {
    CellToView.map(myCellContainer, myViewContainer);
    myRootMapper.attachRoot();
    myCellContainer.root.children().add(myRootMapper.getTarget());
    RootController.install(myCellContainer);
  }
}
