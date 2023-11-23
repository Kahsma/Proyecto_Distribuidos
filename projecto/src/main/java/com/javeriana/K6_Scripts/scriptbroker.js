import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 10, // Número de usuarios virtuales (simulando 10 sensores)
    duration: '30s', // Duración de la prueba
};

export default function () {
    // Configurar el mensaje que enviará el broker al monitor
    let sensorMessage = {
        sensorType: 'temperatura',
        measurement: Math.random() * 20 + 68,
        timestamp: new Date().toISOString(),
    };

    // Enviar el mensaje al monitor
    let response = http.post('http://localhost:5555', JSON.stringify(sensorMessage), {
        headers: { 'Content-Type': 'application/json' },
    });

    // Verificar que la solicitud fue exitosa (código de respuesta 200)
    check(response, {
        'status is 200': (r) => r.status === 200,
    });

    // Esperar un tiempo antes de enviar el próximo mensaje (simula el intervalo entre mediciones)
    sleep(1);
}
//mvn exec:java -Dexec.mainClass="com.javeriana.Monitor" -Dexec.args="-t temperatura"
