package org.c2graph.util.env;

import java.util.ArrayList;

public abstract class Env {
    public boolean isFinished;

    // it represents if the last stmt of it is `if-like` or `for-like`
    // it means the address of it should add to parent
    public boolean childMergeUp;
    // it represents if the stmt like `if ... else if ... else ...`
    // it means the address of it should add to parent
    public boolean siblingMergeUp;

    // should the child empty edge mergeUp?
    // we use mergeUpEmptyList to store
    public boolean shouldEmptyMergeUp;
    // should the child false edge mergeUp?
    // we use mergeUpFalseList to store
    public boolean shouldFalseMergeUp;
    public ArrayList<Integer> mergeUpEmptyList;
    public ArrayList<Integer> mergeUpFalseList;
}
