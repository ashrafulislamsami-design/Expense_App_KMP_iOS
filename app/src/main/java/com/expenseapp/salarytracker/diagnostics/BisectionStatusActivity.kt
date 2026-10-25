package com.expenseapp.salarytracker.diagnostics

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Activity displayed during safe startup bisection to show which stage was successfully completed
 * and allow advancing to the next stage.
 */
class BisectionStatusActivity : Activity() {

    companion object {
        const val EXTRA_STAGE_LEVEL = "extra_stage_level"

        fun start(context: Context, stageLevel: Int) {
            val intent = Intent(context, BisectionStatusActivity::class.java).apply {
                putExtra(EXTRA_STAGE_LEVEL, stageLevel)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stageLevel = intent.getIntExtra(EXTRA_STAGE_LEVEL, 1)
        val stage = StartupStage.entries.firstOrNull { it.level == stageLevel } ?: StartupStage.STAGE_1_APPLICATION_ONLY
        val nextStage = StartupStage.entries.firstOrNull { it.level == stageLevel + 1 }

        val rootScrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#111827"))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(20)
            setPadding(pad, dpToPx(36), pad, dpToPx(36))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Badge
        val badge = TextView(this).apply {
            text = "SAFE STARTUP BISECTION"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#10B981"))
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#064E3B"))
                cornerRadius = dpToPx(6).toFloat()
            }
            background = bg
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(12)
            layoutParams = lp
        }
        container.addView(badge)

        // Title
        val titleView = TextView(this).apply {
            text = "Stage $stageLevel Passed Successfully"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F9FAFB"))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(8)
            layoutParams = lp
        }
        container.addView(titleView)

        // Description
        val descView = TextView(this).apply {
            text = "Completed: ${stage.title}\nNo crashes occurred up to this initialization boundary."
            textSize = 13f
            setTextColor(Color.parseColor("#9CA3AF"))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(24)
            layoutParams = lp
        }
        container.addView(descView)

        // Next Stage Action
        if (nextStage != null) {
            val btnNext = createButton("ADVANCE TO STAGE ${nextStage.level} (${nextStage.title})", "#2563EB") {
                StartupDiagnostics.setBisectionStage(this, nextStage.level)
                restartMainActivity()
            }
            container.addView(btnNext)
        }

        val btnRunFull = createButton("RUN FULL APPLICATION", "#059669") {
            StartupDiagnostics.resetBisection(this)
            restartMainActivity()
        }
        container.addView(btnRunFull)

        val btnReset = createButton("RESTART FROM STAGE 1", "#4B5563") {
            StartupDiagnostics.setBisectionStage(this, 1)
            restartMainActivity()
        }
        container.addView(btnReset)

        rootScrollView.addView(container)
        setContentView(rootScrollView)
    }

    private fun createButton(label: String, bgColorHex: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor(bgColorHex))
                cornerRadius = dpToPx(8).toFloat()
            }
            background = bg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            lp.bottomMargin = dpToPx(12)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun restartMainActivity() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
            finish()
        } catch (_: Throwable) {
            finish()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
