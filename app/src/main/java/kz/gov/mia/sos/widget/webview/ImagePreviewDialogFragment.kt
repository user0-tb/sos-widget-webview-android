package kz.gov.mia.sos.widget.webview

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import coil.clear
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView

internal class ImagePreviewDialogFragment : DialogFragment() {

    companion object {
        private val TAG = ImagePreviewDialogFragment::class.java.simpleName

        fun newInstance(uri: Uri, caption: String?): ImagePreviewDialogFragment {
            val fragment = ImagePreviewDialogFragment()
            fragment.arguments = bundleOf("uri" to uri.toString(), "caption" to caption)
            return fragment
        }

        fun show(fragmentManager: FragmentManager, uri: Uri, caption: String?) {
            fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(android.R.id.content, newInstance(uri, caption))
                .addToBackStack(null)
                .commit()
        }
    }

    private var imageView: ShapeableImageView? = null
    private var closeButton: MaterialButton? = null
    private var textView: MaterialTextView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.view_image_preview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageView)
        closeButton = view.findViewById(R.id.closeButton)
        textView = view.findViewById(R.id.textView)

        val uri = Uri.parse(arguments?.getString("uri"))
        val caption = arguments?.getString("caption")

        imageView?.load(uri)

        if (caption.isNullOrBlank()) {
            textView?.text = null
            textView?.visibility = View.GONE
        } else {
            textView?.text = caption
            textView?.visibility = View.VISIBLE
        }

        closeButton?.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        imageView?.clear()
        imageView = null

        closeButton?.setOnClickListener(null)
        closeButton = null

        textView = null

        super.onDestroyView()
    }

}
