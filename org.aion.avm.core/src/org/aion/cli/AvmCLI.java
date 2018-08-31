package org.aion.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.Avm;
import org.aion.avm.core.NodeEnvironment;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.kernel.*;
import com.beust.jcommander.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AvmCLI implements UserInterface{
    static String DEFAULT_STORAGE = "./storage";

    static byte[] DEFAULT_SENDER = KernelInterfaceImpl.PREMINED_ADDRESS;

    static String DEFAULT_SENDER_STRING = Helpers.bytesToHexString(DEFAULT_SENDER);

    static long ENERGY_LIMIT = 100_000_000_000L;

    static Block block = new Block(new byte[32], 1, Helpers.randomBytes(Address.LENGTH), System.currentTimeMillis(), new byte[0]);

    private JCommander jc;

    private CLIFlag flag;
    private CommandOpen open;
    private CommandDeploy deploy;
    private CommandCall call;
    private CommandExplore explore;

    private AvmCLI(){
        flag = new CLIFlag();
        open = new CommandOpen();
        deploy = new CommandDeploy();
        call = new CommandCall();
        explore = new CommandExplore();
        jc = JCommander.newBuilder()
                .addObject(flag)
                .addCommand("open", open)
                .addCommand("deploy", deploy)
                .addCommand("call", call)
                .addCommand("explore", explore)
                .build();

        jc.setProgramName("AvmCLI");
    }

    public class CLIFlag {

        @Parameter(names = {"-st", "--storage"}, description = "Specify the storage directory")
        private String storage = "./storage";

        @Parameter(names = {"-h", "--help"}, description = "Show usage of AVMCLI")
        private boolean usage = false;
    }


    @Parameters(commandDescription = "Open new account")
    public class CommandOpen{

        @Parameter(names = {"-a","--address"}, description = "The address to open")
        private String address = "Random generated address";
    }

    @Parameters(commandDescription = "Deploy Dapp")
    public class CommandDeploy{

        @Parameter(description = "DappJar", required = true)
        private String contract = null;

        @Parameter(names = { "-sd" ,"--sender"}, description = "The sender of the request")
        private String sender = DEFAULT_SENDER_STRING;
    }

    @Parameters(commandDescription = "Explore storage")
    public class CommandExplore{

        @Parameter(names = "-Dapp", description = "List deployed Dapp only")
        private boolean dapp = false;

        @Parameter(names = "-account", description = "Explore the specified account")
        private String account = null;

    }

    @Parameters(commandDescription = "Call Dapp")
    public class CommandCall{

        @Parameter(description = "DappAddress", required = true)
        private String contract = null;

        @Parameter(names = {"-sd", "--sender"}, description = "The sender of the request")
        private String sender = DEFAULT_SENDER_STRING;

        @Parameter(names = {"-m", "--method"}, description = "The requested method")
        private String methodName = "";

        @Parameter(names = {"-a", "--args"}, description = "The requested arguments. " +
                "User provided arguments have the format of (Type Value)*. " +
                "The following type are supported " +
                "-I int, -J long, -S short, -C char, -F float, -D double, -B byte, -Z boolean. "+
                "For example, option \"-a -I 1 -C c -Z true\" will form arguments of [(int) 1, (char) 'c', (boolean) true].",
                variableArity = true)
        public List<String> arguments = new ArrayList<>();

    }

    @Override
    public void deploy(IEnvironment env, String storagePath, String jarPath, byte[] sender) {

        reportDeployRequest(env, storagePath, jarPath, sender);

        if (sender.length != Address.LENGTH){
            throw env.fail("deploy : Invalid sender address");
        }

        File storageFile = new File(storagePath);

        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);
        Avm avm = NodeEnvironment.singleton.buildAvmInstance(kernel);

        Path path = Paths.get(jarPath);
        byte[] jar;
        try {
            jar = Files.readAllBytes(path);
        }catch (IOException e){
            throw env.fail("deploy : Invalid location of Dapp jar");
        }

        Transaction createTransaction = new Transaction(Transaction.Type.CREATE, sender, null, kernel.getNonce(sender), 0,
                new CodeAndArguments(jar, null).encodeToBytes(), ENERGY_LIMIT, 1);

        TransactionContext createContext = new TransactionContextImpl(createTransaction, block);
        TransactionResult createResult = avm.run(createContext);

        reportDeployResult(env, createResult);
    }

    public void reportDeployRequest(IEnvironment env, String storagePath, String jarPath, byte[] sender) {
        lineSeparator();
        env.logLine("DApp deployment request");
        env.logLine("Storage      : " + storagePath);
        env.logLine("Dapp Jar     : " + jarPath);
        env.logLine("Sender       : " + Helpers.bytesToHexString(sender));
    }

    public void reportDeployResult(IEnvironment env, TransactionResult createResult){
        lineSeparator();
        env.logLine("DApp deployment status");
        env.logLine("Result status: " + createResult.getStatusCode().name());
        env.logLine("Dapp Address : " + Helpers.bytesToHexString(createResult.getReturnData()));
        env.logLine("Energy cost  : " + createResult.getEnergyUsed());
    }

    @Override
    public void call(IEnvironment env, String storagePath, byte[] contract, byte[] sender, String method, String[] args) {
        reportCallRequest(env, storagePath, contract, sender, method, args);

        if (contract.length != Address.LENGTH){
            System.out.println("call : Invalid Dapp address ");
            return;
        }

        if (sender.length != Address.LENGTH){
            System.out.println("call : Invalid sender address");
            return;
        }

        byte[] arguments = ABIEncoder.encodeMethodArguments(method, parseArgs(args));

        File storageFile = new File(storagePath);

        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);
        Avm avm = NodeEnvironment.singleton.buildAvmInstance(kernel);

        Transaction callTransaction = new Transaction(Transaction.Type.CALL, sender, contract, kernel.getNonce(sender), 0, arguments, ENERGY_LIMIT, 1l);
        TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
        TransactionResult callResult = avm.run(callContext);

        reportCallResult(env, callResult);
    }

    private void reportCallRequest(IEnvironment env, String storagePath, byte[] contract, byte[] sender, String method, String[] args){
        lineSeparator();
        System.out.println("DApp call request");
        System.out.println("Storage      : " + storagePath);
        System.out.println("Dapp Address : " + Helpers.bytesToHexString(contract));
        System.out.println("Sender       : " + Helpers.bytesToHexString(sender));
        System.out.println("Method       : " + method);
        System.out.println("Arguments    : ");
        for (int i = 0; i < args.length; i += 2){
            System.out.println("             : " + args[i] + " " + args[i + 1]);
        }
    }

    private void reportCallResult(IEnvironment env, TransactionResult callResult){
        lineSeparator();
        System.out.println("DApp call result");
        System.out.println("Result status: " + callResult.getStatusCode().name());
        System.out.println("Return value : " + Helpers.bytesToHexString(callResult.getReturnData()));
        System.out.println("Energy cost  : " + callResult.getEnergyUsed());
    }

    private Object[] parseArgs(String[] args){

        Object[] argArray = new Object[args.length / 2];

        for (int i = 0; i < args.length; i += 2){
            switch (args[i]){
                case "-I":
                    argArray[i / 2] = Integer.valueOf(args[i + 1]);
                    break;
                case "-S":
                    argArray[i / 2] = Short.valueOf(args[i + 1]);
                    break;
                case "-L":
                    argArray[i / 2] = Long.valueOf(args[i + 1]);
                    break;
                case "-F":
                    argArray[i / 2] = Float.valueOf(args[i + 1]);
                    break;
                case "-D":
                    argArray[i / 2] = Double.valueOf(args[i + 1]);
                    break;
                case "-C":
                    argArray[i / 2] = Character.valueOf(args[i + 1].charAt(0));
                    break;
                case "-B":
                    argArray[i / 2] = Helpers.hexStringToBytes(args[i + 1])[0];
                    break;
                case "-Z":
                    argArray[i / 2] = Boolean.valueOf(args[i + 1]);
                    break;
            }
        }

        return argArray;
    }

    private void lineSeparator(){
        System.out.println("*******************************************************************************************");
    }

    @Override
    public void openAccount(IEnvironment env, String storagePath, byte[] toOpen){
        lineSeparator();

        if (toOpen.length != Address.LENGTH){
            System.out.println("open : Invalid address to open");
            return;
        }

        System.out.println("Creating Account " + Helpers.bytesToHexString(toOpen));

        File storageFile = new File(storagePath);
        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);

        kernel.createAccount(toOpen);
        kernel.adjustBalance(toOpen, 100000000000L);

        System.out.println("Account Balance : " + kernel.getBalance(toOpen));
    }

    public static void testingMain(IEnvironment env, String[] args) {
        internalMain(env, args);
    }

    static public void main(String[] args){
        IEnvironment env = new IEnvironment() {
            @Override
            public RuntimeException fail(String message) {
                if (null != message) {
                    System.err.println(message);
                }
                System.exit(1);
                throw new RuntimeException();
            }
            @Override
            public void noteRelevantAddress(String address) {
                // This implementation doesn't care.
            }
            @Override
            public void logLine(String line) {
                System.out.println(line);
            }
        };
        internalMain(env, args);
    }

    private static void internalMain(IEnvironment env, String[] args) {
        // We handle all the parsing and dispatch through a special instance of ourselves (although this should probably be split into a few concerns).
        AvmCLI instance = new AvmCLI();
        try {
            instance.jc.parse(args);
        }catch (ParameterException e){
            callUsage(env, instance);
            throw env.fail(e.getMessage());
        }

        if (instance.flag.usage){
            callUsage(env, instance);
        }

        String parserCommand = instance.jc.getParsedCommand();
        if (null != parserCommand) {
            // Before we run any command, make sure that the specified storage directory exists.
            // (we want the underlying storage engine to remain very passive so it should always expect that the directory was created for it).
            verifyStorageExists(env, instance.flag.storage);
            
            if (parserCommand.equals("open")) {
                if (instance.open.address.equals("Random generated address")) {
                    instance.openAccount(env, instance.flag.storage, Helpers.randomBytes(Address.LENGTH));
                } else {
                    instance.openAccount(env, instance.flag.storage, Helpers.hexStringToBytes(instance.open.address));
                }
            }

            if (parserCommand.equals("deploy")) {
                instance.deploy(env, instance.flag.storage, instance.deploy.contract, Helpers.hexStringToBytes(instance.deploy.sender));
            }

            if (parserCommand.equals("call")) {
                instance.call(env, instance.flag.storage, Helpers.hexStringToBytes(instance.call.contract),
                        Helpers.hexStringToBytes(instance.call.sender), instance.call.methodName,
                        instance.call.arguments.toArray(new String[0]));
            }
        } else {
            // If we specify nothing, print the usage.
            callUsage(env, instance);
        }
    }

    private static void verifyStorageExists(IEnvironment env, String storageRoot) {
        File directory = new File(storageRoot);
        if (!directory.isDirectory()) {
            boolean didCreate = directory.mkdirs();
            // Is this the best way to handle this failure?
            if (!didCreate) {
                // System.exit isn't ideal but we are very near the top of the entry-point so this shouldn't cause confusion.
                throw env.fail("Failed to create storage root: \"" + storageRoot + "\"");
            }
        }
    }

    private static void callUsage(IEnvironment env, AvmCLI instance) {
        StringBuilder builder = new StringBuilder();
        instance.jc.usage(builder);
        env.logLine(builder.toString());
    }
}
