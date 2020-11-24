import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MonitorReleasePort {


    public static void main(String[] args) throws Exception {
        for (int i = 37701; i <= 65534; i++) {
            if (!isLoclePortUsing(i)) {
                processBuilderLinux(i);
            }
        }
    }

    public static boolean runCommand(final String[] command, final String work_path) throws IOException, InterruptedException {
        List<String> result_list = new ArrayList<>();
        ProcessBuilder hiveProcessBuilder = new ProcessBuilder(command);
        File fi = new File(work_path);
        hiveProcessBuilder.directory(fi);
        hiveProcessBuilder.redirectErrorStream(true);
        Process hiveProcess = hiveProcessBuilder.start();
        BufferedReader std_input = new BufferedReader(new InputStreamReader(hiveProcess.getInputStream(), "UTF-8"));
        BufferedReader std_error = new BufferedReader(new InputStreamReader(hiveProcess.getErrorStream(), "UTF-8"));
        String line;
        while ((line = std_input.readLine()) != null) {
            result_list.add(line);
            if (line.contains("success")) {
                return true;
            }
        }
        while ((line = std_error.readLine()) != null) {
            return false;
        }
        hiveProcess.waitFor();
        if (hiveProcess.exitValue() != 0) {
            System.out.println("failed to execute:" + command);
            return false;
        }
        System.out.println("execute success:" + command);
        return true;
    }

    /**
     * Process the command
     */

    public static String[] partitionCommandLine(final String command) {
        final ArrayList<String> commands = new ArrayList<>();
        int index = 0;
        StringBuffer buffer = new StringBuffer(command.length());
        boolean isApos = false;
        boolean isQuote = false;
        while (index < command.length()) {
            final char c = command.charAt(index);
            switch (c) {
                case ' ':
                    if (!isQuote && !isApos) {
                        final String arg = buffer.toString();
                        buffer = new StringBuffer(command.length() - index);
                        if (arg.length() > 0) {
                            commands.add(arg);
                        }
                    } else {
                        buffer.append(c);
                    }
                    break;
                case '\'':
                    if (!isQuote) {
                        isApos = !isApos;
                    } else {
                        buffer.append(c);
                    }
                    break;
                case '"':
                    if (!isApos) {
                        isQuote = !isQuote;
                    } else {
                        buffer.append(c);
                    }
                    break;
                default:
                    buffer.append(c);
            }
            index++;
        }
        if (buffer.length() > 0) {
            final String arg = buffer.toString();
            commands.add(arg);
        }
        return commands.toArray(new String[commands.size()]);
    }

    public static boolean processBuilderLinux(int port) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        try {
            runCommand(partitionCommandLine("./frps -c ./frps_" + port + ".ini"), getCurrentPath());
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    /***
     *  true:already in using  false:not using
     * @param port
     */
    public static boolean isLoclePortUsing(int port){
        boolean flag = false;
        try {
            flag = isPortUsing("127.0.0.1", port);
        } catch (Exception e) {
        }
        return flag;
    }
    /***
     *  true:already in using  false:not using
     * @param host
     * @param port
     * @throws UnknownHostException
     */
    public static boolean isPortUsing(String host,int port) throws IOException {
        boolean flag = false;
        InetAddress theAddress = InetAddress.getByName(host);
        Socket socket =null;
        try {
            socket = new Socket(theAddress,port);
            flag = true;
            socket.close();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("The port: "+port+" is not open");
        }finally {
            socket.close();
        }
        return flag;
    }

    public static String getCurrentPath() {
        Class<?> caller = getCaller();
        if (caller == null) {
            caller = MonitorReleasePort.class;
        }
        return getCurrentPath(caller);
    }

    public static Class<?> getCaller() {
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        if (stack.length < 3) {
            return MonitorReleasePort.class;
        }
        String className = stack[2].getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCurrentPath(Class<?> cls) {
        String path = cls.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.replaceFirst("file:/", "");
        path = path.replaceAll("!/", "");
        if (path.lastIndexOf(File.separator) >= 0) {
            path = path.substring(0, path.lastIndexOf(File.separator));
        }
        if ("/".equalsIgnoreCase(path.substring(0, 1))) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("window")) {
                path = path.substring(1);
            }
        }
        return path;
    }
}
