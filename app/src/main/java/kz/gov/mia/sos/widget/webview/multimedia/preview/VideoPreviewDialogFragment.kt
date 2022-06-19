package kz.gov.mia.sos.widget.webview.multimedia.preview

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
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DefaultDrmSessionManagerProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kz.gov.mia.sos.widget.webview.R

internal class VideoPreviewDialogFragment : DialogFragment() {

    companion object {
        private val TAG = VideoPreviewDialogFragment::class.java.simpleName

        fun newInstance(uri: Uri, caption: String?): VideoPreviewDialogFragment {
            val fragment = VideoPreviewDialogFragment()
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

    private var playerView: StyledPlayerView? = null
    private var closeButton: MaterialButton? = null
    private var textView: MaterialTextView? = null

    private var exoPlayer: ExoPlayer? = null

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
        inflater.inflate(R.layout.view_video_preview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerView = view.findViewById(R.id.playerView)
        closeButton = view.findViewById(R.id.closeButton)
        textView = view.findViewById(R.id.textView)

        setupPlayer()

        val uri = Uri.parse(arguments?.getString("uri"))
        val caption = arguments?.getString("caption")

        try {
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.BASE_TYPE_VIDEO)
                .build()

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15 * 1000)
                .setReadTimeoutMs(15 * 1000)

            val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
            drmSessionManagerProvider.setDrmHttpDataSourceFactory(httpDataSourceFactory)

            val mediaSource = DefaultMediaSourceFactory(requireContext())
                .setDrmSessionManagerProvider(drmSessionManagerProvider)
                .createMediaSource(mediaItem)

            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }

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

    override fun onPause() {
        super.onPause()

        releasePlayer()
    }

    override fun onStop() {
        super.onStop()

        releasePlayer()
    }

    override fun onDestroyView() {
        playerView = null

        closeButton?.setOnClickListener(null)
        closeButton = null

        textView = null

        super.onDestroyView()
    }

    private fun setupPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(requireContext())
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            playerView?.player = exoPlayer
            playerView?.setShowPreviousButton(false)
            playerView?.setShowNextButton(false)
            playerView?.setShowRewindButton(false)
            playerView?.setShowRewindButton(false)
            playerView?.setShowBuffering(StyledPlayerView.SHOW_BUFFERING_NEVER)
            playerView?.setShowFastForwardButton(false)
            playerView?.setShowShuffleButton(false)
            playerView?.useController = false
            playerView?.controllerAutoShow = false

            exoPlayer?.playWhenReady = true
            exoPlayer?.pauseAtEndOfMediaItems = true
            exoPlayer?.repeatMode = ExoPlayer.REPEAT_MODE_ALL
            exoPlayer?.setWakeMode(C.WAKE_MODE_NONE)
        }
    }

    private fun releasePlayer() {
        exoPlayer?.clearMediaItems()
        exoPlayer?.release()
        exoPlayer = null
    }

}
