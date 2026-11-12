package com.example.tp_listecontact

import java.io.Serializable

/**
 * Un contact. Serializable pour pouvoir le passer dans un Intent (putExtra).
 * photoPath = chemin du fichier photo dans le stockage de l'application, ou null = avatar par défaut.
 */
data class Contact(
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String,
    val photoPath: String? = null
) : Serializable
