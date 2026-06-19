package com.fei.infoaula

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.firebase.database.core.Context

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
        return super.getView(position, convertView, parent)
    }
}
