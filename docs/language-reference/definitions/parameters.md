<h1 id="parameters">Parameters<a class="headerlink" href="#implicit-arguments" title="Permanent link">&para;</a></h1>

The syntax of a number of language constructions in Arend includes parameter declarations.
Parameter declarations can be named or unnamed, typed or untyped and explicit or implicit.

Some of the language constructions allow unnamed parameter declarations at the same time requiring them to be typed
 (examples are definitions of data or its constructors, `\Pi` or `\Sigma` expressions).
Most of the other constructions require their parameter declarations to be named while allowing them to be untyped
 (exceptions to this rule are functions, records and instances which require parameter declarations to be named and typed at the same time).
```arend
\data d
  | c1 (a : Nat)   -- typed named explicit
  | c2 Nat         -- typed unnamed explicit
  | c3 {a : Nat}   -- typed named implicit
  | c4 {Nat}       -- typed unnamed implicit
{-| c5 b           -- untyped parameter declarations are not allowed in constructors -}

\func g1 => 
  \lam a => suc a  -- untyped named explicit

\func g2 =>
  \lam {a} =>      -- untyped named implicit
    suc a

{-
\func f a => a     -- parameters of functions must be named and typed
\func f Nat => 1
 -}
```

## Syntax

A named explicit parameter has the form `(x : T)`, where `x` is the name of the parameter and `T` is its type expression.
An implicit parameter has the form `{x : T}`, where `x` and `T` are the same as before.

Parameters are specified after the name of a definition.
For example, all of the definitions below have three parameters: `x1` of type `A1`, `x2` of type `A2`, and `x3` of type `A3`.

```arend
\func f {x1 : A1} {x2 : A2} {x3 : A3} => 0
\data D (x1 : A1) (x2 : A2) (x3 : A3)
\class C (x1 : A1) {x2 : A2} (x3 : A3)
```

Multiple parameters of the same type can be specified via the following syntax: `x_1 ... x_n : T`.
For example, the following function has two implicit parameters of type `A1`, three explicit parameters of type `A2`, and one explicit parameter of type `A3`:

```arend
\func f {x1 x2 : A1} (y1 y2 y3 : A2) (z : A3) => B
```

This definition is equivalent to the following one:

```arend
\func f {x1 : A1} {x2 : A1} (y1 : A2) (y2 : A2) (y3 : A2) (z : A3) => B
```
The types of subsequent parameters may depend on the previous ones.
In the example above, parameters `x1` and `x2` may appear in `A2` and `A3`, parameters `y1`, `y2`, and `y3` may appear in `A3`, and all of the parameters may appear in `B`.

If a parameter is never used, its name can be replaced with `_`.
Such a name cannot be refered to, so this simply indicated that this parameter is ignored.

## Implicit arguments

Let `f` be a definition with parameters of types `A_1`, ... `A_n`.
If all of the parameters are explicit, then we can form an expression of the form `f a_1 ... a_n`, where `a_i` is an expression of type `A_i`.
Such an expression invokes `f` with the specified arguments.
If some of the parameters of `f` are implicit, then corresponding arguments must be omitted.
For example, consider the following code:

```arend
\func f (x : A1) {y y' : A2} (z : A3) {z : A4} => 0
\func g => f a1 a3
```

In the expression `f a1 a3`, arguments corresponding to parameters `y`, `y'`, and `z` are omitted.
The typechecker tries to infer these parameters and reports an error if it fails to do so.
We can ask typechecker to try to infer an explicit parameter by writing `_` instead of the corresponding argument:

```arend
\func f (x : A1) {y y' : A2} (z : A3) {z : A4} => 0
\func g => f _ a3
```

In the example above, the typechecker will try to infer the argument corresponding to `x`.
Actually, the expression `_` can be written anywhere at all.
The typechecker infers the omitted expression only if there is a unique solutions to the inference problem 
 (i. e. there is only one expression with which `_` can be replaced so that the surrounding definition typechecks correctly).

Finally, if the typechecker cannot infer an implicit argument, it can be specified explicitly by writing `{e}`.
For example, to specify explicitly the second and the last arguments of `f`, we can write the following code:

```arend
\func f (x : A1) {y y' : A2} (z : A3) {z : A4} => 0
\func g => f _ {a2} a3 {a4}
```

In this example, arguments corresponding to `x` and `y'` are left implicit and other arguments are explicitly specified.

If `op` is an infix operator, then we can write `x op {a_1} ... {a_n} y`, which is equivalent to `op {a_1} ... {a_n} x y`.
In other words, implicit arguments which are written immediately after an infix operator are considered to be its first arguments.
```arend
\func f (A : \Type) => \lam a b => a = {A} b
```
