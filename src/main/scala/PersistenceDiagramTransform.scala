package org.example

import com.github.nscala_time.time.Imports._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import org.tinylog.Logger
import scala.jdk.CollectionConverters._
import better.files._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import ai.djl.ndarray.{NDArrays, NDManager} 
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.index.NDIndex
import ai.djl.ndarray.types.{Shape, DataType}
import scala.annotation.targetName
import ai.djl.Device
import scala.collection.mutable.ListBuffer

class PersistenceDiagramTransform:
    extension (manager: NDManager)
        @targetName("1D")
        def create(x: Seq[Double]): NDArray = 
            manager.create(x.toArray)
        @targetName("2D")
        def create(x: Seq[Seq[Double]]): NDArray =
            manager.create(x.toArray.map(f => f.toArray)) 
//    val manager = NDManager.newBaseManager(Device.cpu)
    val hc = py.module("homcloud.interface")
    val np = py.module("numpy")
    val pv = py.module("homcloud.paraview_interface")
    val go = py.module("plotly.graph_objects")
    val p3d = py.module("homcloud.plotly_3d")
    val plt = py.module("matplotlib.pyplot")
    val manager = NDManager.newBaseManager(Device.cpu)
    def loadDailyData(basename: String, plan: String = "A") =
        val year = Property.startDateTime.year.get
        val month = Property.startDateTime.getMonthOfYear
        val day = Property.startDateTime.getDayOfMonth
        val duration = Property.interval.toDuration.getStandardDays.toInt + 1
        val baseFilename = f"${basename}-${year}%04d-${month}%02d"
        val intervalRange = Range(day, day + duration)
        var pcs = manager.create(Shape(0, 4), DataType.FLOAT64)
        var index = 0
        // Plan A
        for i <- intervalRange do
            val dir_name = f"${year}%04d-${month}%02d-${i}%02d"
            val filename = f"${dir_name}/${baseFilename}-${i}%02d.npy"
            val pc = manager.create(np.load(filename).T.as[Seq[Seq[Double]]]).get("0:7, :")
            Logger.info("loadded. shape={}, {}", pc.getShape, pc.get("0:3, :"))
            val dayPc = plan match
                case "A" =>
                    //val z = np.full(pc.shape[0], index) // z-axis
                    val z = manager.full(Shape(pc.getShape.get(0), 1), index.toFloat, DataType.FLOAT64)
                    Logger.info("z coordinate shape={}, {}", z.getShape, z.get("0:3, :"))
                    val pc4 = pc.concat(z, 1)
                    Logger.info("stacked. shape={}", pc4.getShape)
                    pc4
                case "B" =>
                    //end_index = len(pc[:, 0]) - 1 # len(pc[:, 0])は86400になるはず
                    //endIndex = pc.getShape.get(0) - 1
                    val pc4 = createEmbedding(pc)
                    val dayOffset = (i - day) * 60*60*24
                    val updateRange = NDIndex().addAllDim.addIndices(0)
                    pc4.set(updateRange, pc4.get(updateRange).add(dayOffset))
                    pc4
            pcs = pcs.concat(dayPc , 0)
            index += 1

        //manager.close
        //hc.del()
        //np.del()
        //pv.del()
        //go.del()
        pcs
    def close = 
       manager.close
    def getMinOrMaxPcs(pcs: NDArray, axis: Int) =
        pcs.get(":, 0").stack(pcs.get(f":, ${axis}"), 1)
                              .concat(pcs.get(":, 3").addHDim(1), 1)
    /**
      * extract column 0(time), 1(min value), 3(z-axis)
      * */
    def getMinPcs(pcs: NDArray) = getMinOrMaxPcs(pcs, 1)
    /**
      * extract column 0(time), 2(max value), 3(z-axis)
      * */
    def getMaxPcs(pcs: NDArray) = getMinOrMaxPcs(pcs, 2)
    def createSleepDataset =
        createDataset("sleep")
    def createActiveDataset =
        createDataset("active")
    def createDataset(label: String) =
        // ダウンサンプリングしたmax-min睡眠時心拍データ
        val baseName = f"heartrate-sampled-${label}"
        // 24時間分のロード
        val pcs = loadDailyData(baseName)
        Logger.debug("pcs={}", pcs)
        Logger.debug("pcs.shape={}", pcs.getShape)
        //val sleeppcsMax = sleeppcs.get(":, (0, 1, 3)")
        val pcsMax = pcs.get(":, 0").stack(pcs.get(":, 1"), 1)
                              .concat(pcs.get(":, 3").addHDim(1), 1)
        Logger.debug("pcsMax.shape={}", pcsMax.getShape)
        //val sleeppcsMin = sleeppcs.get(":, (0, 2, 3)")
        val pcsMin = pcs.get(":, 0").stack(pcs.get(":, 2"), 1)
                              .concat(pcs.get(":, 3").addHDim(1), 1)
        var numDataset = 0
        //sleeptmp=sleeppcs_max[(sleeppcs_max[:, 0] > 1900) & (sleeppcs_max[:, 0] < (1900+60*30))]
        val timeRange = pcsMax.get(":, 0").getMinMaxPair
        Logger.debug("time range={}", timeRange)
        for startIndex <- Range(timeRange(0), timeRange(1), 
                               Property.detectWindowByMinutes) do
            indexToTime(startIndex, Property.detectWindowByMinutes)
            //split_pc = sleeppcs_max[start_index: start_index + detect_window]
            val splitPc = pcsMax.get((pcsMax.get(":, 0") >= startIndex) * (pcsMax.get(":, 0") < startIndex + Property.detectWindowByMinutes))
            //np.set_printoptions(threshold=np.inf, suppress=True)
            //logger.debug("slplitted-%d=%s", start_index, split_pc)
            Logger.debug("splitted-pc-{}.shape={}", startIndex, splitPc.getShape)
            val zindex = splitPc.get(":, 2").uniq
            //save_pcpdpng("sleep", split_pc, base_name, start_index)
            //if len(split_pc) > 0: # JSIAM
            Logger.debug(splitPc.size)
            //if splitPc.size() > 90 && zindex.size == Property.interval.toPeriod.getDays + 1 then // FIT
            
            if splitPc.size() > 3 && zindex.size == Property.interval.toPeriod.getDays + 1 then // FIT
                drawPcpd("sleep", splitPc) // ToDo: Visualize
                numDataset += 1
            //return sleeppd1    
        Logger.info("collected dataset = {}", numDataset)
    def indexToTimeString(index: Int): String = 
        val day = (index / (60*60*24)).toInt
        val index2 = index % (60*60*24)
        val minutes = (index2 / 60).toInt
        val second = index2 - minutes * 60
        val hour = (minutes / 60).toInt
        val minute = minutes - hour * 60
        return f"${day} ${hour}:${minute}:${second}"

    def indexToTime(index: Int, window: Int) =
        val start = indexToTimeString(index)
        val `end` = indexToTimeString(index + window)
        Logger.debug("index={}, window={}, {}--{}", index, window, start, `end`)
    def pyList(x: Seq[Double]) =
        py.Dynamic.global.list(x.toPythonProxy)
//    def pyList(x: py.Any) =
//        py.Dynamic.global.list(x)
    def pyDict(`type`: String = "", b: Double = -0.1) =
    //def pyDict(`type`: String = "") =
    //    py.Dynamic.global.dict(`type` = `type`, b = b)
        val dic = py.Dynamic.global.dict(`type` = `type`)
        if b != -0.1 then
            dic.update(b = b)
        dic
    /**
      * extract persistence diagram from the point cloud data in the NDArray
      * and save in the file ${basename}.pdgm
      * */
    def getPd(basename: String, pcs: NDArray) =
        val filtrationFile = f"${basename}.pdgm"
        hc.PDList.from_alpha_filtration(pcs.toNumpy(np), 
                                    save_to=filtrationFile,
                                    save_boundary_map=true)
        val pdlist = hc.PDList(filtrationFile)
        val pd1 = pdlist.dth_diagram(1)
        //print("histogram", pd1.histogram())
        //print("pair", pd1.pairs())
        val births = pd1.births.as[Seq[Double]].toArray
        val deaths = pd1.deaths.as[Seq[Double]].toArray
        Logger.info("births={}", births.toSeq)
        Logger.info("deaths={}", deaths.toSeq)
        val pairs = 
            if births.length > 0 && deaths.length > 0 then
                           Option(manager.create(births.zip(deaths).map(f => Array(f._1, f._2))))

            else
                           None
        //Logger.info("paris={}", pairs.get("-3:, :"))

        pairs
    def getHist(arr: NDArray, bins: Int = 50, filename: String = "figure.png", title: String = "", binarization: Boolean = false) =
        val hist = np.histogram2d(arr.get(":, 0").toNumpy(np), arr.get(":, 1").toNumpy(np), bins=bins, density=false)
        Logger.info("hist={}", hist)
        Logger.info("hist={}", hist.bracketAccess(0))
        val H = hist.bracketAccess(0)
        val xedges = hist.bracketAccess(1)
        val yedges = hist.bracketAccess(2)
        //if binarize then
        //    H = binarize(H, 0.001)
        val extent = py.Dynamic.global.list(Seq(xedges.bracketAccess(0), xedges.bracketAccess(-1), yedges.bracketAccess(0), yedges.bracketAccess(-1)).toPythonProxy)
        plt.imshow(H.T, origin="lower", interpolation="nearest", extent=extent, cmap="tab20")
        plt.title(title)
        plt.savefig(filename, dpi=96)
        //if is_env_notebook():
        //    plt.show()
        H

    def getPdScatter(title: String, pd: NDArray) =
        val x = pd.get(":, 0")
        val y = pd.get(":, 1")
        val xmin = x.min.getDouble()
        val xmax = x.max.getDouble()
        val ymin = y.min.getDouble()
        val ymax = y.max.getDouble()
        val size = pd.getShape.size
        Logger.info("xmin: {}", xmin)
        Logger.info("xmax: {}", xmax)
        Logger.info("count: {}", size)
        /*
        #value=pd.histogram((xmin, xmax), size).values
        #print("value", value)
        #fig, ax = plt.subplots()
        #cax=ax.imshow(value, cmap="Paired", origin="lower")
        #cbar = fig.colorbar(cax)
        #print(cbar)
        #plt.savefig("{}-pd.png".format(base_name))
         */
        val axisrange = (xmin.min(ymin), xmax.max(ymax))
        val scatter = go.Scatter(x=x.toNumpy(np), y=y.toNumpy(np), name=title, mode="markers")
        (scatter, axisrange)
    def drawPd(title: String, basename: String, pd: NDArray) =
        val (scatter, axisrange) = getPdScatter(title, pd)
        val data = py.Dynamic.global.list(Seq(scatter).toPythonProxy)
        Logger.info("Scatter: {}", scatter)
        Logger.info("data: {}",py.Dynamic.global.list(Seq(scatter).toPythonProxy))
        val fig = go.Figure(
            data=data,
            layout=py.Dynamic.global.dict(
                            scene=py.Dynamic.global.dict(
                                xaxis=py.Dynamic.global.dict(visible=true), 
                                yaxis=py.Dynamic.global.dict(visible=true)),
                        height=600, width=600)
        )
        fig.update_xaxes(range=axisrange)
        fig.update_yaxes(range=axisrange)
        fig.update_layout(title=title)
        //if is_env_notebook():
        //    fig.show()
        fig.write_image(f"${basename}.png")

    def drawPcpd(target: String, pcs: NDArray) =
        val baseName = f"heartrate-sampled-${target}"
        val timeRange = pcs.getMinMaxPair
        val startIndex = timeRange(0)
        val day = (startIndex / (60*60*24)).toInt
        val targetDate = Property.startDateTime + day.days
        var titleString = f"${target} pc-${startIndex}"
        val dirName = targetDate.toString("yyyy-MM-dd")
        var filename = f"${dirName}/${target}-pc-${startIndex}"
        np.save(filename, pcs.toNumpy(np))
        filename = f"${dirName}/${baseName}-pc-${startIndex}"
        //draw_pcs(title_string, filename, pcs)
        //from plotly.subplots import make_subplots
        val subplots = py.module("plotly.subplots")
        Logger.debug(pyDict(`type`="scene", b=0.1))
        Logger.debug(pyDict(`type` = "xy"))
        Logger.debug(py.Dynamic.global.list(Seq((pyDict(`type`="scene", b=0.1), pyDict(`type` = "xy"))).toPythonProxy))
        val specs = py.Dynamic.global.list(Seq(py.Dynamic.global.list((pyDict(`type`="scene", b=0.1), pyDict(`type` = "xy")))).toPythonProxy)
        Logger.debug("specs={}",specs)
        val fig = subplots.make_subplots(rows=1, cols=2,
                           ///specs=[[{"type": "scene", "b": 0.1}, {"type": "xy"}]],
                           specs = specs,
                           column_widths= pyList(Seq(50, 40)),
                           //row_heights=[600],
                           //insets=[{"cell":(1,1), "l": 0.5}, {"cell":(1, 2), "w": 0.1}]
                           )
        fig.add_trace(p3d.PointCloud(pcs.toNumpy(np), color="red"), row=1, col=1)

        //fig = go.Figure(
        //        data=[p3d.PointCloud(pcs, color="red")],
        //        layout=dict(scene=dict(xaxis=dict(visible=True), yaxis=dict(visible=True), zaxis=dict(visible=True)))
        //    )
        //fig.update_layout(title=title_string, autosize=False,
        //                      width=500, height=500,
        //                      margin=dict(l=65, r=50, b=65, t=90), row=1, col=1)

        //fig.show()
        //fig.write_image("{}-pc.png".format(base_name))
        val pd1 = getPd(f"${target}pcs", pcs)
        if pd1.isDefined then
            val pd = pd1.get
            filename = f"${dirName}/${baseName}-pd-${startIndex}"
            np.save(filename, pd.toNumpy(np))
            titleString = f"${target} pd-${startIndex}"
            drawPd(titleString, filename, pd)

            titleString = f"${dirName}. sampled-pf-pd-histogram(${startIndex})"
            filename = f"${dirName}/${baseName}-hist-${startIndex}.png"
            val hist = getHist(pd, title = titleString, filename = filename)
            Logger.info("hist:{}",hist)
            Logger.info("hist.size:{}",hist.shape)
            filename = f"${dirName}/${target}-hist-${startIndex}"
            np.save(filename, hist)

            val (scatter, axisrange) = getPdScatter(titleString, pd)
            fig.add_trace(
                scatter, row=1, col=2
            )
            fig.update_layout(width=700, height=400)
            fig.update_xaxes(range=axisrange, row=1, col=2)
            fig.update_yaxes(range=axisrange, row=1, col=2)
            //if is_env_notebook():
            //    fig.show()
            fig.write_image(f"${dirName}/${baseName}-pcpd-${startIndex}.png")

    def getClusteringTarget(target: String, topn_labels: Int = 10, topn_clusters: Int = 2, reverse: Boolean =true) =
        val dirName = Property.startDateTime.toString("yyyy-MM-dd")
        print("target", target)
        Logger.info("dirName={}", dirName)
        val l = File(f"${dirName}").glob(f"${target}-hist-*.npy").toArray
        Logger.info("original files: {}", l.toSeq)
        val simmatrix = manager.zeros(Shape(l.size, l.size), DataType.FLOAT64)
        for i <- Range(0, l.size.toInt - 1) do
            val target1 = manager.create(np.load(l(i).pathAsString).as[Seq[Seq[Double]]])
            for j <- Range(0, l.size.toInt - 1) do
                val target2 = manager.create(np.load(l(j).pathAsString).as[Seq[Seq[Double]]])

                //target1 = binarize(target1, 0.001)
                //target2 = binarize(target2, 0.001)
                val sim = target1.jfip(target2)
                simmatrix.set(NDIndex(i,j), 1.0 - sim)
        Logger.info("simmatrix: {}", simmatrix)
        val cluster = py.module("sklearn.cluster")
        val model = cluster.AgglomerativeClustering(affinity="precomputed", linkage="complete", n_clusters=5).fit(simmatrix.toNumpy(np))

        Logger.info("cluster: {}", model.labels_)
      
        Logger.info(np.unique(model.labels_, return_counts = true))
        val labels_counts = np.unique(model.labels_, return_counts = true)
        val (labels, counts) = (labels_counts.bracketAccess(0).as[Seq[Int]],labels_counts.bracketAccess(1).as[Seq[Int]])
        Logger.info("labels={}", labels)
        Logger.info("counts={}", counts)
        val countedLabels = labels.zip(counts).sortBy((f, g) => g).reverse
        // INFO: List((0,13), (1,4), (4,2), (3,1), (2,1))
        Logger.info(countedLabels)
        val targetClusters = countedLabels.take(3)
        Logger.info("target={}", targetClusters)
        val targetFiles = ListBuffer.empty[File]
        for i <- targetClusters do
            Logger.info(i)
            val file = l(i._1.toInt)
            Logger.info(file)
            targetFiles.append(file)
        Logger.info("final target files={}", targetFiles)
        targetFiles.toSeq
/*
        counted_labels = sorted(zip(labels, counts), key=itemgetter(1), reverse=reverse)
        print("sorted counted labels", counted_labels)
        target_clusters = counted_labels[:topn_clusters]
        print("target_clusters", target_clusters)
        target_files = []
        for i in target_clusters:
            for j, label in enumerate(model.labels_):
                if i[0] == label:
                    target_files.append(l[j])
        return target_files[:topn_labels]
 */
    def createSimirarityMatrix(l: Seq[better.files.File]) =
        val dirName = Property.startDateTime.toString("yyyy-MM-dd")
        //base_name = "heartrate-sampled-{}".format(target)
        //l = glob.glob("{}/sleep-hist-*.npy".format(dir_name))
        //l += glob.glob("{}/test-hist-*.npy".format(dir_name))
        //l += glob.glob("{}/active-hist-*.npy".format(dir_name))
        val simmatrix = manager.zeros(Shape(l.size, l.size), DataType.FLOAT64)
        for i <- Range(0, l.size) do
            val target1 = manager.create(np.load(l(i).path.toString).as[Seq[Seq[Double]]])
            for j <- Range(0, l.size) do
                val target2 = manager.create(np.load(l(j).path.toString).as[Seq[Seq[Double]]])
                val sim = target1.jfip(target2)
                simmatrix.set(NDIndex(i.toLong,j.toLong),sim)
        print(simmatrix)
        plt.imshow(simmatrix.toNumpy(np))
        val title = f"${Property.startDateTime.toString("yyyy-MM-dd")} sampled-pf-pd-histogram-simmatrix"
        val filename = f"${dirName}/simmatrix.png"
        plt.title(title)
        plt.savefig(filename, dpi=96)
    /**
    # 0 -- len(pc) - delay_window
    # 1 -- len(pc) - delay_window + 1
    # 2 -- len(pc) - delay_window + 2
    # 3 -- len(pc) - delay_window + 3
    # 4 -- len(pc) - delay_window + 4
    # という遅延埋込ポイントクラウドを作る
    # サイズがdelay_windowだけ小さくなる
      * */
    def createEmbedding(pcRaw: NDArray) =
        val pc = pcRaw.toType(DataType.FLOAT64, false)
        // logger.debug("input=%s", pc.shape)
        Logger.debug("input={}", pc.getShape)
        Logger.debug("input={}", pc)
        val dataSize = pc.getShape.get(1)
        val blockSize = pc.getShape.get(0) - Property.delayWindow
        val totalBlockSize = blockSize * Property.delayWindow
        val pcs = manager.create(Shape(totalBlockSize, dataSize + 1), DataType.FLOAT64) // append z-axis
        for j <- Range(0, Property.delayWindow) do
            //val pc1 = pc.get(f"${j}: ${-Property.delayWindow + j}, :")
            val pc1 = pc.get(NDIndex().addSliceDim(j, j + blockSize)
                                      .addAllDim).toType(DataType.FLOAT64, false)
            //pc1[:, 0] = pc[0: -delay_window, 0] # 時間は最初のに合わせる
            Logger.debug("block={}", pc1.getShape)
            Logger.debug("block={}", pc1)
            if pc1.getShape.get(0) == pc.getShape.get(0) - Property.delayWindow then
                pc1.set(NDIndex().addAllDim.addIndices(0), 
                            pc.get(NDIndex().addSliceDim(0, blockSize).addIndices(0)))
                Logger.debug("prepared block={}", pc1.getShape)
                Logger.debug("prepared block={}", pc1)
                val z = manager.full(Shape(blockSize), j.toFloat).toType(DataType.FLOAT64, false) // z-axis
                Logger.debug("prepared z={}", z.getShape)
                Logger.debug("prepared z={}", z)
                //pcs.stapcs, np.insert(pc1, 3, z, axis=1) , axis=0)
                val index = j * blockSize
                if pcs.getShape.get(0) > pc1.getShape.get(0) &&
                   pcs.getShape.get(1) == pc1.getShape.get(1) + 1 then
                    pcs.set(NDIndex().addSliceDim(index, index + blockSize)
                                     .addSliceDim(0, dataSize), pc1)
                if pcs.getShape.get(0) > z.getShape.get(0) then
                    pcs.set(NDIndex().addSliceDim(index, index + blockSize)
                                     .addIndices(dataSize + 0), z)
                       
        //logger.debug("output=%s", pcs.shape)
        pcs
object PersistenceDiagramTransform:
    @main
    def createSupervisedData() =
        val pdt = PersistenceDiagramTransform()
        pdt.createSleepDataset
        pdt.createActiveDataset
        var l = scala.collection.mutable.ArrayBuffer.empty[File]
        l ++= pdt.getClusteringTarget("sleep", topn_clusters=2, topn_labels=5)
        l ++= pdt.getClusteringTarget("active", topn_clusters=2, topn_labels=5)
        pdt.createSimirarityMatrix(l.toSeq)