Arend Tutorial
===============

Here is the data type for booleans in Arend:

~~~~.arend
\import Data.Unit
\import Data.Empty

\data Bool | true | false

\function
True (b : Bool) : \Prop
    | true => Unit
    | false => Empty

\function
not (b : Bool) : Bool
    | true => false
    | false => true

\function
if {A : \Type} (b : Bool) (then else : A) : A => \elim b
    | true => then
    | false => else
~~~~

Here is some fine algebra for you reading pleasure:


~~~~arend
\import Data.Bool

\class Semigroup {
  | X : \Type0
  | op : X -> X -> X
  | assoc : \Pi (x y z : X) -> op (op x y) z = op x (op y z)
}

\function
xor-semigroup => \new Semigroup { X => Bool | op => xor | assoc => {?} }

\function
and-semigroup => \new Semigroup { X => Bool | op => and | assoc => {?} }

\class Monoid {
  | S : Semigroup
  | id : S.X
  | lunit : \Pi (x : S.X) -> S.op id x = x
  | runit : \Pi (x : S.X) -> S.op x id = x
}

\function
xor-monoid => \new Monoid {
  | S => xor-semigroup
  | id => false
  | lunit => {?}
  | runit => {?}
}

\function
and-monoid => \new Monoid {
  | S => and-semigroup
  | id => true
  | lunit => {?}
  | runit => {?}
}

\class Group {
  | M : Monoid
  | inv : M.S.X -> M.S.X
  | linv : \Pi (x : M.S.X) -> M.S.op (inv x) x = M.id
  | rinv : \Pi (x : M.S.X) -> M.S.op x (inv x) = M.id
}

\function
xor-group => \new Group {
  | M => xor-monoid
  | inv => {?}
  | linv => {?}
  | rinv => {?}
}

\class AbelianGroup {
  | G : Group
  | comm : \Pi (x y : G.M.S.X) -> G.M.S.op x y = G.M.S.op y x
}

\function
xor-abelian => \new AbelianGroup {
  | G => xor-group
  | comm => {?}
}

{-
\class Ring {
  | A : AbelianGroup
  | M : Monoid { S : Semigroup { X => A.G.M.S.X } }
  | ldistr : \Pi (x y z : A.G.M.S.X) -> M.S.op x (A.G.M.S.op y z) = A.G.M.S.op (M.S.op x y) (M.S.op x z)
  | rdistr : \Pi (x y z : A.G.M.S.X) -> M.S.op (A.G.M.S.op y z) x = A.G.M.S.op (M.S.op y x) (M.S.op z x)
}

\function
xor-ring => \new Ring {
  | A => xor-abelian
  | M => and-monoid
  | distr => {?}
}

\function
mul-zero (R : Ring) (x : R.A.G.M.S.X) : M.S.op x R.A.G.M.id = R.A.G.M.id
    => {?}
-}
~~~~
