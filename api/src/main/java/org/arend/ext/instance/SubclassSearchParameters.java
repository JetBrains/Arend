package org.arend.ext.instance;

import org.arend.ext.core.definition.CoreClassDefinition;
import org.jetbrains.annotations.NotNull;

public class SubclassSearchParameters implements InstanceSearchParameters {
  public final CoreClassDefinition classDefinition;

  public SubclassSearchParameters(CoreClassDefinition classDefinition) {
    this.classDefinition = classDefinition;
  }

  @Override
  public boolean testClass(@NotNull CoreClassDefinition classDefinition) {
    return classDefinition.isSubClassOf(this.classDefinition);
  }
}
