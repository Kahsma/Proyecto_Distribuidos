package com.javeriana;

import java.io.Serializable;

public class MeasurementData implements Serializable {
    private String sensorType;
    private double measurement;
    private String timestamp;

    // Constructor vacío (requerido para Jackson)
    public MeasurementData() {
    }

    // Constructor con parámetros
    public MeasurementData(String sensorType, double measurement, String timestamp) {
        this.sensorType = sensorType;
        this.measurement = measurement;
        this.timestamp = timestamp;
    }

    // Getters y setters
    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public double getMeasurement() {
        return measurement;
    }

    public void setMeasurement(double measurement) {
        this.measurement = measurement;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
