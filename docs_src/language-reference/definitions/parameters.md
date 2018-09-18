<h1 id="parameters">Parameters<a class="headerlink" href="#implicit-arguments" title="Permanent link">&para;</a></h1>

Most kinds of definitions can have parameters.
A parameter can be named or unnamed, explicit or implicit.
Implicit parameters are always named.
An unnamed parameter is simply an expression which denotes the type of the parameter.

## Syntax

A named explicit parameter has the form `(x : T)`, where `x` is the name of the parameter and `T` is an expression which denotes the type of the parameter.
An implicit parameter has the form `{x : T}`, where `x` and `T` are the same as before.

Parameters are specified after the name of a definition.
For example, all of the definitions below have three parameters: `x1` of type `A1`, `x2` of type `A2`, and `x3` of type `A3`.

```arend
\func f {x1 : A1} {x2 : A2} {x3 : A3} => 0
\data D (x1 : A1) (x2 : A2) (x3 : A3)
\class C (x1 : A1) {x2 : A2} (x3 : A3)
```

If several consecutive parameters have the same type, it can be specified only once by using the following syntax: `x_1 ... x_n : T`.
For example, the following function has two implicit parameters of type `A1`, three explicit parameters of type `A2`, and one explicit parameter of type `A3`:

```arend
\func f {x1 x2 : A1} (y1 y2 y3 : A2) (z : A3) => 0
```

This definition is equivalent to the following one:

```arend
\func f {x1 : A1} {x2 : A1} (y1 : A2) (y2 : A2) (y3 : A2) (z : A3) => 0
```

Parameters can be refered from the types of subsequent parameters and from the body of the definition.
In the example above, parameters `x1` and `x2` may appear in `A2` and `A3`, parameters `y1`, `y2`, and `y3` may appear in `A3`, and all of the parameters may appear in the expression after `=>`.

If a pattern is never used anywhere, its name can be replaced with `_`.
Such a name cannot be refered to, so this simply indicated that this parameter is ignored.

## Implicit arguments

Let `f` be a definition with parameters of types `A_1`, ... `A_n`.
If all of the parameters are explicit, then to we can form an expression of the form `f a_1 ... a_n`, where `a_i` is an expression of type `A_i`.
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
The expression `_` can be written actually anywhere at all.
The typechecker infers omitted expressions only when there is a unique solution that makes the definition in which they appear to typecheck.

Finally, if the typechecker cannot infer an implicit argument, it can be specified explicitly by writing `{e}`.
For example, to specify explicitly the second and the last arguments of `f`, we can write the following code:

```arend
\func f (x : A1) {y y' : A2} (z : A3) {z : A4} => 0
\func g => f _ {a2} a3 {a4}
```

In this example, arguments corresponding to `x` and `y'` are left implicit and other arguments are explicitly specified.

If `op` is an infix operator, then we can write `x op {a_1} ... {a_n} y`, which is equivalent to `op {a_1} ... {a_n} x y`.
That is, implicit arguments which are written immediately after an infix operator are considered to be its first arguments.
