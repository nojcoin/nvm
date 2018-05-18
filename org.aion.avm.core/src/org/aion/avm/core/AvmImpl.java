package org.aion.avm.core;

import org.aion.avm.core.exceptionwrapping.ExceptionWrapping;
import org.aion.avm.core.instrument.ClassMetering;
import org.aion.avm.core.instrument.HeapMemoryCostCalculator;
import org.aion.avm.core.shadowing.ClassShadowing;
import org.aion.avm.core.stacktracking.StackWatcherClassAdapter;
import org.aion.avm.rt.BlockchainRuntime;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AvmImpl implements Avm {

    private static final String HELPER_CLASS = "org/aion/avm/internal/Helper";

    /**
     * Represents a DApp module in memory.
     */
    private class DappModule {
        private Map<String, byte[]> classes;
        private String mainClass;

        public DappModule(Map<String, byte[]> classes, String mainClass) {
            this.classes = classes;
            this.mainClass = mainClass;
        }

        public Map<String, byte[]> getClasses() {
            return classes;
        }

        public String getMainClass() {
            return mainClass;
        }

        public void setClasses(Map<String, byte[]> classes) {
            this.classes = classes;
        }

        public void setMainClass(String mainClass) {
            this.mainClass = mainClass;
        }
    }

    /**
     * Extracts the DApp module in compressed format into the designated folder.
     *
     * @param module the DApp module in JAR format
     * @return the parsed DApp module if this operation is successful, otherwise null
     */
    public DappModule readDapp(byte[] module) {

        // TODO: Rom

        return null;
    }

    /**
     * Validates all classes, including but not limited to:
     *
     * <ul>
     * <li>class format (hash, version, etc.)</li>
     * <li>no native method</li>
     * <li>no invalid opcode</li>
     * <li>package name does not start with <code>org.aion</code></li>
     * <li>main class is a <code>Contract</code></li>
     * <li>TODO: add more</li>
     * </ul>
     *
     * @param dapp the classes of DApp
     * @return the class hierarchy if the classes are valid, otherwise null
     */
    public ClassHierarchyForest validateDapp(DappModule dapp) {

        // TODO: Rom

        return null;
    }

    /**
     * Computes the object size of runtime classes
     *
     * @return
     */
    public Map<String, Integer> computeRuntimeObjectSizes() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("java/lang/Object", 4);
        map.put("java/lang/Class", 4);
        map.put("java/lang/Math", 4);
        map.put("java/lang/String", 4);

        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns the sizes of all the classes, including the runtime ones and the DApp ones.
     *
     * @param classHierarchy     the class hierarchy
     * @param runtimeObjectSizes the object size of runtime classes
     * @return a mapping between class name and object size, for all classes, including the runtime ones from "runtimeObjectSizes"; and the DApp ones passed-in with "classes".
     */
    public Map<String, Integer> computeObjectSizes(ClassHierarchyForest classHierarchy, Map<String, Integer> runtimeObjectSizes) {
        HeapMemoryCostCalculator objectSizeCalculator = new HeapMemoryCostCalculator();

        // copy over the runtime classes sizes
        Map<String, Integer> objectSizes = new HashMap<>(runtimeObjectSizes);

        // compute the object size of every one in 'classes'
        objectSizeCalculator.calcClassesInstanceSize(classHierarchy, runtimeObjectSizes);

        // copy over the DApp classes sizes
        objectSizes.putAll(objectSizeCalculator.getClassHeapSizeMap());

        return Collections.unmodifiableMap(objectSizes);
    }

    /**
     * Replaces the <code>java.base</code> package with the shadow implementation.
     *
     * @param classes        the class of DApp
     * @param classHierarchy the class hierarchy
     * @param objectSizes    the sizes of object
     * @return the classes after
     */
    public Map<String, byte[]> analyzeClasses(Map<String, byte[]> classes, ClassHierarchyForest classHierarchy, Map<String, Integer> objectSizes) {

        Map<String, byte[]> processedClasses = new HashMap<>();
        Map<String, byte[]> generatedClasses = new HashMap<>();

        for (String name : classes.keySet()) {
            ClassReader in = new ClassReader(classes.get(name));

            // in reverse order
            ClassWriter out = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ExceptionWrapping exceptionHandling = new ExceptionWrapping(out, HELPER_CLASS, classHierarchy, generatedClasses);
            StackWatcherClassAdapter stackTracking = new StackWatcherClassAdapter(exceptionHandling);
            ClassShadowing classShadowing = new ClassShadowing(stackTracking, HELPER_CLASS);
            ClassMetering classMetering = new ClassMetering(classShadowing, HELPER_CLASS, classHierarchy, objectSizes);

            // traverse
            // TODO:  ClassReader.EXPAND_FRAMES is needed for stacktracking injector
            in.accept(classMetering, ClassReader.EXPAND_FRAMES);

            // emit bytecode
            processedClasses.put(name, out.toByteArray());
        }

        // merge the generated classes and processed classes, assuming the package spaces do not conflict.
        processedClasses.putAll(generatedClasses);
        return processedClasses;
    }

    /**
     * Stores the instrumented bytecode into database.
     *
     * @param address the address of the DApp
     * @param dapp    the dapp module
     */
    public void storeTransformedDapp(String address, DappModule dapp) {

        // TODO: Rom
    }

    @Override
    public boolean deploy(byte[] code) {
        // STEP-1: compute the hash of the code, which will be used as identifier

        // STEP-2: extract the classes to a temporary folder

        // STEP-3: walk through all the classes and inject metering code

        // STEP-4: store the instrumented code and metadata(e.g. main class name)

        return false;
    }

    @Override
    public AvmResult run(byte[] codeHash, BlockchainRuntime rt) {
        // STEP-1: retrieve the instrumented bytecode using the given codeHash

        // STEP-2: load the classed. class loading fees should apply during the process

        // STEP-3: invoke the `run` method of the main class

        // STEP-4: return the DApp output


        return null;
    }
}
