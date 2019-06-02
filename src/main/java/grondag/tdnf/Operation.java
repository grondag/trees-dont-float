package grondag.tdnf;

public enum Operation
{
    IDLE,
    @Deprecated
    SEARCHING,
    SEARCHING_LOGS,
    SEARCHING_FOLIAGE,
    CLEARING_LOGS,
    CLEARING_FOLIAGE,
    @Deprecated
    CLEARING,
    TICKING,
    COMPLETE
}