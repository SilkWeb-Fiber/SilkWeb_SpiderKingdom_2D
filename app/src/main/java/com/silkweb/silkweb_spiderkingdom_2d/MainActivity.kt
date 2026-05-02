package com.silkweb.silkweb_spiderkingdom_2d

import android.graphics.*
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.silkweb.silkweb_spiderkingdom_2d.databinding.ActivityMainBinding
import kotlin.math.*

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var binding: ActivityMainBinding
    private var gameThread: Thread? = null
    private var running = false
    
    private var spiderBitmap: Bitmap? = null
    private var mediaPlayer: android.media.MediaPlayer? = null

    private val pressedKeys = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fullscreen logic
        supportRequestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()

        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameSurface.holder.addCallback(this)
        
        binding.btnStart.setOnClickListener {
            startGame()
            binding.startMenu.isVisible = false
            binding.joystickZone.isVisible = true
            startMusic()
        }
        binding.btnStart.requestFocus()
        
        binding.btnRestart.setOnClickListener {
            initGame()
            startGame()
            binding.gameOverMenu.isVisible = false
            binding.btnRestart.clearFocus()
        }

        binding.gameSurface.setOnGenericMotionListener { v, event ->
            if (event.source and android.view.InputDevice.SOURCE_MOUSE != 0 &&
                event.action == MotionEvent.ACTION_HOVER_MOVE && pressedKeys.isEmpty()) {
                val dx = event.x - v.width / 2f
                val dy = event.y - v.height / 2f
                val dist = hypot(dx, dy)
                if (dist > 10) {
                    setJoystick(dx / dist, dy / dist)
                }
            }
            false
        }

        setupJoystick()
        initGame()
        
        try {
            spiderBitmap = BitmapFactory.decodeResource(resources, R.drawable.spider_app_icon)
        } catch (_: Exception) {}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun startMusic() {
        try {
            mediaPlayer = android.media.MediaPlayer.create(this, R.raw.guqin)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle menu interactions first
        if (binding.startMenu.isVisible || binding.gameOverMenu.isVisible) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                if (binding.startMenu.isVisible) binding.btnStart.performClick()
                else binding.btnRestart.performClick()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        pressedKeys.add(keyCode)
        updateJoystickFromKeys()
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (binding.startMenu.isVisible || binding.gameOverMenu.isVisible) {
            return super.onKeyUp(keyCode, event)
        }
        pressedKeys.remove(keyCode)
        updateJoystickFromKeys()
        return true
    }

    private fun updateJoystickFromKeys() {
        var jx = 0f
        var jy = 0f
        
        if (pressedKeys.contains(KeyEvent.KEYCODE_W) || pressedKeys.contains(KeyEvent.KEYCODE_DPAD_UP)) jy -= 1f
        if (pressedKeys.contains(KeyEvent.KEYCODE_S) || pressedKeys.contains(KeyEvent.KEYCODE_DPAD_DOWN)) jy += 1f
        if (pressedKeys.contains(KeyEvent.KEYCODE_A) || pressedKeys.contains(KeyEvent.KEYCODE_DPAD_LEFT)) jx -= 1f
        if (pressedKeys.contains(KeyEvent.KEYCODE_D) || pressedKeys.contains(KeyEvent.KEYCODE_DPAD_RIGHT)) jx += 1f

        // Normalize
        val mag = hypot(jx, jy)
        if (mag > 0) {
            jx /= mag
            jy /= mag
        }
        setJoystick(jx, jy)
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupJoystick() {
        binding.joystickZone.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                v.performClick()
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val centerX = v.width / 2f
                    val centerY = v.height / 2f
                    var dx = event.x - centerX
                    var dy = event.y - centerY
                    val dist = hypot(dx, dy)
                    val maxDist = 70f * resources.displayMetrics.density
                    
                    if (dist > maxDist) {
                        dx = (dx / dist) * maxDist
                        dy = (dy / dist) * maxDist
                    }
                    
                    binding.joystickKnob.translationX = dx
                    binding.joystickKnob.translationY = dy
                    
                    setJoystick(dx / maxDist, dy / maxDist)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.joystickKnob.animate().translationX(0f).translationY(0f).setDuration(100).start()
                    setJoystick(0f, 0f)
                }
            }
            true
        }
    }

    external fun initGame()
    external fun startGame()
    external fun updateGame(dt: Float)
    external fun setJoystick(x: Float, y: Float)
    external fun getPlayerState(): FloatArray
    external fun getWebs(): FloatArray
    external fun getNPCs(): FloatArray

    companion object {
        private const val MAP_RADIUS = 8500f
        private const val MAP_CENTER = 8500f
        init {
            System.loadLibrary("silkweb_spiderkingdom_2d")
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        gameThread = Thread {
            var lastTime = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now
                
                updateGame(dt)
                
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    drawGame(canvas)
                    holder.unlockCanvasAndPost(canvas)
                }
                
                val frameTimeMs = (System.nanoTime() - now) / 1_000_000
                val sleepTime = 4 - frameTimeMs
                if (sleepTime > 0) {
                    try { Thread.sleep(sleepTime) } catch (_: Exception) {}
                }
            }
        }
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            gameThread?.join()
        } catch (_: InterruptedException) {}
    }

    private fun drawGame(canvas: Canvas) {
        val playerState = getPlayerState()
        if (playerState.size < 6) return
        
        val px = playerState[0]
        val py = playerState[1]
        val pr = playerState[2]
        val pa = playerState[3]
        val score = playerState[4].toInt()
        val active = playerState[5] > 0.5f

        if (!active && !binding.startMenu.isVisible && !binding.gameOverMenu.isVisible) {
            runOnUiThread {
                binding.gameOverMenu.isVisible = true
                binding.finalScoreText.text = getString(R.string.final_size, pr.toInt())
                binding.scoreDisplay.text = getString(R.string.score_format, score)
                binding.btnRestart.requestFocus()
            }
        }

        canvas.drawColor("#A9A9A9".toColorInt())
        
        val ox = canvas.width / 2f - px
        val oy = canvas.height / 2f - py
        
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = "#888888".toColorInt()
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE

        // Octagon Boundary and Background Web
        paint.color = "#A52A2A".toColorInt()
        paint.strokeWidth = 3f
        paint.clearShadowLayer()
        
        for (r in 50..MAP_RADIUS.toInt() step 50) {
            if (r == MAP_RADIUS.toInt()) {
                paint.strokeWidth = 5f
                paint.setShadowLayer(20f, 0f, 0f, "#A52A2A".toColorInt())
            } else {
                paint.strokeWidth = 3f
                paint.clearShadowLayer()
            }
            
            val pathRing = Path()
            for (i in 0..8) {
                val angle = (PI / 4.0) * i
                val vx = MAP_CENTER + ox + r * cos(angle.toFloat())
                val vy = MAP_CENTER + oy + r * sin(angle.toFloat())
                if (i == 0) pathRing.moveTo(vx, vy) else pathRing.lineTo(vx, vy)
            }
            canvas.drawPath(pathRing, paint)
        }
        
        paint.strokeWidth = 3f
        paint.clearShadowLayer()
        for (i in 0 until 8) {
            val angle = (PI / 4.0) * i
            canvas.drawLine(
                MAP_CENTER + ox,
                MAP_CENTER + oy,
                MAP_CENTER + ox + MAP_RADIUS * cos(angle.toFloat()),
                MAP_CENTER + oy + MAP_RADIUS * sin(angle.toFloat()),
                paint
            )
        }
        
        // UI TEXT ON CANVAS
        val uiPaint = Paint()
        uiPaint.isAntiAlias = true
        
        // Auto-resize text based on screen width
        val responsiveTextSize = canvas.width * 0.035f 
        uiPaint.textSize = responsiveTextSize
        uiPaint.setShadowLayer(responsiveTextSize / 6f, 0f, 0f, Color.BLACK)
        
        val marginX = canvas.width * 0.04f
        val marginY = canvas.height * 0.1f
        
        // SIZE (Top Left)
        uiPaint.color = "#00ffcc".toColorInt()
        uiPaint.textAlign = Paint.Align.LEFT
        uiPaint.isFakeBoldText = true
        canvas.drawText(getString(R.string.size_format, pr.toInt()), marginX, marginY, uiPaint)
        
        // Score (Top Right)
        uiPaint.color = Color.WHITE
        uiPaint.textAlign = Paint.Align.RIGHT
        uiPaint.isFakeBoldText = false
        canvas.drawText(getString(R.string.score_format, score), canvas.width - marginX, marginY, uiPaint)

        // Webs
        val webs = getWebs()
        paint.color = Color.rgb(255, 253, 231)
        paint.strokeWidth = 1.2f
        for (i in webs.indices step 3) {
            val wx = webs[i]
            val wy = webs[i+1]
            val wr = webs[i+2]
            if (abs(wx - px) < canvas.width && abs(wy - py) < canvas.height) {
                drawDetailedWeb(canvas, wx + ox, wy + oy, wr, paint)
            }
        }

        // NPCs
        val npcs = getNPCs()
        for (i in npcs.indices step 4) {
            val nx = npcs[i]
            val ny = npcs[i+1]
            val nr = npcs[i+2]
            val na = npcs[i+3]
            if (abs(nx - px) < canvas.width && abs(ny - py) < canvas.height) {
                drawSpider(canvas, nx + ox, ny + oy, nr, na)
            }
        }

        // Player
        drawSpider(canvas, canvas.width / 2f, canvas.height / 2f, pr, pa)
    }

    private fun drawDetailedWeb(canvas: Canvas, x: Float, y: Float, radius: Float, paint: Paint) {
        val sides = 8
        val rings = 8
        paint.isAntiAlias = true
        for (i in 0 until sides) {
            val a = (i * 2 * PI.toFloat()) / sides
            canvas.drawLine(x, y, x + radius * cos(a), y + radius * sin(a), paint)
        }
        for (r in 1..rings) {
            val currentR = radius * (r.toFloat() / rings)
            val path = Path()
            for (i in 0..8) {
                val a = (PI.toFloat() / 4.0f) * i
                val vx = x + currentR * cos(a)
                val vy = y + currentR * sin(a)
                if (i == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawSpider(canvas: Canvas, sx: Float, sy: Float, r: Float, a: Float) {
        if (spiderBitmap == null) {
            val p = Paint()
            p.isAntiAlias = true
            p.color = Color.BLACK
            p.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, r, p)
            val hx = sx + r * 1.1f * cos(a)
            val hy = sy + r * 1.1f * sin(a)
            canvas.drawCircle(hx, hy, r * 0.7f, p)
            p.strokeWidth = r * 0.15f
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            for (i in 0 until 4) {
                val baseAngle = a + PI.toFloat() / 2f + (i - 1.5f) * 0.4f
                val x1 = sx + r * 0.8f * cos(baseAngle)
                val y1 = sy + r * 0.8f * sin(baseAngle)
                val x2 = sx + r * 2.0f * cos(baseAngle + 0.3f)
                val y2 = sy + r * 2.0f * sin(baseAngle + 0.3f)
                canvas.drawLine(x1, y1, x2, y2, p)
                val baseAngleL = a - PI.toFloat() / 2f - (i - 1.5f) * 0.4f
                val x1L = sx + r * 0.8f * cos(baseAngleL)
                val y1L = sy + r * 0.8f * sin(baseAngleL)
                val x2L = sx + r * 2.0f * cos(baseAngleL - 0.3f)
                val y2L = sy + r * 2.0f * sin(baseAngleL - 0.3f)
                canvas.drawLine(x1L, y1L, x2L, y2L, p)
            }
        } else {
            canvas.withTranslation(sx, sy) {
                rotate(Math.toDegrees(a.toDouble() + PI / 2).toFloat())
                val rect = RectF(-r * 1.4f, -r * 1.4f, r * 1.4f, r * 1.4f)
                drawBitmap(spiderBitmap!!, null, rect, null)
            }
        }
    }
}
