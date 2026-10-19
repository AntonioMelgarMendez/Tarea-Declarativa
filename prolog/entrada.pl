:- module(entrada, [preparar_entrada/2]).
:- use_module(library(dcg/basics), [blanks//0, number//1]).

% Adaptacion de notacion de calculadora, NO evaluacion matematica.
% Tokenizar antes de insertar * preserva decimales, exponentes y nombres de funcion.
preparar_entrada(Texto, Preparado) :-
    string_codes(Texto, Codigos), phrase(tokens(Tokens), Codigos),
    Tokens \= [], \+ append(_, [n(_),n(_)|_], Tokens),
    multiplicaciones(Tokens, Explicitos),
    representar(Explicitos, Fragmentos), atomics_to_string(Fragmentos, "", Preparado).

tokens(Ts) --> blanks, tokens_resto(Ts).
tokens_resto([T|Ts]) --> token(T), !, blanks, tokens_resto(Ts).
tokens_resto([]) --> [].
token(n(N)) --> inicio_numero, number(N), !.
token(id(pi)) --> "π", !.
token(id(A)) --> [C], {code_type(C, csymf)}, nombre(Cs), {atom_codes(A,[C|Cs])}, !.
token(op(A)) --> [C], {memberchk(C, `+-*/^()`), char_code(A,C)}, !.
token(op('*')) --> [C], {memberchk(C,[0'×,0'·])}, !.
token(op('/')) --> "÷", !.
token(op('-')) --> "−", !.

inicio_numero(S,S) :- S = [C|R],
    (code_type(C,digit) ; C =:= 0'., R = [D|_], code_type(D,digit)).
nombre([C|Cs]) --> [C], {code_type(C,csym)}, !, nombre(Cs).
nombre([]) --> [].

multiplicaciones([], []).
multiplicaciones([T], [T]).
multiplicaciones([A,B|Ts], [A,op('*')|Rs]) :-
    termina(A), empieza(B), \+ (A=n(_), B=n(_)), !,
    multiplicaciones([B|Ts], Rs).
multiplicaciones([A,B|Ts], [A|Rs]) :- multiplicaciones([B|Ts],Rs).
termina(n(_)).
termina(id(A)) :- memberchk(A,[x,pi,e]).
termina(op(')')).
empieza(n(_)).
empieza(id(_)).
empieza(op('(')).

representar([], []).
representar([A], [S]) :- texto_token(A,S).
representar([A,B|Ts], [S,Sep|Rs]) :- texto_token(A,S),
    (A=id(_), B=op('(') -> Sep="" ; Sep=" "), representar([B|Ts],Rs).
texto_token(n(N),S) :- number_string(N,S).
texto_token(id(A),A).
texto_token(op(A),A).
