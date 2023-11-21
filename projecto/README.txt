Requerimientos:
- Java 19 como minimo
- Ultima version de maven

Commandos

//para todos se toca que cambiar las direcciones dentro del codigo. revisar comentarios.

//Sensor. Ejecutar en la misma maquina
mvn exec:java -Dexec.mainClass="com.javeriana.Sensor" -Dexec.args="-t <tipo_sensor> -i <intervalo_ms> -c <config_file>"

//Monitor
mvn exec:java -Dexec.mainClass="com.javeriana.Monitor" -Dexec.args="-t <tipo_de_sensor_a_suscribir>"

//Broker Se ejecuta en la misma maquina que los sensores
mvn exec:java -Dexec.mainClass="com.javeriana.Broker"

//Health Checker se recomienda ejecutar con algun monitor
mvn exec:java -Dexec.mainClass="com.javeriana.HealthChecker"

//Sistema De Calidad
mvn exec:java -Dexec.mainClass="com.javeriana.SistemaDeCalidad"




