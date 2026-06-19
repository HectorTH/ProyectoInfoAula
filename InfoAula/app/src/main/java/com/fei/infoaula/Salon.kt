package com.fei.infoaula
//Definicion de la clase Salon
data class Salon(
    val numero: Int,
    var estado: EstadoSalon,
    var horaDisponible: String? = null,
    var horaOcupado: String? = null,
    var horaReserva: String? = null
)
//Estados del salon
enum class EstadoSalon{
    DISPONIBLE,
    OCUPADO,
    RESERVADO
}
