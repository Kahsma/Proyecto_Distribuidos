import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
    vus: 30, // Número de usuarios virtuales (sensores)
    duration: '30s', // Duración de la prueba
};

export default function () {
    // Configurar el mensaje que enviará el sensor al broker
    let sensorMessage = {
        sensorType: 'temperatura', // Puedes ajustar esto según los tipos de sensores que tengas
        measurement: Math.random() * 20 + 68, // Generar un valor de temperatura aleatorio
        timestamp: new Date().toISOString(),
    };

    // Enviar el mensaje al broker
    let response = http.post('http://localhost:5559', JSON.stringify(sensorMessage), {
        headers: { 'Content-Type': 'application/json' },
    });

    // Verificar que la solicitud fue exitosa (código de respuesta 200)
    if (response.status === 200) {
        console.log('Sensor sent data successfully');
    } else {
        console.error('Failed to send sensor data. Status code: ' + response.status);
    }

    // Esperar un tiempo antes de enviar el próximo mensaje (simula el intervalo entre mediciones)
    sleep(1);
}
