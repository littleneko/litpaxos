# LitPaxos

## 整体架构
![](http://mmbiz.qpic.cn/mmbiz/UqFrHRLeCAnty2ANWDEaHx5JN1cTN1ogcXXXjZC8qsWRDqfHicrHrVj1tonEBdYdWsQnNdOSLbXeJXkrIRzZYcg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1)

## How To Use
使用LitPaxos只需要完成下面两个步骤：
* 实现至少一个状态机(StateMachine)
* 启动至少3个Paxos节点(Node)
* 通过Node提交请求

下面以一个简单的KV存储为例说明如何使用LitPaxos，完整代码在源码的sample目录下。

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
### 2. 启动Node
```java
    // init options
    Options options = new Options(1);
    options.setPaxosConf(paxosConf);
    options.setNodes(nodes);
    options.setMyNodeID(myNodeID);

    // Add SM
    GroupSMInfo groupSMInfo = new GroupSMInfo(0);
    groupSMInfo.addSM(new KVStateMachine());
    options.addGroupSMInfo(groupSMInfo);

    // Run Node
    Node node = new Node()
    node.runNode(options);
```
其中`paxosConf`是类`org.littleneko.node.PaxosConf`的一个实例，保存了下面三个信息：
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

`nodes`是节点信息`org.littleneko.node.NodeInfo`的List。

### 3. 提交请求
提交请求直接通过`Node`的commit方法：
```java
    node.commit(0, value);
```

提交请求的value为字符串，该字符串最终会作为参数传递到`StateMachine`中的`execute`方法中，用户可以自定义value的格式。

上诉完整代码参见：[https://github.com/littleneko/litpaxos/tree/master/src/main/java/org/littleneko/sample](https://github.com/littleneko/litpaxos/tree/master/src/main/java/org/littleneko/sample)