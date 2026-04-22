# 🏁 Proyecto Damas (Checkers) - Android Jetpack Compose

Este proyecto es una aplicación de Android completa para jugar a las **Damas**, diseñada como un caso de estudio práctico que aplica los conocimientos de los **Temas 2 y 3** y las **MiniActividades 2 a 5** del curso de Programación de Dispositivos Móviles.

---

## 🚀 Análisis Detallado del Proyecto

El desarrollo se ha dividido en módulos claros para asegurar que el código sea legible, mantenible y, sobre todo, que cumpla con los estándares académicos de **Montserrat Sendín**.

### 1. Arquitectura MVVM y Gestión de Estado (Tema 3)
La aplicación utiliza el patrón **Model-View-ViewModel**, que es el estándar recomendado para Android moderno.

*   **El Modelo (`CheckersModel.kt`)**: Contiene las clases de datos (`data class`) y enumeraciones. Es una capa "tonta" que solo define qué es una pieza, un color o una casilla. Esto asegura una **alta cohesión**.
*   **El ViewModel (`CheckersViewModel.kt`)**: 
    *   **Encapsulamiento**: El estado del tablero se define con `mutableStateOf` pero se expone con un `private set`. Esto significa que la Vista puede leer el estado pero **solo el ViewModel puede modificarlo**, evitando errores de sincronización.
    *   **Ciclo de Vida**: Al heredar de `ViewModel`, los datos del juego (como la posición de las piezas y el tiempo transcurrido) no se pierden cuando el usuario gira el móvil.
    *   **Lógica de Negocio**: Aquí reside la validación de movimientos (diagonales, saltos de captura, conversión a reinas).

### 2. Interfaz de Usuario y Composición (MiniActividades 2 y 3)
La UI se ha construido usando **Jetpack Compose**, eliminando por completo el uso de layouts XML tradicionales para las vistas.

*   **State Hoisting (Elevación de Estado)**: 
    *   Hemos separado los componentes en **Stateful** (ej. `GameScreen`), que conoce el ViewModel, y **Stateless** (ej. `CheckersBoard`), que solo recibe parámetros.
    *   **Ventaja**: Esto hace que el código sea mucho más fácil de testear y reutilizar, ya que los componentes visuales no dependen de la lógica interna.
*   **Componentes de Diseño**:
    *   Uso de `Scaffold`, `Column`, `Row` y `Box` para estructurar la pantalla de forma adaptativa.
    *   Implementación de `Canvas` para dibujar las piezas de forma eficiente, permitiendo diferenciar visualmente entre piezas normales y **Reinas** (mediante un círculo interior).

### 3. Sistema de Recursos y Tematización (Tema 2)
Siguiendo las reglas académicas, el proyecto tiene **CERO "Hardcoding"**.

*   **Localización (`strings.xml`)**: Todos los mensajes (turnos, ganadores, botones) están centralizados. Esto permitiría traducir la app a otros idiomas sin cambiar una sola línea de código Kotlin.
*   **Estilo Visual (`colors.xml` y `dimens.xml`)**: 
    *   Los colores del tablero (claro/oscuro) y de las piezas (rojo/negro) están definidos en recursos.
    *   Las dimensiones (márgenes, tamaños de piezas, grosores de bordes) usan `dp` y están en `dimens.xml`.
    *   **Uso en código**: Se accede a ellos mediante `stringResource(R.string...)`, `colorResource(R.color...)` y `dimensionResource(R.dimen...)`.

### 4. Navegación e Interactividad (Tema 3 / MiniActividades 4 y 5)
La navegación entre pantallas se gestiona mediante el sistema de **Intents**, tal como se ha practicado en las MiniActividades.

*   **Navegación Explícita**: Se usan `Intents` para lanzar `GameActivity` o `HelpActivity` desde el menú principal (`MenuActivity`). Cada actividad tiene su propio `setContent` para mantener la modularidad.
*   **Interactividad**:
    *   El usuario selecciona una pieza (se resalta con un borde) y luego toca el destino. El ViewModel valida si el movimiento es legal antes de actualizar el estado.
    *   Uso de Corrutinas (`viewModelScope.launch`) para el cronómetro, asegurando que el conteo de tiempo se realice en un hilo secundario y no bloquee la interfaz de usuario.

---

## 🛠️ Desglose de Temas Implementados

| Tema / Actividad | Concepto Aplicado | Archivo de Ejemplo |
| :--- | :--- | :--- |
| **Tema 2** | Gestión de Recursos Externos | `res/values/strings.xml` |
| **Tema 2** | Definición de Colores y Temas | `ui/theme/Color.kt` |
| **Tema 3** | Ciclo de Vida del ViewModel | `CheckersViewModel.kt` |
| **Tema 3** | Estados Mutables (`mutableStateOf`) | `CheckersViewModel.kt` |
| **MiniActv 2** | Composición Básica (Column/Row) | `MenuActivity.kt` |
| **MiniActv 3** | Elevación de Estado (Hoisting) | `GameActivity.kt` |
| **MiniActv 4** | Navegación con Intents | `MenuActivity.kt` |
| **MiniActv 5** | Interacción Compleja y Eventos | `CheckersBoard` en `GameActivity` |

---

## 🏁 Conclusión

Este proyecto no es solo un juego, sino una demostración técnica de cómo estructurar una aplicación Android moderna. La combinación de **MVVM**, **Compose** y una **gestión estricta de recursos** garantiza un software de alta calidad, robusto ante errores y fácil de extender en el futuro (por ejemplo, añadiendo un modo multijugador online o niveles de dificultad).

---
*Desarrollado con ❤️ para la asignatura de Programación de Dispositivos Móviles.*
