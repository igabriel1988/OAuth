package com.example.oauth

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.oauth.databinding.ActivityMainBinding
import com.example.oauth.databinding.ActivityOpcionesBinding
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import org.json.JSONObject


enum class TipoProveedor{
    CORREO,
    GOOGLE,
    FACEBOOK
}

class OpcionesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOpcionesBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInOptions: GoogleSignInOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpcionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Datos de la actividad
        var bundle: Bundle? = intent.extras
        var email: String? = bundle?.getString("email")
        var proveedor: String? = bundle?.getString("proveedor")

        inicio(email ?: "", proveedor ?: "")

        //Guardar datos de session
        val preferencias = getSharedPreferences(
            getString(R.string.file_preferencia),
            Context.MODE_PRIVATE
        ).edit()

        preferencias.putString("email", email)
        preferencias.putString("proveedor", proveedor)

    }

    private fun inicio(email: String, proveedor: String) {
        binding.mail.text = email
        binding.provedor.text = proveedor


        binding.closeSesion.setOnClickListener {

            val preferencias = getSharedPreferences(
                getString(R.string.file_preferencia),
                Context.MODE_PRIVATE
            ).edit()

            preferencias.clear()
            preferencias.apply()

            if (proveedor == TipoProveedor.FACEBOOK.name) {
                LoginManager.getInstance().logOut()
            }




            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        //google
        if (proveedor == TipoProveedor.GOOGLE.name) {
            googleSignInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
                    .build()
            googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
            val data = GoogleSignIn.getLastSignedInAccount(this)
            if (data != null) {
                Picasso.get().load(data.photoUrl).into(binding.img)
            }
        } else if (proveedor == TipoProveedor.FACEBOOK.name) {
            val accessToken = AccessToken.getCurrentAccessToken()
            Toast.makeText(this, "FACEBOOK", Toast.LENGTH_SHORT).show()
            if (accessToken != null) {
                val request: GraphRequest =
                    GraphRequest.newMeRequest(accessToken, GraphRequest.GraphJSONObjectCallback(
                        { obj: JSONObject, response: GraphResponse ->
                            val correo = obj.getString("email")
                            binding.mail.text = correo
                            val url = obj.getJSONObject("picture").getJSONObject("data")
                                .getString("url")
                            Picasso.get().load(url).into(binding.img)

                        } as (JSONObject?, GraphResponse?) -> Unit))
                val paramters = Bundle()
                paramters.putString("fields", "id,name,link,email,picture.type(large)")
                request.parameters = paramters
                request.executeAsync()
            }
        }
    }
}