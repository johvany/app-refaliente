package com.di.refaliente

import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.di.refaliente.databinding.ActivityHomeMenuBinding
import com.di.refaliente.home_menu_ui.FavoritesFragment
import com.di.refaliente.home_menu_ui.PublicationsFragment
import com.di.refaliente.home_menu_ui.PurchasesFragment
import com.di.refaliente.home_menu_ui.ShoppingCartFragment
import com.google.android.material.navigation.NavigationView

class HomeMenuActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityHomeMenuBinding

    private lateinit var publicationsFragment: PublicationsFragment
    private lateinit var shoppingCartFragment: ShoppingCartFragment
    private lateinit var favoritesFragment: FavoritesFragment
    private lateinit var purchasesFragment: PurchasesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupMenus()

        if (savedInstanceState == null) {
            initFragments()
            binding.navView.setCheckedItem(R.id.nav_publications)
            loadFragment(R.id.nav_publications)
        } else {
            initFragmentsByBundle(savedInstanceState)
            loadFragmentByBundle(savedInstanceState)
        }

        // ... Change this (get user profile img and setup here) ...
        /* Glide.with(this)
            .load("https://uproxx.com/wp-content/uploads/2018/05/john-wick-31.jpg")
            .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
            .into(NavHeaderHomeMenuBinding.bind(binding.navView.getHeaderView(0)).userImg) */
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

        supportFragmentManager.getFragment(bundle, "shopping_cart_fragment").let { fragment ->
            shoppingCartFragment = if (fragment == null) { ShoppingCartFragment() } else { fragment as ShoppingCartFragment }
        }

        supportFragmentManager.getFragment(bundle, "favorites_fragment").let { fragment ->
            favoritesFragment = if (fragment == null) { FavoritesFragment() } else { fragment as FavoritesFragment }
        }

        supportFragmentManager.getFragment(bundle, "purchases_fragment").let { fragment ->
            purchasesFragment = if (fragment == null) { PurchasesFragment() } else { fragment as PurchasesFragment }
        }
    }

    private fun initFragments() {
        publicationsFragment = PublicationsFragment()
        shoppingCartFragment = ShoppingCartFragment()
        favoritesFragment = FavoritesFragment()
        purchasesFragment = PurchasesFragment()
    }

    private fun setupMenus() {
        // Useful to create a popup menu with a button which has a 3 dots icon.
        // This works along with the "onCreateOptionsMenu" function.
        setSupportActionBar(binding.appBarHomeMenu.toolbar)

        // Handle popup menu (toolbar) item click event.
        binding.appBarHomeMenu.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    // ... Make somthing here if you need ...
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

        // Add listener to handle item clicks in the menu.
        binding.navView.setNavigationItemSelectedListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)

        if (publicationsFragment.isAdded) { supportFragmentManager.putFragment(outState, "publications_fragment", publicationsFragment) }
        if (shoppingCartFragment.isAdded) { supportFragmentManager.putFragment(outState, "shopping_cart_fragment", shoppingCartFragment) }
        if (favoritesFragment.isAdded) { supportFragmentManager.putFragment(outState, "favorites_fragment", favoritesFragment) }
        if (purchasesFragment.isAdded) { supportFragmentManager.putFragment(outState, "purchases_fragment", purchasesFragment) }

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
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        loadFragment(item.itemId)
        binding.drawerLayout.closeDrawer(GravityCompat.START, false)
        return true
    }

    private fun loadFragment(menuItemId: Int) {
        when (menuItemId) {
            R.id.nav_publications -> {
                title = "Publicaciones"

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_home_menu, publicationsFragment)
                    .commit()
            }
            R.id.nav_shopping_cart -> {
                title = "Carrito de compras"

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_home_menu, shoppingCartFragment)
                    .commit()
            }
            R.id.nav_favorites -> {
                title = "Favoritos"

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_home_menu, favoritesFragment)
                    .commit()
            }
            R.id.nav_purchases -> {
                title = "Mis compras"

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_home_menu, purchasesFragment)
                    .commit()
            }
        }
    }
}