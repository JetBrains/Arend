<h1 id="records">Records<a class="headerlink" href="#records" title="Permanent link">&para;</a></h1>

A _record_ is a [sigma type](/language-reference/expressions/sigma) with named projections.
The basic syntax looks like this:

```arend
\record R (p_1 : A_1) ... (p_n : A_n) {
  | f_1 : B_1
  ...
  | f_k : B_k
}
```

It is also possible to write `\field f_i : B_i` instead of `| f_i : B_i`, but there is a difference between these
notations, which is discussed [here](#properties). 

The type `A_i` can depend on variables `p_1`, ... `p_{i-1}`.
The type `B_i` can depend on variables `p_1`, ... `p_n`, `f_1`, ... `f_{i-1}`.

`f_1`, ... `f_k`, `p_1`, ... `p_n` are _fields_ of `R`. Note that records do not have parameters
and `p_1`, ... `p_n` are also fields of `R`.
The only difference between `p_i` and `f_j` is that `f_j` are visible in the scope of `R` and `p_i` are not:

```arend
\func test1 => f_1
\func test2 => R.f_1
\func test3 => R.p_1

-- p_1 is not in scope in the following function:
-- \func test4 => p_1
```

The record `R` is equivalent to the sigma type `\Sigma (p_1 : A_1) ... (p_n : A_n) (f_1 : B_1) ... (f_k : B_k)`.
To create an instance of this type you can use one of the options listed
below, which are equivalent.
See [this section](/language-reference/expressions/class-ext) for more information about these constructions.

```arend
\func r1 => \new R a_1 ... a_n { | f_1 => b_1 ... | f_k => b_k }
\func r2 => \new R { | p_1 => a_1 ... | p_n => a_n | f_1 => b_1 ... | f_k => b_k }
\func r3 => \new R a_1 ... a_n b_1 ... b_k
\func r4 => \new R a_1 ... a_i { | p_{i+1} => a_{i+1} ... | p_n => a_n | f_1 => b_1 ... | f_k => b_k }
\func r5 => \new R a_1 ... a_n b_1 ... b_i { f_{i+1} => b_{i+1} ... | f_k => b_k }
```

You can also define the same function using [copattern matching](/language-reference/definitions/functions/#copattern-matching):
```arend
\func r6 : R \cowith
  | p_1 => a_1
  ...
  | p_n => a_n
  | f_1 => b_1
  ...
  | f_k => b_k
```

Fields are projections:
```arend
\func test5 (x : R) => R.p_1 {x}
\func test6 (x : R) => f_1 {x}
```

You can also use the following syntax:
```arend
\func test5' (x : R) => x.p_1
\func test6' (x : R) => x.f_1
```

This syntax is allowed only when the expression before `.` is a variable with an explicitly specified type which is a record.
If `x : R` and `f` is a field of `R`, then `x.f` is equivalent to `R.f {x}`.

Records satisfy the eta rule.
This means that the expression `\new R r.p_1 ... r.p_n r.f_1 ... r.f_k` is equivalent to `r`.

## Properties

Some fields can be marked as a _property_.
To do this, you need to use the keyword `\property` instead of `\field`:

```arend
\record NegativeInt {
  \field x : Int
  \property isNeg : x < 0
}
```

The type of a property must be a proposition.
The field defined as `\field f : A` is not marked as a property and a field defined as `| f : A` is marked as a property if `A` is a proposition.
When you define a field as `\property f : A`, the typechecker produces an error message if `A` is not a proposition.

Properties do not evaluate.
Thus, they are related to fields in the same way as [lemmas](/language-reference/definitions/functions/#lemmas) are related to functions.
For example, consider the following function:

```arend
\func test (x : Int) (p : x < 0) => isNeg {\new NegativeInt x p}
```

Then `test x p` evaluates to `p` if `isNeg` is not a property and does not evaluate if it is.

## Extensions

An extension `S` of a record `R` is another record which adds some fields to `R` and implements some of the fields of `R`.
The record `R` is called a _super class_ of `S` and `S` is called a _subclass_ of `R`.

```arend
\record S \extends R {
  | g_1 : C_1
  ...
  | g_m : C_m
  | p_{i_1} => a_{i_1}
  ...
  | p_{i_q} => a_{i_q}
  | f_{j_1} => b_{j_1}
  ...
  | f_{j_s} => b_{j_s}
}
```

Here `d_i` has type `A_i` and `f_j` has type `B_j`.
Expressions `a_i` and `b_i` may refer to any field of `S`, but implementations must not form a cycle.

The type `S` is a subtype of `R`.
That is, every expression of type `S` also has type `R`.

A record is equivalent to the sigma type consisting of all of its not implemented fields.
For example, consider the following records:

```arend
\record C (x y : Nat) {
  | x<=y : x <= y
  | y<=0 : y <= 0
}

\record D \extends C {
  | y => x
  | x<=y => <=-reflexive x
}
```

Then `D` is equivalent to `\Sigma (x : Nat) (x <= 0)`:

```arend
\func fromD (d : D) : \Sigma (x : Nat) (x <= 0) => (d.x, d.y<=0)
\func toD (p : \Sigma (x : Nat) (x <= 0)) => \new D p.1 p.2
\func fromToD (d : D) : toD (fromD d) = d => idp
\func toFromD (p : \Sigma (x : Nat) (x <= 0)) : fromD (toD p) = p => idp
```

where `idp` is the proof by reflexivity.
This works since both records and sigma types satisfy eta rules.

A record can extend several records.
If these records extends some base record themselves, then the fields of this base record will not be repeated in the final record.
For example, consider the following records:

```arend
\record A (x : Nat)
\record B \extends A
\record C \extends A
\record D \extends B,C
```

Then `D` has a single field `x`.
If super classes have fields with the same name which are not defined in some common super class, then the final record will have two different fields with the same name.
To access these fields, you need to use fully qualified names:

```arend
\record B (x : Nat)
\record C (x : Nat)
\record D \extends B,C

\func fromD (d : D) : \Sigma Nat Nat => (B.x {d}, C.x {d})
\func toD (p : \Sigma Nat Nat) => \new D p.1 p.2
\func fromToD (d : D) : toD (fromD d) = d => idp
\func toFromD (p : \Sigma Nat Nat) : fromD (toD p) = p => idp
```

## This

Every field has additional implicit parameter.
You can refer to it with the keyword `\this`:

```arend
\record R (X : \Type) (t : X -> X)

\func f (r : R) => r.t

\record S \extends R {
  | x : X
  | p : f \this x = x
}
```

This keyword can appear only in arguments of definitions and these arguments also must satisfy this condition.
