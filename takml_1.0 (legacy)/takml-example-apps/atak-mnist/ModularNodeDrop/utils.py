# SYSTEM IMPORTS
from typing import Optional
import torch as pt

# PYTHON PROJECT IMPORTS


""" This function just converts an Optional[Tensor] to a Tensor. Optional[Tensor] can either
    be a Tensor or None, and c++ doesn't like that when matching aten (Pytorch's c++ linear algebra
    library) calls, which expect Tensor types and NOT Optional[Tensor] type arguments.

    So, if the Optional[Tensor] actually contains a Tensor, just return it, otherwise return
    pt.zeros() on whatever device you want (which can also be None which will default to the cpu)
"""
def strip_optional(X: Optional[pt.Tensor], device: Optional[pt.device]) -> pt.Tensor:
    Y: Optional[pt.Tensor] = X
    if Y is None:
        Y = pt.zeros((), device=device)
    return Y

