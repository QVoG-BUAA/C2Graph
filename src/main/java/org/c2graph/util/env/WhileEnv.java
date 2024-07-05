package org.c2graph.util.env;

import lombok.Data;

import java.util.ArrayList;

/**
 * 处理 while 这类向后看的同时需要向前看的"回填"
 * 只需要使用 falseMergeUp
 */
@Data
public class WhileEnv extends Env {
    public int conditionIdx;
    public int iterationIdx;
    public int lastStmtIdx;

    public boolean isDo;
    public boolean isFor;
    public boolean shouldTrueJump2;
    public boolean isWhileFinished;

    /**
     * 用于集成 break
     */
    private ArrayList<Integer> falseList;

    /**
     * continue jump2condition, but the condition may be unknown4do-while
     */
    private ArrayList<Integer> continueList;

    public WhileEnv() {
        this.mergeUpFalseList = new ArrayList<Integer>();
        this.mergeUpEmptyList = new ArrayList<Integer>();

        this.falseList = new ArrayList<Integer>();
        this.continueList = new ArrayList<>();
    }
}
