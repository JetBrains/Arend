package org.arend.ext.prettyprinting;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.module.LongName;

import java.util.Map;
import java.util.Set;

public interface DefinitionRenamer {
  Set<String> getNames();
  Map<String, CoreDefinition> getNotRenamedDefs();
  Map<CoreDefinition, LongName> getDefLongNames();
  void clear();
}
