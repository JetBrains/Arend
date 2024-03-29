package org.arend.term.expr.visitor;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.LetExpression;
import org.arend.core.subst.LevelPair;
import org.arend.term.prettyprint.MinimizedRepresentation;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class MinimizationTest extends TypeCheckingTestCase {

    private void selectiveCheck(String module, String expected, boolean isGround, Function<? super FunctionDefinition, ? extends Expression> selector) {
        typeCheckModule(module);
        var selected = selector.apply((FunctionDefinition) getDefinition("test"));
        var minimizedConcrete = MinimizedRepresentation.generateMinimizedRepresentation(selected, null, null, null);
        assertEquals(expected, minimizedConcrete.toString());
        if (isGround) {
            typeCheckExpr(minimizedConcrete, selected.getType());
        }
    }

    private void checkType(String module, String expected) {
        selectiveCheck(module, expected, true, definition -> definition.getTypeWithParams(new ArrayList<>(), LevelPair.STD));
    }

    private void checkLet(String module, String expected) {
        selectiveCheck(module, expected, false, definition -> ((LetExpression) Objects.requireNonNull(definition.getBody())).getExpression());
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

    @Test
    public void projections() {
        checkLet("\\func foo : \\Sigma Nat Nat => (1, 1)\n" +
                "\\data D {n : Nat} (m : Nat) | d\n" +
                "\\func test : \\Type => \\let (a, b) => foo \\in D {a} b", "D {a} b");
    }

    @Test
    public void nestedProjections() {
        checkLet("\\func foo : \\Sigma (\\Sigma Nat Nat) Nat => ((1, 1), 1)" +
                "\\data D {n : Nat} (m : Nat) | d\n" +
                "\\func test : \\Type => \\let ((a, b), c) => foo \\in D {a} c", "D {a} c");
    }

    @Test
    public void lambdaWithoutKnownReturnType() {
        selectiveCheck("\\func test : Nat -> Nat => \\lam a => a", "\\lam (a : Nat) => a", false, definition -> (Expression) definition.getBody());
    }

    @Test
    public void trailingImplicitArguments() {
        checkType("\\data D (y : Nat) {x : Nat} | d\n"
                        + "\\func test : D 2 {1} => d",
                "D 2 {1}");
    }

    @Test
    public void field() {
        checkLet("\\record Cl | x : Nat\n" +
                "\\func test : Nat => \\let ccl : Cl => \\new Cl { | x => 1} \\in ccl.x", "ccl.x");
    }

    @Test
    public void clazz() {
        checkLet("\\class Cl | x : Nat\n" +
                "\\func test : Nat => \\let ccl : Cl => \\new Cl { | x => 1} \\in ccl.x", "ccl.x");
    }
}
