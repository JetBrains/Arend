<h1 id="records">Records<a class="headerlink" href="#records" title="Permanent link">&para;</a></h1>

A _record_ is a [Sigma type](/language-reference/expressions/sigma) with named projections.
The basic syntax looks like this:

```arend
\record R (p_1 : A_1) ... (p_n : A_n) {
  | f_1 : B_1
  ...
  | f_k : B_k
}
```

where `f_1`, ... `f_k`, `p_1`, ... `p_n` are _fields_ of `R`. Note that records do not have parameters
and `p_1`, ... `p_n` are also fields of `R`.
The only difference between `p_i` and `f_j` is that `f_j` are visible in the scope of `R` and `p_i` are not:

```arend
\func test1 => f_1
\func test2 => R.f_1
\func test3 => R.p_1

-- p_1 is not in scope in the following function:
-- \func test4 => p_1
```

It is also possible to write `\field f_i : B_i` instead of `| f_i : B_i`, but there is a difference between these
notations, which is discussed [here](#properties).

Fields can be accessed using projection functions:
```arend
\func test5 (x : R) => R.p_1 {x}
\func test6 (x : R) => f_1 {x}
```

An alternative way to access fields is provided by the following syntax:
```arend
\func test5' (x : R) => x.p_1
\func test6' (x : R) => x.f_1
```

This syntax is allowed only when the expression before `.` is a variable with an explicitly specified type which is a record.
If `x : R` and `f` is a field of `R`, then `x.f` is equivalent to `R.f {x}`. 

The type `A_i` can depend on variables `p_1`, ... `p_{i-1}`.
The type `B_i` can depend on variables `p_1`, ... `p_n`, `f_1`, ... `f_{i-1}`.

Records are essentially Sigma types. For example, the record `R` above is equivalent to the Sigma type
`\Sigma (p_1 : A_1) ... (p_n : A_n) (f_1 : B_1) ... (f_k : B_k)`.

Instances of type `R` can be created using _new expression_. Any of the variants of the syntax listed below can be used,
they are all equivalent. 
See [this section](/language-reference/expressions/class-ext) for more information about new expressions and related
constructions.

```arend
\func r1 => \new R a_1 ... a_n { | f_1 => b_1 ... | f_k => b_k }
\func r2 => \new R { | p_1 => a_1 ... | p_n => a_n | f_1 => b_1 ... | f_k => b_k }
\func r3 => \new R a_1 ... a_n b_1 ... b_k
\func r4 => \new R a_1 ... a_i { | p_{i+1} => a_{i+1} ... | p_n => a_n | f_1 => b_1 ... | f_k => b_k }
\func r5 => \new R a_1 ... a_n b_1 ... b_i { f_{i+1} => b_{i+1} ... | f_k => b_k }
```

The same function can also be defined using [copattern matching](/language-reference/definitions/functions/#copattern-matching):
```arend
\func r6 : R \cowith
  | p_1 => a_1
  ...
  | p_n => a_n
  | f_1 => b_1
  ...
  | f_k => b_k
```

Records satisfy the eta rule.
This means that the expression `\new R r.p_1 ... r.p_n r.f_1 ... r.f_k` is equivalent to `r`.

## Properties

Some fields can be marked as a _property_.
This is done by using the keyword `\property` instead of `\field`:

```arend
\record NegativeInt {
  \field x : Int
  \property isNeg : x < 0
}
```

The type of a property must be a proposition, otherwise the definition does not typecheck.

If `A` is a proposition, then `| f : A` is also marked as a property. In this case, `f` can be defined as a
normal field, which is not a property, by writing `\field f : A`. 

Properties do not evaluate.
Thus, they are related to fields in the same way as [lemmas](/language-reference/definitions/functions/#lemmas) are related to functions.
For example, consider the following function:

```arend
\func test (x : Int) (p : x < 0) => isNeg {\new NegativeInt x p}
```

Then `test x p` evaluates to `p` if `isNeg` is not a property and does not evaluate if it is.

## Extensions

An extension `S` of a record `R` is another record which adds some fields to `R` and implements some of the fields of `R`.
The record `R` is called a _super class_ of `S` and `S` is called a _subclass_ of `R`. If `R` is the definition of
a record from the beginning of this page, then an extension `S` of `R` can be defined as follows:

```arend
\record S (r_1 : D_1) ... (r_t : D_t) \extends R {
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

Here expressions `a_i` and `b_j` have types `A_i` and `B_j` respectively.
Expressions `a_i` and `b_j` may refer to any field of `S`, but implementations must not form a cycle.

The type `S` is a subtype of `R`. That is, every expression of type `S` is also of type `R`.

A record is equivalent to the Sigma type, consisting of all of its unimplemented fields.
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
This works since both records and Sigma types satisfy eta rules.

A record can extend several records.
If these records extend some base record themselves, then the fields of this base record will not be repeated in the final record.
For example, consider the following records:

```arend
\record A (x : Nat)
\record B \extends A
\record C \extends A
\record D \extends B,C
```

Then `D` has a single field `x`.
If super classes have fields with the same name which are not defined in some common super class, then the final record
will have several different fields with the same name.
In order to access these fields, fully qualified names should be used:

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

Every field of a record `R` has additional implicit parameter of type `R`, which can be referred to with the keyword `\this`:

```arend
\record R (X : \Type) (t : X -> X)

\func f (r : R) => r.t

\record S \extends R {
  | x : X
  | p : f \this x = x
}
```

The keyword `\this` can appear only in arguments of definitions and only in those arguments, which in turn satisfy this condition.
