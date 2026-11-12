package com.example.tp_listecontact

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

/** Adaptateur personnalisé : transforme chaque Contact en une ligne (item_contact.xml) de la ListView. */
class ContactAdapter(
    private val context: Context,
    private val contacts: List<Contact>
) : BaseAdapter() {

    override fun getCount(): Int = contacts.size

    override fun getItem(position: Int): Contact = contacts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // On réutilise la ligne si possible (plus rapide), sinon on la crée
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)

        val contact = contacts[position]
        val imgPhoto = view.findViewById<ImageView>(R.id.imgPhoto)
        val txtName = view.findViewById<TextView>(R.id.txtName)
        val txtPhone = view.findViewById<TextView>(R.id.txtPhone)

        txtName.text = "${contact.nom} ${contact.prenom}"
        txtPhone.text = contact.telephone
        PhotoUtils.showPhoto(imgPhoto, contact.photoPath)

        return view
    }
}
