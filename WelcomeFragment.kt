package com.vienhealth.patient.modules.welcome

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import cafe.adriel.kbus.KBus
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.applinks.AppLinkData
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.vienhealth.patient.R
import com.vienhealth.patient.base.presentation.extensions.observe
import com.vienhealth.patient.base.presentation.fragment.BaseContainerFragment
import com.vienhealth.patient.data.model.SignupModel
import com.vienhealth.patient.data.network.preferences.PrefsManager
import com.vienhealth.patient.data.network.responses.UserResponse
import com.vienhealth.patient.databinding.FragmentWelcomeBinding
import com.vienhealth.patient.modules.signup.viewModels.SignupViewModels
import com.vienhealth.patient.navHost.HomeActivity
import com.vienhealth.patient.utils.*
import org.kodein.di.generic.instance
import permissions.dispatcher.*
import timber.log.Timber
import java.net.URL


@RuntimePermissions
class WelcomeFragment : BaseContainerFragment<FragmentWelcomeBinding>(), LocationResultListener {
    override val layoutResourceId: Int
        get() = R.layout.fragment_welcome


    private lateinit var binding: FragmentWelcomeBinding
    private lateinit var activity: Activity


    private val callbackManager = CallbackManager.Factory.create()

    private lateinit var signupModel: SignupModel


    private val viewModel: SignupViewModels by instance()


    var mGoogleSignInClient: GoogleSignInClient? = null


    private var isFacebook = false


    private val locationHandler: LocationHandler by lazy {
        LocationHandler(requireActivity(), this)
    }


    private lateinit var loginResponse: UserResponse

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = getViewDataBinding()
        binding.vm = viewModel
        activity = requireActivity()
        Click()


        binding.defFbButton.fragment = this

        binding.defFbButton.setPermissions("email")
        binding.defFbButton.registerCallback(callbackManager, callbackFacebook)

        observe(viewModel.stateLiveData, stateObserver)


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun openDialog() {
        if (requireActivity().CheckForBiometrics()) {
            EnableSecurityFragment.newInstance(false)
                .show(childFragmentManager, "dialog")
        }else{
            loginResponse.let { userData ->
                if (!userData.patient.basicInfo.isPhoneNumberVerified) {
                    moveNext(R.id.action_loginFragment_to_addPhoneNumberFragment)
                } else if (userData.patient.healthInfo == null) {
                    moveNext(R.id.moveToHealth)
                } else if (!userData.patient.healthInfo.medicalHistory.isSkip && userData.patient.healthInfo.medicalHistory.data == null) {
                    moveNext(R.id.moveToHealth)
                } else if (!userData.patient.healthInfo.typesOfDoctors.isSkip && userData.patient.healthInfo.typesOfDoctors.data.isEmpty()) {
                    moveNext(R.id.moveToTypeOfDoctors)
                } else if (!userData.patient.healthInfo.prescribedMedication.isSkip && userData.patient.healthInfo.prescribedMedication.data == null) {
                    moveNext(R.id.moveToPrecribed)
                } else if (!userData.patient.healthInfo.allergies.isSkip && userData.patient.healthInfo.allergies.data == null) {
                    moveNext(R.id.moveToAllergies)
                } else if (!userData.patient.healthInfo.mentalHealth.isSkip && userData.patient.healthInfo.mentalHealth.data.isEmpty()) {
                    moveNext(R.id.moveToMentalHealth)
                } else if (!userData.patient.healthInfo.lifestyle.isSkip && userData.patient.healthInfo.lifestyle.data.isEmpty()) {
                    moveNext(R.id.moveToLifeStyle)
                } else {
                    startActivity(Intent(requireActivity(), HomeActivity::class.java))
                    requireActivity().finishAffinity()
                }
            }
        }
    }

    private fun Click() {
        binding.backBar.setOnClickListener {
            activity.onBackPressed()
        }

        binding.emailLogin.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
        }

        binding.joinButton.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_joinCorporate1Fragment)
        }

        binding.fbLogin.onClick {
            isFacebook = true
            LoginManager.getInstance().logOut()
            fetchLocationWithPermissionCheck()
        }

        binding.googleLogin.onClick {
            isFacebook = false
            fetchLocationWithPermissionCheck()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun GoogleLogin() {
        mGoogleSignInClient!!.signOut()
        val signInIntent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, 1001)
    }

    @SuppressLint("RestrictedApi", "BinaryOperationInTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            val task: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val user = task.getResult(ApiException::class.java)

                var pic =
                    "https://drive.google.com/uc?export=view&id=1rcTyPTW0KDXdsMG3GjSlueiKbXmYTD-A"

                if (user!!.photoUrl != null) {
                    pic = user.photoUrl.toString()
                }
                Timber.e("user_googledata " + user.displayName)
                Timber.e("user_googledata " + user.email)
                Timber.e("user_googledata " + user.id)
                Timber.e("user_googledata " + pic)

                var names = user.displayName?.split(" ")

                signupModel = signupModel.copy(
                    socialId = user.id,
                    firstName = names?.get(0),
                    lastName = names?.get(1),
                    email = user.email,
                    image = pic,
                    type = "google"
                )
                PrefsManager.get().save(Appkeys.LOGIN_TYPE, "google")
                PrefsManager.get().save(Appkeys.SOCIAL_ID, user.id.toString())

                var checkLogin = SignupModel(
                    socialId = user.id.toString(),
                    type = "google",
                    lat = signupModel.lat,
                    lng = signupModel.lng,
                    deviceType = signupModel.deviceType,
                    token = signupModel.token,
                    deviceId = signupModel.deviceId
                )
                showLoading()
                viewModel.socialLogin(checkLogin)

            } catch (e: Exception) {
                Timber.e("ERROR " + e.toString())
            }
        }
    }


    private val callbackFacebook = object : FacebookCallback<LoginResult> {
        override fun onSuccess(result: LoginResult?) {
            result?.let {
                Timber.e("facebook login : SUCCESS")
                getFacebookData(it)
            }
        }

        override fun onCancel() {
            Timber.e("facebook login : CANCEL")
        }

        override fun onError(error: FacebookException?) {
            error?.let {
                Timber.e("facebook login : Error " + error.message)
            }
        }
    }

    private fun getFacebookData(result: LoginResult?) {
        showLoading()
        var request = GraphRequest.newMeRequest(
            result?.accessToken
        ) { `object`, response ->
            try {
                val id = `object`.getString("id")
                val profilePic = URL("https://graph.facebook.com/$id/picture?width=200&height=150")
                var email = if (`object`.has("email")) {
                    `object`.getString("email").toString()
                } else {
                    ""
                }
                signupModel = signupModel.copy(
                    socialId = `object`.getString("id").toString(),
                    firstName = `object`.getString("first_name").toString(),
                    lastName = `object`.getString("last_name").toString(),
                    email = email,
                    image = "https://graph.facebook.com/$id/picture?width=200&height=150",
                    type = "facebook"
                )
                PrefsManager.get().save(Appkeys.LOGIN_TYPE, "facebook")
                PrefsManager.get().save(Appkeys.SOCIAL_ID, id)

                var checkLogin = SignupModel(
                    socialId = id,
                    type = "facebook",
                    lat = signupModel.lat,
                    lng = signupModel.lng,
                    deviceType = signupModel.deviceType,
                    token = signupModel.token,
                    deviceId = signupModel.deviceId
                )
                viewModel.socialLogin(checkLogin)
            } catch (error: Exception) {
                hideLoading()
                error.printStackTrace()
            }
        }

        val parameters = Bundle()
        parameters.putString(
            "fields",
            "id, first_name, last_name, email, gender, birthday,age_range, location"
        )
        request.parameters = parameters
        request.executeAsync()
    }

    private val stateObserver = Observer<SignupViewModels.ViewState> {
        hideLoading()
        if (it.isError) {
            var bundle = bundleOf("signupValue" to signupModel)
            moveNextArgs(R.id.move_ToSocialRegister, bundle)
        } else {
            performLoginOperation(it.data)
        }
    }

    private fun performLoginOperation(data: UserResponse?) {
        data?.let { userData ->
            loginResponse = data
            PrefsManager.get().save(Appkeys.IS_LOGIN, true)
            PrefsManager.get().saveObj(
                Appkeys.SIGN_IN_RESPONSE,
                Gson().toJson(userData).toString()
            )
            PrefsManager.get().save(Appkeys.USER_ID, userData.patient._id)
            PrefsManager.get().save(Appkeys.ACESS_TOKEN, userData.token)
            openDialog()
        }
    }

    @NeedsPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    fun fetchLocation() {
        showLoading()
        locationHandler.getUserLocation()
    }

    @OnShowRationale(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    fun showRationaleForLocation(request: PermissionRequest) {
        request.proceed()
    }

    @OnPermissionDenied(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    fun onDenied() {

    }

    @OnNeverAskAgain(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    fun onNeverAskAgain() {
        requireActivity().explain()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // NOTE: delegate the permission handling to generated function
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun getLocation(location: Location) {
        hideLoading()

        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            it.addOnCompleteListener {
                Timber.e(it.result)

                PrefsManager.get().save(Appkeys.USER_LAT, location.latitude.toString())
                PrefsManager.get().save(Appkeys.USER_LNG, location.longitude.toString())
                signupModel = SignupModel(
                    lat = location.latitude,
                    lng = location.longitude,
                    deviceId = deviceId(),
                    deviceType = "android",
                    token = it.result
                )
                if (isFacebook) {
                    binding.defFbButton.performClick()
                } else {
                    GoogleLogin()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        KBus.subscribe<ShowMessageEvent>(this) {
            var data = it.data as Int
            if (data == 111) {
                loginResponse.let { userData ->
                    if (!userData.patient.basicInfo.isPhoneNumberVerified) {
                        moveNext(R.id.action_loginFragment_to_addPhoneNumberFragment)
                    } else if (userData.patient.healthInfo == null) {
                        moveNext(R.id.moveToHealth)
                    } else if (!userData.patient.healthInfo.medicalHistory.isSkip && userData.patient.healthInfo.medicalHistory.data == null) {
                        moveNext(R.id.moveToHealth)
                    } else if (!userData.patient.healthInfo.typesOfDoctors.isSkip && userData.patient.healthInfo.typesOfDoctors.data.isEmpty()) {
                        moveNext(R.id.moveToTypeOfDoctors)
                    } else if (!userData.patient.healthInfo.prescribedMedication.isSkip && userData.patient.healthInfo.prescribedMedication.data == null) {
                        moveNext(R.id.moveToPrecribed)
                    } else if (!userData.patient.healthInfo.allergies.isSkip && userData.patient.healthInfo.allergies.data == null) {
                        moveNext(R.id.moveToAllergies)
                    } else if (!userData.patient.healthInfo.mentalHealth.isSkip && userData.patient.healthInfo.mentalHealth.data.isEmpty()) {
                        moveNext(R.id.moveToMentalHealth)
                    } else if (!userData.patient.healthInfo.lifestyle.isSkip && userData.patient.healthInfo.lifestyle.data.isEmpty()) {
                        moveNext(R.id.moveToLifeStyle)
                    } else {
                        startActivity(Intent(requireActivity(), HomeActivity::class.java))
                        requireActivity().finishAffinity()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        KBus.unsubscribe(this)
    }
}
