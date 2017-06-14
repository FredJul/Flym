package net.frju.flym.ui.main

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.MenuRes
import android.support.annotation.StringRes
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.view_main_toolbar.view.*
import net.fred.feedex.R

class CustomAppBar : AppBarLayout {

    private var state: MainNavigator.State? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.view_main_toolbar, this, true)

        if (toolbar_general != null) {
            toolbar_general.setNavigationIcon(R.drawable.ic_menu_24dp)
        } else {
            toolbar_specific.setNavigationIcon(R.drawable.ic_menu_24dp)
        }
    }

    fun hasGeneralToolbar(): Boolean {
        return toolbar_general != null
    }

    fun setOnNavigationClickListener(onNavigationClickListener: View.OnClickListener) {
        if (toolbar_general != null) {
            toolbar_general.setNavigationOnClickListener(onNavigationClickListener)
        } else {
            toolbar_specific.setNavigationOnClickListener(onNavigationClickListener)
        }
    }

    fun setOnMenuItemClickListener(listener: Toolbar.OnMenuItemClickListener) {
        toolbar_general?.setOnMenuItemClickListener(listener)
        toolbar_specific?.setOnMenuItemClickListener(listener)
    }

    fun setTitle(title: String) {
        toolbar_specific.title = title
    }

    fun setTitle(@StringRes title: Int) {
        toolbar_specific.setTitle(title)
    }

    fun clearMenu() {
        toolbar_specific.menu.clear()
        if (toolbar_general != null) {
            toolbar_general.menu.clear()
        }
    }

    fun setMenuRes(@MenuRes menuGeneral: Int, @MenuRes menuSpecific: Int, @MenuRes menuMerged: Int) {
        toolbar_specific.menu.clear()
        if (toolbar_general != null) {
            toolbar_general.menu.clear()
            toolbar_general.inflateMenu(menuGeneral)
            toolbar_specific.inflateMenu(menuSpecific)
        } else {
            toolbar_specific.inflateMenu(menuMerged)
        }
    }

    fun setState(state: MainNavigator.State) {
        this.state = state
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState())
        bundle.putString(STATE_TITLE, toolbar_specific.title as String?)
        bundle.putString(STATE_TOOLBAR_STATE, state?.name)
        return bundle
    }

    public override fun onRestoreInstanceState(parcelable: Parcelable) {
        var superParcelable = parcelable
        if (parcelable is Bundle) {
            toolbar_specific.title = parcelable.getString(STATE_TITLE)
            setState(MainNavigator.State.valueOf(parcelable.getString(STATE_TOOLBAR_STATE)))
            superParcelable = parcelable.getParcelable<Parcelable>(STATE_SUPER)
        }
        super.onRestoreInstanceState(superParcelable)
    }

    companion object {

        private val STATE_SUPER = "state_super"
        private val STATE_TITLE = "state_title"
        private val STATE_TOOLBAR_STATE = "state_toolbar_state"
    }
}
