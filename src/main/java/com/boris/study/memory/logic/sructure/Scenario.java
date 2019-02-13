package com.boris.study.memory.logic.sructure;

import java.io.Serializable;

public interface Scenario<I extends Serializable, O extends Serializable> {
    O process(I input, boolean forceRestart);
}
