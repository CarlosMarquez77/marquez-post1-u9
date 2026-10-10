package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.domain.Cita;
import org.springframework.stereotype.Component;

@Component
public class EmailReminderChannel implements ReminderChannel {

    @Override
    public void enviar(Cita cita, String mensaje) {
        // En producción: cliente SMTP real. Aquí se simula el envío por consola.
        System.out.printf("[EMAIL a %s] %s%n", cita.getPacienteContacto(), mensaje);
    }
}
