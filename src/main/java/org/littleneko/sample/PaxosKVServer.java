package org.littleneko.sample;

import com.github.luohaha.connection.Conn;
import com.github.luohaha.param.ServerParam;
import com.github.luohaha.server.LightCommServer;
import com.google.gson.Gson;
import org.littleneko.node.GroupSMInfo;
import org.littleneko.node.Node;
import org.littleneko.node.Options;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PaxosKVServer {
    private Node node = new Node();
    private String confFile;
    private ServerConf serverConf;

    private Set<Conn> conns = new HashSet<>();
    private LightCommServer commServer;

    public PaxosKVServer(String confFile) {
        this.confFile = confFile;
    }

    /**
     *
     */
    public void init() {
        Gson gson = new Gson();
        serverConf = gson.fromJson(FileUtils.readFromFile(confFile), ServerConf.class);

        ServerParam param = new ServerParam(serverConf.getRequestIP(), serverConf.getRequestPort());
        param.setBacklog(128);
        param.setOnAccept(conn -> conns.add(conn));
        param.setOnRead((conn, msg) -> node.commit(0, new String(msg)));
        param.setOnClose(conn -> conns.remove(conn));
        param.setOnReadError((conn, err) -> System.out.println(err.getMessage()));
        param.setOnWriteError((conn, err) -> System.out.println(err.getMessage()));
        param.setOnAcceptError(err -> System.out.println(err.getMessage()));

        commServer = new LightCommServer(param, 4);
    }

    /**
     * start kv server
     */
    public void startServer() {
        // start comm server to recv client request
        try {
            commServer.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Init options
        Options options = new Options(1);
        options.setPaxosConf(serverConf.getPaxosConf());
        options.setNodes(serverConf.getNodes());
        options.setMyNodeID(serverConf.getMyNodeID());

        // add sm
        GroupSMInfo groupSMInfo = new GroupSMInfo(0);
        groupSMInfo.addSM(new KVStateMachine());
        options.addGroupSMInfo(groupSMInfo);

        // Run Node
        node.runNode(options);
    }


    public static void main(String[] args) {
        String confFile = System.getProperty("user.dir") + "/sample_conf/" + "server1.json";
        PaxosKVServer server = new PaxosKVServer(confFile);
        server.init();
        server.startServer();
    }
}
