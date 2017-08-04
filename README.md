# LitPaxos
A light paxos implement
## 说明
这是对Base-Paxos\Multi-Paxos协议的简单实现，以独立基础库的形式给使用者提供了简单易用的接口，使用者可以基于litpaxos实现自定义的分布式服务。

特性：
* 提供简单易用的接口给使用者，使用者可以实现自定义的分布式服务
* 实现了Base-Paxos的完整协议流程，实现了Instance对齐
* 提供分组功能，一个节点上可以添加多个分组（Group），各个分组之间相对独立
* 实现了Multi-Paxos中同一Leader连续提交时，优化到只有accept流程
* 实现了乱序/并行提交


TODO:
1. Acceptor和Prpposer状态日志的持久化，Instance日志的持久化，实现节点崩溃后恢复
2. 乱序提交中使用滑动窗口机制保证Instance乱序到达情况下状态机的顺序输入

## 整体架构
整体的设计参考微信[PhxPaxos](https://github.com/Tencent/phxpaxos)，并做了一些精简。

Acceptor,Proposer,Learner三个角色运行在一台机器的同一个进程中，Learner学习的结果输入到状态机，NetWork部分作为一个相对独立的部分运行。

![](http://mmbiz.qpic.cn/mmbiz/UqFrHRLeCAnty2ANWDEaHx5JN1cTN1ogcXXXjZC8qsWRDqfHicrHrVj1tonEBdYdWsQnNdOSLbXeJXkrIRzZYcg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1)

## How To Use
使用LitPaxos只需要完成下面两个步骤：
* 实现至少一个状态机(StateMachine)
* 实现Paxos服务端
* 实现Paxos客户端

下面以一个简单的KV存储为例说明如何使用LitPaxos，完整代码在[example](https://github.com/littleneko/litpaxos/tree/master/example)目录下。

### 1. 实现一个状态机
状态机需要继承自`org.littleneko.sm.StateMachine`并实现`execute`方法

一个简单的状态机如下所示：
```java
public class KVStateMachine extends StateMachine {
    private Map<String, String> kv = new HashMap<>();

    @Override
    public void execute(String value) {
        Gson gson = new Gson();
        KVMessage kvMessage = gson.fromJson(value, KVMessage.class);
        switch (kvMessage.getTypeEnum()) {
            case PUT:
                kv.put(kvMessage.getKey(), kvMessage.getValue());
                System.out.println("Put: " + kvMessage.getKey() + "-> " + kvMessage.getValue());
                break;
            case DEL:
                kv.remove(kvMessage.getKey());
                System.out.println("Del: " + kvMessage.getKey() + "-> " + kvMessage.getValue());
                break;
            case GET:
                String v = kv.get(kvMessage.getKey());
                System.out.println("Get: " + kvMessage.getKey() + "-> " + v);
                break;
        }
    }
}
```
### 2. 实现KVServer
```java
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
        param.setOnRead((conn, msg) -> {
            // 通过Node的commit方法提交请求
            node.commit(0, new String(msg));
            System.out.println("Recv: " + new String(msg));
        });
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
        String confFile = Paths.get(System.getProperty("user.dir"),"/example/conf/server3.json").toString();
        PaxosKVServer server = new PaxosKVServer(confFile);
        server.init();
        server.startServer();
    }
}
```
其中类`org.littleneko.node.PaxosConf`保存了下面三个信息：
```java
    // 用于Proposer的超时定时器, 毫秒
    @SerializedName("commTimeOut")
    private int commTimeOut;
    // Learner的学习时间间隔，毫秒
    @SerializedName("learnInterval")
    private int learnInterval;
    // 日志持久化存储的位置
    @SerializedName("logFile")
    private String logFile;
```

类`org.littleneko.node.NodeInfo`保存了所有Paxos节点的ID，IP和端口。

### 3. 实现KVClient
KVClient通过TCP Socket向KVServer发送请求：
```java
    public class PaxosKVClient {
    public static void main(String[] args) {
        ClientParam param = new ClientParam();
        param.setOnConnect(conn -> {
            new Thread(() -> {
                KVMessage kvMessage1 = new KVMessage(KVMessage.TypeEnum.PUT, "key1", "value1");
                KVMessage kvMessage2 = new KVMessage(KVMessage.TypeEnum.PUT, "key2", "value2");
                KVMessage kvMessage3 = new KVMessage(KVMessage.TypeEnum.PUT, "key3", "value3");
                try {
                    conn.write(kvMessage1.getJson().getBytes());
                    conn.write(kvMessage2.getJson().getBytes());
                    conn.write(kvMessage3.getJson().getBytes());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }).start();
        });
        param.setOnRead((conn, msg) -> System.out.println("Receive " + new String(msg)));
        param.setOnClose(conn -> System.out.println("Server close!"));


        try {
            LightCommClient client = new LightCommClient(4);
            client.connect("127.0.0.1", 1201, param);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
```

提交请求的value为字符串，该字符串最终会作为参数传递到`StateMachine`中的`execute`方法中，用户可以自定义value的格式。

这里我们定义了类`KVMessage`并序列化为Json作为提交的Value类型。

## 参考资料
1. [Paxos理论介绍(1): 朴素Paxos算法理论推导与证明](https://mp.weixin.qq.com/s?__biz=MzI4NDMyNTU2Mw==&mid=2247483712&idx=1&sn=5da6e0850acc0c2543b198a627ae5836&scene=21#wechat_redirect)
2. [Paxos理论介绍(2): Multi-Paxos与Leader](https://mp.weixin.qq.com/s?__biz=MzI4NDMyNTU2Mw==&mid=2247483798&idx=1&sn=42dd222ae255b13f1f67cd9e6d3f3dc0&scene=21#wechat_redirect)
3. [微信自研生产级paxos类库PhxPaxos实现原理介绍](https://mp.weixin.qq.com/s?__biz=MzI4NDMyNTU2Mw==&mid=2247483695&idx=1&sn=91ea422913fc62579e020e941d1d059e&scene=21#wechat_redirect)
4. [The Part-Time Parliament - Leslie Lamport](http://lamport.azurewebsites.net/pubs/lamport-paxos.pdf)
5. [Paxos Made Simple - Leslie Lamport](http://lamport.azurewebsites.net/pubs/paxos-simple.pdf)
6. [Paxos Made Live – An Engineering Perspective](https://research.google.com/archive/paxos_made_live.html)
