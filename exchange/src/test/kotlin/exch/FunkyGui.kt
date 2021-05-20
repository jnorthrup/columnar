package exchg

import cursors.io.RowVec
import cursors.io.left
import cursors.io.right
import vec.macros.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import kotlin.math.min

class PlotThing : JFrame() {
    var caption: String? = null
    var payload: RowVec? = null
    var nextAction: Action? = null

    init {
        jMenuBar = JMenuBar()
        jMenuBar.add(object : AbstractButton() {init {
            text = "Next"

            addActionListener(object : ActionListener {
                override fun actionPerformed(p0: ActionEvent?) = nextAction?.actionPerformed(p0) ?: Unit
            })
        }
        })
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        preferredSize = Dimension(1000, 1000)

        size=preferredSize
        isVisible=true
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
                        val plotc = min(300, row.size)
                        val plotxy = (0 until plotc).map { x ->
                            (+500 + x).toInt() t2 (500 - left[x] as Double).toInt()
                        }.toVect0r()
                        g.drawPolyline(plotxy.left.toArray().toIntArray(),
                            plotxy.right.toArray().toIntArray(),
                            plotc)
                    }
                }
            }, BorderLayout.CENTER)


    }

}

fun main(args: Array<String>) {
    PlotThing().isVisible=true
}