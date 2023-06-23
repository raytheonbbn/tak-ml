# SYSTEM IMPORTS
from typing import Optional, Tuple
import numpy as np
import torch as pt
import torch.nn.functional as F
import torch_sparse as ptsp


# PYTHON PROJECT IMPORTS
from utils import strip_optional


""" NOTE: torch.jit is c++, which means it is *strongly* typed (vs python, matlab, etc. which is
    weakly typed). This means that some of the weakly typed nonsense won't work here. Specifically,
    this code is very sensitive to what the type of the actual variable is. For instance,
    the NodeDrop::blayer attribute is *either* a pt.nn.Identity object or a pt.nn.BatchNorm1d object.
    This matters because pt.nn.Identity does *not* have attribute .bias and .data, which causes
    compiler errors.

    Another thing to worry about is that UNLESS you use python's 3.6+ typing scheme, torch.jit
    will infer the type of your variables *FOR YOU* and will assume they are torch.Tensor types
    which they may not be. So, be explicit regarding what types variables are.

    You can use the @pt.jit.export tag to make any method compiled, otherwise the method is ignored
    when an object is converted into a torchscript object (the exception is the ::forward(...) method
    which will ALWAYS be compiled unless a @pt.jit.ignore or @pt.jit.unused tag is added.

    what changed:
        -> I added typing for variables
        -> you cannot use python api calls in compiled methods, so if we want to do things like
           self.wlayer.__class__.__name__ or isinstance(...), we need to instead store that info
           in attributes in the object's constructor (__init__ method), which is always python
           bound (i.e. not compiled).
        -> If we want to access attributes that don't always exist (like self.blayer.weight and
           self.blayer.bias), I decided to, since pt.nn.Identity is a no-op, replace self.blayer
           from being type torch.nn.Module into Optional[torch.nn.Module]. If originally, we would've
           used pt.nn.Identity, then self.blayer will be None, and we will ignore it.

           This is useful because now, I can check for None instead of an isinstance(...) call,
           AND, if self.blayer is not None, I guarantee self.blayer.bias and self.blayer.weight
           exist, which means it is compile-time safe. Woo!
"""


class ClampedReLU(pt.nn.Module):
    def __init__(self, beta: float = 10.0) -> None:
        super(ClampedReLU, self).__init__()
        self.softplus: pt.nn.Module = pt.nn.Softplus(beta=beta)

    def forward(self, X: pt.Tensor) -> pt.Tensor:
        return F.relu(1-self.softplus(1-X))


class NodeDrop(pt.nn.Module):
    def __init__(self, in_features: int, out_features: int,
                 bias: bool = True, reg_pam: float = 0.0,
                 batch_norm: bool = True) -> None:
        super(NodeDrop, self).__init__()
        self.in_features: int = in_features
        self.out_features: int = out_features
        self.reg_lambda: float = reg_pam
        self.batch_norm: bool = batch_norm

        self.wlayer: pt.nn.Module = pt.nn.Linear(in_features, out_features)
        self.alayer: pt.nn.Module = None
        self.blayer: Optional[pt.nn.Module] = None

        if self.batch_norm:
            self.blayer_bnorm = pt.nn.BatchNorm1d(out_features)
            self.alayer = pt.nn.ReLU()
        else:
            self.alayer = ClampedReLU()

        if bias:
            self.bias = pt.nn.Parameter(pt.FloatTensor(out_features))
        else:
            self.register_parameter("bias", None)
        self.register_parameter("weight", self.wlayer.weight)

    def forward(self, X: pt.Tensor) -> pt.Tensor:
        X = self.wlayer(X)
        if self.blayer is not None:
            X = self.blayer(X)

        return self.alayer(X)

    @pt.jit.export
    def reset_parameters(self) -> None:
        self.wlayer.weight.data.normal_(std=0.01)
        if self.bias is not None:
            self.bias.data.uniform_(0.0, 0.0)

    @pt.jit.export
    def node_count(self) -> int:
        device = self.wlayer.weight.device
        B: pt.Tensor = strip_optional(self.wlayer.bias, device)
        W: pt.Tensor = strip_optional(self.wlayer.weight, device)

        W_sum: pt.Tensor = pt.zeros((), device=self.wlayer.weight.device)
        if self.blayer is not None:
            B = strip_optional(self.blayer.bias, device)
            W = strip_optional(self.blayer.weight, device)
            varr: str = "var"
            if varr == "var":
                W_sum = pt.abs(W) * pt.sqrt(W.size(0))
            elif varr == "l2":
                W_sum = pt.abs(W)
        else:
            W = W.view((W.size(0), -1))
            W_sum = pt.sum(pt.max(W, pt.zeros((), device=W.device)), 1)
        return pt.sum((W_sum + B) >= 0).item()

    @pt.jit.export
    def get_reg(self) -> pt.Tensor:
        device = self.wlayer.weight.device

        power: float = 1.0
        zero: pt.Tensor = pt.zeros((), device=self.wlayer.weight.device)
        self.weight = strip_optional(self.wlayer.weight, device)
        self.bias = strip_optional(self.wlayer.bias, device)

        reg_loss: pt.Tensor = zero
        if self.blayer is not None:
            self.weight = strip_optional(self.blayer.weight, device)
            self.bias = strip_optional(self.blayer.bias, device)

            varr: str = "var"
            if varr == "var":
                W_sum = pt.abs(self.weight) * pt.sqrt(self.size(0))
            else:
                W_sum = pt.abs(self.weight)
            max_val = W_sum + self.bias
            vpc = max_val + 0.1
            l1_loss = pt.sum(pt.where(vpc <= 0, zero, vpc))
            reg_loss = self.reg_lambda * l1_loss
        else:
            W_l1_loss = pt.sum(pt.where(self.weight <= 0, zero, self.weight).pow(power))
            bpc = self.bias + 0.1
            bias_l1_loss = pt.sum(pt.where(bpc <= 0, zero, bpc).pow(power))
            reg_loss = self.reg_lambda * (W_l1_loss + bias_l1_loss)

        return reg_loss


class NodeDropConv(pt.nn.Module):
    def __init__(self, in_features: int, out_features: int, bias: bool = True,
                 reg_pam: float = 0.0, batch_norm: bool = True, kernel_size: int = 3,
                 stride: int = 1, padding: Tuple[int, ...] = None) -> None:
        super(NodeDropConv, self).__init__()
        if padding is None:
            padding = (1,1,)

        self.in_features: int = in_features
        self.out_features = out_features
        self.reg_lambda: float = reg_pam
        self.batch_norm: bool = batch_norm
        self.wlayer: pt.nn.Module = pt.nn.Conv2d(in_features, out_features, kernel_size=kernel_size,
                                                 stride=stride, padding=padding)
        self.alayer: pt.nn.Module = None
        self.blayer: Optional[pt.nn.Module] = None

        if self.batch_norm:
            self.blayer = pt.nn.BatchNorm2d(out_features)
            self.alayer = pt.nn.ReLU()
        else:
            self.alayer = ClampedReLU()

        if bias:
            self.bias = pt.nn.Parameter(pt.FloatTensor(out_features))
        else:
            self.register_parameter("bias", None)
        self.register_parameter("weight", self.wlayer.weight)

    def forward(self, X: pt.Tensor) -> pt.Tensor:
        X = self.wlayer(X)
        if self.blayer is not None:
            X = self.blayer(X)
        return self.alayer(X)

    @pt.jit.export
    def node_count(self) -> int:
        device = self.wlayer.weight.device
        B: pt.Tensor = strip_optional(self.wlayer.bias, device)
        W: pt.Tensor = strip_optional(self.wlayer.weight, device)

        W_sum: pt.Tensor = pt.zeros((), device=self.wlayer.weight.device)
        if self.blayer is not None:
            B = strip_optional(self.blayer.bias, device)
            W = strip_optional(self.blayer.weight, device)

            varr: str = "var"
            if varr == "var":
                W_sum = pt.abs(W) * pt.sqrt(W.size(0))
            else:
                W_sum = pt.abs(W)
        else:
            W = W.view((W.size(0), -1))
            W_sum = pt.sum(pt.max(W, pt.zeros((), device=W.device)), 1)

        return pt.sum((W_sum + B) >= 0).item()

    @pt.jit.export
    def get_reg(self) -> pt.Tensor:
        device = self.wlayer.weight.device
        self.bias = strip_optional(self.wlayer.bias, device)
        self.weight = strip_optional(self.wlayer.weight, device)

        power: float = 1.0
        zero: pt.Tensor = pt.zeros((), device=self.wlayer.weight.device)

        reg_loss: pt.Tensor = zero
        if self.blayer is not None:
            self.bias = strip_optional(self.blayer.bias, device)
            self.weight = strip_optional(self.blayer.weight, device)

            varr: str = "var"
            if varr == "var":
                W_sum = pt.abs(self.weight) * pt.sqrt(self.weight.size(0))
            else:
                W_sum = pt.abs(self.weight)
            max_val = W_sum + self.weight
            vpc = max_val + 1.0e-1
            l1_loss = pt.sum(pt.where(vpc <= 0, zero, vpc))
            reg_loss = self.reg_lambda * l1_loss
        else:
            W_l1_loss = pt.sum(pt.where(self.weight <= 0, zero, self.weight).pow(power))
            bpc = self.bias + 1.0e-1
            B_l1_loss = pt.sum(pt.where(bpc <= 0, zero, bpc).pow(power))
            reg_loss = self.reg_lambda * (W_l1_loss + B_l1_loss)

        return reg_loss


def get_ard_reg(module: pt.nn.Module, reg: pt.Tensor = 0.0) -> pt.Tensor:
    if hasattr(module, "get_reg"):
        return reg + module.get_reg()
    if hasattr(module, "children"):
        return reg + sum(get_ard_reg(submodule) for submodule in module.children())
    return reg


if __name__ == "__main__":
    # test compiling to jit
    m = pt.jit.script(NodeDrop(10,10, bias=True, batch_norm=True))
    m = pt.jit.script(NodeDropConv(10, 10, bias=True, batch_norm=True))

