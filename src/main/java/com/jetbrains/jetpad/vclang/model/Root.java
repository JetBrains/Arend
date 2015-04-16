package com.jetbrains.jetpad.vclang.model;

import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.otmodel.entity.Entity;
import jetbrains.jetpad.otmodel.entity.EntityConceptId;
import jetbrains.jetpad.otmodel.entity.EntityModelBlobId;
import jetbrains.jetpad.otmodel.wrapper.EntityWrapper;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

public class Root extends EntityWrapper<Root> {
  public static final EntityConceptId CONCEPT_ID = new EntityConceptId("HwGr4DlvUE0.BpUgnXa6Fcn", "RootEntity");
  public static final String WORKBOOK_ID = "D3-kEJcsCW4.GioU8DRqbLj";

  public final Property<Module> input = getModel(new EntityModelBlobId(WORKBOOK_ID, "input"));

  public Root(WrapperContext ctx) {
    super(ctx, CONCEPT_ID);
  }

  public Root(WrapperContext ctx, Entity entity) {
    super(ctx, entity);
  }
}
