package org.arend.extImpl;

import org.arend.core.definition.Definition;
import org.arend.ext.dependency.ArendDependencyProvider;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.dependency.Dependency;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.ArendRef;
import org.arend.library.Library;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collections;

public class ArendDependencyProviderImpl extends Disableable implements ArendDependencyProvider {
  private final TypecheckingOrderingListener myTypechecking;
  private final ModuleScopeProvider myModuleScopeProvider;
  private final DefinitionRequester myDefinitionRequester;
  private final Library myLibrary;

  public ArendDependencyProviderImpl(TypecheckingOrderingListener typechecking, ModuleScopeProvider moduleScopeProvider, DefinitionRequester definitionRequester, Library library) {
    myTypechecking = typechecking;
    myModuleScopeProvider = moduleScopeProvider;
    myDefinitionRequester = definitionRequester;
    myLibrary = library;
  }

  private @NotNull Referable getReferable(@NotNull ModulePath module, @NotNull LongName name) {
    checkEnabled();
    Scope scope = myModuleScopeProvider.forModule(module);
    Referable ref = scope == null ? null : Scope.resolveName(scope, name.toList(), true);
    if (ref == null) {
      throw new IllegalArgumentException("Cannot find definition '" + name + "'");
    }
    return ref;
  }

  @Override
  public @NotNull Referable getRef(@NotNull ModulePath module, @NotNull LongName name) {
    return myTypechecking.getReferableConverter().convert(getReferable(module, name));
  }

  @NotNull
  @Override
  public <T extends CoreDefinition> T getDefinition(@NotNull ModulePath module, @NotNull LongName name, Class<T> clazz) {
    Referable ref = getReferable(module, name);
    Concrete.ReferableDefinition def;
    var generalDef = ref instanceof GlobalReferable ? myTypechecking.getConcreteProvider().getConcrete((GlobalReferable) ref) : null;
    if (generalDef instanceof Concrete.ReferableDefinition) {
      def = (Concrete.ReferableDefinition) generalDef;
    } else {
      throw new IllegalArgumentException("Cannot find definition '" + name + "'");
    }
    myTypechecking.typecheckDefinitions(Collections.singletonList(def.getRelatedDefinition()), null, false);
    Definition result = def.getData().getTypechecked();
    if (!clazz.isInstance(result)) {
      throw new IllegalArgumentException(result == null ? "Cannot find definition '" + name + "'" : "Cannot cast definition '" + name + "' of type '" + result.getClass() + "' to '" + clazz + "'");
    }
    myDefinitionRequester.request(result, myLibrary);
    return clazz.cast(result);
  }

  private void load(@NotNull Object dependencyContainer, boolean isDef) {
    try {
      for (Field field : dependencyContainer.getClass().getDeclaredFields()) {
        Class<?> fieldType = field.getType();
        if (isDef ? CoreDefinition.class.isAssignableFrom(fieldType) : ArendRef.class.isAssignableFrom(fieldType)) {
          Dependency dependency = field.getAnnotation(Dependency.class);
          if (dependency != null) {
            field.setAccessible(true);
            String name = dependency.name();
            ModulePath module = ModulePath.fromString(dependency.module());
            LongName longName = name.isEmpty() ? new LongName(field.getName()) : LongName.fromString(name);
            field.set(dependencyContainer, isDef ? getDefinition(module, longName, fieldType.asSubclass(CoreDefinition.class)) : getRef(module, longName));
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void loadRefs(@NotNull Object dependencyContainer) {
    load(dependencyContainer, false);
  }

  @Override
  public void load(@NotNull Object dependencyContainer) {
    load(dependencyContainer, true);
  }
}
