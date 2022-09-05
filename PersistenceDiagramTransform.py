import homcloud.interface as hc  # HomCloudのインターフェス
import homcloud.plotly_3d as p3d  # 3次元可視化用

import plotly.graph_objects as go  # これも3次元可視化用
import numpy as np
import matplotlib.pyplot as plt
import os
import toml
import datetime
from logging import getLogger
from logging import Formatter, StreamHandler, FileHandler, DEBUG, INFO
import pickle

BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE = range(8)
RESET_SEQ = "\033[0m"
COLOR_SEQ = "\033[3{:d}m"
BOLD_SEQ = "\033[1m"

GREEN_COLOR_SEQ = COLOR_SEQ.format(GREEN)
print(GREEN_COLOR_SEQ)
logger = getLogger(__name__)
format_string = f'{GREEN_COLOR_SEQ}%(asctime)s %(name)s:%(lineno)s %(funcName)s [%(levelname)s]:{RESET_SEQ} %(message)s'
print(format_string)
if not logger.hasHandlers():
    handler = StreamHandler()
    handler.setFormatter(Formatter(format_string))
    logger.addHandler(handler)
logger.setLevel(DEBUG)

prop_file = "src/main/resources/ActionDetection.toml"
with open(prop_file) as f:
    props = toml.loads(f.read())
start_date = datetime.datetime.strptime(props["StartDate"], "%Y-%m-%d")
print("start_date", start_date)
end_date = datetime.datetime.strptime(props["EndDate"], "%Y-%m-%d")
print("end_date", end_date)
interval = end_date - start_date
print("interval", interval)
down_sampling_window_by_seconds = props["DownSamplingWindowBySeconds"]
detect_window_by_minutes = props["DetectWindowByMinutes"]
detect_window = detect_window_by_minutes * 60
detect_window_by_index = int(detect_window / down_sampling_window_by_seconds)

delay_window = props["DelayWindow"]

np.set_printoptions(threshold=10, suppress=True)

def is_env_notebook():
    """Determine wheather is the environment Jupyter Notebook"""
    #logger.debug("globals=%d",'get_ipython' not in  globals())
    #if 'get_ipython' not in globals():
    #    # Python shell
    #    return False
    try:
      env_name = get_ipython().__class__.__name__
      #logger.debug("env_name=%s", env_name)
      if env_name == 'TerminalInteractiveShell':
          # IPython shell
          return False
      # Jupyter Notebook
      return True
    except NameError:
      return False

def FIP(f, g):
    fip = np.dot(f.ravel(), g.ravel())
    return fip
def JFIP(f, g):
    jfip = 2*FIP(f, g)/(FIP(f, f)+FIP(g, g))
    return jfip

# 0 -- len(pc) - delay_window
# 1 -- len(pc) - delay_window + 1
# 2 -- len(pc) - delay_window + 2
# 3 -- len(pc) - delay_window + 3
# 4 -- len(pc) - delay_window + 4
# という遅延埋込ポイントクラウドを作る
# サイズがdelay_windowだけ小さくなる
def create_embedding(pc):
    #logger.debug("input=%s", pc.shape)
    pcs = np.empty((0, 4))
    for j in range(delay_window):
      pc1 = pc[j: -delay_window + j, :].copy()
      pc1[:, 0] = pc[0: -delay_window, 0] # 時間は最初のに合わせる
      z = np.full(pc1.shape[0], j) # z-axis
      pcs = np.append(pcs, np.insert(pc1, 3, z, axis=1) , axis=0)
    #logger.debug("output=%s", pcs.shape)
    return pcs

# ダウンサンプリング後のデータが入る想定
# つまり時間データは15秒毎の単位で入っていて，欠損値は補間されている
# 24時間*intervalのロード
def load_daily_data(basename):
    logger.debug(basename)
    year = start_date.year
    month = start_date.month
    day = start_date.day
    global interval
    duration = interval.days + 1
    #print("duration(should be 5)", duration)
    base_filename = "{}-{:04d}-{:02d}".format(basename, year, month)
    interval_range = range(day, day + duration)
    pcs = np.empty((0, 4))
    index = 0
    # Plan A
    """
    for i in interval_range:
        filename = "{}-{:02d}.npy".format(base_filename, i)
        print(filename)
        if os.path.exists(filename):
            pc=np.load(filename).T
            print("loadded. shape=", pc.shape)
            z = np.full(pc.shape[0], index) # z-axis
            #print(z)
            pcs = np.append(pcs, np.insert(pc, 3, z, axis=1) , axis=0)
        index += 1
    """
    # Plan B 時間遅れ埋込
    for i in interval_range:
        dir_name = "{:04d}-{:02d}-{:02d}".format(year, month, i)
        filename = "{}/{}-{:02d}.npy".format(dir_name, base_filename, i)
        if os.path.exists(filename):
            pc=np.load(filename).T
            #pc=pc[0:11, :] # debug
            logger.debug("loadded. shape=%s", pc.shape)
            logger.debug("loadded. data=%s", pc)
            #np.set_printoptions(threshold=np.inf)
            logger.debug("loadded. time data=%s", pc[:, 0])
            #np.set_printoptions(threshold=1000)
            #start_index = pc[:, 0].min() # 1日のうちで活動が記録されている最初の秒
            start_index = 0
            #end_index = pc[:, 0].max()
            end_index = len(pc[:, 0]) - 1 # len(pc[:, 0])は86400になるはず
            logger.debug("creating time-delay embedding point clouds")
            pc1 = create_embedding(pc[0: end_index + 1].copy())
            day_offset = (i - day) * 60*60*24
            pc1[:, 0] = pc1[:, 0] + day_offset
            pcs = np.append(pcs, pc1 , axis=0)
    return pcs

def get_pd(base_name, pcs):
    #logger.debug(base_name)
    filtration_file = "{}.pdgm".format(base_name)
    #print("pcs=", pcs)
    #print("pcs.shape=", pcs.shape)
    #print("join=", "".join([str(_) for _ in pcs.tolist()]))
    #print("join=", "".join(map(str, pcs.tolist())))
    #logger.debug("pcs={}", "".join(map(str, pcs.tolist())))
    #logger.debug("pcs={}", "".join([str(_) for _ in  pcs.tolist()]))
    #logger.debug("shape={}", pcs.shape)
    hc.PDList.from_alpha_filtration(pcs, 
                                    save_to=filtration_file,
                                    save_boundary_map=True)
    pdlist = hc.PDList(filtration_file)
    pd1 = pdlist.dth_diagram(1)
    #print("histogram", pd1.histogram())
    #print("pair", pd1.pairs())
    return pd1

def get_pdarray(pd):
    #print("pd", list(filter(lambda x: x.death_time()<1.2, pd.pairs())))
    pdarray = np.array(list(map(lambda x: np.array([x.birth_time(), x.death_time()]), pd.pairs())))
    #print("pdarray.shape", pdarray.shape)
    #print("pdarray", pdarray)
    return pdarray

def draw_pcs(title, base_name, pcs):
    logger.debug(title)
    fig = go.Figure(
        data=[p3d.PointCloud(pcs, color="red")],
        layout=dict(scene=dict(xaxis=dict(visible=True), yaxis=dict(visible=True), zaxis=dict(visible=True)))
    )
    fig.update_layout(title=title, autosize=False,
                      width=500, height=500,
                      margin=dict(l=65, r=50, b=65, t=90))

    if is_env_notebook():
        fig.show()
    fig.write_image("{}-pc.png".format(base_name))

def draw_pd(title, base_name, pd):
    #pd.histogram().plot()
    #value=pd.histogram((50, 90), 40).values
    values = pd.birth_death_times()
    xmin = values[0].min()
    xmax = values[0].max()
    ymin = values[1].min()
    ymax = values[1].max()
    size = values[0].size
    #print("xmin", xmin)
    #print("xmax", xmax)
    #print("count", size)
    #value=pd.histogram((xmin, xmax), size).values
    #print("value", value)
    #fig, ax = plt.subplots()
    #cax=ax.imshow(value, cmap="Paired", origin="lower")
    #cbar = fig.colorbar(cax)
    #print(cbar)
    #plt.savefig("{}-pd.png".format(base_name))
    axisrange=(min(xmin, ymin), max(xmax, ymax))
    fig = go.Figure(
        data=[go.Scatter(x=values[0], y=values[1], name=title, mode="markers")],
        layout=dict(scene=dict(xaxis=dict(visible=True), 
                               yaxis=dict(visible=True)),
                    height=600, width=600)
    )
    fig.update_xaxes(range=axisrange)
    fig.update_yaxes(range=axisrange)
    fig.write_image("{}-pd.png".format(base_name))

#target = "sleep"
def draw_pcpd(target, pcs):
    # create_sleep_dataset
    base_name = "heartrate-sampled-{}".format(target)
    #pcs = load_daily_data(base_name)
    #pcs_max=pcs[:, (0, 1, 3)]
    time_range = (int(pcs[:, 0].min()), int(pcs[:, 0].max()))
    start_index = time_range[0]
    #index_to_time(start_index, detect_window)
    #split_pc = pcs_max[(pcs_max[:, 0] >= start_index) & (pcs_max[:, 0] < start_index + detect_window)]
    # save_pcpdpng("sleep", split_pc, base_name, start_index)
    #target = "sleep"
    #  save_pcpdpng
    title_string = "{} data-{}".format(target, start_index)
    filename = "{}-pc-{}".format(target, start_index)
    np.save(filename, pcs)
    filename = "{}-{}".format(base_name, start_index)
    #draw_pcs(title_string, filename, split_pc)
    from plotly.subplots import make_subplots
    fig = make_subplots(rows=1, cols=2,
                       specs=[[{"type": "scene", "b": 0.1}, {"type": "xy"}]],
                       column_widths=[50, 40],
                       #row_heights=[600],
                       #insets=[{"cell":(1,1), "l": 0.5}, {"cell":(1, 2), "w": 0.1}]
                       )
    fig.add_trace(p3d.PointCloud(pcs, color="red"), row=1, col=1)

    #fig = go.Figure(
    #        data=[p3d.PointCloud(pcs, color="red")],
    #        layout=dict(scene=dict(xaxis=dict(visible=True), yaxis=dict(visible=True), zaxis=dict(visible=True)))
    #    )
    #fig.update_layout(title=title_string, autosize=False,
    #                      width=500, height=500,
    #                      margin=dict(l=65, r=50, b=65, t=90), row=1, col=1)

    #fig.show()
    #fig.write_image("{}-pc.png".format(base_name))

    pd1 = get_pd("{}pcs".format(target), pcs)
    pdarray = get_pdarray(pd1)
    filename = "{}-pdarray-{}".format(target, start_index)
    np.save(filename, pdarray)
    #draw_pd(title_string, filename, pd1)

    values = pd1.birth_death_times()
    xmin = values[0].min()
    xmax = values[0].max()
    ymin = values[1].min()
    ymax = values[1].max()
    size = values[0].size
    axisrange=(min(xmin, ymin), max(xmax, ymax))
    #fig = go.Figure(
    #        data=[go.Scatter(x=values[0], y=values[1], name=title, mode="markers")],
    #        layout=dict(scene=dict(xaxis=dict(visible=True),
    #                               yaxis=dict(visible=True)),
    #                    height=600, width=600)
    #)
    fig.add_trace(
        go.Scatter(x=values[0], y=values[1], name=title_string, mode="markers"), row=1, col=2
    )
    fig.update_layout(width=700, height=400)
    fig.update_xaxes(range=axisrange, row=1, col=2)
    fig.update_yaxes(range=axisrange, row=1, col=2)
    if is_env_notebook():
        fig.show()
    fig.write_image("{}-pcpd-{}.png".format(base_name, start_index))


def save_pcpdpng(target, split_pc, base_name, start_index):
    if split_pc.shape[0] > 200:
        title_string = "{} data-{}".format(target, start_index)
        filename = "{}-pc-{}".format(target, start_index) 
        logger.debug("save as %s", filename)
        np.save(filename, split_pc)
        filename = "{}-{}".format(base_name, start_index)
        #draw_pcs(title_string, filename, split_pc)
        pd1 = get_pd("{}pcs".format(target), split_pc)
        #logger.debug("pairs=%s", sleeppd1.pairs())
        pdarray = get_pdarray(pd1)
        logger.info("pdarray-%d.shape=%s", start_index, pdarray.shape)
        filename = "{}-pdarray-{}".format(target, start_index)
        np.save(filename, pdarray)
        logger.debug("save %s to %s", pdarray.shape, filename)
        #draw_pd(title_string, filename, pd1)
    else:
        print("Too few samples. skip")

def create_sleep_dataset():
    # ダウンサンプリングしたmax-min睡眠時心拍データ
    base_name = "heartrate-sampled-sleep"
    # 24時間分のロード
    sleeppcs = load_daily_data(base_name)
    logger.debug("sleeppcs=%s", sleeppcs)
    logger.debug("sleeppcs.shape=%s", sleeppcs.shape)
    sleeppcs_max=sleeppcs[:, (0, 1, 3)]
    sleeppcs_min=sleeppcs[:, (0, 2, 3)]

    num_sleep_dataset = 0
    #sleeptmp=sleeppcs_max[(sleeppcs_max[:, 0] > 1900) & (sleeppcs_max[:, 0] < (1900+60*30))]
    time_range = (int(sleeppcs_max[:, 0].min()), int(sleeppcs_max[:, 0].max()))
    logger.debug("time range=%s", time_range)
    #draw_pcs("Sleep data", base_name, sleeppcs_max)
    for start_index in range(time_range[0], time_range[1], detect_window):
        index_to_time(start_index, detect_window)
        #split_pc = sleeppcs_max[start_index: start_index + detect_window]
        split_pc = sleeppcs_max[(sleeppcs_max[:, 0] >= start_index) & (sleeppcs_max[:, 0] < start_index + detect_window)]
        #np.set_printoptions(threshold=np.inf, suppress=True)
        #logger.debug("slplitted-%d=%s", start_index, split_pc)
        logger.debug("splitted-pc-%d.shape=%s", start_index, split_pc.shape)
        #save_pcpdpng("sleep", split_pc, base_name, start_index)
        if len(split_pc) > 0:
            draw_pcpd("sleep", split_pc)
            num_sleep_dataset += 1
        #return sleeppd1    
    logger.info("collected sleep dataset = %d", num_sleep_dataset)

def create_active_dataset():
    base_name = "heartrate-sampled-active"
    activepcs = load_daily_data(base_name)

    activepcs_max=activepcs[:, (0, 1, 3)]
    activepcs_min=activepcs[:, (0, 2, 3)]

    num_active_dataset = 0
    logger.debug("activepcs_max=%s",activepcs_max)
    #activetmp=activepcs_max[(activepcs_max[:, 0] > 72195) & (activepcs_max[:, 0] < (72195+120*2))]
    #activetmp=activepcs_max[(activepcs_max[:, 0] > 72195) & (activepcs_max[:, 0] < (72195+60*30))]
    time_range = (int(activepcs_max[:, 0].min()), int(activepcs_max[:, 0].max()))

    #draw_pcs("Active data", base_name, activetmp)
    draw_pcs("Active data", base_name, activepcs_max)
    for start_index in range(time_range[0], time_range[1], detect_window):
        index_to_time(start_index, detect_window)
        #split_pc = sleeppcs_max[start_index: start_index + detect_window]
        split_pc = activepcs_max[(activepcs_max[:, 0] >= start_index) & (activepcs_max[:, 0] < start_index + detect_window)]
        #np.set_printoptions(threshold=np.inf, suppress=True)
        logger.debug("slplitted-%d=%s", start_index, split_pc)
        logger.debug("slplitted.shape=%s", split_pc.shape)
        #save_pcpdpng("active", split_pc, base_name, start_index)
        if len(split_pc) > 0:
            draw_pcpd("active", split_pc)
            num_active_dataset += 1
    logger.info("collected active dataset = %d", num_active_dataset)
    #activetmppd1 = get_pd("activetmp", activetmp)

    #draw_pd("Active data", base_name, activetmppd1)
    #pdarray = get_pdarray(activetmppd1)
    #logger.debug("save??=%s", pdarray.__class__.__name__)
    #np.save("active-pdarray", pdarray)
    #print("save active-pdarray", pdarray.shape)
    #return activetmppd1

def create_test_data():
    base_name = "heartrate-sampled-pf"
    testpcs = load_daily_data(base_name)

    testpcs_max=testpcs[:, (0, 1, 3)]
    testpcs_min=testpcs[:, (0, 2, 3)]

    num_active_dataset = 0
    logger.debug("testpcs_max=%s",testpcs_max[40000:47000, :])
    #activetmp=activepcs_max[(activepcs_max[:, 0] > 72195) & (activepcs_max[:, 0] < (72195+120*2))]
    #activetmp=activepcs_max[(activepcs_max[:, 0] > 72195) & (activepcs_max[:, 0] < (72195+60*30))]
    target_window = "2022-6-6 13:00"
    target_window = datetime.datetime.strptime(target_window, "%Y-%m-%d %H:%M")
    logger.debug("target_window = %s", target_window)
    index = time_to_index(target_window)
    #time_range = (int(activepcs_max[:, 0].min()), int(activepcs_max[:, 0].max()))
    logger.debug("target window by index=(%d, %d)", index, index + detect_window)
    #target_pc = testpcs_max[index - 1000: index + detect_window_by_index + 1000, :]
    target_pc = testpcs_max[(testpcs_max[:, 0] > index) & (testpcs_max[:, 0] < (index+detect_window))]
    logger.debug("target_pc=%s", target_pc.shape)
    #draw_pcs("Active data", base_name, activetmp)
    draw_pcs("Test data", base_name, target_pc)
    if len(target_pc) > 0:
            draw_pcpd("test", target_pc)


def index_to_time_string(index):
    day = index // (60*60*24)
    index = index % (60*60*24)
    minutes = int(index / 60)
    second = index - minutes * 60
    hour = int(minutes / 60)
    minute = minutes - hour * 60
    return "{} {}:{}:{}".format(day, hour, minute, second)

def index_to_time(index, window):
    start = index_to_time_string(index)
    end = index_to_time_string(index + window)
    logger.debug("index=%d, window=%d, %s--%s", index, window, start, end)

def time_to_index(datetime):
    day = datetime.day - start_date.day 
    hour = datetime.hour
    minute = datetime.minute
    second = datetime.second
    index = day * (60*60*24) + hour * 60*60 + minute * 60 + second
    return index

def create_supervised_data():
    create_sleep_dataset()
    create_active_dataset()

if __name__ == '__main__':
    create_supervised_data()
    create_test_data()
    logger.close()
