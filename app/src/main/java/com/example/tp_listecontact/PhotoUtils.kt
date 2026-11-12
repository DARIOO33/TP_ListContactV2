package com.example.tp_listecontact

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.ImageView
import java.io.File
import java.io.IOException

/** Petites fonctions pour gérer les photos des contacts. */
object PhotoUtils {

    /** Affiche la photo du contact, ou l'avatar par défaut s'il n'y a pas de photo. */
    fun showPhoto(imageView: ImageView, photoPath: String?) {
        val bitmap = if (photoPath != null) loadBitmap(photoPath, 400) else null
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.setImageResource(R.drawable.ic_avatar)
        }
    }

    /** Crée un nouveau fichier vide (nom unique) dans le dossier "photos" de l'application. */
    fun newPhotoFile(context: Context): File {
        val dir = File(context.filesDir, "photos")
        dir.mkdirs()
        return File(dir, "photo_${System.currentTimeMillis()}.jpg")
    }

    /**
     * Copie l'image choisie dans la galerie vers le stockage de l'application.
     * Ainsi la photo reste lisible même après la fermeture de DetailActivity.
     */
    fun copyFromUri(context: Context, uri: Uri): File? {
        return try {
            val file = newPhotoFile(context)
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { inStream ->
                file.outputStream().use { outStream -> inStream.copyTo(outStream) }
            }
            file
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Charge une image réduite (pour ne pas manquer de mémoire) et la remet à l'endroit. */
    private fun loadBitmap(path: String, maxSize: Int): Bitmap? {
        if (!File(path).exists()) return null

        // 1) Lire seulement la taille de l'image
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        // 2) Calculer de combien il faut réduire l'image
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= maxSize &&
            bounds.outHeight / (sampleSize * 2) >= maxSize
        ) {
            sampleSize *= 2
        }

        // 3) Charger l'image réduite
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null

        // 4) Les photos de l'appareil photo sont parfois enregistrées "couchées" : on les tourne
        val rotation = try {
            when (ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: IOException) {
            0f
        }
        if (rotation == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
