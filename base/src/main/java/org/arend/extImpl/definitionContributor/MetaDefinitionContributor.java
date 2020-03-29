package org.arend.extImpl.definitionContributor;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.library.Library;
import org.arend.naming.reference.MetaReferable;

public interface MetaDefinitionContributor {
  MetaReferable declare(Library library, ModulePath module, LongName longName, Precedence precedence, MetaDefinition meta);

  MetaDefinitionContributor INSTANCE = (library, module, longName, precedence, meta) -> new MetaReferable(precedence, longName.getLastName(), meta);
}
