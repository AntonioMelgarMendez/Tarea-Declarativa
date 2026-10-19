:- use_module(integrales).
:- use_module(library(readutil)).
:- initialization(main, main).

% Protocolo UTF-8: tres lineas de entrada (expresion, a, b).
% Respuesta TSV: OK, primitiva, F(a), F(b), integral, area, pasos y puntos.
% El comando de Prolog es fijo; la entrada nunca se interpola en un objetivo.
main :-
    set_prolog_flag(encoding, utf8),
    catch((call_with_time_limit(12, resolver) -> true
          ; throw(error(integral('No fue posible completar el calculo.'),_))),
          Error, informar_error(Error)).

resolver :-
    read_line_to_string(user_input, Expresion),
    read_line_to_string(user_input, Inferior),
    read_line_to_string(user_input, Superior),
    maplist(string, [Expresion,Inferior,Superior]),
    leer_limite(Inferior,A), leer_limite(Superior,B), analizar(Expresion,A,B,R),
    format('OK~nPRIMITIVA\t~s~nFA\t~16g~nFB\t~16g~nINTEGRAL\t~16g~nAREA\t~16g~n',
           [R.primitiva,R.inferior,R.superior,R.definida,R.area]),
    forall(member(P,R.pasos), format('PASO\t~w~n',[P])),
    forall(member(X-Y,R.puntos), format('PUNTO\t~16g\t~16g~n',[X,Y])).

informar_error(error(integral(M),_)) :- !, mensaje(M).
informar_error(time_limit_exceeded) :- !, mensaje('Calculo demasiado costoso: tiempo limite de 12 segundos.').
informar_error(error(syntax_error(_),_)) :- !, mensaje('Sintaxis incorrecta. Ejemplo: 3*x^2 + sin(x).').
informar_error(error(evaluation_error(_),_)) :- !, mensaje('Operacion numerica fuera de dominio, division por cero o desbordamiento.').
informar_error(_) :- mensaje('Expresion no valida o fuera del rango numerico admitido.').
mensaje(M) :- split_string(M, "\n\r\t", "", Partes), atomics_to_string(Partes," ",Limpio),
    format('ERROR\t~s~n',[Limpio]).
