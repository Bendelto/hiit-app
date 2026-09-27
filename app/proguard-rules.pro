# Reglas de R8 para la app HIIT.
#
# La app está escrita 100% en Kotlin + Compose y no usa reflexión,
# serialización ni bibliotecas que requieran keep-rules propias.
# Kotlin añade sus reglas por defecto vía las librerías de stdlib;
# estas líneas cubren la anotación de metadatos de Kotlin por si acaso.

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# TextToSpeech se resuelve por nombre vía Intent del sistema; R8 no lo
# toca porque no se referencia la clase directamente, pero lo dejamos
# explícito para que un refactor futuro no lo elimine por reflexión.
-keep class android.speech.tts.** { *; }
