package org.example
import javax.swing.JFrame
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.internal.chartpart.Chart
/**
class SwingWrapperWrapper[T <: Chart[_,_]] extends SwingWrapper[T]:
    def this[T](chart: T) =
       super.SwingWrapper[T](chart)
       this

    override def displayChart() =

        // Create and set up the window.
        val frame = new JFrame(windowTitle);

        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        try {
          javax.swing.SwingUtilities.invokeAndWait(
              new Runnable() {

                @Override
                public void run() {

                  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                  XChartPanel<T> chartPanel = new XChartPanel<T>(charts.get(0));
                  chartPanels.add(chartPanel);
                  frame.add(chartPanel);

                  // Display the window.
                  frame.pack();
                  if (isCentered) {
                    frame.setLocationRelativeTo(null);
                  }
                  frame.setVisible(true);
                }
              });
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }

        return frame;
      }
  * */
