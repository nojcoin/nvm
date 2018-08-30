package org.aion.avm.core;

import org.aion.avm.api.Address;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.userlib.AionList;
import org.aion.avm.userlib.AionMap;
import org.aion.avm.userlib.AionSet;
import org.aion.kernel.Block;
import org.aion.kernel.KernelInterface;
import org.aion.kernel.KernelInterfaceImpl;
import org.aion.kernel.Transaction;
import org.aion.kernel.TransactionContextImpl;
import org.aion.kernel.TransactionResult;

import org.junit.Assert;
import org.junit.Test;


/**
 * This is a very basic performance test.  While it can be run as a standard unit test, it is designed to be easily tunable for more hands-on
 * measurement and profiling.
 * See BasicPerfContract for details of what the test application is doing.  In short, it is a graph traversal/manipulation algorithm, so it
 * demonstrates how our persistence model fits with this.
 */
public class BasicPerfTest {
    private final static int COUNT = 1000;
    private final static int THREAD_COUNT = 1;

    @Test
    public void testDeployAndRun() throws Exception {
        TestRunnable[] threads = new TestRunnable[THREAD_COUNT];
        // Setup.
        byte[] jar = JarBuilder.buildJarForMainAndClasses(BasicPerfContract.class
                , AionList.class
                , AionMap.class
                , AionSet.class
        );
        
        // Deploy.
        for (int i = 0; i < THREAD_COUNT; ++i) {
            threads[i] = new TestRunnable();
            threads[i].deploy(jar, new byte[0]);
        }
        
        // Run.
        long start = System.nanoTime();
        for (int i = 0; i < THREAD_COUNT; ++i) {
            threads[i].start();
        }
        for (int i = 0; i < THREAD_COUNT; ++i) {
            threads[i].join();
        }
        long end = System.nanoTime();
        // Report - note that this doesn't take the thread count into account.
        System.out.println("NANOS PER RUN: " + ((end - start) / COUNT));
    }


    private static class TestRunnable extends Thread {
        private byte[] deployer = KernelInterfaceImpl.PREMINED_ADDRESS;
        private KernelInterface kernel;
        private Avm avm;
        private byte[] contractAddress;
        public void deploy(byte[] jar, byte[] arguments) {
            // Deploy.
            this.kernel = new KernelInterfaceImpl();
            this.avm = NodeEnvironment.singleton.buildAvmInstance(kernel);
            Block block = new Block(new byte[32], 1, Helpers.randomBytes(Address.LENGTH), System.currentTimeMillis(), new byte[0]);
            long transaction1EnergyLimit = 1_000_000_000l;
            Transaction tx1 = new Transaction(Transaction.Type.CREATE, deployer, null, kernel.getNonce(deployer), 0, new CodeAndArguments(jar, arguments).encodeToBytes(), transaction1EnergyLimit, 1l);
            TransactionResult result1 = this.avm.run(new TransactionContextImpl(tx1, block));
            Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
            this.contractAddress = result1.getReturnData();
        }
        @Override
        public void run() {
            int blockStart = 2;
            for (int i = blockStart; i < (COUNT + blockStart); ++i) {
                Block block = new Block(new byte[32], i, Helpers.randomBytes(Address.LENGTH), System.currentTimeMillis(), new byte[0]);
                long transaction1EnergyLimit = 1_000_000_000l;
                Transaction tx1 = new Transaction(Transaction.Type.CALL, deployer, this.contractAddress, kernel.getNonce(deployer), 0, new byte[0], transaction1EnergyLimit, 1l);
                TransactionResult result1 = this.avm.run(new TransactionContextImpl(tx1, block));
                Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
            }
        }
    }
}