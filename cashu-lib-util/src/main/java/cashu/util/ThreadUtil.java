package cashu.util;

import java.util.concurrent.locks.ReentrantLock;

public class ThreadUtil {
    public static final ReentrantLock MINT_MELT_LOCK = new ReentrantLock();
    public static final ReentrantLock PROOF_STATE_LOCK = new ReentrantLock();
}

