package org.c2graph.model.concurrent;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConcurrentTask {
    private Integer st;
    private Integer ed;
    private Integer minv;
    private Integer maxv;

    private Boolean isFunctionFinish;
    private Boolean isSystemFinish;
    private Boolean isGlobalDecl;

    public ConcurrentTask() {
        this.isFunctionFinish = false;
        this.isSystemFinish = false;
        this.isGlobalDecl = false;
    }

    public ConcurrentTask(int st, int ed) {
        this.st = st;
        this.ed = ed;
        this.minv = -1;
        this.maxv = Integer.MAX_VALUE;
        this.isFunctionFinish = false;
        this.isSystemFinish = false;
        this.isGlobalDecl = false;
    }

    public ConcurrentTask(int st, int ed, Boolean isFunctionFinish) {
        this.st = st;
        this.ed = ed;
        this.isFunctionFinish = isFunctionFinish;
        this.isSystemFinish = false;
        this.isGlobalDecl = false;
    }

    public ConcurrentTask(int st, int ed, int minv, int maxv) {
        this.st = st;
        this.ed = ed;
        this.minv = minv;
        this.maxv = maxv;
        this.isFunctionFinish = false;
        this.isSystemFinish = false;
        this.isGlobalDecl = false;
    }
}
