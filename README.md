# Recordatorios Citas Service

## Descripción
Repositorio del post-contenido de la Unidad 9 de Patrones de Diseño
de Software — Sexto Semestre. Un único proyecto Spring Boot que
envía recordatorios de citas por el canal preferido del paciente
(correo o SMS), construido desde el inicio con Strategy
(ReminderChannel) y Factory Method (ReminderChannelFactory)
inyectados por constructor.

## Parte 1 — Pruebas Unitarias con JUnit 5 y Mockito
CitaReminderServiceImplTest aísla CitaReminderServiceImpl de sus
tres colaboradores (CitaRepository, ReminderChannelFactory y Clock)
sin levantar el contexto de Spring. Ver "Decisiones de diseño" para
la justificación de por qué cada uno se mockeó, se stubeó o se
resolvió con un fake.

## Decisiones de diseño

### Mock vs. Stub vs. Fake (Parte 1)

| Colaborador | Test Double | Qué necesita la prueba de él |
|-------------|-------------|------------------------------|
| CitaRepository | Mock | Controlar la lista que devuelve y verificar `save()` |
| ReminderChannelFactory / ReminderChannel | Mock | Verificar qué canal se pidió y que recibió el mensaje |
| Clock | Fake (`Clock.fixed`) | Una hora fija y real para que la ventana de 24 horas sea exacta |

**CitaRepository → Mock.** La prueba necesita dos cosas del
repositorio: que `findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse`
devuelva la lista de citas que yo decida, y comprobar que después se
llamó `save(cita)` una sola vez con la cita ya marcada como enviada.
Lo primero lo resuelve un Stub, pero lo segundo no: si el servicio
dejara de guardar la cita, un Stub no lo notaría y el paciente
recibiría el mismo recordatorio en cada ejecución. Como hay que
controlar la respuesta y además verificar la interacción, es un Mock.
*Por qué no un Fake:* un repositorio en memoria serviría si la prueba
guardara y luego consultara, pero aquí basta una respuesta fija por
escenario y habría que mantener una clase extra.

**ReminderChannelFactory y ReminderChannel → Mock.** Lo que se prueba
es la selección de la estrategia: que para una cita con canal EMAIL el
servicio pida `obtenerCanal(EMAIL)` y que el canal devuelto reciba
`enviar(cita, mensaje)`. `enviar` es `void`, así que no hay un valor
de retorno que revisar; la única forma de comprobarlo es con
`verify(reminderChannelMock, times(1)).enviar(eq(cita), anyString())`.
*Por qué no un Stub:* un Stub de la Factory devolvería el canal, pero
no permitiría comprobar que el canal se usó, ni el caso contrario de
la tercera prueba (`verifyNoInteractions(reminderChannelFactory)`
cuando no hay citas pendientes).

**Clock → Fake, no Mock.** `Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"))`
es una implementación real de `java.time.Clock` que trae el JDK, solo
que con el instante congelado. Con ella, `LocalDateTime.now(clock)`
funciona igual que en producción y la prueba
`enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos`
puede afirmar los límites exactos de la consulta (10 de agosto 09:00 a
11 de agosto 09:00). *Por qué no un Mock:* habría que stubear
`instant()` y `getZone()` por separado, y si faltara uno,
`LocalDateTime.now(clock)` fallaría con un error que no tiene que ver
con el servicio. Además no interesa verificar cuántas veces se leyó el
reloj, solo que la hora sea conocida.

### Diagnóstico antes del refactor (Parte 2)
`enviarRecordatorioUrgente(Long citaId)` funciona al ejecutarlo, pero
no se puede probar de forma aislada. La línea responsable es la 52 de
`CitaReminderServiceImpl.java`:

```java
ReminderChannel canalUrgente = new SmsReminderChannel(new TwilioNotificadorSmsGateway());
```

El canal y el gateway se construyen con `new` dentro del método, así
que la prueba no tiene por dónde entregarle al servicio un canal
distinto. El escenario que más interesa probar, "el gateway de SMS
falla y la cita no debe quedar marcada como enviada", es imposible de
escribir: no hay forma de hacer que `TwilioNotificadorSmsGateway`
lance una excepción desde la prueba, y cada ejecución usaría el
gateway real.

La diferencia con `enviarRecordatoriosPendientes()` está en la línea
39: ahí el canal sale de `reminderChannelFactory.obtenerCanal(...)`, y
la Factory llegó por el constructor (línea 22). Por eso en la Parte 1
bastó pasar un `@Mock ReminderChannelFactory` para decidir qué canal
recibe el servicio. El método urgente viola DIP porque el servicio
depende de dos clases concretas en vez de la abstracción
`ReminderChannel`. `Mockito.mockConstruction()` podría interceptar ese
`new`, pero sería arreglar con la herramienta de pruebas un problema
que es del diseño.

## Ejecutar las pruebas
```bash
mvn test                  # Pruebas unitarias
```
