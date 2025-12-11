ParadeRMI  
Alumno: Justino Bernal  
Legajo: 190118

Descripción  
Parade es un juego de cartas para varios jugadores basado en un desfile (el “carnaval”) donde cada jugador intenta terminar con la menor cantidad de puntos posible.

El mazo está formado por 66 cartas: 6 colores distintos, con valores del 0 al 10.  
Cada jugador tiene una mano de cartas y, en el centro de la mesa, se forma una fila de cartas llamada “carnaval”.

El proyecto implementa este juego en forma distribuida:  
existe un servidor que mantiene el estado de la partida (mazo, jugadores, carnaval, puntajes) y varios clientes que se conectan al servidor mediante RMI. Cada cliente puede usar una vista gráfica (Swing) o una vista por consola, pero ambos hablan con el mismo modelo remoto.

Fases del juego

1) Unión de jugadores y preparación  
Antes de empezar a jugar, los clientes se conectan al servidor y cada jugador se “une” a la partida indicando su nombre.  
El servidor va registrando a los jugadores y, cuando se decide iniciar la partida, realiza:

- Creación y mezcla del mazo de 66 cartas.  
- Reparto de cartas iniciales a cada jugador.  
- Colocación de las cartas iniciales en la mesa para formar el carnaval.  
- Inicialización del turno (quién empieza).

En esta fase no se realizan jugadas todavía: solo se prepara el estado inicial del juego.

2) Desarrollo de la partida (turnos y jugadas)  
La partida se desarrolla por turnos, siguiendo el orden en que los jugadores se unieron al juego.  
En su turno, cada jugador realiza exactamente una acción principal:

- Elegir una carta de su mano.  
- Jugar esa carta al final del carnaval (la fila de cartas en la mesa).

Al jugar una carta, se aplican las reglas de Parade:

- Se mira cuántas cartas había en el carnaval antes de jugar (prev).  
- Sea v el valor de la carta jugada.  
  - Si prev ≤ v → no se retira ninguna carta.  
  - Si prev > v → se miran solo las primeras (prev − v) cartas del carnaval.  
    - De esas, se retira toda carta que tenga el mismo color que la carta jugada o valor menor o igual que v.  
- Las cartas retiradas pasan a la pila de cartas recolectadas del jugador que hizo la jugada.  
- Luego, si todavía no estamos en la última ronda y quedan cartas en el mazo, el jugador roba una carta para reponer su mano.  
- Finalmente, el turno pasa al siguiente jugador.

Durante esta fase, el servidor lleva el control del turno actual, del contenido del mazo, del carnaval y de las cartas recolectadas por cada jugador.  
Cuando ocurre un cambio (por ejemplo, se juega una carta), el modelo notifica a los clientes mediante eventos, y cada cliente actualiza su vista (gráfica o consola) para mostrar el nuevo estado de la partida.

3) Última ronda y fin de partida  
En el proyecto se considera especialmente el caso de 2 jugadores.

La “última ronda” se dispara cuando:

- algún jugador logra recolectar al menos una carta de cada uno de los 6 colores, o  
- se queda sin cartas el mazo.

A partir de ese momento, ya no se roba más del mazo y se juegan las últimas cartas que quedan en las manos.

La partida termina cuando se cumple:

- hay última ronda activa, y  
- todos los jugadores tienen exactamente 4 cartas en la mano (regla particular implementada para la versión de 2 jugadores).

En ese momento, el servidor calcula los puntajes finales de cada jugador y los registra en una tabla de clasificación (ranking histórico).

Objetivo  
El objetivo del juego es terminar la partida con el menor puntaje posible.

El puntaje de cada jugador se calcula a partir de sus cartas recolectadas:

- Se suman todos los valores de las cartas recolectadas.  
- Para cada color, se otorgan puntos adicionales al jugador que tiene mayoría en ese color (con una regla especial para 2 jugadores, donde la diferencia debe ser de al menos 2 cartas para que cuente como mayoría).  
- El resultado final es:  
  puntos totales = suma de valores de las cartas recolectadas + bonificación por mayorías de color.

El jugador con menor puntaje final es el ganador de la partida.

Implementación (resumen técnico breve)  
El proyecto utiliza una arquitectura MVC distribuida:

- Modelo: contiene la lógica del juego y el estado de la partida (`ModeloParade`, `EstadoPartida`, `Jugador`, `Carta`, etc.).  
- Vistas: hay dos implementaciones intercambiables, una gráfica (Swing) y otra por consola, que muestran la mano, el carnaval, el turno y los puntajes.  
- Controlador: se ejecuta del lado del cliente, recibe las acciones del usuario (unirse, iniciar partida, jugar carta) y las traduce en llamadas remotas al modelo mediante RMI.

Además, se emplea un patrón Observer remoto:  
el modelo notifica a los controladores cuando ocurre un evento (como una carta jugada, inicio de partida, llegada de la última ronda o final de partida), y el controlador actualiza la vista para mantener sincronizado lo que ve cada jugador.

Diagrama de clases UML  

El diagrama de clases UML incluye:

- Las clases principales del modelo del juego (ModeloParade, EstadoPartida, Jugador, Carta, TablaClasificacion, EntradaRanking).  
- La interfaz remota IParade, utilizada por los clientes para comunicarse con el servidor.  
- El controlador (ControladorParade), que implementa la interfaz de controlador remoto y actúa como puente entre las vistas y el modelo.  
- Las vistas (VistaGrafica y VistaConsola), que implementan una interfaz común IVistaParade.

A continuación se muestra la imagen del UML:

![Diagrama UML](uml.png)
