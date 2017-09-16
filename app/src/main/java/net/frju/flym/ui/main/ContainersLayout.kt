package net.frju.flym.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ObjectAnimator.ofFloat
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_main_containers.view.*
import net.fred.feedex.R
import net.frju.flym.utils.onLaidOut

class ContainersLayout : FrameLayout {

    var state: MainNavigator.State? = null
        set(state) {
            field = state
            when (state) {
                MainNavigator.State.SINGLE_COLUMN_MASTER -> singleColumnMaster()
                MainNavigator.State.SINGLE_COLUMN_DETAILS -> singleColumnDetails()
                MainNavigator.State.TWO_COLUMNS_EMPTY -> twoColumnsEmpty()
                MainNavigator.State.TWO_COLUMNS_WITH_DETAILS -> twoColumnsWithDetails()
            }
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.view_main_containers, this, true)
    }

    fun hasTwoColumns(): Boolean {
        return two_columns_container != null
    }

    private fun singleColumnMaster() {
        if (hasTwoColumns()) {
            frame_details.visibility = View.GONE
            toolbar.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            toolbar.layoutParams = toolbar.layoutParams
            frame_master.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            frame_master.layoutParams = frame_master.layoutParams
        } else {
            animateOutFrameDetails()
        }
        frame_master.visibility = View.VISIBLE
    }

    private fun singleColumnDetails() {
        frame_master.visibility = View.GONE
        frame_details.visibility = View.VISIBLE
    }

    private fun twoColumnsEmpty() {
        if (hasTwoColumns()) {
            frame_details.visibility = View.VISIBLE
            val res = frame_master.context.resources
            toolbar.layoutParams.width = res.getDimensionPixelSize(R.dimen.container_max_width)
            toolbar.layoutParams = toolbar.layoutParams
            frame_master.layoutParams.width = res.getDimensionPixelSize(R.dimen.container_max_width)
            frame_master.layoutParams = frame_master.layoutParams
            frame_details.visibility = View.VISIBLE
            (frame_details.layoutParams as FrameLayout.LayoutParams).marginStart = res.getDimensionPixelSize(R.dimen.container_max_width)
            frame_details.layoutParams = frame_details.layoutParams
        } else {
            animateOutFrameDetails()
        }
        frame_master.visibility = View.VISIBLE
    }

    private fun twoColumnsWithDetails() {
        if (hasTwoColumns()) {
            frame_master.visibility = View.VISIBLE
            val res = frame_master.context.resources
            toolbar.layoutParams.width = res.getDimensionPixelSize(R.dimen.container_max_width)
            toolbar.layoutParams = toolbar.layoutParams
            frame_master.layoutParams.width = res.getDimensionPixelSize(R.dimen.container_max_width)
            frame_master.layoutParams = frame_master.layoutParams
            frame_details.visibility = View.VISIBLE
            (frame_details.layoutParams as FrameLayout.LayoutParams).marginStart = res.getDimensionPixelSize(R.dimen.container_max_width)
            frame_details.layoutParams = frame_details.layoutParams
        } else {
            animateInFrameDetails()
        }
    }

    private fun animateInFrameDetails() {
        frame_details.visibility = View.VISIBLE
        frame_details.onLaidOut {
            val alpha = ObjectAnimator.ofFloat(frame_details, View.ALPHA, 0.4f, 1f)
            val translate = ofFloat(frame_details, View.TRANSLATION_Y, frame_details.height * 0.3f, 0f)

            val set = AnimatorSet()
            set.playTogether(alpha, translate)
            set.duration = ANIM_DURATION.toLong()
            set.interpolator = LinearOutSlowInInterpolator()
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    frame_master.visibility = View.GONE
                }
            })
            set.start()
        }
    }

    private fun animateOutFrameDetails() {
        frame_details.onLaidOut {
            if (frame_details.isShown) {
                val alpha = ObjectAnimator.ofFloat(frame_details, View.ALPHA, 1f, 0f)
                val translate = ofFloat(frame_details, View.TRANSLATION_Y, 0f, frame_details.height * 0.3f)

                val set = AnimatorSet()
                set.playTogether(alpha, translate)
                set.duration = ANIM_DURATION.toLong()
                set.interpolator = FastOutLinearInInterpolator()
                set.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        frame_details.alpha = 1f
                        frame_details.translationY = 0f
                        frame_details.visibility = View.GONE
                    }
                })
                set.start()
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState())
        bundle.putString(STATE_CONTAINERS_STATE, state?.name)
        return bundle
    }

    public override fun onRestoreInstanceState(parcelable: Parcelable) {
        var superParcelable = parcelable
        if (parcelable is Bundle) {
            state = MainNavigator.State.valueOf(parcelable.getString(STATE_CONTAINERS_STATE))
            superParcelable = parcelable.getParcelable<Parcelable>(STATE_SUPER)
        }
        super.onRestoreInstanceState(superParcelable)
    }

    companion object {

        val ANIM_DURATION = 250

        private val STATE_SUPER = "state_super"
        private val STATE_CONTAINERS_STATE = "state_containers_state"
    }
}
