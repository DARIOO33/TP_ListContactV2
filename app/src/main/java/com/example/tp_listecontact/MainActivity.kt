package com.example.tp_listecontact

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private val contacts = ArrayList<Contact>()
    private lateinit var adapter: ContactAdapter

    /**
     * Retour de résultat d'activité : appelé quand DetailActivity se ferme.
     * RESULT_OK = "Enregistrer", RESULT_CANCELED = "Annuler" ou bouton retour.
     */
    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val contact = IntentCompat.getSerializableExtra(
                    data, DetailActivity.EXTRA_CONTACT, Contact::class.java
                ) ?: return@registerForActivityResult
                val position = data.getIntExtra(DetailActivity.EXTRA_POSITION, -1)

                if (position in contacts.indices) {
                    contacts[position] = contact   // modification d'un contact existant
                } else {
                    contacts.add(contact)          // nouveau contact (bouton +)
                }
                adapter.notifyDataSetChanged()     // actualise la liste et les photos
                Toast.makeText(this, R.string.contact_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.changes_cancelled, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupInsets(toolbar)

        // Contacts : restaurés après une rotation de l'écran, sinon contacts d'exemple
        val saved = savedInstanceState?.let {
            @Suppress("UNCHECKED_CAST")
            BundleCompat.getSerializable(it, KEY_CONTACTS, ArrayList::class.java) as? ArrayList<Contact>
        }
        contacts.addAll(saved ?: sampleContacts())

        val listView = findViewById<ListView>(R.id.listContacts)
        adapter = ContactAdapter(this, contacts)
        listView.adapter = adapter
        listView.emptyView = findViewById(R.id.txtEmpty)

        // Clic sur un contact -> DetailActivity (Intent explicite)
        listView.setOnItemClickListener { _, _, position, _ ->
            openDetail(contacts[position], position)
        }
    }

    private fun openDetail(contact: Contact?, position: Int) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra(DetailActivity.EXTRA_CONTACT, contact)
        intent.putExtra(DetailActivity.EXTRA_POSITION, position)
        detailLauncher.launch(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_add) {
            openDetail(null, -1)  // -1 = nouveau contact
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_CONTACTS, contacts)
    }

    /** La barre violette passe sous la barre d'état ; la liste ne passe pas sous la barre de navigation. */
    private fun setupInsets(toolbar: MaterialToolbar) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.updatePadding(top = bars.top)
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    private fun sampleContacts() = listOf(
        Contact("Dupont", "Claire", "06 12 34 56 78", "claire.dupont@example.com"),
        Contact("Martin", "Thomas", "07 23 45 67 89", "thomas.martin@example.com"),
        Contact("Bernard", "Sophie", "06 98 76 54 32", "sophie.bernard@example.com"),
        Contact("Petit", "Lucas", "07 11 22 33 44", "lucas.petit@example.com"),
        Contact("Moreau", "Julien", "06 55 44 33 22", "julien.moreau@example.com")
    )

    companion object {
        private const val KEY_CONTACTS = "contacts"
    }
}
