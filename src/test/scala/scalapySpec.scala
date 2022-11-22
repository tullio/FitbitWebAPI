package org.example
import com.example.pf.Tensor
import org.scalactic.*
import org.scalactic.Tolerance.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.*
import com.github.nscala_time.time.Imports._
import org.example
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
//import ai.kien.python.Python
import scala.collection.mutable.Seq

class scalapySpec extends AnyFunSuite:
  test("scalapy should be introduced") {
      val len = py.Dynamic.global.len(Seq(1, 2, 3).toPythonCopy)
      println(s"len=${len}")
      assert(len.as[Int] == 3)
  }
  test("scalapy should enable numpy") {
      val np = py.module("numpy")
      println(s"np=${np}")
      val a = np.array(Seq(Seq(1, 2), Seq(3, 4)).toArray.toPythonProxy)
      //val a = py.Dynamic.global.list(Seq(Seq(1, 2), Seq(3, 4)).toPythonProxy)
      println(a)
      val b = np.array(Seq(Seq(5, 6), Seq(7, 8)).toArray.toPythonProxy)
      println(b)
      println("[[1*5+2*7, 1*6+2*8], [3*5+4*7, 3*6+4*8]]")
      val c = np.matmul(a, b)
      println(c)
      assert(c.as[Seq[Seq[Int]]] == Seq(Seq(19, 22), Seq(43, 50))) 
      val d = a+b
      assert(d.as[Seq[Seq[Int]]] == Seq(Seq(6, 8), Seq(10, 12))) 
      np.save("d.npy", d)
      val pd = py.module("pandas")
      val npa = np.load("d.npy")
      val e = pd.DataFrame(npa, columns= Seq("column 0", "column 1").toPythonProxy)
      println(s"df=${e}")
      e.to_pickle("f.pickle")
      val pickle = py.module("pickle")
      val f = pickle.load(py.Dynamic.global.open("f.pickle", "rb"))
      println(s"pickle=${f}")
  }
  test("scalapy should enable graphical object saving") {
      val plt = py.module("matplotlib.pyplot")
      
      val np = py.module("numpy")
      val a = np.array(Seq(1, 2).toArray.toPythonProxy)
      val b = np.array(Seq(3, 4).toArray.toPythonProxy)
      plt.title("scalapy test")
      plt.plot(a, b)
      plt.savefig("test.png")
  }
  test("scalapy should realize pytorch tutrials") {
      """
      import torch
      from torch import nn
      from torch.utils.data import DataLoader
      from torchvision import datasets
      from torchvision.transforms import ToTensor
      """
      val torch = py.module("torch")
      val nn = py.module("torch.nn")
      val DataLoader = py.module("torch.utils.data.dataloader").DataLoader
      val datasets = py.module("torchvision").datasets
      val ToTensor = py.module("torchvision.transforms").ToTensor
      """" +
      # Download training data from open datasets.
      training_data = datasets.FashionMNIST(
          root="data",
          train=True,
          download=True,
          transform=ToTensor(),
      )

      # Download test data from open datasets.
      test_data = datasets.FashionMNIST(
          root="data",
          train=False,
          download=True,
          transform=ToTensor(),
      )
      """"
      val trainingData = datasets.FashionMNIST(
          root="data",
          train=true,
          download=true,
          transform=ToTensor(),
      )

      val testData = datasets.FashionMNIST(
          root="data",
          train=false,
          download=true,
          transform=ToTensor(),
      )
      """
      batch_size = 64

      # Create data loaders.
      train_dataloader = DataLoader(training_data, batch_size=batch_size)
      test_dataloader = DataLoader(test_data, batch_size=batch_size)

      for X, y in test_dataloader:
          print(f"Shape of X [N, C, H, W]: {X.shape}")
          print(f"Shape of y: {y.shape} {y.dtype}")
          break
      """
      val batchSize = 64
      val trainDataloader = DataLoader(trainingData, batchSize)
      val testDataloader = DataLoader(testData, batchSize)
      import scala.util.control.Breaks.{break, breakable}
      val item = py.Dynamic.global.iter(testDataloader).next()
      val X = item.bracketAccess(0)
      val y = item.bracketAccess(1)
      println(s"Shape of X [N, C, H, W]: ${X.shape}")
      print(s"Shape of y: ${y.shape} ${y.dtype}")
      """
      # Get cpu or gpu device for training.
      device = "cuda" if torch.cuda.is_available() else "cpu"
      print(f"Using {device} device")

      # Define model
      class NeuralNetwork(nn.Module):
          def __init__(self):
              super(NeuralNetwork, self).__init__()
              self.flatten = nn.Flatten()
              self.linear_relu_stack = nn.Sequential(
                  nn.Linear(28*28, 512),
                  nn.ReLU(),
                  nn.Linear(512, 512),
                  nn.ReLU(),
                  nn.Linear(512, 10)
              )

          def forward(self, x):
              x = self.flatten(x)
              logits = self.linear_relu_stack(x)
              return logits

      model = NeuralNetwork().to(device)
      print(model)
      """
/*
      val device = "cpu"
      class NeuralNetwork extends nn.Module:
          def __init__(self):
              super(NeuralNetwork, self).__init__()
              self.flatten = nn.Flatten()
              self.linear_relu_stack = nn.Sequential(
                  nn.Linear(28*28, 512),
                  nn.ReLU(),
                  nn.Linear(512, 512),
                  nn.ReLU(),
                  nn.Linear(512, 10)
              )

          def forward(self, x):
              x = self.flatten(x)
              logits = self.linear_relu_stack(x)
              return logits
 */
  }
