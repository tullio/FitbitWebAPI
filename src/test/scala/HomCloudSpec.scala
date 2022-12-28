package org.example

import org.scalactic.*
import org.scalactic.Tolerance.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.*
import com.github.nscala_time.time.Imports._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import org.tinylog.Logger

class HomCloudSpec extends AnyFunSuite:
  test("Basic operation should be executed") {
      val hc = py.module("homcloud.interface")
      val np = py.module("numpy")
      val pv = py.module("homcloud.paraview_interface")
      val go = py.module("plotly.graph_objects")
      val pointcloud = np.loadtxt("../pointcloud.txt")
      Logger.info(pointcloud.shape) 
      //pv.show(List(pv.PointCloud.from_array(pointcloud)).toPythonProxy)
      hc.PDList.from_alpha_filtration(pointcloud, save_to="pointcloud.pdgm",
                                      save_boundary_map=true)
      val pdlist = hc.PDList("pointcloud.pdgm")
      val pd1 = pdlist.dth_diagram(1)
      val birth = pd1.births.as[Seq[Double]]
      val deaths = pd1.deaths.as[Seq[Double]]
      val fig = go.Figure(
          data=(go.Scatter(x=pd1.births, y=pd1.deaths, mode="markers"))
      )
      Logger.info(fig)
      fig.write_image("pointcloud.png")
  }
