package org.bitcoinkernel;

import java.lang.foreign.*;

import static org.bitcoinkernel.BitcoinKernelBindings.*;
import static org.bitcoinkernel.BitcoinKernel.*;

public class Transactions {

    // ===== Coin =====
    public static class Coin implements AutoCloseable {


        @Override
        public void close() throws Exception {

        }
    }

    // ===== Transaction Output =====
    public static class TransactionOutput {

    }

    // ===== Script Pubkey =====
    public static class ScriptPubkey {

    }

    // ===== Transaction Spent Outputs
    public static class TransactionsSpentOutputs implements Iterable<Coin> {

    }
}
