# SYSTEM IMPORTS
from typing import List
import torch as pt
import torch.nn.functional as F


# PYTHON PROJECT IMPORTS
from layers import NodeDrop, NodeDropConv, get_ard_reg


class Net(pt.nn.Module):
    def __init__(self) -> None:
        super(Net, self).__init__()
        self.conv1: pt.nn.Module = pt.nn.Conv2d(3, 32, 3, 1) #originally 1
        self.conv2: pt.nn.Module = NodeDropConv(32, 64, reg_pam=1.0e-5)
        self.conv3: pt.nn.Module = NodeDropConv(64, 64, batch_norm=False, reg_pam=1.0e-5)

        self.dropout1: pt.nn.Module = pt.nn.Dropout2d(0.25)
        self.dropout2: pt.nn.Module = pt.nn.Dropout2d(0.5)

        self.fc1: pt.nn.Module = NodeDrop(10816, 128, reg_pam=1.0e-5)
        self.fc2: pt.nn.Module = NodeDrop(128, 128, batch_norm=False, reg_pam=1.0e-5)
        self.fc3: pt.nn.Module = pt.nn.Linear(128, 10)

    def forward(self, X: pt.Tensor) -> pt.Tensor:
        X = self.conv1(X)
        X = self.conv2(X)
        X = self.conv3(X)

        X = F.max_pool2d(X, 2)
        X = self.dropout1(X)
        X = pt.flatten(X, 1)

        X = self.fc1(X)
        X = self.fc2(X)

        X = self.dropout2(X)
        X = self.fc3(X)
        return F.log_softmax(X, dim=1)

    @pt.jit.export
    def node_count(self) -> List[int]:
        c2n: int = self.conv2.node_count()
        c3n: int = self.conv3.node_count()

        fc1n: int = self.fc1.node_count()
        fc2n: int = self.fc2.node_count()
        return [c2n, c3n, fc1n, fc2n]

if __name__ == "__main__":
    m = pt.jit.script(Net())

