import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NetworkNode {
    public static String newServerMes = "ABSOLUTELY_SECRET_MESSAGE_:_I_AM_A_NEW_CLOSEST_SERVER_:_THERE_IS_MY_DATA_TO_CONNECT";
    public static String closServerWantsToAlo = "ABSOLUTELY_SECRET_MESSAGE_:_ALLOCATE_MY_DATA_MY_ID: ";
    public static String closeNodeCanAllocate = "WE_CAN";
    public static String alocated = "ALOCATED";
    public static String failed = "FAILED";
    public static String terminated = "TERMINATE";
    public static String zasobyRequest = "GIVE_ME_YOUR_AVAILABLE_ZASOBS";
    public static String exactlyInputing = "INPUT_THIS_WITHOUT_CONFIRMATION";

    private final int identyfikator;
    private final int tcpport;
    private final String gateway;
    public final Map<Character, Integer> freeZasoby;
    public final Map<Integer, Map<Character, Integer>> allocatedZasobs;
    public final List<String> closeServers;
    private ServerSocket serverSocket;

    public NetworkNode(String id, String tcp, String gate, String zas) {
        this.identyfikator = Integer.parseInt(id);
        this.tcpport = Integer.parseInt(tcp);
        this.gateway = gate;
        this.freeZasoby = new HashMap<>();
        this.closeServers = new LinkedList<>();
        this.allocatedZasobs = new HashMap<>();

        Arrays.stream(zas.split(" ")).forEach(zasob -> {
            freeZasoby.put(zasob.split(":")[0].toUpperCase().charAt(0)
                    , Integer.parseInt(zasob.split(":")[1]));
        });
    }
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("INCORRECT START (ERROR 1)");
            return;
        }

        NetworkNode curNode;
        if (args[4].compareTo("-gateway") == 0) {
            StringBuilder zasss = new StringBuilder("");
            for (int i = 6; i < args.length; i++)
                zasss.append(args[i]).append(" ");
            curNode = new NetworkNode(args[1], args[3], args[5], zasss.toString());

            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(curNode.gateway.split(":")[0]
                        , Integer.parseInt(curNode.gateway.split(":")[1])), 1000);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(newServerMes);
                out.println("-ident " + curNode.identyfikator +
                        " -ip " + socket.getLocalAddress().toString().substring(1) +
                        " -tcpport " + curNode.tcpport);

                String answer = in.readLine();
                System.out.println(answer);
                curNode.closeServers.add(answer);

                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            StringBuilder zasss = new StringBuilder("");
            for (int i = 4; i < args.length; i++)
                zasss.append(args[i]).append(" ");
            curNode = new NetworkNode(args[1], args[3], "null", zasss.toString());
        }
        System.out.println("I_AM_NODE: -ident " + curNode.identyfikator);
        try {
            curNode.serverSocket = new ServerSocket(curNode.tcpport);
            while (true)
                new Server(curNode.serverSocket.accept(), curNode).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putNewClosestServer(String ans) {
        this.closeServers.add(ans);
    }

    public int getIdentyfikator() {
        return identyfikator;
    }

    public int getTcpport() {
        return tcpport;
    }

    public void printZasobs() {
        this.freeZasoby.forEach((key, value) -> {
            System.out.print(key + ":" + value + " ");
        });
        System.out.println();
    }
}

class Server extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private NetworkNode networkNode;

    public Server(Socket socket, NetworkNode nn) {
        this.clientSocket = socket;
        this.networkNode = nn;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            String firstLine = in.readLine();
            System.out.println(firstLine);
            if (firstLine.compareTo(NetworkNode.newServerMes) == 0) {           //new close node
                String data_to_connect = in.readLine();
                System.out.println(data_to_connect);

                networkNode.putNewClosestServer(data_to_connect);
                out.println("-ident " + networkNode.getIdentyfikator() +
                        " -ip " + clientSocket.getLocalAddress().toString().substring(1) +
                        " -tcpport " + networkNode.getTcpport());

                in.close();
                out.close();
                clientSocket.close();
                return;
            }
            if (firstLine.contains(NetworkNode.closServerWantsToAlo)) {       //ask close node to allocate
                int identyfikatorKlienta = Integer.parseInt(firstLine.split(" ")[1]);

                String zasobs = in.readLine();

                Map<Character, Integer> whatToAllocate = new HashMap<>();
                Arrays.stream(zasobs.split(" ")).forEach(par -> {
                    whatToAllocate.put(par.split(":")[0].toUpperCase().charAt(0)
                            , Integer.parseInt(par.split(":")[1]));
                });

                //poisk mesta na allocating
                final boolean[] canBeAllocatedInCurNode = new boolean[]{true};
                Map<Character, Integer> whatPairCannotAllocateCurNode = new HashMap<>();
                List<Character> canBeFullyAllocatedCurNode = new LinkedList<>();

                whatToAllocate.forEach((key, value) -> {
                    if (networkNode.freeZasoby.containsKey(key)) {
                        if (networkNode.freeZasoby.get(key) >= value) {
                            canBeFullyAllocatedCurNode.add(key);
                        } else {
                            whatPairCannotAllocateCurNode.put(key
                                    , whatToAllocate.get(key) - networkNode.freeZasoby.get(key));
                        }
                    } else {
                        whatPairCannotAllocateCurNode.put(key, value);
                    }
                });

                whatToAllocate.forEach((key, value) -> {
                    if (canBeAllocatedInCurNode[0] && !canBeFullyAllocatedCurNode.contains(key)) {
                        canBeAllocatedInCurNode[0] = false;
                    }
                });

                if (canBeAllocatedInCurNode[0]) {
                    System.out.println(identyfikatorKlienta + " ALLOCATING_BY_MYSELF");
                    StringBuilder res = new StringBuilder("");
                    whatToAllocate.forEach((key, value) -> {
                        if (networkNode.allocatedZasobs.containsKey(identyfikatorKlienta)) {
                            networkNode.allocatedZasobs.get(identyfikatorKlienta).put(key, value);
                        } else {
                            networkNode.allocatedZasobs.put(identyfikatorKlienta, new HashMap<>(key, value));
                        }
                        if (networkNode.freeZasoby.get(key) - value > 0) {
                            networkNode.freeZasoby.put(key, networkNode.freeZasoby.get(key) - value);
                        } else {
                            networkNode.freeZasoby.remove(key);
                        }

                        res.append(key).append(":").append(value).append(":")
                                .append(clientSocket.getLocalAddress().toString().substring(1)).append(":")
                                .append(networkNode.getTcpport())
                                .append("\n");
                    });
                    out.println(NetworkNode.closeNodeCanAllocate);
                    out.println(res.toString());

                    System.out.println(identyfikatorKlienta + " I_ALLOCATE_THIS_BY_MYSELF");
                    System.out.println(res.toString());
                }
            } else if (firstLine.contains(NetworkNode.zasobyRequest)) { //request to show available zasobs
                int id = Integer.parseInt(firstLine.split(" ")[1]);
                StringBuilder sb = new StringBuilder("");

                sb.append(clientSocket.getLocalAddress().toString().substring(1)).append(":")
                        .append(networkNode.getTcpport()).append(" ");
                networkNode.freeZasoby
                        .forEach((key, value) -> sb.append(key).append(":").append(value).append(" "));

                sb.append("%");
                networkNode.closeServers.stream()
                        .filter(str -> str.split(" ")[1].compareTo("" + id) != 0)
                        .forEach(nodeStr -> {
                            try {
                                Socket socket_to_neig = new Socket();
                                socket_to_neig.connect(new InetSocketAddress(nodeStr.split(" ")[3]
                                        , Integer.parseInt(nodeStr.split(" ")[5])), 1000);

                                PrintWriter out_to_neig = new PrintWriter(socket_to_neig.getOutputStream(), true);
                                BufferedReader in_to_neig = new BufferedReader(new InputStreamReader(socket_to_neig.getInputStream()));

                                out_to_neig.println(NetworkNode.zasobyRequest + " " + networkNode.getIdentyfikator());
                                String line = in_to_neig.readLine();

                                sb.append(line).append("%");
                                socket_to_neig.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });

                out.println(sb.toString());
            } else if (firstLine.contains(NetworkNode.exactlyInputing)) {  //node is saying to allocate
                if (firstLine.length() > 1) {
                    firstLine = in.readLine();
                    Map<Character, Integer> whatToAlo = new HashMap<>();
                    Arrays.stream(firstLine.split(" ")).forEach(pair -> {
                        whatToAlo.put(pair.split(":")[0].charAt(0)
                                , Integer.parseInt(pair.split(":")[1]));
                    });

                    whatToAlo.forEach((key, value) -> {
                        networkNode.freeZasoby.put(key, networkNode.freeZasoby.get(key) - value);
                    });
                    System.out.println("    I WAS ASKED TO EXACTLY INPUT");
                    System.out.println("    MAP AFTER INPUTTING:");
                    networkNode.printZasobs();
                }
            } else if (firstLine.contains(NetworkNode.terminated)) {
                StringBuilder idFrom = new StringBuilder("");
                if (firstLine.length() > NetworkNode.terminated.length()) {
                    idFrom.append(firstLine.split(" ")[1]);
                }
                networkNode.closeServers
                        .stream().filter(str -> str.split(" ")[1].compareTo(idFrom.toString()) != 0)
                        .forEach(str -> {
                            try {
                                Socket socket_to_neig = new Socket();
                                socket_to_neig.connect(new InetSocketAddress(str.split(" ")[3]
                                        , Integer.parseInt(str.split(" ")[5])), 1000);

                                PrintWriter out_to_neig = new PrintWriter(socket_to_neig.getOutputStream(), true);
                                BufferedReader in_to_neig = new BufferedReader(new InputStreamReader(socket_to_neig.getInputStream()));

                                out_to_neig.println(NetworkNode.terminated + " " + networkNode.getIdentyfikator());

                                socket_to_neig.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                clientSocket.close();
                System.exit(0);
            } else {                                                                //client want to allocate
                int identyfikatorKlienta = Integer.parseInt(firstLine.split(" ")[0]);

                firstLine = firstLine.substring(firstLine.split(" ")[0].length() + 1);

                Map<Character, Integer> whatToAllocate = new HashMap<>();
                Arrays.stream(firstLine.split(" ")).forEach(par -> {
                    whatToAllocate.put(par.split(":")[0].toUpperCase().charAt(0)
                            , Integer.parseInt(par.split(":")[1]));
                });

                //poisk mesta na allocating
                final boolean[] canBeAllocatedInCurNode = new boolean[]{true};
                Map<Character, Integer> whatPairCannotAllocateCurNode = new HashMap<>();
                List<Character> canBeFullyAllocatedCurNode = new LinkedList<>();

                whatToAllocate.forEach((key, value) -> {
                    if (networkNode.freeZasoby.containsKey(key)) {
                        if (networkNode.freeZasoby.get(key) >= value) {
                            canBeFullyAllocatedCurNode.add(key);
                        } else {
                            whatPairCannotAllocateCurNode.put(key
                                    , whatToAllocate.get(key) - networkNode.freeZasoby.get(key));
                        }
                    } else {
                        whatPairCannotAllocateCurNode.put(key, value);
                    }
                });

                whatToAllocate.forEach((key, value) -> {
                    if (canBeAllocatedInCurNode[0] && !canBeFullyAllocatedCurNode.contains(key)) {
                        canBeAllocatedInCurNode[0] = false;
                    }
                });

                if (canBeAllocatedInCurNode[0]) {
                    System.out.println(identyfikatorKlienta + " ALLOCATING_BY_MYSELF");
                    StringBuilder res = new StringBuilder("");
                    whatToAllocate.forEach((key, value) -> {
                        if (networkNode.allocatedZasobs.containsKey(identyfikatorKlienta)) {
                            networkNode.allocatedZasobs.get(identyfikatorKlienta).put(key, value);
                        } else {
                            networkNode.allocatedZasobs.put(identyfikatorKlienta, new HashMap<>(key, value));
                        }
                        if (networkNode.freeZasoby.get(key) - value > 0) {
                            networkNode.freeZasoby.put(key, networkNode.freeZasoby.get(key) - value);
                        } else {
                            networkNode.freeZasoby.remove(key);
                        }

                        res.append(key).append(":").append(value).append(":")
                                .append(clientSocket.getLocalAddress().toString().substring(1)).append(":")
                                .append(networkNode.getTcpport())
                                .append("\n");
                    });
                    out.println(NetworkNode.alocated);
                    out.println(res.toString());

                    System.out.println(identyfikatorKlienta + " I_ALLOCATE_THIS_BY_MYSELF");
                    System.out.println(res.toString());
                } else {
                    System.out.println("I_CANNOT_ALLOCATE_BY_MYSELF:I_AM_GOING_TO_NEAR_SERVER");

                    Map<String, Map<Character, Integer>> whatCanBeAllocated = new HashMap<>();
                    whatCanBeAllocated.put("" + clientSocket.getLocalAddress().toString().substring(1) +
                                    ":" + networkNode.getTcpport()
                            , networkNode.freeZasoby);

                    outer:
                    for (int i = 0; i < networkNode.closeServers.size(); i++) {
                        try {
                            Socket socket_to_neig = new Socket();
                            socket_to_neig.connect(new InetSocketAddress(networkNode.closeServers.get(i).split(" ")[3]
                                    , Integer.parseInt(networkNode.closeServers.get(i).split(" ")[5])), 1000);

                            PrintWriter out_to_neig = new PrintWriter(socket_to_neig.getOutputStream(), true);
                            BufferedReader in_to_neig = new BufferedReader(new InputStreamReader(socket_to_neig.getInputStream()));

                            out_to_neig.println(NetworkNode.zasobyRequest + " " + networkNode.getIdentyfikator());
                            String line = in_to_neig.readLine();

                            Pattern p = Pattern.compile("%+");
                            Matcher m = p.matcher(line);
                            line = m.replaceAll("%");

                            if (line.split(" ").length > 2 && line.contains("%")) {
                                Arrays.stream(line.split("%")).forEach(str -> {
                                    Map<Character, Integer> zasobyFromNeigh = new HashMap<>();

                                    Arrays.stream(str.substring(str.split(" ")[0].length() + 1).split(" "))
                                            .forEach(el -> zasobyFromNeigh.put(el.split(":")[0].charAt(0)
                                                    , Integer.parseInt(el.split(":")[1])));

                                    whatCanBeAllocated.put(str.split(" ")[0]
                                            , zasobyFromNeigh);
                                });
                            }
                            socket_to_neig.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Map<Character, Integer> whatToAllocateCOPY = new HashMap<>();
                        whatToAllocate.forEach(whatToAllocateCOPY::put);

                        whatCanBeAllocated.entrySet().forEach(entry -> {
                            entry.getValue().forEach((key, value) -> {
                                if (whatToAllocateCOPY.containsKey(key)) {
                                    if (value >= whatToAllocateCOPY.get(key))
                                        whatToAllocateCOPY.put(key, 0);
                                    else
                                        whatToAllocateCOPY.put(key
                                                , whatToAllocateCOPY.get(key) - value);
                                }
                            });
                        });
                        final boolean[] canBeAllocated = new boolean[]{true};

                        whatToAllocateCOPY.forEach((key, value) -> {
                            if (canBeAllocated[0]) {
                                if (value != 0) {
                                    canBeAllocated[0] = false;
                                }
                            }
                        });

                        if (canBeAllocated[0]) { //we can allocate in available nodes
                            StringBuilder answerToClient = new StringBuilder("");
                            whatCanBeAllocated.entrySet().stream()          //allocate what we can in curNode firstly
                                    .filter(e -> e.getKey().compareTo("" + clientSocket.getLocalAddress().toString().substring(1) +
                                            ":" + networkNode.getTcpport()) == 0)
                                    .forEach(entry -> {
                                        whatToAllocate.forEach((key, value) -> {
                                            if (entry.getValue().containsKey(key)) {
                                                if (entry.getValue().get(key) >= value) {
                                                    answerToClient.append(key).append(":")
                                                            .append(value).append(":")
                                                            .append(clientSocket.getLocalAddress().toString().substring(1)).append(":")
                                                            .append(networkNode.getTcpport());
                                                    entry.getValue().put(key, entry.getValue().get(key) - value);
                                                    networkNode.freeZasoby.put(key, entry.getValue().get(key));
                                                    whatToAllocate.put(key, 0);
                                                } else {
                                                    answerToClient.append(key).append(":")
                                                            .append(entry.getValue().get(key)).append(":")
                                                            .append(clientSocket.getLocalAddress().toString().substring(1)).append(":")
                                                            .append(networkNode.getTcpport());
                                                    whatToAllocate.put(key, value - entry.getValue().get(key));
                                                    entry.getValue().put(key, 0);
                                                    networkNode.freeZasoby.put(key, 0);
                                                }
                                                answerToClient.append("\n");
                                            }
                                        });
                                    });

                            whatCanBeAllocated.entrySet()                   //allocate in another nodes
                                    .stream()
                                    .filter(e -> e.getKey().compareTo("" + clientSocket.getLocalAddress().toString().substring(1) +
                                            ":" + networkNode.getTcpport()) != 0)
                                    .forEach(entry -> {
                                        StringBuilder answerToNode = new StringBuilder("");

                                        whatToAllocate.forEach((key, value) -> {        //filling in answers to all the nodes and to the client
                                            if (value > 0) {
                                                if (entry.getValue().containsKey(key)) {
                                                    String ip = entry.getKey().split(":")[0];
                                                    int port = Integer.parseInt(entry.getKey().split(":")[1]);
                                                    if (entry.getValue().get(key) >= value) {
                                                        answerToClient.append(key).append(":")
                                                                .append(value).append(":")
                                                                .append(ip).append(":")
                                                                .append(port);
                                                        answerToNode.append(key).append(":")
                                                                .append(value).append(" ");
                                                        entry.getValue().put(key, entry.getValue().get(key) - value);
                                                        whatToAllocate.put(key, 0);
                                                    } else {
                                                        answerToClient.append(key).append(":")
                                                                .append(entry.getValue().get(key)).append(":")
                                                                .append(ip).append(":")
                                                                .append(port);
                                                        answerToNode.append(key).append(":")
                                                                .append(entry.getValue().get(key)).append(" ");
                                                        whatToAllocate.put(key, value - entry.getValue().get(key));
                                                        entry.getValue().put(key, 0);
                                                    }
                                                    answerToClient.append("\n");
                                                }
                                            }
                                        });

                                        if (answerToNode.length() > 0) {
                                            try {
                                                Socket socket_to_node = new Socket();
                                                socket_to_node.connect(new InetSocketAddress(entry.getKey().split(":")[0]
                                                        , Integer.parseInt(entry.getKey().split(":")[1])), 1000);

                                                PrintWriter out_to_node = new PrintWriter(socket_to_node.getOutputStream(), true);
                                                BufferedReader in_to_node = new BufferedReader(new InputStreamReader(socket_to_node.getInputStream()));

                                                out_to_node.println(NetworkNode.exactlyInputing);
                                                out_to_node.println(answerToNode.toString());

                                                socket_to_node.close();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                            StringBuilder resAnswerToClient = new StringBuilder("");
                            Arrays.stream(answerToClient.toString().split("\n"))
                                    .filter(str -> str.length() > 2)
                                    .filter(str -> str.charAt(2) != 0)
                                    .forEach(str -> resAnswerToClient.append(str).append("\n"));
                            String res = resAnswerToClient.substring(0, resAnswerToClient.length() - 1);
                            out.println(NetworkNode.alocated);
                            out.println(res);
                            out.close();
                            networkNode.printZasobs();
                            return;
                        }
                    }
                    out.println(NetworkNode.failed);
                    out.close();
                    networkNode.printZasobs();
                }
            }
            in.close();
            out.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
