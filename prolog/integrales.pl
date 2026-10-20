:- module(integrales, [s/3, integrar/4, evaluar/4, analizar/4,
                       leer_expresion/3, leer_limite/2]).

:- use_module(library(lists)).
:- use_module(entrada).

% Clases 11 y 12: construir una expresión, NO evaluarla todavía.
% También funciona s(X, X^2, R), con X libre, como en la clase 13.
s(Var, Expresion, Resultado) :- integrar(Var, Expresion, Resultado, _).

integrar(V, E, F, Pasos) :-
    (var(V); atom(V)), valida(V, E),
    regla(V, E, Bruta, Pasos), simplificar(Bruta, Simple),
    fracciones(Simple, F), !.

regla(V, E, 1/2*V^2, ['Variable: integral de x = x^2/2.']) :- E == V, !.
regla(V, K, K*V, ['Constante: integral de k = k*x.']) :- number(K), !.
regla(V, U+W, A+B, ['Linealidad: integrar cada sumando.'|P]) :- !,
    regla(V, U, A, P1), regla(V, W, B, P2), append(P1, P2, P).
regla(V, U-W, A-B, ['Linealidad: integrar cada termino de la resta.'|P]) :- !,
    regla(V, U, A, P1), regla(V, W, B, P2), append(P1, P2, P).
regla(V, -U, -A, ['Extraer signo negativo.'|P]) :- !, regla(V, U, A, P).
regla(V, K*U, K*A, ['Extraer factor constante.'|P]) :- number(K), !,
    regla(V, U, A, P).
regla(V, U*K, K*A, ['Extraer factor constante.'|P]) :- number(K), !,
    regla(V, U, A, P).
regla(V, U/K, A/K, ['Dividir por una constante no nula.'|P]) :-
    number(K), K =\= 0, !, regla(V, U, A, P).
regla(V, U^N, F, ['Potencia con argumento afin (caso -1: logaritmo).' ]) :-
    integer(N), afin(V, U, M, _), M =\= 0, !,
    (N =:= -1 -> F = log(abs(U))/M ; J is N+1, F = U^J/(M*J)).
regla(V, K/U, K*log(abs(U))/M, ['Reciproco de argumento afin.']) :-
    number(K), afin(V, U, M, _), M =\= 0, !.
regla(V, A^U, A^U/(M*log(A)), ['Exponencial a^u: dividir por pendiente y ln(a).']) :-
    number(A), A > 0, A =\= 1, afin(V, U, M, _), M =\= 0, !.
regla(V, A^_, V, ['Base uno: la funcion es constante uno.']) :- number(A), A =:= 1, !.
regla(V, E, F/M, [Descripcion]) :-
    compound(E), E =.. [Nombre, U], afin(V, U, M, _), M =\= 0,
    primitiva(Nombre, U, F, Descripcion).

% Tabla de conocimiento. U puede ser x o a*x+b (a distinto de cero).
primitiva(sin, U, -cos(U), 'Seno: integral de sin(u) = -cos(u)/u\'.').
primitiva(cos, U, sin(U), 'Coseno: integral de cos(u) = sin(u)/u\'.').
primitiva(tan, U, -log(abs(cos(U))), 'Tangente: -ln(abs(cos(u)))/u\'.').
primitiva(asin, U, U*asin(U)+sqrt(1-U^2), 'Arcoseno: integracion por partes.').
primitiva(acos, U, U*acos(U)-sqrt(1-U^2), 'Arcocoseno: integracion por partes.').
primitiva(atan, U, U*atan(U)-log(1+U^2)/2, 'Arcotangente: integracion por partes.').
primitiva(sinh, U, cosh(U), 'Seno hiperbolico: cosh(u)/u\'.').
primitiva(cosh, U, sinh(U), 'Coseno hiperbolico: sinh(u)/u\'.').
primitiva(tanh, U, log(cosh(U)), 'Tangente hiperbolica: ln(cosh(u))/u\'.').
primitiva(asinh, U, U*asinh(U)-sqrt(U^2+1), 'Arcoseno hiperbolico: por partes.').
primitiva(acosh, U, U*acosh(U)-sqrt(U^2-1), 'Arcocoseno hiperbolico: por partes.').
primitiva(atanh, U, U*atanh(U)+log(1-U^2)/2, 'Arcotangente hiperbolica: por partes.').
primitiva(exp, U, exp(U), 'Exponencial natural: exp(u)/u\'.').
primitiva(log, U, U*log(U)-U, 'Logaritmo natural: (u*ln(u)-u)/u\'.').

afin(V, E, 1, 0) :- E == V, !.
afin(_, K, 0, K) :- number(K), !.
afin(V, U+W, A, B) :- !, afin(V,U,C,D), afin(V,W,F,G), A is C+F, B is D+G.
afin(V, U-W, A, B) :- !, afin(V,U,C,D), afin(V,W,F,G), A is C-F, B is D-G.
afin(V, -U, A, B) :- !, afin(V,U,C,D), A is -C, B is -D.
afin(V, K*U, A, B) :- number(K), !, afin(V,U,C,D), A is K*C, B is K*D.
afin(V, U*K, A, B) :- number(K), !, afin(V,U,C,D), A is K*C, B is K*D.
afin(V, U/K, A, B) :- number(K), K =\= 0, !,
    afin(V,U,C,D), aritmetica(C/K,A), aritmetica(D/K,B).

% Clase 13: copia independiente para evaluar sucesivamente sin ligar el original.
evaluar(V, E, Punto, Valor) :-
    copy_term(V-E, X-Copia),
    (var(X) -> X = Punto, Valor is Copia
    ; sustituir(X, Copia, Punto, Sust), Valor is Sust), finito(Valor).

sustituir(V, E, P, P) :- E == V, !.
sustituir(_, E, _, E) :- atomic(E), !.
sustituir(V, E, P, R) :- E =.. [F|Args],
    maplist(reemplazar(V,P), Args, Nuevos), R =.. [F|Nuevos].
reemplazar(V, P, E, R) :- sustituir(V, E, P, R).

finito(N) :- (rational(N) -> true ; float(N), float_class(N, C), memberchk(C,[zero,subnormal,normal])).

% No introducir flotantes al dividir dos constantes racionales.
aritmetica(A/B, R) :- rational(A), rational(B), !, R is A rdiv B.
aritmetica(E, R) :- R is E.

% Entrada: solo datos matemáticos permitidos, nunca call/1 sobre texto del usuario.
leer_expresion(Texto, V, E) :-
    string_length(Texto, L), exigir(L > 0, 'Escriba una expresion.'),
    exigir(L =< 2000, 'Expresion demasiado larga (maximo 2000 caracteres).'),
    exigir(preparar_entrada(Texto, Preparado), 'Revise la expresion. Ejemplo: 2x + sin(x).'),
    % Un centinela comprueba que no queden terminos tras el primero.
    string_concat(Preparado, "\n . '$fin_integrales'.", Completo),
    setup_call_cleanup(open_string(Completo, Flujo),
        (read_term(Flujo,T,[variable_names(Vars),syntax_errors(error)]),
         read_term(Flujo,Fin,[syntax_errors(error)]),
         exigir(Fin == '$fin_integrales', 'Introduzca una sola expresion, sin punto final.'),
         read_term(Flujo,EOF,[]), exigir(EOF == end_of_file, 'Sobran terminos en la entrada.')),
        close(Flujo)),
    exigir((Vars == [], ground(T)), 'Use x minuscula; no se permiten variables Prolog.'),
    exigir(acyclic_term(T), 'Expresion ciclica no permitida.'),
    exigir(normalizar(T, V, E, 0), 'Sintaxis no admitida. Use x, *, ^ y funciones de la ayuda.').

leer_limite(Texto, N) :- leer_expresion(Texto, V, E),
    exigir(ground(E), 'Los limites deben ser constantes, no funciones de x.'),
    evaluar(V, E, 0, N), exigir(abs(N) =< 1.0e6, 'Limites fuera del rango permitido (+/- 1000000).').

normalizar(x, V, V, _) :- !.
normalizar(pi, _, N, _) :- !, N is pi.
normalizar(e, _, N, _) :- !, N is exp(1).
normalizar(N, _, R, _) :- number(N), finito(N), !, abs(N) =< 1.0e12,
    (float(N) -> R is rationalize(N) ; R = N).
normalizar(T, V, E, D) :-
    D < 60, compound(T), T =.. [F|Args], alias(F, G),
    length(Args, A), permitida(G, A), D1 is D+1,
    maplist(normalizar_arg(V,D1), Args, Nuevos), R =.. [G|Nuevos],
    potencia_acotada(R),
    (ground(R) -> aritmetica(R,E), finito(E), abs(E) =< 1.0e12 ; E = R).
normalizar_arg(V,D,T,E) :- normalizar(T,V,E,D).
potencia_acotada(T) :- (T = _^N, number(N) -> abs(N) =< 1000 ; true).

alias(sh, sinh) :- !.
alias(arcsinh, asinh) :- !.
alias(arccosh, acosh) :- !.
alias(arctanh, atanh) :- !.
alias(ln, log) :- !.
alias(F, F).
permitida(F, 2) :- memberchk(F, [+, -, *, /, ^]).
permitida(F, 1) :- memberchk(F, [-, sin, cos, tan, asin, acos, atan,
                                sinh, cosh, tanh, asinh, acosh, atanh, exp, log]).
valida(V, E) :- E == V, !.
valida(_, E) :- number(E), !.
valida(V, E) :- nonvar(E), compound(E), E =.. [F|Args],
    length(Args,A), permitida(F,A), maplist(valida(V),Args).

exigir(Objetivo, Mensaje) :- (call(Objetivo) -> true ; throw(error(integral(Mensaje), _))).

% Simplificaciones locales: no es un CAS ni intenta equivalencias generales.
simplificar(E, E) :- var(E), !.
simplificar(E, E) :- atomic(E), !.
simplificar(E, R) :- E =.. [F|Args], maplist(simplificar,Args,N),
    T =.. [F|N], reducir(T,R).
% Usar == evita ligar por accidente la X libre al simplificar.
reducir(A+X, X) :- A == 0, !.
reducir(X+A, X) :- A == 0, !.
reducir(X-A, X) :- A == 0, !.
reducir(A-X, -X) :- A == 0, !.
reducir(A*X, X) :- A == 1, !.
reducir(X*A, X) :- A == 1, !.
reducir(A*_, 0) :- A == 0, !.
reducir(_*A, 0) :- A == 0, !.
reducir(X/A, X) :- A == 1, !.
reducir(X^A, X) :- A == 1, !.
reducir(_^A, 1) :- A == 0, !.
reducir(-A, X) :- nonvar(A), A = -X, !.
reducir(A*(B*X), R) :- number(A), number(B), !,
    K is A*B, factor(K,X,R).
reducir(A*(X/B), R) :- number(A), number(B), B =\= 0, !,
    aritmetica(A/B,K), factor(K,X,R).
reducir((A*X)/B, R) :- number(A), number(B), B =\= 0, !,
    aritmetica(A/B,K), factor(K,X,R).
reducir((X/A)/B, X/K) :- number(A), number(B), A =\= 0, B =\= 0, !,
    K is A*B.
reducir(T, R) :- ground(T), T =.. [F|A], memberchk(F,[+,-,*,/]), maplist(number,A), !,
    aritmetica(T,R).
reducir(T, T).
factor(K,X,X) :- K =:= 1, !.
factor(K,_,0) :- K =:= 0, !.
factor(K,X,K*X).

% 3r2 es una representacion interna: en la formula visible se usa 3/2.
% No volver a simplificar despues de esta conversion ni ligar la variable libre.
fracciones(E, E) :- var(E), !.
fracciones(E, N/D) :- rational(E), \+ integer(E), !,
    N is numerator(E), D is denominator(E).
fracciones(E, E) :- atomic(E), !.
fracciones(E, R) :- E =.. [F|Args], maplist(fracciones,Args,Nuevos), R =.. [F|Nuevos].

% Servicio matematico: rechaza intervalos no continuos, incluidas singularidades interiores.
analizar(Texto, A, B, Resultado) :-
    leer_expresion(Texto, V, E),
    exigir((number(A),number(B),finito(A),finito(B)), 'Limites no numericos.'),
    exigir(integrar(V,E,F,Pasos), 'No hay una regla para esta expresion. Consulte la cobertura.'),
    Li is min(A,B), Ls is max(A,B),
    exigir(dominio(V,E,Li,Ls), 'La funcion no es continua/real en todo el intervalo; no se admiten integrales impropias.'),
    evaluar(V,F,A,FA), evaluar(V,F,B,FB), Definida is FB-FA, finito(Definida),
    area(V,E,Li,Ls,Area),
    sustituir(V,F,x,Simbolica), term_string(Simbolica, Primitiva, [quoted(false)]),
    muestras(V,E,Li,Ls,Puntos),
    Resultado = resultado{primitiva:Primitiva, inferior:FA, superior:FB,
                          definida:Definida, area:Area, pasos:Pasos, puntos:Puntos}.

dominio(V,E,_,_) :- E == V, !.
dominio(_,K,_,_) :- number(K), !.
dominio(V,E,A,B) :- E =.. [Op,U,W], memberchk(Op,[+,-,*]), !,
    dominio(V,U,A,B), dominio(V,W,A,B).
dominio(V,U/K,A,B) :- number(K), K =\= 0, !, dominio(V,U,A,B).
dominio(V,K/U,A,B) :- number(K), !, rango_afin(V,U,A,B,L,H), (L > 0 ; H < 0).
dominio(V,U^N,A,B) :- integer(N), !, rango_afin(V,U,A,B,L,H),
    (N >= 0 -> true ; (L > 0 ; H < 0)).
dominio(V,K^U,A,B) :- number(K), K > 0, !, dominio(V,U,A,B).
dominio(V,-U,A,B) :- !, dominio(V,U,A,B).
dominio(V,E,A,B) :- E =.. [F,U], rango_afin(V,U,A,B,L,H), dominio_funcion(F,L,H).

rango_afin(V,U,A,B,L,H) :- afin(V,U,M,C), P is M*A+C, Q is M*B+C,
    L is min(P,Q), H is max(P,Q).
dominio_funcion(F,_,_) :- memberchk(F,[sin,cos,atan,sinh,cosh,tanh,asinh,exp]), !.
dominio_funcion(F,L,H) :- memberchk(F,[asin,acos]), !, L >= -1, H =< 1.
dominio_funcion(acosh,L,_) :- L >= 1.
dominio_funcion(atanh,L,H) :- L > -1, H < 1.
dominio_funcion(log,L,_) :- L > 0.
dominio_funcion(tan,L,H) :-
    % Existe un polo si pi/2+k*pi cae en [L,H]. Margen contra redondeo.
    K is ceiling((L-pi/2)/pi-1.0e-12), Polo is pi/2+K*pi, Polo > H+1.0e-12.

% Area geometrica = integral de |f|. Simpson adaptativo con tolerancia absoluta
% repartida por subintervalo y tope de profundidad; nunca devolver exito sin converger.
area(_,_,A,B,0.0) :- A =:= B, !.
area(V,E,A,B,Area) :-
    frecuencia(V,E,Frecuencia), N is max(32,ceiling((B-A)*Frecuencia*8/pi)),
    exigir(N =< 4096, 'Intervalo demasiado oscilatorio; reduzca los limites.'),
    H is (B-A)/N, Tol is 1.0e-7/N,
    findall(S, (between(1,N,I), L is A+(I-1)*H, R is A+I*H,
                simpson(V,E,L,R,Tol,S)), Valores),
    length(Valores,N), sum_list(Valores,Area), finito(Area).

frecuencia(V,E,0) :- E == V, !.
frecuencia(_,E,0) :- number(E), !.
frecuencia(V,E,F) :- E =.. [Op,U], memberchk(Op,[sin,cos,tan]), !,
    afin(V,U,M,_), F is abs(M).
frecuencia(V,E,F) :- E =.. [_|Args], maplist(frecuencia(V),Args,Fs), max_list(Fs,F).
valor_abs(V,E,X,Y) :- evaluar(V,E,X,F), Y is abs(F).
simpson(V,E,A,B,Tol,S) :- M is (A+B)/2,
    valor_abs(V,E,A,FA), valor_abs(V,E,M,FM), valor_abs(V,E,B,FB),
    S0 is (B-A)*(FA+4*FM+FB)/6,
    adaptar(V,E,A,B,FA,FM,FB,S0,Tol,32,S).
adaptar(V,E,A,B,FA,FM,FB,S0,Tol,D,S) :-
    M is (A+B)/2, L is (A+M)/2, R is (M+B)/2,
    valor_abs(V,E,L,FL), valor_abs(V,E,R,FR),
    SL is (M-A)*(FA+4*FL+FM)/6, SR is (B-M)*(FM+4*FR+FB)/6,
    Delta is SL+SR-S0,
    (abs(Delta) =< 15*Tol -> S is SL+SR+Delta/15
    ; exigir(D > 0, 'El area numerica no convergio; reduzca el intervalo.'),
      D1 is D-1, T is Tol/2,
      adaptar(V,E,A,M,FA,FL,FM,SL,T,D1,S1),
      adaptar(V,E,M,B,FM,FR,FB,SR,T,D1,S2), S is S1+S2).

% Solo para dibujar: 241 puntos; no intervienen en el calculo de la integral.
muestras(V,E,A,B,Puntos) :- findall(X-Y,
    (between(0,240,I), X is A+(B-A)*I/240, evaluar(V,E,X,Y)), Puntos).
