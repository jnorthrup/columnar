package exchg

import cursors.io.RowVec
import cursors.io.left
import cursors.io.right
import vec.macros.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.*
import kotlin.math.min


class PlotThing : JFrame() {
    var caption: String? = null
    var payload: RowVec? = null
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
        contentPane.add(
            object : JComponent() {

                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    with(g)
                    {
                        color = (Color.white)
                        fillRect(0, 0, 1000, 1000)
                        color = Color.black
                        drawLine(0, 500, 1000, 500)
                        drawLine(500, 0, 500, 1000)
                    }
                    caption?.let { title = caption }
                    payload?.let { row ->
                        g.color = Color.red
                        val left = row.left
                        val plotc = min(365, row.size)
                        val plotxy = (0 until plotc).map { x ->

                            (x).toDouble() t2 ((left[x] as Double))
                        }.toVect0r()

                        g.drawPolyline(
                            (plotxy.left α { i -> (i + 500.0 ).toInt()}).toArray().toIntArray(),
                            (plotxy.right α { i -> (500.0 - i*10.0 ).toInt()}).toArray().toIntArray(),
                            plotc)
                    }
                }
            }, BorderLayout.CENTER)


    }

}

fun main(args: Array<String>) {
    PlotThing().isVisible = true
}