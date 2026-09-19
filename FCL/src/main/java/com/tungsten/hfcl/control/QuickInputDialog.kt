package com.tungsten.hfcl.control

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tungsten.hfcl.control.data.QuickInputTexts
import com.tungsten.hfcl.databinding.DialogQuickInputBinding
import com.tungsten.hfclauncher.bridge.FCLBridge
import com.tungsten.hfclauncher.keycodes.FCLKeycodes
import com.tungsten.hfclauncher.keycodes.MinecraftKeyBindingMapper
import com.tungsten.hfcllibrary.component.dialog.FCLDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuickInputDialog(private val activity: AppCompatActivity, private val menu: GameMenu) :
    FCLDialog(activity),
    View.OnClickListener {
    private val binding: DialogQuickInputBinding

    init {
        setCancelable(false)
        window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        binding = DialogQuickInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addText.setOnClickListener(this)
        binding.positive.setOnClickListener(this)

        refreshList(menu)
    }

    private fun refreshList(menu: GameMenu) {
        val adapter = InputTextAdapter(
            context,
            QuickInputTexts.getInputTexts()
        ) {
            if (it.isNotEmpty()) {
                if (menu.cursorMode == FCLBridge.CursorEnabled) {
                    it.forEach { s ->
                        menu.input.sendChar(s)
                    }
                } else {
                    val gameOption = menu.gameOption
                    menu.input.sendBoundKeyEvent(
                        gameOption,
                        MinecraftKeyBindingMapper.BINDING_CHAT,
                        FCLKeycodes.KEY_T,
                        true
                    )
                    menu.input.sendBoundKeyEvent(
                        gameOption,
                        MinecraftKeyBindingMapper.BINDING_CHAT,
                        FCLKeycodes.KEY_T,
                        false
                    )
                    activity.lifecycleScope.launch {
                        delay(50)
                        it.forEach { s ->
                            menu.input.sendChar(s)
                        }
                        menu.input.sendKeyEvent(FCLKeycodes.KEY_ENTER, true)
                        menu.input.sendKeyEvent(FCLKeycodes.KEY_ENTER, false)

                    }
                }
            }

            dismiss()
        }
        binding.list.setAdapter(adapter)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.addText -> AddInputTextDialog(
                context
            ) { refreshList(menu) }.show()

            binding.positive -> dismiss()
        }
    }
}
