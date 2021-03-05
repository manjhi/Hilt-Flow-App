package com.vienhealth.patient.utils

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.hardware.fingerprint.FingerprintManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.text.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.textfield.TextInputLayout
import com.vienhealth.patient.R
import com.vienhealth.patient.data.network.preferences.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap
import kotlin.math.roundToInt

/**
 * Created by Manjinder Singh on 09,October,2020
 */

fun Activity.makeStatusBarTransparent() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            statusBarColor = Color.TRANSPARENT
        }
    }
}

fun FragmentActivity.makeStatusBarTransparent() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            statusBarColor = Color.TRANSPARENT
        }
    }
}

fun View.Gone() {
    this.visibility = View.GONE
}

fun View.Visible() {
    this.visibility = View.VISIBLE
}


fun View.InVisible() {
    this.visibility = View.INVISIBLE
}

fun ImageView.setImageDrawableWithAnimation(drawable: Drawable, duration: Int = 300) {
    val currentDrawable = getDrawable()
    if (currentDrawable == null) {
        setImageDrawable(drawable)
        return
    }

    val transitionDrawable = TransitionDrawable(
        arrayOf(
            currentDrawable,
            drawable
        )
    )
    setImageDrawable(transitionDrawable)
    transitionDrawable.startTransition(duration)
}

fun Context.getHeight(): Int {

    val displayMetrics = DisplayMetrics()
    val windowmanager: WindowManager =
        this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowmanager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
}

fun RecyclerView.setManager(
    isItHorizontal: Boolean = false,
    isItGrid: Boolean = false,
    spanCount: Int = 2
) {
    if (isItGrid)
        this.layoutManager = GridLayoutManager(this.context, spanCount)
    else {
        if (isItHorizontal)
            this.layoutManager = LinearLayoutManager(this.context, RecyclerView.HORIZONTAL, false)
        else
            this.layoutManager = LinearLayoutManager(this.context, RecyclerView.VERTICAL, false)
    }
}

fun String.toast() {
    Toast.makeText(App.getInstance()?.applicationContext, this, Toast.LENGTH_LONG).show()
}

fun getJsonDataFromAsset(context: Context, fileName: String): String? {
    val jsonString: String
    try {
        jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return null
    }
    return jsonString
}

fun String.isValidPassword(): Boolean {
    this.let {
        val passwordREGEX = Pattern.compile(
            "^" +
                    "(?=.*[0-9])" +         //at least 1 digit
                    "(?=.*[a-z])" +         //at least 1 lower case letter
                    "(?=.*[A-Z])" +         //at least 1 upper case letter
//                "(?=.*[a-zA-Z])" +      //any letter
                    "(?=.*[!@#$%^&+=])" +    //at least 1 special character
                    "(?=\\S+$)" +           //no white spaces
                    ".{8,}" +               //at least 8 characters
                    "$"
        );
        return passwordREGEX.matcher(this).matches()
    }
}

fun String.isOneDigit(): Boolean {
    this.let {
        val passwordREGEX = Pattern.compile(
            "^" +
                    "(?=.*[0-9])" +         //at least 1 digit
                    ".{1,}" +               //at least 1 characters
                    "$"
        );
        return passwordREGEX.matcher(this).matches()
    }
}

fun String.isOneUpper(): Boolean {
    this.let {
        val passwordREGEX = Pattern.compile(
            "^" +
                    "(?=.*[A-Z])" +         //at least 1 upper case letter
                    ".{1,}" +               //at least 1 characters
                    "$"
        );
        return passwordREGEX.matcher(this).matches()
    }
}

fun String.isOneLower(): Boolean {
    this.let {
        val passwordREGEX = Pattern.compile(
            "^" +
                    "(?=.*[a-z])" +         //at least 1 lower case letter
                    ".{1,}" +               //at least 1 characters
                    "$"
        );
        return passwordREGEX.matcher(this).matches()
    }
}

fun String.isOneSpecial(): Boolean {
    this.let {
        val passwordREGEX = Pattern.compile(
            "^" +
                    "(?=.*[!@#$%^&+=])" +    //at least 1 special character
                    ".{1,}" +               //at least 1 characters
                    "$"
        );
        return passwordREGEX.matcher(this).matches()
    }
}

fun String.is8Char(): Boolean {
    this.let {
        val passwordREGEX = Pattern.compile(
            "^" +
                    ".{8,}" +               //at least 8 characters
                    "$"
        );
        return passwordREGEX.matcher(this).matches()
    }
}

fun TextView.showStrikeThrough(show: Boolean) {
    paintFlags =
        if (show) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
}

fun getHeaderMap(): Map<String, String> {
    val map = HashMap<String, String>()
    map["Content-Type"] = "application/json"
    map["Accept"] = "application/json"
    map["x-access-token"] = PrefsManager.get().getString(Appkeys.ACESS_TOKEN, "")!!
    return map
}

fun addPartBody(builder: MultipartBody.Builder, name: String, path: String?) {
    path?.let {
        val file = File(it)
        val body = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
        builder.addFormDataPart(name, file.name, body)
    }
}

fun Intent?.getFilePath(context: Context): String {
    return this?.data?.let { data -> RealPathUtil.getRealPath(context, data) ?: "" } ?: ""
}

fun Uri?.getFilePath(context: Context): String {
    return this?.let { uri -> RealPathUtil.getRealPath(context, uri) ?: "" } ?: ""
}

fun ClipData.Item?.getFilePath(context: Context): String {
    return this?.uri?.getFilePath(context) ?: ""
}


fun FragmentActivity.explain() {
    val dialog = AlertDialog.Builder(this)
    dialog.setMessage("You need to give some mandatory permissions to continue. Do you want to go to app settings?")
        .setPositiveButton("Yes") { paramDialogInterface, _ ->

            paramDialogInterface.dismiss()

            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + this.packageName)
            ).also {
                this.startActivity(it)
            }
        }
    /*.setNegativeButton(
        getString(R.string.cancel)
    ) { paramDialogInterface, _ -> paramDialogInterface.dismiss() }*/
    dialog.show()
}

@BindingAdapter("image")
fun ImageView.loadImage(path: String?) {
    path?.let {
        Glide.with(this).load(it).placeholder(R.mipmap.ic_logo_icon_round)
            .error(R.mipmap.ic_logo_icon_round).into(this)
    }
    if (path.isNullOrEmpty()) {
        this.setImageResource(R.mipmap.ic_logo_icon_round)
    }
}

@BindingAdapter("image_signup")
fun ImageView.loadImageSignUp(path: String?) {
    path?.let {
        Glide.with(this).load(it).placeholder(R.drawable.ic_add_user)
            .error(R.drawable.ic_add_user).into(this)
    }
    if (path.isNullOrEmpty()) {
        this.setImageResource(R.drawable.ic_add_user)
    }
}

@BindingAdapter("tintColor")
fun ImageView.setTintColor(isBookmarked: Boolean?) {
    if (isBookmarked ?: false) {
        this.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this.context, R.color.colorAccent))
    } else {
        this.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this.context, R.color.TextLight))
    }
}

@BindingAdapter("text_set")
fun TextView.showText(value: String?) {
    value?.let {
        this.text = Html.fromHtml(value.replace("null", ""))
    }
    if (value.isNullOrEmpty()) {
        this.text = "-NA-"
    }
}

@BindingAdapter("text_set_speciality")
fun TextView.showTextSpeciality(value: String?) {
    value?.let {
        if (it.contains(" ")) {
            this.text = Html.fromHtml(value.replace("null", ""))
        } else {
            if (value.length > 10) {
                Timber.e(addChar(it, "\n-", 9))
                this.text = Html.fromHtml(addChar(it, "\n-", 9)?.replace("null", ""))
            }else{
                this.text = Html.fromHtml(value.replace("null", ""))
            }
        }
    }
    if (value.isNullOrEmpty()) {
        this.text = "-NA-"
    }
}

fun addChar(str: String, ch: String, position: Int): String? {
    return str.substring(0, position) + ch + str.substring(position)
}


@BindingAdapter("text_set_capital")
fun TextView.showCapitalText(value: String?) {
    value?.let {
        this.text = value.replace("null", "")
        this.text = this.text.split(' ').joinToString(" ") { it.capitalize() }
    }
    if (value.isNullOrEmpty()) {
        this.text = "-NA-"
    }
}

@BindingAdapter("text_set")
fun EditText.showText(value: String?) {
    value?.let {
        this.setText(value.replace("null", ""))
    }
    if (value.isNullOrEmpty()) {
        this.setText("-NA-")
    }
}

fun TextView.showTimeText(dateStr: Date?) {
    val destFormat = SimpleDateFormat("hh:mm a")
    destFormat.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    val convertedDate = destFormat.format(dateStr)
    this.setText(convertedDate)
}

fun TextView.showDateText(dateStr: Date?) {
    val destFormat = SimpleDateFormat("dd-MM-yyyy")
    destFormat.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    val convertedDate = destFormat.format(dateStr)
    this.setText(convertedDate)
}

@BindingAdapter("set_visibility")
fun View.setVisibility(value: Boolean) {
    value.let {
        if (it) {
            this.visibility = View.VISIBLE
        } else {
            this.visibility = View.GONE
        }
    }
}


fun getGreetingMessage(): String {
    val c = Calendar.getInstance()
    val timeOfDay = c.get(Calendar.HOUR_OF_DAY)
    return when (timeOfDay) {
        in 0..11 -> "Good Morning"
        in 12..15 -> "Good Afternoon"
        in 16..20 -> "Good Evening"
        in 21..23 -> "Good Night"
        else -> "Hello"
    }
}

fun String.centimeterToFeet(): String? {
    var feetPart = 0
    var inchesPart = 0
    if (!TextUtils.isEmpty(this)) {
        val dCentimeter = java.lang.Double.valueOf(this)
        feetPart = Math.floor(dCentimeter / 2.54 / 12).toInt()
        println(dCentimeter / 2.54 - feetPart * 12)
        inchesPart = Math.ceil(dCentimeter / 2.54 - feetPart * 12).toInt()
    }
    return String.format("%d' %d''", feetPart, inchesPart)
}

fun String.centimeterToInches(): String? {
    var inchesPart = 0
    if (!TextUtils.isEmpty(this)) {
        val dCentimeter = java.lang.Double.valueOf(this)
        inchesPart = (0.3937 * dCentimeter).roundToInt()
    }
    return String.format("%d", inchesPart)
}

fun CharSequence?.isValidEmail() =
    !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()


fun CharSequence?.isValidEmail1() =
    Pattern.compile(
        "[a-zA-Z0-9]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    ).matcher(this).matches()

fun randomFloatBetween(min: Float, max: Float): Float {
    val r = Random()
    return min + r.nextFloat() * (max - min)
}

fun Window.getSoftInputMode(): Int {
    return attributes.softInputMode
}

fun Window.setSoftInputAdjustPan(mode: Int) {
    this.setSoftInputMode(
        mode
    )
}

fun View.onClick(action: (v: View) -> Unit) {
    this.setOnClickListener {
        action.invoke(it)
    }
}

@SuppressLint("ClickableViewAccessibility")
fun EditText.onTouch(action: (v: View, event: MotionEvent) -> Boolean) {
    this.setOnTouchListener { v, event ->
        action.invoke(v, event)
    }
}

fun EditText.onTextChanged(onTextChanged: (CharSequence?) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged.invoke(s)
        }

        override fun afterTextChanged(editable: Editable?) {

        }
    })
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.disableAutofill() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        getWindow().getDecorView()
            .setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS)
    }
}

fun getFormattedDate(
    originalFormat: SimpleDateFormat,
    targetFormat: SimpleDateFormat,
    inputDate: String
): String {
    try {
        return targetFormat.format(originalFormat.parse(inputDate))
    } catch (e: Exception) {
        return "-NA-"
    }

}

fun getMinsReadFormatDate(
    inputDate: String, timeToRead: String
): String {
    return SimpleDateFormat("dd MMMM yyyy").format(
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        ).parse(inputDate)
    ) + " • " + timeToRead + " mins read"
}

fun textToHtml(text: String): Spanned {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
    } else {
        return Html.fromHtml(text)
    }
}

@SuppressLint("NewApi")
fun getAge(birthDate: String): Int {
    var birth = birthDate.split("-")
    return Period.between(
        LocalDate.of(birth[0].toInt(), birth[1].toInt(), birth[2].toInt()),
        LocalDate.now()
    ).years
}


fun FragmentActivity.CheckForBiometrics(): Boolean {
    var fingerprintManager: FingerprintManager? =
        getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager?
    var keyguardManager: KeyguardManager =
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager


    if (!(fingerprintManager?.isHardwareDetected() ?: false)) {
        "Fingerprint Scanner not detected in Device".toast()
        return false
    }

    if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.USE_FINGERPRINT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        "Permission not granted to use Fingerprint Scanner".toast()
        return false
    }

    if (!keyguardManager.isKeyguardSecure()) {
        "Add Screen Lock to your Phone in Settings".toast()
        return false
    }

    if (!(fingerprintManager?.hasEnrolledFingerprints() ?: false)) {
        "You should add atleast 1 Fingerprint to use this Feature".toast()
        return false
    }
    return true
}

fun GoogleSignInClient.googleLogout() {
    this.signOut()
}


@SuppressLint("HardwareIds")
fun deviceId(): String {
    return Settings.Secure.getString(
        App.getInstance()?.contentResolver,
        Settings.Secure.ANDROID_ID
    )
}


fun getCityName(lat: Double, lng: Double): String {
    try {
        val addresses: List<Address>
        val geocoder = Geocoder(App.getInstance()?.applicationContext, Locale.getDefault())
        addresses = geocoder.getFromLocation(
            lat,
            lng,
            1
        )
        val city: String = addresses[0].locality
        val country: String = addresses[0].countryName
        Timber.e(city)
        Timber.e(country)
        return "$city, $country"
    } catch (e: Exception) {
        return "-NA-"
    }
}

fun getAddress(lat: Double, lng: Double): String {
    try {
        val addresses: List<Address>
        val geocoder = Geocoder(App.getInstance()?.applicationContext, Locale.getDefault())
        addresses = geocoder.getFromLocation(
            lat,
            lng,
            1
        )
        val addressLine: String = addresses[0].featureName
        Timber.e(addressLine)
        return "$addressLine"
    } catch (e: Exception) {
        return "-NA-"
    }
}

fun getPin(lat: Double, lng: Double): String {
    try {
        val addresses: List<Address>
        val geocoder = Geocoder(App.getInstance()?.applicationContext, Locale.getDefault())
        addresses = geocoder.getFromLocation(
            lat,
            lng,
            1
        )
        val addressLine: String = addresses[0].postalCode
        Timber.e(addressLine)
        return "$addressLine"
    } catch (e: Exception) {
        return "-NA-"
    }
}

fun getCity(lat: Double, lng: Double): String {
    try {
        val addresses: List<Address>
        val geocoder = Geocoder(App.getInstance()?.applicationContext, Locale.getDefault())
        addresses = geocoder.getFromLocation(
            lat,
            lng,
            1
        )
        val addressLine: String = addresses[0].locality
        Timber.e(addressLine)
        return "$addressLine"
    } catch (e: Exception) {
        return "-NA-"
    }
}

fun getState(lat: Double, lng: Double): String {
    try {
        val addresses: List<Address>
        val geocoder = Geocoder(App.getInstance()?.applicationContext, Locale.getDefault())
        addresses = geocoder.getFromLocation(
            lat,
            lng,
            1
        )
        val addressLine: String = addresses[0].adminArea
        Timber.e(addressLine)
        return "$addressLine"
    } catch (e: Exception) {
        return "-NA-"
    }
}

fun getCountry(lat: Double, lng: Double): String {
    try {
        val addresses: List<Address>
        val geocoder = Geocoder(App.getInstance()?.applicationContext, Locale.getDefault())
        addresses = geocoder.getFromLocation(
            lat,
            lng,
            1
        )
        val addressLine: String = addresses[0].countryName
        Timber.e(addressLine)
        return "$addressLine"
    } catch (e: Exception) {
        return "-NA-"
    }
}

fun Bitmap?.getImagePath(): String {
    val bytes = ByteArrayOutputStream()
    this?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path: String =
        MediaStore.Images.Media.insertImage(
            App.getInstance()?.applicationContext?.contentResolver,
            this,
            "my_img",
            null
        )
    return Uri.parse(path).getFilePath(App.getInstance()?.applicationContext?.applicationContext!!)
}


@SuppressLint("NewApi")
fun String?.maskPhone(): String {
    val map = mapOf(")" to "", "(" to "", "-" to "", " " to "")
    val sentence = this
    var result = sentence
    map.forEach { t, u -> result = result?.replace(t, u) }
    println(result)
    return result.toString()
}


fun getIndex(spinner: Spinner, myString: String?): Int {
    myString?.let {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(it, ignoreCase = true)) {
                return i
            }
        }
    }
    return 0
}

fun getTextLineCount(textView: TextView, text: String, lineCount: (Int) -> (Unit)) {
    val params: PrecomputedTextCompat.Params = TextViewCompat.getTextMetricsParams(textView)
    val ref: WeakReference<TextView>? = WeakReference(textView)

    GlobalScope.launch(Dispatchers.Default) {
        val text = PrecomputedTextCompat.create(text, params)
        GlobalScope.launch(Dispatchers.Main) {
            ref?.get()?.let { textView ->
                TextViewCompat.setPrecomputedText(textView, text)
                lineCount.invoke(textView.lineCount)
            }
        }
    }
}


@SuppressLint("SimpleDateFormat")
@BindingAdapter("read_time")
fun TextView.setReadTime(date:String){
    var data=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    data.timeZone= TimeZone.getTimeZone("UTC")
    this.text=data.parse(date).getTimeAgo()
}

fun Date.getTimeAgo(): String {
    val calendar = Calendar.getInstance()
    calendar.time = this

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val currentCalendar = Calendar.getInstance()

    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH)
    val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
    val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCalendar.get(Calendar.MINUTE)

    return if (year < currentYear ) {
        val interval = currentYear - year
        if (interval == 1) "$interval year ago" else "$interval years ago"
    } else if (month < currentMonth) {
        val interval = currentMonth - month
        if (interval == 1) "$interval month ago" else "$interval months ago"
    } else  if (day < currentDay) {
        val interval = currentDay - day
        if (interval == 1) "$interval day ago" else "$interval days ago"
    } else if (hour < currentHour) {
        val interval = currentHour - hour
        if (interval == 1) "$interval hour ago" else "$interval hours ago"
    } else if (minute < currentMinute) {
        val interval = currentMinute - minute
        if (interval == 1) "$interval minute ago" else "$interval minutes ago"
    } else {
        "a moment ago"
    }
}
fun TextView.formatToYesterdayOrToday(date: Date?) {
    val calendar = Calendar.getInstance()
    calendar.time = date
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance()
    yesterday.add(Calendar.DATE, -1)
    val timeFormatter: DateFormat = SimpleDateFormat("hh:mm a")
    timeFormatter.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    this.setText(
        if (calendar[Calendar.YEAR] === today[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] === today[Calendar.DAY_OF_YEAR]
        ) {
            "Today " + timeFormatter.format(date)
        } else if (calendar[Calendar.YEAR] === yesterday[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] === yesterday[Calendar.DAY_OF_YEAR]
        ) {
            "Yesterday " + timeFormatter.format(date)
        } else {
            SimpleDateFormat("dd MMMM yyyy 'at' hh:mm a").apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(date)
        }
    )
}

fun TextInputLayout.showError(){
    this.isErrorEnabled=true
}

fun TextInputLayout.setErrorMsg(value: String){
    this.showError()
    this.error=value
}

fun TextInputLayout.hideError(){
    this.isErrorEnabled=false
}

