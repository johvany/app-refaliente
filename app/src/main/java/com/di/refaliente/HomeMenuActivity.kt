package com.di.refaliente

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.databinding.ActivityHomeMenuBinding
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.databinding.NavHeaderHomeMenuBinding
import com.di.refaliente.home_menu_ui.*
import com.di.refaliente.local_database.Database
import com.di.refaliente.local_database.UsersDetailsTable
import com.di.refaliente.local_database.UsersTable
import com.di.refaliente.shared.SessionHelper
import com.di.refaliente.shared.Utilities
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

class HomeMenuActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityHomeMenuBinding

    private lateinit var publicationsFragment: PublicationsFragment
    private lateinit var rematesFragment: RematesFragment
    private lateinit var shoppingCartFragment: ShoppingCartFragment
    private lateinit var addressesFragment: AddressesFragment
    private lateinit var purchasesFragment: PurchasesFragment
    private lateinit var favoritesFragment: FavoritesFragment
    private lateinit var aboutFragment: AboutFragment
    private var userImageProfile: String? = null
    private var isMenuItemSelected = true
    private lateinit var launcher: ActivityResultLauncher<Intent>

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ProfileActivity.USER_DATA_UPDATED) {
                result.data?.let { data ->
                    if (data.extras!!.getBoolean("user_data_updated")) {
                        loadUserDataInTheSideMenu()
                    }

                    if (data.extras!!.getBoolean("user_image_updated")) {
                        Database(this).let { db -> UsersDetailsTable.find(db, 1)?.let { userDetail -> userImageProfile = userDetail.profileImage } }
                        loadUserDataInTheSideMenu()
                    }
                }
            }
        }

        // Here we initialize Facebook SDK to use login with facebook feature.
        FacebookSdk.setApplicationId(resources.getString(R.string.facebook_app_id))
        FacebookSdk.setClientToken(resources.getString(R.string.facebook_client_token))
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

        binding = ActivityHomeMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUserData()

        // Initialize main request queue to be used by any component (activities or fragments).
        Utilities.queue = Volley.newRequestQueue(this)

        setupMenus()
        initFragmentsAndLoadOne(savedInstanceState)
        loadUserDataInTheSideMenu()
        checkIfShouldLoadPurchases()

        // Handle on back pressed event
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.navView.checkedItem.let { menuItem ->
                    if (menuItem == null || menuItem.itemId != R.id.nav_publications) {
                        binding.navView.setCheckedItem(R.id.nav_publications)
                        loadFragment(R.id.nav_publications)
                    } else {
                        confirmExit()
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun confirmExit() {
        MaterialAlertDialogBuilder(this).create().also { dialog ->
            dialog.setCancelable(true)

            dialog.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                viewBinding.icon.setImageResource(R.drawable.question_dialog)
                viewBinding.title.visibility = View.VISIBLE
                viewBinding.title.text = "¿Desea salir de la aplicación?"
                viewBinding.message.visibility = View.GONE
                viewBinding.positiveButton.text = "Sí"
                viewBinding.negativeButton.text = "No"

                viewBinding.positiveButton.setOnClickListener {
                    dialog.dismiss()
                    finish()
                }

                viewBinding.negativeButton.setOnClickListener { dialog.dismiss() }
            }.root)
        }.show()
    }

    // This function is used to know if a new purchase was made and the user want to see it.
    // The function verify a boolean parameter passed to this activity, if the value is true
    // then the purchases fragmente is loaded and also reset the parameter, to prevent always
    // load this fragmente, for example, on screen rotation.
    private fun checkIfShouldLoadPurchases() {
        intent.extras?.getBoolean("should_load_purchases")?.let { loadPurchases ->
            if (loadPurchases) {
                intent = Intent(this, HomeMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                binding.navView.setCheckedItem(R.id.nav_purchases)
                loadFragment(R.id.nav_purchases)
            }
        }
    }

    // Initialize user data (if there is a logged user) and user image profile to load it later.
    private fun initializeUserData() {
        Database(this).let { db ->
            SessionHelper.user = UsersTable.find(db, 1)
            UsersDetailsTable.find(db, 1)?.let { userDetail -> userImageProfile = userDetail.profileImage }
        }
    }

    // Check if we can restore all fragments and load the last loaded, otherwise load default.
    private fun initFragmentsAndLoadOne(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            initFragments()
            binding.navView.setCheckedItem(R.id.nav_publications)
            loadFragment(R.id.nav_publications)
        } else {
            initFragmentsByBundle(savedInstanceState)
            loadFragmentByBundle(savedInstanceState)
        }
    }

    // Load user profile image if there is a logged user and also load the user name (in the side menu).
    @SuppressLint("SetTextI18n")
    private fun loadUserDataInTheSideMenu() {
        NavHeaderHomeMenuBinding.bind(binding.navView.getHeaderView(0)).let { viewBinding ->
            if (SessionHelper.userLogged()) {
                viewBinding.userName.text = "Bienvenido " + SessionHelper.user!!.name

                if (userImageProfile != null && userImageProfile != "null") {
                    Glide.with(this)
                        .load(resources.getString(R.string.api_url_storage) + SessionHelper.user!!.sub.toString() + "/profile/" + userImageProfile)
                        .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                        .into(viewBinding.userImg)
                } else {
                    viewBinding.userImg.setImageResource(R.drawable.user_no_img)
                }
            } else {
                viewBinding.userName.setTextColor(Color.parseColor("#CCCCCC"))
                viewBinding.userName.text = "Cuenta de invitado"
                viewBinding.userImg.setImageResource(R.drawable.user_no_img)
            }
        }
    }

    private fun loadFragmentByBundle(bundle: Bundle) {
        bundle.getString("selected_menu_item")?.let {
            it.toIntOrNull()?.let { menuItemId -> loadFragment(menuItemId) }
        }
    }

    private fun initFragmentsByBundle(bundle: Bundle) {
        supportFragmentManager.getFragment(bundle, "publications_fragment").let { fragment ->
            publicationsFragment = if (fragment == null) { PublicationsFragment() } else { fragment as PublicationsFragment }
        }

        supportFragmentManager.getFragment(bundle, "remates_fragment").let { fragment ->
            rematesFragment = if (fragment == null) { RematesFragment() } else { fragment as RematesFragment }
        }

        supportFragmentManager.getFragment(bundle, "shopping_cart_fragment").let { fragment ->
            shoppingCartFragment = if (fragment == null) { ShoppingCartFragment() } else { fragment as ShoppingCartFragment }
        }

        supportFragmentManager.getFragment(bundle, "addresses_fragment").let { fragment ->
            addressesFragment = if (fragment == null) { AddressesFragment() } else { fragment as AddressesFragment }
        }

        supportFragmentManager.getFragment(bundle, "purchases_fragment").let { fragment ->
            purchasesFragment = if (fragment == null) { PurchasesFragment() } else { fragment as PurchasesFragment }
        }

        supportFragmentManager.getFragment(bundle, "favorites_fragment").let { fragment ->
            favoritesFragment = if (fragment == null) { FavoritesFragment() } else { fragment as FavoritesFragment }
        }

        supportFragmentManager.getFragment(bundle, "about_fragment").let { fragment ->
            aboutFragment = if (fragment == null) { AboutFragment() } else { fragment as AboutFragment }
        }
    }

    private fun initFragments() {
        publicationsFragment = PublicationsFragment()
        rematesFragment = RematesFragment()
        shoppingCartFragment = ShoppingCartFragment()
        addressesFragment = AddressesFragment()
        purchasesFragment = PurchasesFragment()
        favoritesFragment = FavoritesFragment()
        aboutFragment = AboutFragment()
    }

    private fun setupMenus() {
        // Useful to create a popup menu with a button which has a 3 dots icon.
        // This works along with the "onCreateOptionsMenu" function.
        setSupportActionBar(binding.appBarHomeMenu.toolbar)

        // Handle popup menu (toolbar) item click event.
        binding.appBarHomeMenu.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_login -> {
                    login()
                }
                R.id.action_logout -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Cerrar sesión")
                        .setMessage("Se cerrará la sesión actual...")
                        .setCancelable(true)
                        .setNegativeButton("CANCELAR", null)
                        .setPositiveButton("CONTINUAR") { _, _ -> logout() }
                        .show()
                }
            }

            true
        }

        // Setup a button in the top-left position to show the menu.
        ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.appBarHomeMenu.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ).let { toggle ->
            binding.drawerLayout.addDrawerListener(toggle)
            toggle.syncState()
        }

        // Add listener to handle item clicks in the side menu.
        binding.navView.setNavigationItemSelectedListener(this)
    }

    private fun login() {
        SessionHelper.login(this)
    }

    private fun logout() {
        SessionHelper.logout(this)
    }

    // This make a backup of the fragments and the checked item in the side menu. Useful to
    // restore it when the activity is recreated, for example, on screen rotation.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (publicationsFragment.isAdded) {
            supportFragmentManager.putFragment(outState, "publications_fragment", publicationsFragment)
        }

        if (rematesFragment.isAdded) {
            supportFragmentManager.putFragment(outState, "remates_fragment", rematesFragment)
        }

        if (shoppingCartFragment.isAdded) {
            supportFragmentManager.putFragment(outState, "shopping_cart_fragment", shoppingCartFragment)
        }

        if (addressesFragment.isAdded) {
            supportFragmentManager.putFragment(outState, "addresses_fragment", addressesFragment)
        }

        if (purchasesFragment.isAdded) {
            supportFragmentManager.putFragment(outState, "purchases_fragment", purchasesFragment)
        }

        if (favoritesFragment.isAdded) {
            supportFragmentManager.putFragment(outState, "favorites_fragment", favoritesFragment)
        }

        if (aboutFragment.isAdded) {
            supportFragmentManager.putFragment(outState, "about_fragment", aboutFragment)
        }

        binding.navView.checkedItem.let { menuItem ->
            if (menuItem == null) {
                outState.putString("selected_menu_item", "")
            } else {
                outState.putString("selected_menu_item", menuItem.itemId.toString())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu. This adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home_menu, menu)

        // Check if there is a logged user and hide or show the login and logout menu items.
        if (SessionHelper.userLogged()) {
            menu.findItem(R.id.action_login).isVisible = false
            menu.findItem(R.id.action_logout).isVisible = true
        } else {
            menu.findItem(R.id.action_login).isVisible = true
            menu.findItem(R.id.action_logout).isVisible = false
        }

        return true
    }

    fun loadShoppingCart() {
        binding.navView.menu.findItem(R.id.nav_shopping_cart).let { menuItem ->
            binding.navView.setCheckedItem(menuItem)
            onNavigationItemSelected(menuItem)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        loadFragment(item.itemId)
        binding.drawerLayout.closeDrawer(GravityCompat.START, false)
        return isMenuItemSelected
    }

    private fun loadFragment(menuItemId: Int) {
        isMenuItemSelected = true

        when (menuItemId) {

            // Publicaciones
            R.id.nav_publications -> {
                title = "Publicaciones"

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_home_menu, publicationsFragment)
                    .commit()
            }

            // Remates
            R.id.nav_remates -> {
                title = "Remates"

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_home_menu, rematesFragment)
                    .commit()
            }

            // Perfil
            R.id.nav_profile -> {
                if (SessionHelper.userLogged()) {
                    launcher.launch(Intent(this, ProfileActivity::class.java))
                } else {
                    SessionHelper.showRequiredSessionMessage(this)
                }
                isMenuItemSelected = false
            }

            // Carrito
            R.id.nav_shopping_cart -> {
                if (SessionHelper.userLogged()) {
                    title = "Carrito de compras"

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment_content_home_menu, shoppingCartFragment)
                        .commit()
                } else {
                    isMenuItemSelected = false
                    SessionHelper.showRequiredSessionMessage(this)
                }
            }

            // Direcciones
            R.id.nav_addresses -> {
                if (SessionHelper.userLogged()) {
                    title = "Direcciones"

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment_content_home_menu, addressesFragment)
                        .commit()
                } else {
                    isMenuItemSelected = false
                    SessionHelper.showRequiredSessionMessage(this)
                }
            }

            // Compras
            R.id.nav_purchases -> {
                if (SessionHelper.userLogged()) {
                    title = "Mis compras"

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment_content_home_menu, purchasesFragment)
                        .commit()
                } else {
                    isMenuItemSelected = false
                    SessionHelper.showRequiredSessionMessage(this)
                }
            }

            // Favoritos
            R.id.nav_favorites -> {
                if (SessionHelper.userLogged()) {
                    title = "Favoritos"

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment_content_home_menu, favoritesFragment)
                        .commit()
                } else {
                    isMenuItemSelected = false
                    SessionHelper.showRequiredSessionMessage(this)
                }
            }

            // Informacion
            R.id.nav_about -> {
                title = "Información"

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_home_menu, aboutFragment)
                    .commit()
            }
        }
    }
}