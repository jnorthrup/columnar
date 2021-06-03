package exchg

import cursors.Cursor
import cursors.at
import cursors.io.left
import cursors.io.right
import vec.macros.*
import vec.util._a
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.*
import kotlin.math.min


class PlotThing : JFrame() {
    var caption: String? = null
    var payload: Cursor? = null
    var nextAction: Action? = null

    init {
        jMenuBar = JMenuBar()
        val menu = JMenuItem("Next")
        jMenuBar.add(menu)
        menu.addActionListener { nextAction?.actionPerformed(it) }
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        preferredSize = Dimension(1000, 1000)
        size = preferredSize
        isVisible = true

        val colors = _a[Color.red, Color.green, Color.blue, Color.cyan, Color.lightGray, Color.pink, Color.orange]
        contentPane.add(
            object : JComponent() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    with(g) {
                        color = (Color.white)
                        fillRect(0, 0, 1000, 1000)
                        color = Color.black
                        drawLine(0, 500, 1000, 500)
                        drawLine(500, 0, 500, 1000)
                    }
                    caption?.let { title = caption }
                    payload?.let { paload ->
                        (0 until paload.size).forEach {
                            g.color = colors[(it).rem(colors.size)]
                            val rowVect0r = paload at it
                            val left = rowVect0r.left
                            val plotc = min(1000, rowVect0r.size)
                            val plotxy = (0 until plotc).map { x ->
                                x.toDouble() t2 (left[x] as Number).toDouble()
                            }.toVect0r()
                            g.drawPolyline(
                                (plotxy.left α { i -> i.toInt() }).toArray().toIntArray(),
                                (plotxy.right α { i -> (500.0 - i * 10.0).toInt() }).toArray().toIntArray(),
                                plotc
                            )
                        }
                    }
                }
            }, BorderLayout.CENTER
        )
    }
}

fun main(args: Array<String>) {
    PlotThing().isVisible = true
}