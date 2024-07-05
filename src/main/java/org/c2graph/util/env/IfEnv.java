package org.c2graph.util.env;

import lombok.Data;

import java.util.ArrayList;

/**
 * 处理 if 这类只需要向后看的"回填"
 * 需要使用 falseMergeUp、emptyMergeUp
 *
 * @author milo on 12/4/2023
 */
@Data
public class IfEnv extends Env {
    public int conditionIdx;
    public boolean isSwitch;

    /**
     * 用于处理 if{} else{} 中的最后一句跳转
     */
    private ArrayList<Integer> ifEmptyList;

    /**
     * should we handle this now?
     */
    public boolean addTrueList;
    public boolean addFalseList;

    /**
     * 专门用于处理 if elseIf else 这类多出口
     * trueList is designed for the multiple entrances of switch
     */
    private ArrayList<Integer> trueList;
    private ArrayList<Integer> falseList;

    public IfEnv() {
        this.trueList = new ArrayList<Integer>();
        this.falseList = new ArrayList<Integer>();
        this.ifEmptyList = new ArrayList<Integer>();

        this.mergeUpEmptyList = new ArrayList<Integer>();
        this.mergeUpFalseList = new ArrayList<Integer>();
    }
}
