# Guía técnica para la defensa

## 1. Arquitectura

```text
Usuario → Swing → PuenteProlog → proceso SWI-Prolog
                                     ↓
                           normalizar y validar entrada
                                     ↓
                           s/3 → reglas simbólicas
                                     ↓
                         dominio del intervalo completo
                                     ↓
                          F(a), F(b), F(b) − F(a)
                                     ↓
                     área aproximada de |f| y muestras
                                     ↓
                   respuesta TSV → Java → vista y gráfica
```

La base de conocimiento y el cálculo viven exclusivamente en Prolog. Swing solo captura datos, transporta cadenas, interpreta campos numéricos y dibuja puntos calculados por Prolog. Se ejecuta una consulta en un proceso independiente, sin servidor, puerto ni dependencia JPL. `SwingWorker` evita bloquear el hilo de interfaz durante el cálculo.

## 2. Qué lo hace un sistema experto

- **Conocimiento:** reglas de integración y tabla `primitiva/4`.
- **Inferencia:** unificación de patrones, condiciones de aplicabilidad, recursión y selección de reglas en `regla/4`.
- **Explicación:** lista de reglas aplicadas disponible en **Ver procedimiento**. No es una demostración algebraica paso a paso completa.
- **Validación:** vocabulario de entrada cerrado, dominios y límites de recursos.

Los cortes seleccionan la regla aplicable; las sumas/restas resuelven cada subobjetivo de manera recursiva. Un producto de dos funciones no constantes no activa la regla de factor constante.

## 3. Conexión con el material docente

### Representar no es evaluar — clase 11

`F = x^3/3` construye un término. No intenta asignar un valor numérico al átomo `x`. `Valor is Expresion` solo se utiliza cuando ya se dispone de un punto y la expresión es evaluable.

### Casos base y casos recursivos — clase 12

`s/3` mantiene la firma de la clase. Internamente `integrar/4` devuelve también la explicación. Constante y variable son casos base; suma, resta y factor constante descomponen el problema.

La regla de la potencia requiere separar **n = -1**, pues dividir por `n+1` daría cero en el denominador. Ese caso devuelve `ln(abs(x))`. Para `a^(m*x+c)` se divide por `m*ln(a)`, no basta sustituir x por u sin corregir la pendiente.

### Evaluación posterior — clase 13

```prolog
s(X, X^2, F),
evaluar(X, F, 5, FB),
evaluar(X, F, 3, FA),
Area is FB-FA.
```

Se copia el par `X-F`, preservando la relación entre la variable y la fórmula en la copia. Se liga solo la copia al punto y se aplica `is/2`. Se puede repetir sin `asserta`, `abolish` ni modificar el conocimiento global. También se admite la notación con átomo: `s(x,x^2,F)`.

Usar `==` en las comprobaciones de identidad evita ligar accidentalmente la variable libre al reconocer casos o simplificar. La simplificación es local y no elimina las comprobaciones de dominio de la expresión original.

## 4. Integral definida y área

Para `f(x)=x` entre -1 y 1:

- Antiderivada: `x^2/2 + C`.
- Integral definida: `F(1)-F(-1) = 0`.
- Área geométrica: `∫|x|dx = 1`.

No usar `abs(F(b)-F(a))` como área general: en este ejemplo también daría cero. El área geométrica se aproxima en Prolog mediante Simpson adaptativo. Su tolerancia es un objetivo del estimador, no una prueba formal de exactitud. La integral definida se obtiene de la primitiva simbólica y evaluación numérica en los extremos.

Invertir los límites cambia el signo de la integral definida, pero no el área. Si son iguales, ambas son cero siempre que el punto pertenezca al dominio.

## 5. Seguridad y errores

El proceso Java ejecuta un comando fijo, sin shell y sin insertar expresiones en el argumento `-g`. La expresión y límites viajan por entrada estándar. Un lexer DCG Prolog adapta `2x`, `2(x+1)` y los símbolos de calculadora a operadores explícitos, sin confundir nombres de función, notación científica o signos de resta. Después se verifica que exista exactamente una expresión, se rechazan variables proporcionadas por el usuario y se normalizan solo operadores y funciones permitidas.

Nunca se invoca la entrada mediante `call/1`. El `call/1` de `exigir/2` recibe únicamente comprobaciones construidas por el programa. Se rechazan directivas, objetivos como `halt(0)` y términos posteriores.

Se revisan intervalos de argumentos afines para detectar ceros de denominadores y polos de tangente **interiores**. Por ejemplo, `1/x` en [-1,1] se rechaza aunque evaluar `log(abs(x))` en ambos extremos produciría cero. Los cálculos grandes o sin convergencia muestran un error en lugar de un resultado inventado.

## 6. Demostración sugerida

1. `x^2`, de 3 a 5: reproducir la clase 13 (98/3).
2. `3*x^2+sin(x)`, de 0 a `pi`: enseñar suma y casos base.
3. `2^(3*x+1)`, de 0 a 1: enseñar factor de pendiente.
4. `x`, de -1 a 1: contrastar integral cero y área uno.
5. `x^2`, de 2 a 0: orientación negativa.
6. `arcsinh(x)`, de 0 a 2: demostrar función inversa hiperbólica.
7. `1/x`, de -1 a 1: rechazo de singularidad.
8. `sin(x^2)`: explicar honestamente el alcance de las reglas.
9. Ejecutar `sh scripts/probar.sh` como evidencia reproducible.

## 7. Antes de entregar

- Completar nombres/carnés de los cuatro integrantes sin datos ficticios.
- Acordar con el docente si exige JPL o alguna forma particular de comunicación: el enunciado no lo especifica; aquí se usa un proceso local.
- Revisar con el docente la interpretación de los incisos generales u(x), diferenciando cobertura de familias y un integrador universal.
- Probar la interfaz en el equipo de la defensa, con Java y SWI-Prolog disponibles y acceso de Flatpak al proyecto si corresponde.
- Preparar informe y diapositivas según el formato que solicite el docente.
- Empaquetar fuentes, pruebas, documentación y referencias permitidas; `build/` es regenerable.
