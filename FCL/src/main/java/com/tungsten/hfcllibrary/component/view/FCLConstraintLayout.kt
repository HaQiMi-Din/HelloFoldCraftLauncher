package com.tungsten.hfcllibrary.component.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import com.tungsten.hfclcore.fakefx.beans.property.IntegerProperty
import com.tungsten.hfclcore.fakefx.beans.property.IntegerPropertyBase
import com.tungsten.hfcl.R
import com.tungsten.hfcllibrary.component.theme.ThemeEngine

class FCLConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    var autoTint = false
    private val theme: IntegerProperty = object : IntegerPropertyBase() {
        override fun invalidated() {
            get()
            if (autoTint) {
                setBackgroundTintList(
                    ColorStateList(
                        arrayOf<IntArray?>(intArrayOf()),
                        intArrayOf(ThemeEngine.getInstance().getTheme().getLtColor())
                    )
                )
            }
        }

        override fun getBean(): Any {
            return this
        }

        override fun getName(): String {
            return "theme"
        }
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.FCLConstraintLayout) {
            autoTint = getBoolean(R.styleable.FCLConstraintLayout_auto_tint, false)
        }
        theme.bind(ThemeEngine.getInstance().getTheme().colorProperty())
    }

}
