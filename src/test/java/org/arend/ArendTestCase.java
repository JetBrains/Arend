package org.arend;

import org.arend.error.ListErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.extImpl.DefinitionRequester;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.PositionComparator;
import org.arend.frontend.library.PreludeFileLibrary;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.scope.Scope;
import org.arend.prelude.Prelude;
import org.arend.prelude.PreludeLibrary;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.text;
import static org.arend.ext.prettyprinting.doc.DocFactory.vList;
import static org.junit.Assert.assertThat;

public abstract class ArendTestCase {
  protected LibraryManager libraryManager;
  protected Library preludeLibrary;
  protected ModuleScopeProvider moduleScopeProvider;

  protected final TypecheckerState typecheckerState = new SimpleTypecheckerState();
  protected final List<GeneralError> errorList = new ArrayList<>();
  protected final ListErrorReporter errorReporter = new ListErrorReporter(errorList);
  protected final TypecheckingOrderingListener typechecking = new TypecheckingOrderingListener(new InstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, errorReporter, PositionComparator.INSTANCE);

  @Before
  public void loadPrelude() {
    libraryManager = new LibraryManager((lib,name) -> { throw new IllegalStateException(); }, new InstanceProviderSet(), errorReporter, errorReporter, DefinitionRequester.INSTANCE);
    preludeLibrary = new PreludeFileLibrary(null, typecheckerState);
    moduleScopeProvider = preludeLibrary.getModuleScopeProvider();
    libraryManager.loadLibrary(preludeLibrary, null);
    new Prelude.PreludeTypechecking(new InstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, PositionComparator.INSTANCE).typecheckLibrary(preludeLibrary);
    errorList.clear();
  }

  public void setModuleScopeProvider(ModuleScopeProvider moduleScopeProvider) {
    this.moduleScopeProvider = module -> module.equals(Prelude.MODULE_PATH) ? PreludeLibrary.getPreludeScope() : moduleScopeProvider.forModule(module);
  }

  public TCReferable get(Scope scope, String path) {
    Referable referable = Scope.Utils.resolveName(scope, Arrays.asList(path.split("\\.")));
    return referable instanceof TCReferable ? (TCReferable) referable : null;
  }

  @SafeVarargs
  protected final void assertThatErrorsAre(Matcher<? super GeneralError>... matchers) {
    assertThat(errorList, Matchers.contains(matchers));
  }


  protected static Matcher<? super Collection<? extends GeneralError>> containsErrors(final int n) {
    return new TypeSafeDiagnosingMatcher<Collection<? extends GeneralError>>() {
      @Override
      protected boolean matchesSafely(Collection<? extends GeneralError> errors, Description description) {
        if (errors.size() == 0) {
          description.appendText("there were no errors");
        } else {
          List<Doc> docs = new ArrayList<>(errors.size() + 1);
          docs.add(text("there were errors:"));
          for (GeneralError error : errors) {
            docs.add(error.getDoc(PrettyPrinterConfig.DEFAULT));
          }
          description.appendText(vList(docs).toString());
        }
        return n < 0 ? !errors.isEmpty() : errors.size() == n;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("expected number of errors: ").appendValue(n);
      }
    };
  }
}
