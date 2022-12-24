package com.example.oauth

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.example.oauth.databinding.ActivityMainBinding
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.util.Objects

class MainActivity : AppCompatActivity()
{

    private lateinit var binding: ActivityMainBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var callbackManager = CallbackManager.Factory.create()


    override fun onCreate( savedInstanceState: Bundle? )
            {
                super.onCreate( savedInstanceState )
                binding = ActivityMainBinding.inflate( layoutInflater )
                setContentView( binding.root )
                validate()
            }

    private fun sesiones()
            {
                val preferencias = getSharedPreferences(
                    getString( R.string.file_preferencia ),
                    Context.MODE_PRIVATE
                )

                var email : String?=preferencias.getString( "email", null )
                var proveedor : String?=preferencias.getString( "proveedor", null )

                if( email != null && proveedor != null ){
                    opciones( email, TipoProveedor.valueOf( proveedor ) )
                }
            }

    private fun validate()
        {
                binding.updateUser.setOnClickListener(){
                    //Validar campos vacios
                        if( !binding.username.text.toString().isEmpty()
                            && !binding.password.text.toString().isEmpty() )
                            {
                                    //Comunicacion con mi sistema
                                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                                        binding.username.text.toString(),
                                        binding.password.text.toString()
                                    ).addOnCompleteListener{
                                          if( it.isComplete )
                                          {
                                              //Mandar a la actividad
                                                Toast.makeText( binding.signin.context, "Enlace con exito", Toast.LENGTH_SHORT ).show()
                                                opciones(
                                                    it.result?.user?.email?:"",
                                                    TipoProveedor.CORREO
                                                )
                                          }
                                          else
                                          {
                                              //Error
                                                alert()
                                          }
                                    }
                            }
                }
            binding.loginbtn.setOnClickListener{
                if( !binding.username.text.toString().isEmpty()
                    && !binding.password.text.toString().isEmpty() )
                            {
                                    FirebaseAuth.getInstance().signInWithEmailAndPassword(
                                        binding.username.text.toString(),
                                        binding.password.text.toString()
                                    ).addOnCompleteListener{
                                        if( it.isSuccessful )
                                            {
                                                opciones( it.result?.user?.email?:"", TipoProveedor.CORREO )
                                            }
                                        else
                                            {
                                                alert()
                                            }
                                    }
                            }
                }
            ///boton google

                iniciarActividad()
                binding.google.setOnClickListener{
                       val config =
                            GoogleSignInOptions.Builder( GoogleSignInOptions.DEFAULT_SIGN_IN ).requestIdToken(
                                getString( R.string.default_web_client_id )
                            ).requestEmail().build()
                        val clienteGoogle = GoogleSignIn.getClient( this, config )
                        clienteGoogle.signOut()
                        var signIn : Intent = clienteGoogle.signInIntent
                        activityResultLauncher.launch( signIn )
                }
            ////Facebook
                    FirebaseAuth.getInstance().signOut()
                    LoginManager.getInstance().logOut()
                        binding.facebook.setReadPermissions(
                            listOf(
                                "public_profile",
                                "email",
                                "user_birthday",
                                "user_friends",
                                "user_gender"
                            )
                        )


            binding.facebook.registerCallback(callbackManager, object : FacebookCallback<LoginResult>
                            {
                                        override fun onSuccess(result: LoginResult) {
                                            Log.e("TAG", "login")
                                            val request = GraphRequest.newMeRequest(result.accessToken)
                                            { _ /*object tipo Stirng*/, _/*response*/ ->
                                                val token = result.accessToken
                                                val credenciales = FacebookAuthProvider.getCredential(token.token)
                                                FirebaseAuth.getInstance().signInWithCredential(credenciales)
                                                    .addOnCompleteListener {
                                                        if (it.isSuccessful) {
                                                            Toast.makeText(
                                                                binding.signin.context,
                                                                "Sign in successful",
                                                                Toast.LENGTH_SHORT
                                                            )
                                                                .show()
                                                            opciones(it.result?.user?.email ?: "", TipoProveedor.FACEBOOK )
                                                        } else {
                                                            alert()
                                                        }
                                                    }

                                            }
                                            val parameters = Bundle()
                                            parameters.putString(
                                                "fields",
                                                "id,name,email,gender,birthday"
                                            )
                                            request.parameters = parameters
                                            request.executeAsync()
                                        }

                                        override fun onCancel() {
                                            Log.v("MainActivity", "cancel")
                                        }

                                        override fun onError(error: FacebookException) {
                                            Log.v("MainActivity", error.cause.toString())
                                        }
                            })




        }//

    private fun iniciarActividad()
        {
               activityResultLauncher =
                        registerForActivityResult( ActivityResultContracts.StartActivityForResult() ) { result ->
                                    if( result.resultCode == Activity.RESULT_OK )
                                        {
                                            val task = GoogleSignIn.getSignedInAccountFromIntent( result.data )
                                            try
                                                {
                                                        val account = task.getResult( ApiException :: class.java )
                                                        Toast.makeText( this, "Conexión con exito ", Toast.LENGTH_SHORT ).show()
                                                        if( account != null )
                                                                {
                                                                    var credenciales = GoogleAuthProvider.getCredential( account.idToken, null )
                                                                    FirebaseAuth.getInstance().signInWithCredential( credenciales )
                                                                        .addOnCompleteListener {
                                                                            if ( it.isComplete )
                                                                                {
                                                                                    opciones( account.email?:"", TipoProveedor.GOOGLE )
                                                                                }
                                                                            else
                                                                                {
                                                                                    alert()
                                                                                }
                                                                        }
                                                                }
                                                }
                                            catch ( e : ApiException )
                                                {
                                                    Toast.makeText( this, "Error al conectar ", Toast.LENGTH_SHORT ).show()
                                                }
                                        }
                            }
        }

    private fun alert()
        {
            val bulder = AlertDialog.Builder(this)
            bulder.setTitle("Mensaje")
            bulder.setMessage("Se produjo un error, contacte al provesor")
            bulder.setPositiveButton("Aceptar", null)
            val dialog: AlertDialog = bulder.create()
            dialog.show()
        }

    private fun opciones( email : String, proveedor : TipoProveedor )
        {
                var pasos = Intent( this, OpcionesActivity::class.java ).apply {
                    putExtra( "email", email )
                    putExtra( "proveedor", proveedor.name )
                }

            startActivity( pasos )
        }

}