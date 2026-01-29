package com.example.tripapp2.ui.common

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.example.tripapp2.ui.common.base.BaseFragment
import com.example.tripapp2.ui.common.base.BaseViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Context

/**
 * Bazowy fragment z obsługą klawiatury
 *
 * Automatycznie:
 * - Chowa bottom navigation gdy klawiatura jest widoczna
 * - Dodaje padding do scroll view (zawsze, nie tylko gdy klawiatura)
 * - Scrolluje do pola z fokusem
 *
 * Użycie:
 * ```
 * class MyFragment : KeyboardAwareFragment<MyViewModel>(R.layout.fragment_my) {
 *     override val viewModel: MyViewModel by viewModels()
 *
 *     override fun initKeyboardViews(view: View) {
 *         keyboardScrollView = view.findViewById(R.id.scrollView)
 *         keyboardBottomNav = (activity as? DashboardActivity)?.dashboardBottomNav
 *     }
 * }
 * ```
 */
abstract class KeyboardAwareFragment<VM : BaseViewModel>(
    @LayoutRes layoutId: Int
) : BaseFragment<VM>(layoutId) {

    /**
     * ScrollView, który ma być obsługiwany przez mechanizm klawiatury.
     */
    protected var keyboardScrollView: NestedScrollView? = null

    /**
     * BottomNavigationView, który ma być chowany przy wysuniętej klawiaturze.
     */
    protected var keyboardBottomNav: BottomNavigationView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Najpierw pozwól dziecku ustawić widoki
        initKeyboardViews(view)

        // Potem wywołaj super (setupUI itp.)
        super.onViewCreated(view, savedInstanceState)

        // Na końcu skonfiguruj klawiaturę - musi być po setupUI
        view.post {
            setupKeyboardHandling()
            setupInitialBottomPadding()
        }
    }
    override fun onPause() {
        super.onPause()
        // Schowaj klawiaturę gdy opuszczamy fragment
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        // Upewnij się że bottom nav jest widoczny po powrocie do fragmentu
        keyboardBottomNav?.post {
            keyboardBottomNav?.translationY = 0f
        }
    }

    /**
     * Zaimplementuj tę metodę aby ustawić keyboardScrollView i keyboardBottomNav
     */
    protected open fun initKeyboardViews(view: View) {
        // Opcjonalne - dzieci mogą override'ować
    }

    /**
     * Ustawia początkowy padding na dole scrollView aby przycisk nie był zakryty przez bottom nav
     */
    private fun setupInitialBottomPadding() {
        keyboardBottomNav?.let { bottomNav ->
            keyboardScrollView?.let { scrollView ->
                bottomNav.post {
                    val navHeight = bottomNav.height
                    scrollView.setPadding(
                        scrollView.paddingLeft,
                        scrollView.paddingTop,
                        scrollView.paddingRight,
                        navHeight + 16 // Dodajemy 16dp marginesu
                    )
                    scrollView.clipToPadding = false
                }
            }
        }
    }

    /**
     * Konfiguruje obsługę klawiatury
     */
    private fun setupKeyboardHandling() {
        val rootView = view ?: return

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // Animacja chowania bottom nav
            keyboardBottomNav?.animate()
                ?.translationY(if (imeVisible) keyboardBottomNav!!.height.toFloat() else 0f)
                ?.setDuration(200)
                ?.start()

            // Padding dla scrollView
            if (imeVisible) {
                // Gdy klawiatura jest widoczna - padding = wysokość klawiatury
                keyboardScrollView?.setPadding(
                    keyboardScrollView!!.paddingLeft,
                    keyboardScrollView!!.paddingTop,
                    keyboardScrollView!!.paddingRight,
                    imeBottom
                )
            } else {
                // Gdy klawiatura schowana - padding = wysokość bottom nav + margines
                keyboardBottomNav?.let { bottomNav ->
                    keyboardScrollView?.setPadding(
                        keyboardScrollView!!.paddingLeft,
                        keyboardScrollView!!.paddingTop,
                        keyboardScrollView!!.paddingRight,
                        bottomNav.height + 16
                    )
                }
            }

            // Scroll do pola z fokusem
            if (imeVisible) {
                keyboardScrollView?.findFocus()?.let { focused ->
                    keyboardScrollView?.post {
                        keyboardScrollView?.smoothScrollTo(0, focused.bottom)
                    }
                }
            }

            insets
        }

        ViewCompat.requestApplyInsets(rootView)
    }

    override fun onDestroyView() {
        // Cleanup - usuń listener
        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, null)
        }
        super.onDestroyView()
    }
}