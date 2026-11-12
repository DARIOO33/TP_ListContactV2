package com.example.tp_listecontact

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File

class DetailActivity : AppCompatActivity() {

    private lateinit var imgPhoto: ImageView
    private lateinit var layoutNom: TextInputLayout
    private lateinit var layoutPrenom: TextInputLayout
    private lateinit var layoutPhone: TextInputLayout
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var editNom: TextInputEditText
    private lateinit var editPrenom: TextInputEditText
    private lateinit var editPhone: TextInputEditText
    private lateinit var editEmail: TextInputEditText

    private var position = -1
    private var photoPath: String? = null        // photo affichée actuellement
    private var cameraPhotoPath: String? = null  // fichier en cours de remplissage par l'appareil photo

    /** Galerie : renvoie l'Uri de l'image choisie, ou null si l'utilisateur annule. */
    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                toast(R.string.photo_cancelled)
                return@registerForActivityResult
            }
            val file = PhotoUtils.copyFromUri(this, uri)
            if (file == null) {
                toast(R.string.photo_error)
            } else {
                photoPath = file.absolutePath
                PhotoUtils.showPhoto(imgPhoto, photoPath)
            }
        }

    /** Appareil photo : renvoie true si la photo a bien été prise. */
    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val path = cameraPhotoPath
            cameraPhotoPath = null
            if (success && path != null && File(path).length() > 0) {
                photoPath = path
                PhotoUtils.showPhoto(imgPhoto, photoPath)
            } else {
                path?.let { File(it).delete() }  // on supprime le fichier vide
                toast(R.string.photo_cancelled)
            }
        }

    /** Demande de la permission caméra à l'utilisateur. */
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera() else toast(R.string.camera_permission_denied)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setupInsets(toolbar)
        toolbar.setNavigationOnClickListener { cancel() }

        imgPhoto = findViewById(R.id.imgPhoto)
        layoutNom = findViewById(R.id.layoutNom)
        layoutPrenom = findViewById(R.id.layoutPrenom)
        layoutPhone = findViewById(R.id.layoutPhone)
        layoutEmail = findViewById(R.id.layoutEmail)
        editNom = findViewById(R.id.editNom)
        editPrenom = findViewById(R.id.editPrenom)
        editPhone = findViewById(R.id.editPhone)
        editEmail = findViewById(R.id.editEmail)

        // Données reçues de MainActivity
        position = intent.getIntExtra(EXTRA_POSITION, -1)
        val contact = IntentCompat.getSerializableExtra(intent, EXTRA_CONTACT, Contact::class.java)
        toolbar.setTitle(if (contact == null) R.string.title_new else R.string.title_detail)

        if (savedInstanceState == null) {
            // Premier affichage : on remplit les champs
            if (contact != null) {
                editNom.setText(contact.nom)
                editPrenom.setText(contact.prenom)
                editPhone.setText(contact.telephone)
                editEmail.setText(contact.email)
                photoPath = contact.photoPath
            }
        } else {
            // Après une rotation : les champs se restaurent seuls, il reste les photos
            photoPath = savedInstanceState.getString(KEY_PHOTO)
            cameraPhotoPath = savedInstanceState.getString(KEY_CAMERA_PHOTO)
        }
        PhotoUtils.showPhoto(imgPhoto, photoPath)

        // L'erreur d'un champ disparaît dès que l'utilisateur tape
        editNom.doAfterTextChanged { layoutNom.error = null }
        editPrenom.doAfterTextChanged { layoutPrenom.error = null }
        editPhone.doAfterTextChanged { layoutPhone.error = null }
        editEmail.doAfterTextChanged { layoutEmail.error = null }

        findViewById<MaterialButton>(R.id.btnCall).setOnClickListener { callContact() }
        findViewById<MaterialButton>(R.id.btnEmail).setOnClickListener { emailContact() }
        findViewById<MaterialButton>(R.id.btnGallery).setOnClickListener { pickFromGallery() }
        findViewById<MaterialButton>(R.id.btnCamera).setOnClickListener { takePhoto() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveContact() }
        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { cancel() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PHOTO, photoPath)
        outState.putString(KEY_CAMERA_PHOTO, cameraPhotoPath)
    }

    // ---------- Intents implicites : appel et e-mail ----------

    private fun callContact() {
        val phone = editPhone.text.toString().trim()
        if (phone.isEmpty()) {
            layoutPhone.error = getString(R.string.error_phone_required)
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
        startSafely(intent, R.string.no_dialer_app)
    }

    private fun emailContact() {
        val email = editEmail.text.toString().trim()
        if (email.isEmpty()) {
            layoutEmail.error = getString(R.string.error_email_required)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.error = getString(R.string.error_email_invalid)
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        startSafely(intent, R.string.no_email_app)
    }

    /** Lance une autre application ; affiche un message si aucune ne peut le faire. */
    private fun startSafely(intent: Intent, errorMessage: Int) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(errorMessage)
        }
    }

    // ---------- Photos : galerie et appareil photo ----------

    private fun pickFromGallery() {
        try {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_gallery_app)
        }
    }

    private fun takePhoto() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            toast(R.string.no_camera)
            return
        }
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val file = PhotoUtils.newPhotoFile(this)
        cameraPhotoPath = file.absolutePath
        // FileProvider : donne à l'appli Appareil photo le droit d'écrire dans notre fichier
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        try {
            takePhotoLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            cameraPhotoPath = null
            toast(R.string.no_camera)
        }
    }

    // ---------- Enregistrer / Annuler ----------

    private fun saveContact() {
        val nom = editNom.text.toString().trim()
        val prenom = editPrenom.text.toString().trim()
        val phone = editPhone.text.toString().trim()
        val email = editEmail.text.toString().trim()

        var valid = true
        if (nom.isEmpty()) {
            layoutNom.error = getString(R.string.error_required)
            valid = false
        }
        if (prenom.isEmpty()) {
            layoutPrenom.error = getString(R.string.error_required)
            valid = false
        }
        if (phone.isEmpty()) {
            layoutPhone.error = getString(R.string.error_required)
            valid = false
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.error = getString(R.string.error_email_invalid)
            valid = false
        }
        if (!valid) {
            toast(R.string.error_fix_fields)
            return
        }

        // On renvoie le contact modifié à MainActivity
        val result = Intent()
        result.putExtra(EXTRA_CONTACT, Contact(nom, prenom, phone, email, photoPath))
        result.putExtra(EXTRA_POSITION, position)
        setResult(RESULT_OK, result)
        finish()
    }

    private fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    // ---------- Divers ----------

    private fun toast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupInsets(toolbar: MaterialToolbar) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            // ime() = clavier : le formulaire remonte quand le clavier s'ouvre
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            toolbar.updatePadding(top = bars.top)
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    companion object {
        const val EXTRA_CONTACT = "extra_contact"
        const val EXTRA_POSITION = "extra_position"
        private const val KEY_PHOTO = "photo_path"
        private const val KEY_CAMERA_PHOTO = "camera_photo_path"
    }
}
