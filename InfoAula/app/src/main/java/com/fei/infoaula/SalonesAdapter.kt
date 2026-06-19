package com.fei.infoaula

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.firebase.database.core.Context
import android.widget.TextView

class SalonesAdapter (
    context: Context,
    private val salones: List<Salon>
) : ArrayAdapter<Salon>(context,0, salones) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val salon = salones[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_salon,parent,false)
        //atributos
        val numero = view.findViewById<TextView>(R.id.txtNumero)
        val estado = view.findViewById<TextView>(R.id.txtEstado)
        val hora = view.findViewById<TextView>(R.id.txtHora)
        //Mostrar salon y obtener estado
        numero.text = "Salón ${salon.numero}"
        estado.text = salon.estado.name
        //Mostrar los diferentes estados
        when (salon.estado){
            EstadoSalon.DISPONIBLE -> hora.text = "Disponible hasta ${salon.horaDisponible}"
            EstadoSalon.OCUPADO -> hora.text = "Ocupado hasta ${salon.horaOcupado}"
            EstadoSalon.RESERVADO -> hora.text = "Reservado hasta ${salon.horaReserva}"
        }
        return view
    }
}
