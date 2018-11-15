package org.arend;

import org.arend.error.GeneralError;
import org.arend.error.ListErrorReporter;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocStringBuilder;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.PositionComparator;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.scope.Scope;
import org.arend.prelude.Prelude;
import org.arend.prelude.PreludeFileLibrary;
import org.arend.term.prettyprint.PrettyPrinterConfig;
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

import static org.arend.error.doc.DocFactory.text;
import static org.arend.error.doc.DocFactory.vList;
import static org.junit.Assert.assertThat;

public abstract class ArendTestCase {
  protected LibraryManager libraryManager;
  protected Library preludeLibrary;

  protected final TypecheckerState typecheckerState = new SimpleTypecheckerState();
  protected final List<GeneralError> errorList = new ArrayList<>();
  protected final ListErrorReporter errorReporter = new ListErrorReporter(errorList);
  protected final TypecheckingOrderingListener typechecking = new TypecheckingOrderingListener(new InstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, errorReporter, PositionComparator.INSTANCE);

  @Before
  public void loadPrelude() {
    libraryManager = new LibraryManager(name -> { throw new IllegalStateException(); }, EmptyModuleScopeProvider.INSTANCE, new InstanceProviderSet(), errorReporter, errorReporter);
    preludeLibrary = new PreludeFileLibrary(null, typecheckerState);
    libraryManager.setModuleScopeProvider(preludeLibrary.getModuleScopeProvider());
    libraryManager.loadLibrary(preludeLibrary);
    new Prelude.PreludeTypechecking(new InstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE).typecheckLibrary(preludeLibrary);
    errorList.clear();
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
          description.appendText(DocStringBuilder.build(vList(docs)));
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
