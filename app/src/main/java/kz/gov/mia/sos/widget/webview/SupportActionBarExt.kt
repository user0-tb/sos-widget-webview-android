package kz.gov.mia.sos.widget.webview

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.WindowDecorActionBar
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar

inline fun Fragment.setupActionBar(
    toolbar: MaterialToolbar?,
    isBackButtonEnabled: Boolean = true,
    title: String? = null,
    subtitle: String? = null,
    @StringRes titleResId: Int? = null,
    @StringRes subtitleResId: Int? = null,
    crossinline onNavigationClickAction: (view: View) -> Unit
): Boolean {
    if (toolbar == null) return false
    val activity = activity
    if (activity is AppCompatActivity) {
        return activity.setupActionBar(
            toolbar = toolbar,
            isBackButtonEnabled = isBackButtonEnabled,
            title = title,
            subtitle = subtitle,
            titleResId = titleResId,
            subtitleResId = subtitleResId,
            onNavigationClickAction = onNavigationClickAction
        )
    }
    return false
}

inline fun AppCompatActivity.setupActionBar(
    toolbar: MaterialToolbar?,
    isBackButtonEnabled: Boolean = true,
    title: String? = null,
    subtitle: String? = null,
    @StringRes titleResId: Int? = null,
    @StringRes subtitleResId: Int? = null,
    crossinline onNavigationClickAction: (view: View) -> Unit
): Boolean {
    if (toolbar == null) return false

    return if (supportActionBar is WindowDecorActionBar) {
        toolbar.setNavigationOnClickListener {
            onNavigationClickAction.invoke(it)
        }

        false
    } else {
        setSupportActionBar(toolbar)

        if (isBackButtonEnabled) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        when {
            titleResId != null -> {
                supportActionBar?.setTitle(titleResId)
                supportActionBar?.setDisplayShowTitleEnabled(true)
            }
            title != null -> {
                supportActionBar?.title = title
                supportActionBar?.setDisplayShowTitleEnabled(true)
            }
            else -> {
                supportActionBar?.setDisplayShowTitleEnabled(false)
            }
        }

        if (subtitleResId != null) {
            supportActionBar?.setSubtitle(subtitleResId)
        } else if (subtitle != null) {
            supportActionBar?.subtitle = subtitle
        }

        toolbar.setNavigationOnClickListener {
            onNavigationClickAction.invoke(it)
        }

        supportActionBar != null
    }
}
