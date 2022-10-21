package net.turtton.ytalarm.ui.menu

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.ColorRes
import androidx.annotation.MenuRes
import androidx.core.view.MenuProvider
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import net.turtton.ytalarm.R

class AttachableMenuProvider(
    val parent: Fragment,
    @MenuRes val menuId: Int,
    vararg onItemSelectedListener: Pair<Int, (MenuItem) -> Boolean>,
    @ColorRes val iconColors: Int = R.color.white
) : MenuProvider {
    private val listener = onItemSelectedListener.toMap()

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(menuId, menu)
        menu.forEach {
            val icon = it.icon ?: return@forEach
            icon.mutate()
            val colorRes = parent.resources.getColor(iconColors, null)
            if (Build.VERSION.SDK_INT > 29) {
                icon.colorFilter = BlendModeColorFilter(colorRes, BlendMode.SRC_ATOP)
            } else {
                icon.colorFilter = PorterDuffColorFilter(colorRes, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = listener[menuItem.itemId]?.let {
        it(menuItem)
    } ?: false
}