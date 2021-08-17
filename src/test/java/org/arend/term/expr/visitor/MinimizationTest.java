package org.arend.term.expr.visitor;

import org.arend.core.subst.LevelPair;
import org.arend.term.prettyprint.MinimizedRepresentation;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class MinimizationTest extends TypeCheckingTestCase {

    private void checkType(String module, String expected) {
        typeCheckModule(module);
        var type = getDefinition("test").getTypeWithParams(new ArrayList<>(), LevelPair.STD);
        var minimizedConcrete = MinimizedRepresentation.generateMinimizedRepresentation(type, null, null);
        typeCheckExpr(minimizedConcrete, null); // printed expression should be type checkable
        assertEquals(expected, minimizedConcrete.toString());
    }

    @Test
    public void minimizeImplicitNat() {
        checkType("\\data D {x : Nat} (y : Nat) | d \n"
                + "\\func test : D {1} 2 => d",
                "D {1} 2");

    }

    @Test
    public void minimizeShouldNotInsertInferrableAguments() {
        checkType("\\data D {A : \\Type} (x : A) | d" +
                "\\func test : D {Nat} 1 => d", "D 1");
    }

    @Test
    public void testSigma() {
        checkType("\\data D {A : \\Type} (x : A) | d\n"
        + "\\data C {y : Nat} (x : Nat) | c\n" +
                "\\func test : \\Sigma (D 1) (C {2} 1) => (d, c)", "\\Sigma (D 1) (C {2} 1)");
    }

    @Test
    public void testFunction() {
        checkType("\\func D {A : \\Type} (x : A) : \\Type => x = x\n" +
                "\\func test : D 1 => idp", "D 1");
    }

    @Test
    public void testInfix() {
        checkType("\\data D {A : \\Type} (x : A) | d\n" +
                "\\func \\infixr 10 === {A : \\Type} (x : A) (y : A) : \\Type => x = y\n" +
                "\\func test : (D 1) === (D 1) => idp", "D 1 === D 1");
    }

    @Test
    public void testInfix2() {
        checkType("\\data D {A : \\Type} (x : A) | d\n" +
                "\\func \\infixr 10 === {z : Nat} (x : Nat) (y : Nat) : \\Type => x = y\n" +
                "\\func test : 1 === {2} 1 => idp", "1 === {2} 1");
    }
}
