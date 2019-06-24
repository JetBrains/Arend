<h1 id="prelude">Prelude<a class="headerlink" href="#prelude" title="Permanent link">&para;</a></h1>

Prelude is a built-in module, containing several definitions, which behave differently from ordinary definitions.
It is always imported implicitly in every module.
You can also import it explicitly to hide or rename some of its definitions.

We will discuss these definitions in this module.
You can look at the file `Prelude.ard`, which contains these definitions, but note that they only roughly correspond
to actual definitions since most of them cannot be actually defined in the ordinary syntax.

# Nat and Int

The definitions of `Nat`, `Int`, `Nat.+`, `Nat.*`, `Nat.-`, and `Int.fromNat` are actually correct,
they could have been as well defined in an ordinary file and successfully typechecked according to normal
Arend typechecking rules.
The only difference is that these definitions from Prelude are implemented more efficiently.

# Interval and squeeze functions

The definition of the interval type `\data I | left | right` looks like the definition of the set with two
elements, but this is not true actually. 
One way to think about this data type is that it has one more constructor, which connects `left` and 
`right` and which cannot be accessed explicitly. This means that it is forbidden to define a function 
on `I` by pattern matching. Functions from `I` can be defined by means of several auxiliary functions:
`coe`, `coe2`, `squeeze`, `squeezeR`. Function `coe` plays the role of eliminator for `I`, it is discussed
further in this module. 

Functions `squeeze` and `squeezeR` satisfy the following computational conditions:
```arend
squeeze left j => left
squeeze right j => j
squeeze i left => left
squeeze i right => i

squeezeR left j => j
squeezeR right j => right
squeezeR i left => i
squeezeR i right => right
```

Such functions can be defined in terms of the function `coe`,
but for efficiency purposes they are defined as primitives in Arend.

# Path

The definition of `Path A a a'` is not correct.
By the definition, it should consist of all functions `\Pi (i : I) -> A i`, but actually it consists of all such
functions `f` that also satisfy computational conditions `f left => a` and `f right => a'`.
This means that while typechecking the expression `path f` the typechecker also checks that these computational
conditions hold and, if they don't, produces an error message.
For example, if you write `\func test : 1 = 0 => path (\lam _ => 0)`, you will see the following error message:

```bash
[ERROR] test.ard:1:23: The left path endpoint mismatch
  Expected: 1
    Actual: 0
  In: path (\lam _ => 0)
  While processing: test
```

The homotopy level of the universe, which is the type of `Path`, is also computed differently. If -1 ≤ n and
`A` is in a universe of (n+1)-types, then `Path A a a'` is in a universe of n-types. Otherwise, it has the same
homotopy level as `A`.

Prelude also contains an infix form of `Path` called `=` which is actually a correctly defined function.
The definition of `@` is also correct, but the typechecker has an eta rule for this definition: 
`path (\lam i => p @ i) = p`.
This rule does not apply to functions `@` defined in other files.

Finally, function `Path.inProp` is not correct since it does not have a body.
It postulates the proof irrelevance for types in `\Prop`, namely that any two elements of a type in `\Prop` are equal.

# coe and coe2

Function `coe` is an eliminator for the interval type.
For every type over the interval, it allows to transport elements from the fiber over `left` to the fiber over an
arbitrary point.
It can be used to prove that `I` is contractible and that `=` satisfies the rules for
ordinary identity types.
The definition of `coe` is not correct since it uses pattern matching on the interval.
This function satisfies one additional reduction rule: `coe (\lam x => A) a i => a` if `x` is not free in `A`.

Function `coe2` is a generalization of `coe`, which allows to transport elements between any two fibers of a type
over the interval.

# iso

The definition of `iso` is not correct since it uses pattern matching on the interval.
This definition implies the univalence axiom.
