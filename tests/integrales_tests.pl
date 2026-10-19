:- use_module('../prolog/integrales').
:- use_module(library(plunit)).

:- begin_tests(integrales).

cerca(A,B) :- assertion(abs(A-B) < 1.0e-6).
resuelve(E,A,B,D,Area) :- analizar(E,A,B,R), cerca(R.definida,D), cerca(R.area,Area).

% Los 22 incisos: e) y j) no representan integracion universal.
caso(a, '7', 0.4).
caso(b, 'x', 0.4).
caso(c, '3*x', 0.4).
caso(d, 'x^5', 0.4).
caso(e, 'sin(2*x+1)', 0.4).
caso(f, '4*cos(x)', 0.4).
caso(g, 'x^2+sin(x)', 0.4).
caso(h, 'x^3-cos(x)', 0.4).
caso(i, '2^x', 0.4).
caso(j, '2^(3*x+1)', 0.4).
caso(k, 'sin(x)', 0.4).
caso(l, 'cos(x)', 0.4).
caso(m, 'tan(x)', 0.4).
caso(n, 'asin(x)', 0.4).
caso(o, 'acos(x)', 0.4).
caso(p, 'atan(x)', 0.4).
caso(q, 'sh(x)', 0.4).
caso(r, 'cosh(x)', 0.4).
caso(s, 'tanh(x)', 0.4).
caso(t, 'arcsinh(x)', 0.4).
caso(u, 'arccosh(x)', 1.4).
caso(v, 'arctanh(x)', 0.4).

test(derivada_de_primitiva, [forall(caso(_,Texto,P))]) :-
    leer_expresion(Texto,V,E), integrar(V,E,F,_),
    H = 1.0e-5, A is P-H, B is P+H,
    evaluar(V,F,A,FA), evaluar(V,F,B,FB), evaluar(V,E,P,Esperado),
    D is (FB-FA)/(2*H), cerca(D,Esperado), assertion(var(V)).

test(clase_13_variable_libre) :-
    s(X,X^2,R), assertion(var(X)),
    evaluar(X,R,5,Superior), evaluar(X,R,3,Inferior),
    Area is Superior-Inferior, cerca(Area,98/3), assertion(var(X)).
test(clase_12_atomo) :- s(x,x^2,R), evaluar(x,R,3,Y), cerca(Y,9).
test(no_ligar_al_simplificar) :- s(X, X+0, F), assertion(var(X)), evaluar(X,F,2,Y), cerca(Y,2).
test(no_ligar_potencia_ajena, [fail]) :- s(X,X^X,_).
test(no_aceptar_variable_ajena, [fail]) :- s(_,Y+1,_), nonvar(Y).
test(constante) :- resuelve('5',0,2,10,10).
test(cero) :- resuelve('0',-3,3,0,0).
test(lineal) :- resuelve('3*x',0,2,6,6).
test(polinomio) :- resuelve('3*x^2+2*x-1',0,2,10,280/27).
test(cambio_signo) :- resuelve('x',-1,1,0,1).
test(funcion_negativa) :- resuelve('-2',0,3,-6,6).
test(limites_invertidos) :- resuelve('x',1,-1,0,1).
test(orientacion) :- resuelve('x^2',2,0,-8/3,8/3).
test(limites_iguales) :- resuelve('sin(x)',1,1,0,0).
test(potencia_menos_uno) :- E is exp(1), resuelve('x^(-1)',1,E,1,1).
test(reciproco_negativo) :- resuelve('1/x',-2,-1,-log(2),log(2)).
test(reciproco_afin) :- resuelve('3/(2*x+1)',0,1,1.5*log(3),1.5*log(3)).
test(potencia_negativa) :- resuelve('x^(-2)',1,2,0.5,0.5).
test(potencia_afin) :- resuelve('(2*x+1)^2',0,1,13/3,13/3).
test(exponencial) :- resuelve('2^(3*x+1)',0,1,14/(3*log(2)),14/(3*log(2))).
test(base_uno) :- resuelve('1^x',-1,1,2,2).
test(exponencial_natural) :- resuelve('exp(x)',0,1,exp(1)-1,exp(1)-1).
test(logaritmo) :- resuelve('ln(x)',1,2,2*log(2)-1,2*log(2)-1).
test(seno_completo) :- P is 2*pi, resuelve('sin(x)',0,P,0,4).
test(seno_oscilante) :- P is 2*pi, resuelve('sin(100*x)',0,P,0,4).
test(arcoseno_extremos) :- resuelve('asin(x)',-1,1,0,pi-2).
test(arcocoseno_extremos) :- resuelve('acos(x)',-1,1,pi,pi).
test(acosh_extremo) :- resuelve('acosh(x)',1,2,2*acosh(2)-sqrt(3),2*acosh(2)-sqrt(3)).
test(composicion_pendiente_negativa) :- resuelve('sin(-2*x+1)',0,1,0,1-cos(1)).
test(factor_derecha) :- resuelve('sin(x)*3',0,1,3*(1-cos(1)),3*(1-cos(1))).
test(cociente_constante) :- resuelve('x/2',0,2,1,1).
test(limite_pi) :- leer_limite('pi/2',N), cerca(N,pi/2).
test(limite_e) :- leer_limite('e',N), cerca(N,exp(1)).
test(puntos) :- analizar('x^2',0,1,R), length(R.puntos,241).

test(singularidad_interior, [throws(error(integral(_),_))]) :- analizar('x^(-1)',-1,1,_).
test(singularidad_extremo, [throws(error(integral(_),_))]) :- analizar('1/x',0,1,_).
test(polo_tangente, [throws(error(integral(_),_))]) :- analizar('tan(x)',0,4,_).
test(polo_tangente_afin, [throws(error(integral(_),_))]) :- analizar('tan(2*x+1)',0,1,_).
test(polo_tangente_extremo, [throws(error(integral(_),_))]) :- P is pi/2, analizar('tan(x)',0,P,_).
test(asin_fuera, [throws(error(integral(_),_))]) :- analizar('asin(x)',0,2,_).
test(atanh_extremo, [throws(error(integral(_),_))]) :- analizar('atanh(x)',0,1,_).
test(acosh_fuera, [throws(error(integral(_),_))]) :- analizar('acosh(x)',0,2,_).
test(log_fuera, [throws(error(integral(_),_))]) :- analizar('log(x)',-1,2,_).
test(producto_general, [throws(error(integral(_),_))]) :- analizar('x*sin(x)',0,1,_).
test(compuesta_no_afin, [throws(error(integral(_),_))]) :- analizar('sin(x^2)',0,1,_).
test(base_negativa, [throws(error(integral(_),_))]) :- analizar('(-2)^x',0,1,_).
test(division_cero, [throws(error(integral(_),_))]) :- analizar('x/0',0,1,_).
test(inyeccion, [throws(error(integral(_),_))]) :- analizar('halt(0)',0,1,_).
test(directiva, [throws(error(integral(_),_))]) :- analizar('assertz(pwned)',0,1,_).
test(variable_prolog, [throws(error(integral(_),_))]) :- analizar('X^2',0,1,_).
test(variable_anonima, [throws(error(integral(_),_))]) :- analizar('_',0,1,_).
test(variable_anonima_anidada, [throws(error(integral(_),_))]) :- analizar('sin(_)',0,1,_).
test(atomo_ajeno, [throws(error(integral(_),_))]) :- analizar('y',0,1,_).
test(limite_variable, [throws(error(integral(_),_))]) :- leer_limite('x',_).
test(limite_grande, [throws(error(integral(_),_))]) :- leer_limite('1000001',_).
test(oscilacion_excesiva, [throws(error(integral(_),_))]) :- analizar('sin(100000*x)',0,1,_).
test(vacio, [throws(error(integral(_),_))]) :- analizar('',0,1,_).
test(terminos_extra, [throws(error(integral(_),_))]) :- analizar('x. halt.',0,1,_).
test(exponente_excesivo, [throws(error(integral(_),_))]) :- analizar('x^1000000000',0,1,_).
test(exponente_constante_excesivo, [throws(error(integral(_),_))]) :- analizar('10^1000000000',0,1,_).

:- end_tests(integrales).
