package com.fei.infoaula
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SalonesAdapter(
    private val mContext: Context,
    private val listaSalones: ArrayList<Salon>
) : ArrayAdapter<Salon>(mContext, 0, listaSalones) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        if (itemView == null) {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.item_salon, parent, false)
        }

        val salon = listaSalones[position]

        val txtNumero = itemView!!.findViewById<TextView>(R.id.txtNumero)
        val txtEstado = itemView.findViewById<TextView>(R.id.txtEstado)
        val txtHora = itemView.findViewById<TextView>(R.id.txtHora)

        // Mostrar datos
        txtNumero.text = salon.nombre ?: salon.id_salon
        txtEstado.text = salon.estado
        txtHora.text = salon.ultima_actualizacion
        //Cambios de color con estado
        when (salon.estado?.lowercase()) {
            "libre" -> txtEstado.setTextColor(Color.parseColor("#2E7D32"))   // Verde
            "ocupado" -> txtEstado.setTextColor(Color.parseColor("#C62828")) // Rojo
            "apartado", "reservado" -> txtEstado.setTextColor(Color.parseColor("#F9A825")) // Amarillo
            else -> txtEstado.setTextColor(Color.BLACK) // Por defecto
        }
        return itemView
    }
}
